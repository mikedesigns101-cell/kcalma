package com.mikel.kcalma.datos

import android.content.Context
import android.os.Build
import com.mikel.kcalma.BuildConfig
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Registro local de errores y sucesos (sin fotos, claves ni datos del diario) para el botón
 * «Mandar informe». Se queda en el móvil; solo sale si el usuario lo envía.
 */
object Registro {
    private const val MAX_LINEAS = 300
    private var archivo: File? = null
    private val hora = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")

    fun iniciar(contexto: Context) {
        archivo = File(contexto.filesDir, "registro.txt")
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, e ->
            anotar("CIERRE: ${e.stackTraceToString().take(1500)}")
            anterior?.uncaughtException(hilo, e)
        }
    }

    @Synchronized
    fun anotar(texto: String) {
        android.util.Log.i("Kcalma", texto)
        val f = archivo ?: return
        runCatching {
            f.appendText("${LocalDateTime.now().format(hora)}  $texto\n")
            val lineas = f.readLines()
            if (lineas.size > MAX_LINEAS * 2) f.writeText(lineas.takeLast(MAX_LINEAS).joinToString("\n", postfix = "\n"))
        }
    }

    fun error(donde: String, e: Throwable) = anotar("ERROR $donde: ${e.javaClass.simpleName}: ${e.message}")

    /** Minutos de registro que se mandan con un informe. */
    const val MINUTOS = 15

    /** Texto completo del informe: versión, móvil y lo ocurrido en los últimos [MINUTOS] minutos. */
    @Synchronized
    fun informe(descripcion: String): String = buildString {
        appendLine("Kcalma ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Móvil: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine()
        appendLine("Qué ha pasado:")
        appendLine(descripcion.ifBlank { "(sin descripción)" })
        appendLine()
        val lineas = archivo?.takeIf { it.exists() }?.readLines() ?: emptyList()
        val (recientes, anterior) = recientes(lineas, LocalDateTime.now())
        if (anterior != null) { appendLine("Último error anterior:"); appendLine(anterior); appendLine() }
        appendLine("Registro (últimos $MINUTOS minutos):")
        append(recientes.joinToString("\n").ifEmpty { "(sin actividad)" })
    }

    /**
     * Líneas de los últimos [MINUTOS] minutos (como mucho 250) y, aparte, el último error o cierre anterior a ese rato.
     * Las líneas sin hora (trazas de un cierre) van con la línea que las precede.
     */
    internal fun recientes(lineas: List<String>, ahora: LocalDateTime): Pair<List<String>, String?> {
        val desde = ahora.minusMinutes(MINUTOS.toLong())
        fun hora(l: String): LocalDateTime? = runCatching {
            val (d, m) = l.substring(0, 5).split('/').map { it.toInt() }
            val (h, mi, s) = l.substring(6, 14).split(':').map { it.toInt() }
            // El registro no guarda el año: una línea «del futuro» es del año pasado
            LocalDateTime.of(ahora.year, m, d, h, mi, s).let { if (it > ahora.plusDays(1)) it.minusYears(1) else it }
        }.getOrNull()
        val primera = lineas.indexOfFirst { l -> hora(l)?.let { it >= desde } == true }
        val antes = if (primera < 0) lineas else lineas.take(primera)
        val anterior = antes.lastOrNull { it.contains("  ERROR ") || it.contains("  CIERRE") }?.take(600)
        return (if (primera < 0) emptyList() else lineas.drop(primera).takeLast(250)) to anterior
    }
}
