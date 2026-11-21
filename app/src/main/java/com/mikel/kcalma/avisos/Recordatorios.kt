package com.mikel.kcalma.avisos

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mikel.kcalma.datos.Actualizador
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mikel.kcalma.R
import com.mikel.kcalma.datos.AlmacenPerfil
import com.mikel.kcalma.datos.Diario
import com.mikel.kcalma.datos.Registro
import com.mikel.kcalma.dominio.Calculadora
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.widget.WidgetHoy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** Un recordatorio: una comida (o pesarse) a una hora. */
data class Recordatorio(val clave: String, val emoji: String, val nombre: String, val activo: Boolean, val hora: Int, val minuto: Int) {
    val horaTexto get() = "%d:%02d".format(hora, minuto)
}

/**
 * Recordatorios locales (sin servidor). Solo avisan si a esa hora aún no se ha apuntado esa comida
 * (o no hay pesada hoy). Alarmas inexactas: no necesitan el permiso de alarmas exactas.
 */
object Recordatorios {
    const val PESO = "PESO"
    private const val CANAL = "recordatorios"

    /** Por defecto (horario español): desayuno, comida y cena activos; el resto apagado. Peso: lunes. */
    private val DEFECTO = linkedMapOf(
        ComidaDia.DESAYUNO.name to Triple(true, 10, 30),
        ComidaDia.MEDIA_MANANA.name to Triple(false, 12, 0),
        ComidaDia.COMIDA.name to Triple(true, 16, 0),
        ComidaDia.MERIENDA.name to Triple(false, 19, 0),
        ComidaDia.CENA.name to Triple(true, 22, 30),
        PESO to Triple(true, 9, 0),
    )

    private fun prefs(c: Context) = c.getSharedPreferences("recordatorios", Context.MODE_PRIVATE)

    /** Si el usuario ya ha abierto la pantalla (hasta entonces no se programa nada). */
    fun configurados(c: Context) = prefs(c).getBoolean("configurados", false)

    fun lista(c: Context): List<Recordatorio> = DEFECTO.map { (k, d) ->
        val p = prefs(c)
        val comida = ComidaDia.entries.firstOrNull { it.name == k }
        Recordatorio(
            k, comida?.icono ?: "⚖️", comida?.nombre ?: "Pesarte (lunes)",
            p.getBoolean("$k.activo", d.first), p.getInt("$k.hora", d.second), p.getInt("$k.minuto", d.third),
        )
    }

    fun guardar(c: Context, r: Recordatorio) {
        prefs(c).edit().putBoolean("configurados", true).putBoolean("${r.clave}.activo", r.activo)
            .putInt("${r.clave}.hora", r.hora).putInt("${r.clave}.minuto", r.minuto).apply()
        programar(c, r)
    }

    /** Programa todos (al arrancar el móvil, al actualizar la app o al abrir la pantalla). */
    fun programarTodos(c: Context) {
        if (!configurados(c)) return
        lista(c).forEach { programar(c, it) }
    }

    fun marcarConfigurados(c: Context) {
        if (!configurados(c)) { prefs(c).edit().putBoolean("configurados", true).apply(); programarTodos(c) }
    }

