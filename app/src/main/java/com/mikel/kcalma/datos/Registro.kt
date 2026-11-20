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

    /** Texto completo del informe: versión, móvil y últimas líneas del registro. */
    @Synchronized
    fun informe(descripcion: String): String = buildString {
        appendLine("Kcalma ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Móvil: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine()
        appendLine("Qué ha pasado:")
        appendLine(descripcion.ifBlank { "(sin descripción)" })
        appendLine()
        appendLine("Registro:")
        append(archivo?.takeIf { it.exists() }?.readLines()?.takeLast(120)?.joinToString("\n") ?: "(vacío)")
    }
}
