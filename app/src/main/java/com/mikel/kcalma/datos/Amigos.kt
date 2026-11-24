package com.mikel.kcalma.datos

import android.content.Context
import com.mikel.kcalma.BuildConfig
import com.mikel.kcalma.dominio.CifradoGrupo
import com.mikel.kcalma.dominio.Competicion.Marca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom

/**
 * Grupo de amigos del usuario: su código, su apodo y las cifras del mes de los demás.
 * Sin servidor: cada móvil deja sus cifras cifradas como comentario en una incidencia del repositorio, un robot las
 * guarda en comunidad/grupos/<id>.json y todos bajan ese archivo. Ver comunidad/grupos.py.
 */
class Amigos(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("amigos", Context.MODE_PRIVATE)

    /** Código normalizado del grupo, o null si no se está en ninguno. */
    val codigo: String? get() = prefs.getString("codigo", null)
    val apodo: String get() = prefs.getString("apodo", "") ?: ""

    /** Identificador al azar de este móvil dentro de los grupos; no dice nada del usuario. */
    val miembro: String
        get() = prefs.getString("miembro", null) ?: ByteArray(4).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
            .also { prefs.edit().putString("miembro", it).apply() }

    fun crear(apodo: String): String {
        val nuevo = CifradoGrupo.normalizar(CifradoGrupo.codigoNuevo())!!
        entrar(nuevo, apodo)
        return nuevo
    }

    /** false si el código no tiene la forma de un código de Kcalma. */
    fun unirse(codigo: String, apodo: String): Boolean {
        entrar(CifradoGrupo.normalizar(codigo) ?: return false, apodo)
        return true
    }

    private fun entrar(normalizado: String, apodo: String) {
        prefs.edit().putString("codigo", normalizado).putString("apodo", apodo.trim().take(20)).remove("publicado").remove("cache").remove("cacheMs").apply()
    }

    fun salir() { prefs.edit().remove("codigo").remove("publicado").remove("cache").remove("cacheMs").apply() }

    private fun json(m: Marca) = JSONObject().put("v", 1).put("m", m.miembro).put("a", m.apodo).put("mes", m.mes)
        .put("c", m.cercania).put("d", m.diasApuntados).put("e", m.entrenos).put("r", m.racha).put("p", m.pasos).toString()

    private fun marcaDe(texto: String, miembro: String, mes: String): Marca? = runCatching {
        val o = JSONObject(texto)
        // El miembro y el mes de dentro tienen que ser los del sitio donde estaba guardado: nadie puede hacerse pasar por otro
        require(o.getString("m") == miembro && o.getString("mes") == mes)
        Marca(
            miembro, o.getString("a").trim().take(20).ifBlank { "Sin nombre" }, mes,
            o.getInt("c").coerceIn(0, 100), o.getInt("d").coerceIn(0, 31), o.getInt("e").coerceIn(0, 200),
            o.getInt("r").coerceIn(0, 31), o.getLong("p").coerceIn(0, 3_000_000),
        )
    }.getOrNull()

    /**
     * Publica mis cifras si han cambiado desde la última vez (y como mucho cada [CADA_SUBIDA_MS], salvo [forzar]).
     * Devuelve false solo si había que publicar y no se pudo.
     */
    suspend fun publicar(m: Marca, forzar: Boolean = false): Boolean {
        val normalizado = codigo ?: return true
        val texto = json(m)
        val ahora = System.currentTimeMillis()
        if (texto == prefs.getString("publicado", null)) return true
        if (!forzar && ahora - prefs.getLong("publicadoMs", 0) < CADA_SUBIDA_MS) return true
        val cifrado = withContext(Dispatchers.Default) { CifradoGrupo.cifrar(CifradoGrupo.llave(normalizado), texto) }
        val cuerpo = JSONObject().put("g", CifradoGrupo.id(normalizado)).put("m", m.miembro).put("mes", m.mes).put("d", cifrado).toString()
        val ok = GitHub.comentario(GitHub.INCIDENCIA_GRUPOS, "```json\n$cuerpo\n```")
        if (ok) prefs.edit().putString("publicado", texto).putLong("publicadoMs", ahora).apply()
        return ok
    }

    /**
     * Las cifras del grupo por mes («2026-11» → marcas). Baja el archivo si la copia tiene más de [CADA_BAJADA_MS]
     * o con [forzar]; sin red devuelve la última copia. Lo que no se descifra con el código del grupo se ignora.
     */
    suspend fun descargar(forzar: Boolean = false): Map<String, List<Marca>> = withContext(Dispatchers.IO) {
        val normalizado = codigo ?: return@withContext emptyMap()
        val ahora = System.currentTimeMillis()
        if (forzar || ahora - prefs.getLong("cacheMs", 0) > CADA_BAJADA_MS) runCatching {
            val c = URL("https://raw.githubusercontent.com/${Actualizador.REPOSITORIO}/main/comunidad/grupos/${CifradoGrupo.id(normalizado)}.json").openConnection() as HttpURLConnection
            c.connectTimeout = 10000; c.readTimeout = 15000
            c.setRequestProperty("User-Agent", "Kcalma/${BuildConfig.VERSION_NAME} (Android)")
            when (c.responseCode) {
                200 -> prefs.edit().putString("cache", c.inputStream.bufferedReader().use { it.readText().take(400_000) }).putLong("cacheMs", ahora).apply()
                // Grupo recién creado: todavía no hay archivo
                404 -> prefs.edit().putString("cache", "{}").putLong("cacheMs", ahora).apply()
            }
            c.disconnect()
        }.onFailure { Registro.error("Amigos", it) }
        val llave = CifradoGrupo.llave(normalizado)
        runCatching {
            val o = JSONObject(prefs.getString("cache", null) ?: "{}")
            o.keys().asSequence().associateWith { mes ->
                val delMes = o.getJSONObject(mes)
                delMes.keys().asSequence().toList().take(MAX_MIEMBROS).mapNotNull { miembro ->
                    CifradoGrupo.descifrar(llave, delMes.getJSONObject(miembro).optString("d"))?.let { marcaDe(it, miembro, mes) }
                }
            }
        }.getOrDefault(emptyMap())
    }

    companion object {
        const val CADA_SUBIDA_MS = 30 * 60_000L
        const val CADA_BAJADA_MS = 5 * 60_000L
        const val MAX_MIEMBROS = 30
    }
}