    private fun pendiente(c: Context, clave: String): PendingIntent {
        val i = Intent(c, ReceptorAvisos::class.java).setAction("com.mikel.kcalma.AVISO").putExtra("clave", clave)
        return PendingIntent.getBroadcast(c, clave.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun programar(c: Context, r: Recordatorio) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendiente(c, r.clave)
        am.cancel(pi)
        if (!r.activo) return
        val ahora = LocalDateTime.now()
        var cuando = LocalDate.now().atTime(LocalTime.of(r.hora, r.minuto))
        if (r.clave == PESO) {
            cuando = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(r.hora, r.minuto)
            if (!cuando.isAfter(ahora)) cuando = cuando.plusWeeks(1)
        } else if (!cuando.isAfter(ahora)) cuando = cuando.plusDays(1)
        val ms = cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Ventana de 10 minutos: el sistema puede agruparlo con otras alarmas y ahorrar batería
        am.setWindow(AlarmManager.RTC_WAKEUP, ms, 10 * 60 * 1000L, pi)
    }

    fun permisoNotificaciones(c: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Salta la alarma: avisa solo si hace falta y deja programada la siguiente. */
    internal fun alSaltar(c: Context, clave: String) {
        val r = lista(c).firstOrNull { it.clave == clave } ?: return
        programar(c, r)
        if (!r.activo || !permisoNotificaciones(c)) return
        val hoy = LocalDate.now().toString()
        val perfil = runBlocking { AlmacenPerfil(c).perfil.first() } ?: return
        val (titulo, texto, accion) = if (clave == PESO) {
            val pesado = runBlocking { AlmacenPerfil(c).pesos.first() }.any { it.fecha == hoy }
            if (pesado) return
            Triple("¿Te pesas hoy?", "Una pesada a la semana basta para ajustar tu plan.", "peso")
        } else {
            val comida = ComidaDia.valueOf(clave)
            val d = Diario(c)
            val entradas = try { runBlocking { d.entradasDe(hoy).first() } } finally { d.close() }
            if (entradas.any { it.comida == comida }) return
            val queda = (Calculadora.calcular(perfil).kcal - entradas.sumOf { it.nutrientes.kcal }).roundToInt()
            val pregunta = when (comida) {
                ComidaDia.DESAYUNO -> "¿Qué has desayunado?"
                ComidaDia.COMIDA -> "¿Qué has comido?"
                ComidaDia.CENA -> "¿Qué has cenado?"
                ComidaDia.MERIENDA -> "¿Qué has merendado?"
                ComidaDia.MEDIA_MANANA -> "¿Has tomado algo a media mañana?"
            }
            Triple(pregunta, "Te quedan $queda kcal. Toca para apuntarlo o hacer una foto.", "anadir:${comida.name}")
        }
        crearCanal(c)
        val n = NotificationCompat.Builder(c, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo).setContentText(texto)
            .setContentIntent(WidgetHoy.abrir(c, accion, 100 + clave.hashCode() % 100))
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        runCatching { NotificationManagerCompat.from(c).notify(clave.hashCode(), n) }.onFailure { Registro.error("Notificación", it) }
    }

    /** Tras actualizarse sola la app se cierra: este aviso permite volver a abrirla con un toque. */
    fun avisarActualizada(c: Context) {
        if (!permisoNotificaciones(c)) return
        if (Build.VERSION.SDK_INT >= 26) c.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel("actualizaciones", "Actualizaciones", NotificationManager.IMPORTANCE_HIGH),
        )
        val n = NotificationCompat.Builder(c, "actualizaciones")
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Kcalma ${com.mikel.kcalma.BuildConfig.VERSION_NAME} instalada")
            .setContentText("Toca para abrirla.")
            .setContentIntent(WidgetHoy.abrir(c, null, 300))
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        runCatching { NotificationManagerCompat.from(c).notify(300, n) }.onFailure { Registro.error("Notificación", it) }
    }

    private fun crearCanal(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        c.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CANAL, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos para apuntar las comidas y pesarte"
            },
        )
    }
}

/** Recibe las alarmas de los recordatorios y el arranque del móvil (las alarmas se borran al apagarlo). */
class ReceptorAvisos : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pendiente = goAsync()
        Thread {
            try {
                when (i.action) {
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        if (i.action == Intent.ACTION_MY_PACKAGE_REPLACED && Actualizador.recienActualizada(c)) Recordatorios.avisarActualizada(c)
                        Recordatorios.programarTodos(c)
                        WidgetHoy.refrescar(c)
                    }
                    else -> i.getStringExtra("clave")?.let { Recordatorios.alSaltar(c, it) }
                }
            } catch (e: Exception) {
                Registro.error("Recordatorio", e)
            } finally {
                pendiente.finish()
            }
        }.start()
    }
}
