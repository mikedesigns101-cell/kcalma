package com.mikel.kcalma.datos

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.mikel.kcalma.avisos.ReceptorAvisos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Podómetro del móvil. El sensor de pasos del teléfono lleva él solo la cuenta desde que se encendió; aquí solo se
 * lee ese contador de vez en cuando (al abrir la app, con cada recordatorio y cada hora) y se apunta la diferencia.
 * No hay ningún servicio en segundo plano, así que casi no gasta batería. Se enciende y se apaga desde Perfil.
 */
class Podometro(private val contexto: Context) {
    private val prefs = contexto.getSharedPreferences("podometro", Context.MODE_PRIVATE)
    private val sensores get() = contexto.getSystemService(SensorManager::class.java)

    val disponible: Boolean get() = sensores?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    val activado: Boolean get() = prefs.getBoolean("activado", false)

    /** Desde Android 10 leer el sensor de pasos necesita el permiso de actividad física. */
    fun conPermiso(): Boolean = Build.VERSION.SDK_INT < 29 ||
        contexto.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun activar() {
        // Sin referencia: la primera lectura solo marca desde dónde se empieza a contar
        prefs.edit().putBoolean("activado", true).putLong("ultimo", -1).putString("ultimaFecha", LocalDate.now().toString()).apply()
        programar(contexto, true)
    }

    fun desactivar() {
        prefs.edit().putBoolean("activado", false).apply()
        programar(contexto, false)
    }

    fun pasosPorDia(): Map<String, Long> = prefs.getString("dias", "").orEmpty().split(';').mapNotNull { t ->
        val (f, p) = t.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        p.toLongOrNull()?.let { f to it }
    }.toMap()

    fun pasosHoy(): Long = pasosPorDia()[LocalDate.now().toString()] ?: 0L

    /** Lee el contador y apunta los pasos nuevos. No hace nada si está apagado, sin permiso o sin sensor. */
    suspend fun leer() = withContext(Dispatchers.IO) { leerAhora() }

    /** Lo mismo, para llamarlo desde un hilo que no es el de la pantalla (recordatorios, arranque del móvil). */
    fun leerAhora() {
        if (!activado || !conPermiso()) return
        val sensor = sensores?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val hilo = HandlerThread("podometro").apply { start() }
        val espera = CountDownLatch(1)
        var valor = -1L
        val oyente = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { valor = e.values[0].toLong(); espera.countDown() }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        runCatching {
            sensores?.registerListener(oyente, sensor, SensorManager.SENSOR_DELAY_NORMAL, Handler(hilo.looper))
            espera.await(2500, TimeUnit.MILLISECONDS)
        }.onFailure { Registro.error("Podómetro", it) }
        sensores?.unregisterListener(oyente)
        hilo.quitSafely()
        if (valor >= 0) synchronized(Podometro::class.java) {
            val (dias, ultimo) = repartir(pasosPorDia(), prefs.getLong("ultimo", -1), prefs.getString("ultimaFecha", null), valor, LocalDate.now())
            prefs.edit().putLong("ultimo", ultimo).putString("ultimaFecha", LocalDate.now().toString())
                .putString("dias", dias.entries.sortedBy { it.key }.takeLast(DIAS_GUARDADOS).joinToString(";") { "${it.key}=${it.value}" }).apply()
        }
    }

    companion object {
        private const val DIAS_GUARDADOS = 70

        /**
         * Reparte los pasos dados desde la última lectura. [contador] es lo que marca el sensor ahora (cuenta desde que se
         * encendió el móvil; si es menor que [ultimo], es que se ha reiniciado). Si la última lectura fue otro día, los
         * pasos se reparten a partes iguales entre esos días: no se sabe cuándo se dieron. Devuelve los días y el contador.
         */
        fun repartir(dias: Map<String, Long>, ultimo: Long, ultimaFecha: String?, contador: Long, hoy: LocalDate): Pair<Map<String, Long>, Long> {
            if (ultimo < 0) return dias to contador
            val nuevos = (if (contador >= ultimo) contador - ultimo else contador).coerceAtMost(MAX_POR_DIA * 7)
            if (nuevos <= 0) return dias to contador
            val desde = ultimaFecha?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.takeIf { !it.isAfter(hoy) } ?: hoy
            val n = (ChronoUnit.DAYS.between(desde, hoy).toInt() + 1).coerceIn(1, 7)
            val salida = dias.toMutableMap()
            for (k in 0 until n) {
                val f = hoy.minusDays(k.toLong()).toString()
                // El resto de la división va al día de hoy
                val parte = nuevos / n + if (k == 0) nuevos % n else 0
                salida[f] = ((salida[f] ?: 0L) + parte).coerceAtMost(MAX_POR_DIA)
            }
            return salida to contador
        }

        private const val MAX_POR_DIA = 100_000L

        /** Lectura cada hora, sin despertar el móvil: si está dormido, se hace cuando despierte. */
        fun programar(c: Context, encender: Boolean) {
            val am = c.getSystemService(AlarmManager::class.java) ?: return
            val pi = PendingIntent.getBroadcast(
                c, 9_001, Intent(c, ReceptorAvisos::class.java).putExtra("clave", CLAVE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            am.cancel(pi)
            if (encender) am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pi)
        }

        const val CLAVE = "podometro"
    }
}
