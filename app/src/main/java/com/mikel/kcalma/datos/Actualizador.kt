package com.mikel.kcalma.datos

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mikel.kcalma.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Versión publicada en GitHub Releases. */
data class VersionNueva(val nombre: String, val notas: String, val urlApk: String)

/**
 * Busca versiones nuevas en las Releases del repositorio público (al abrir la app),
 * descarga la APK y abre el instalador de Android.
 */
class Actualizador(private val contexto: Context) {
    private val prefs = contexto.getSharedPreferences("actualizador", Context.MODE_PRIVATE)

    /**
     * Versión nueva sin ocultar, o null. Pregunta a GitHub cada vez que se abre la app (como mucho cada 10 min);
     * sin conexión, usa la última respuesta guardada. [forzar] pregunta siempre y no tiene en cuenta las ocultas.
     */
    suspend fun comprobar(forzar: Boolean = false): VersionNueva? = withContext(Dispatchers.IO) {
        val ahora = System.currentTimeMillis()
        if (forzar || ahora - prefs.getLong("ultima", 0) > DIEZ_MIN) {
            runCatching { consultarGitHub() }.onSuccess { v ->
                prefs.edit().putLong("ultima", ahora)
                    .putString("nombre", v?.nombre).putString("notas", v?.notas).putString("url", v?.urlApk).apply()
            }
        }
        val nombre = prefs.getString("nombre", null) ?: return@withContext null
        val url = prefs.getString("url", null) ?: return@withContext null
        if (!esMasNueva(nombre, BuildConfig.VERSION_NAME) || (!forzar && prefs.getString("oculta", null) == nombre)) return@withContext null
        VersionNueva(nombre, prefs.getString("notas", "") ?: "", url)
    }

    fun ocultar(v: VersionNueva) = prefs.edit().putString("oculta", v.nombre).apply()

    private fun consultarGitHub(): VersionNueva? {
        val c = URL("https://api.github.com/repos/$REPOSITORIO/releases/latest").openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 6000; c.readTimeout = 8000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "Kcalma/${BuildConfig.VERSION_NAME} (Android)")
            if (c.responseCode == 404) return null // aún no hay ninguna versión publicada
            val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") } ?: return null
            // Primera línea del texto de la versión, para la tarjeta
            val notas = json.optString("body").lineSequence().map { it.trim().trimStart('-', '*', '#', ' ') }
                .firstOrNull { it.isNotBlank() } ?: ""
            return VersionNueva(json.getString("tag_name").removePrefix("v"), notas, apk.getString("browser_download_url"))
        } finally { c.disconnect() }
    }

    /** Descarga la APK informando del progreso (0..1) y abre el instalador. */
    suspend fun instalar(v: VersionNueva, progreso: (Float) -> Unit) {
        val archivo = withContext(Dispatchers.IO) {
            val carpeta = File(contexto.cacheDir, "actualizacion").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
            val destino = File(carpeta, "kcalma-${v.nombre}.apk")
            val c = URL(v.urlApk).openConnection() as HttpURLConnection
            try {
                c.instanceFollowRedirects = true
                c.connectTimeout = 10000; c.readTimeout = 20000
                val total = c.contentLengthLong.takeIf { it > 0 }
                c.inputStream.use { entrada ->
                    destino.outputStream().use { salida ->
                        val buffer = ByteArray(64 * 1024)
                        var leido = 0L
                        while (true) {
                            val n = entrada.read(buffer)
                            if (n < 0) break
                            salida.write(buffer, 0, n)
                            leido += n
                            if (total != null) withContext(Dispatchers.Main) { progreso(leido.toFloat() / total) }
                        }
                    }
                }
            } finally { c.disconnect() }
            destino
        }
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
        contexto.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    companion object {
        const val REPOSITORIO = "mikedesigns101-cell/kcalma"
        private const val DIEZ_MIN = 10 * 60 * 1000L

        /** "0.10.0" > "0.9.2". */
        fun esMasNueva(a: String, b: String): Boolean {
            val x = a.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
            val y = b.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(x.size, y.size)) {
                val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
                if (d != 0) return d > 0
            }
            return false
        }
    }
}
