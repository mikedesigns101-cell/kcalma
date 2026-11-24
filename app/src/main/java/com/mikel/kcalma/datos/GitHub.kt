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

    /** Incidencia que hace de buzón de los grupos de amigos: las apps dejan ahí sus cifras cifradas como comentarios. */
    const val INCIDENCIA_GRUPOS = 6

    /** Crea una incidencia con [etiqueta]. Devuelve true si GitHub la aceptó. */
    suspend fun incidencia(titulo: String, cuerpo: String, etiqueta: String): Boolean =
        enviar("issues", JSONObject().put("title", titulo.take(200)).put("body", cuerpo.takeLast(60000)).put("labels", JSONArray().put(etiqueta)), etiqueta)

    /** Deja un comentario en la incidencia [numero]. */
    suspend fun comentario(numero: Int, cuerpo: String): Boolean =
        enviar("issues/$numero/comments", JSONObject().put("body", cuerpo.take(4000)), "comentario")

    private suspend fun enviar(ruta: String, json: JSONObject, etiqueta: String): Boolean = hayToken && withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://api.github.com/repos/${Actualizador.REPOSITORIO}/$ruta").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 15000; c.readTimeout = 20000
            c.doOutput = true
            c.setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_INFORMES}")
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.outputStream.use { it.write(json.toString().toByteArray()) }
            val codigo = c.responseCode
            c.disconnect()
            if (codigo != 201) Registro.anotar("GitHub ($etiqueta) no enviado: HTTP $codigo")
            codigo == 201
        }.getOrElse { Registro.error("GitHub ($etiqueta)", it); false }
    }
}
