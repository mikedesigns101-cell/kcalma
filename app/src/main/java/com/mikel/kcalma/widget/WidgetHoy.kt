package com.mikel.kcalma.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.mikel.kcalma.MainActivity
import com.mikel.kcalma.R
import com.mikel.kcalma.datos.AlmacenPerfil
import com.mikel.kcalma.datos.Diario
import com.mikel.kcalma.datos.Registro
import com.mikel.kcalma.dominio.Calculadora
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.ui.theme.TemaRaideo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

/** Widget 4×2: lo que queda hoy, foto y añadir. Se refresca al apuntar algo y cada 30 min (cambio de día). */
class WidgetHoy : AppWidgetProvider() {
    override fun onUpdate(contexto: Context, gestor: AppWidgetManager, ids: IntArray) = refrescar(contexto)

    companion object {
        /** Ejercicio extra del día (lo calcula la app con Health Connect), para que el widget cuadre con Hoy. */
        fun guardarEjercicio(contexto: Context, kcal: Int) {
            contexto.getSharedPreferences("widget", Context.MODE_PRIVATE).edit()
                .putString("ejercicio_fecha", LocalDate.now().toString()).putInt("ejercicio", kcal).apply()
        }

        fun refrescar(contexto: Context) {
            val app = contexto.applicationContext
            val gestor = AppWidgetManager.getInstance(app)
            val ids = runCatching { gestor.getAppWidgetIds(ComponentName(app, WidgetHoy::class.java)) }.getOrNull()
            if (ids == null || ids.isEmpty()) return
            Thread {
                runCatching { gestor.updateAppWidget(ids, vistas(app)) }.onFailure { Registro.error("Widget", it) }
            }.start()
        }

        private fun vistas(contexto: Context): RemoteViews {
            val v = RemoteViews(contexto.packageName, R.layout.widget_hoy)
            val perfil = runBlocking { AlmacenPerfil(contexto).perfil.first() }
            val hoy = LocalDate.now().toString()
            if (perfil != null) {
                val plan = Calculadora.calcular(perfil)
                val comido = Diario(contexto).let { d -> try { runBlocking { d.entradasDe(hoy).first() } } finally { d.close() } }.fold(Nutrientes()) { s, e -> s + e.nutrientes }
                val prefs = contexto.getSharedPreferences("widget", Context.MODE_PRIVATE)
                val extra = if (prefs.getString("ejercicio_fecha", null) == hoy) prefs.getInt("ejercicio", 0) else 0
                val objetivo = plan.kcal + extra
                val queda = (objetivo - comido.kcal).roundToInt()
                v.setImageViewBitmap(R.id.widget_anillo, anillo(contexto, (comido.kcal / objetivo).toFloat(), queda))
                v.setTextViewText(R.id.widget_titulo, if (queda >= 0) "Te quedan hoy" else "Te has pasado hoy")
                v.setTextViewText(
                    R.id.widget_macros,
                    "P ${(plan.proteinaG - comido.proteinas).coerceAtLeast(0.0).roundToInt()} · " +
                        "H ${(plan.hidratosG - comido.hidratos).coerceAtLeast(0.0).roundToInt()} · " +
                        "G ${(plan.grasasG - comido.grasas).coerceAtLeast(0.0).roundToInt()} g",
                )
            }
            v.setOnClickPendingIntent(R.id.widget_raiz, abrir(contexto, null, 0))
            v.setOnClickPendingIntent(R.id.widget_anillo, abrir(contexto, null, 0))
            v.setOnClickPendingIntent(R.id.widget_foto, abrir(contexto, "foto", 1))
            v.setOnClickPendingIntent(R.id.widget_anadir, abrir(contexto, "anadir:" + ComidaDia.segunHora(LocalTime.now().hour).name, 2))
            return v
        }

        /** Abre la app y, con [accion], la pantalla correspondiente (ver MainActivity). */
        fun abrir(contexto: Context, accion: String?, codigo: Int): PendingIntent {
            val i = Intent(contexto, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (accion != null) i.putExtra(MainActivity.ACCION, accion)
            return PendingIntent.getActivity(contexto, codigo, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun anillo(contexto: Context, progreso: Float, queda: Int): Bitmap {
            val lado = 240
            val b = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
            val lienzo = Canvas(b)
            val grosor = 22f
            val caja = RectF(grosor, grosor, lado - grosor, lado - grosor)
            val c = TemaRaideo
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = grosor; strokeCap = Paint.Cap.ROUND }
            p.color = c.fondo3.toArgb()
            lienzo.drawArc(caja, 0f, 360f, false, p)
            val prog = progreso.coerceIn(0f, 1f)
            if (prog > 0f) {
                p.shader = SweepGradient(lado / 2f, lado / 2f, intArrayOf(c.acento.toArgb(), c.acento2.toArgb(), c.acento.toArgb()), floatArrayOf(0f, prog.coerceAtLeast(.01f), 1f))
                lienzo.save()
                lienzo.rotate(-90f, lado / 2f, lado / 2f)
                lienzo.drawArc(caja, 0f, 360f * prog, false, p)
                lienzo.restore()
            }
            val fuente = runCatching { ResourcesCompat.getFont(contexto, R.font.outfit) }.getOrNull()
            val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (if (queda < 0) c.rojo else c.texto).toArgb(); textAlign = Paint.Align.CENTER; textSize = 58f
                typeface = fuente; isFakeBoldText = fuente == null
            }
            lienzo.drawText("%,d".format(Locale.US, queda).replace(',', '.'), lado / 2f, lado / 2f + 14f, texto)
            texto.apply { textSize = 28f; color = c.texto3.toArgb(); isFakeBoldText = false }
            lienzo.drawText("kcal", lado / 2f, lado / 2f + 50f, texto)
            return b
        }
    }
}
