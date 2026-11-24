package com.mikel.kcalma.datos

import com.mikel.kcalma.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Incidencias del repositorio de Kcalma: informes de fallos, propuestas y alimentos compartidos.
 * Usa el token de la compilación, que solo puede crear incidencias en este repositorio.
 */
object GitHub {
    val hayToken get() = BuildConfig.GITHUB_INFORMES.isNotEmpty()

    /** Crea una incidencia con [etiqueta]. Devuelve true si GitHub la aceptó. */
    suspend fun incidencia(titulo: String, cuerpo: String, etiqueta: String): Boolean = hayToken && withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://api.github.com/repos/${Actualizador.REPOSITORIO}/issues").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 15000; c.readTimeout = 20000
            c.doOutput = true
            c.setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_INFORMES}")
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val json = JSONObject().put("title", titulo.take(200)).put("body", cuerpo.takeLast(60000)).put("labels", JSONArray().put(etiqueta))
            c.outputStream.use { it.write(json.toString().toByteArray()) }
            val codigo = c.responseCode
            c.disconnect()
            if (codigo != 201) Registro.anotar("GitHub ($etiqueta) no enviado: HTTP $codigo")
            codigo == 201
        }.getOrElse { Registro.error("GitHub ($etiqueta)", it); false }
    }
}
