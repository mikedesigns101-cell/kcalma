package com.mikel.kcalma.datos

import android.content.Context
import com.mikel.kcalma.BuildConfig
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.Compartidos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Alimentos compartidos entre todos los usuarios, sin servidor: el móvil manda cada alimento nuevo como incidencia
 * del repositorio, un robot de GitHub lo valida y lo añade a un archivo, y todas las apps bajan ese archivo una vez al día.
 */
object Comunidad {
    private const val ARCHIVO = "https://raw.githubusercontent.com/${Actualizador.REPOSITORIO}/main/comunidad/alimentos.json"
    private const val CADA_MS = 24 * 3600_000L
    private const val MAX_BYTES = 2_000_000

    /** Manda un alimento recién registrado (con código de barras) para que llegue a los demás. No bloquea ni avisa si falla. */
    suspend fun enviar(a: Alimento) {
        if (!GitHub.hayToken || !Compartidos.valido(a)) return
        val datos = JSONObject().put("nombre", a.nombre.trim()).put("marca", a.marca?.takeIf { it != "Estimado por IA" }).put("codigo", a.codigoBarras)
            .put("kcal", a.kcal).put("proteinas", a.proteinas).put("hidratos", a.hidratos).put("grasas", a.grasas)
            .put("azucares", a.azucares).put("saturadas", a.saturadas).put("fibra", a.fibra).put("sal", a.sal).put("racion_g", a.racionValida)
        val ok = GitHub.incidencia("Alimento: ${a.nombre.take(60)}", "```json\n${datos.toString(1)}\n```\nKcalma ${BuildConfig.VERSION_NAME}", "alimento")
        Registro.anotar("Alimento compartido ${a.codigoBarras}: ${if (ok) "enviado" else "no enviado"}")
    }

    /** Avisa de que un alimento de la comunidad está mal: se retira para todos hasta que se revise, y de este móvil ya. */
    suspend fun denunciar(diario: Diario, a: Alimento, motivo: String): Boolean {
        val codigo = a.codigoBarras ?: return false
        withContext(Dispatchers.IO) { diario.borrarComunidad(codigo) }
        val datos = JSONObject().put("codigo", codigo).put("nombre", a.nombre).put("motivo", motivo.take(300))
        return GitHub.incidencia("Alimento mal: ${a.nombre.take(60)}", "```json\n${datos.toString(1)}\n```", "alimento-mal")
    }

    /** Baja la lista de la comunidad si hace más de un día de la última vez. Lo que no pasa la validación se ignora. */
    suspend fun sincronizar(contexto: Context, diario: Diario, forzar: Boolean = false) = withContext(Dispatchers.IO) {
        val prefs = contexto.getSharedPreferences("comunidad", Context.MODE_PRIVATE)
        if (!forzar && System.currentTimeMillis() - prefs.getLong("ultima", 0) < CADA_MS) return@withContext
        runCatching {
            val c = URL(ARCHIVO).openConnection() as HttpURLConnection
            c.connectTimeout = 10000; c.readTimeout = 15000
            c.setRequestProperty("User-Agent", "Kcalma/${BuildConfig.VERSION_NAME} (Android)")
            if (c.responseCode != 200) { c.disconnect(); return@runCatching }
            val texto = c.inputStream.use { entrada ->
                val salida = java.io.ByteArrayOutputStream()
                val trozo = ByteArray(8192)
                while (salida.size() <= MAX_BYTES) { val n = entrada.read(trozo); if (n < 0) break; salida.write(trozo, 0, n) }
                salida.toByteArray()
            }.also { c.disconnect() }
            if (texto.size > MAX_BYTES) return@runCatching
            val lista = JSONArray(String(texto))
            val alimentos = (0 until minOf(lista.length(), 5000)).mapNotNull { i -> lista.optJSONObject(i)?.let(::alimentoDe) }.filter(Compartidos::valido)
            diario.reemplazarComunidad(alimentos)
            prefs.edit().putLong("ultima", System.currentTimeMillis()).apply()
            Registro.anotar("Comunidad: ${alimentos.size} alimentos")
        }.onFailure { Registro.error("Comunidad", it) }
    }

    private fun alimentoDe(o: JSONObject): Alimento? = runCatching {
        fun d(k: String) = if (o.isNull(k)) null else o.optDouble(k).takeIf { !it.isNaN() }
        Alimento(
            nombre = o.getString("nombre").trim(), marca = o.optString("marca").takeIf { it.isNotBlank() && it != "null" },
            supermercado = Compartidos.ORIGEN, codigoBarras = o.getString("codigo"),
            racion = d("racion_g")?.let { "1 ración" }, racionG = d("racion_g"),
            kcal = o.getDouble("kcal"), proteinas = o.getDouble("proteinas"), hidratos = o.getDouble("hidratos"), grasas = o.getDouble("grasas"),
            azucares = d("azucares"), saturadas = d("saturadas"), fibra = d("fibra"), sal = d("sal"),
        )
    }.getOrNull()
}
