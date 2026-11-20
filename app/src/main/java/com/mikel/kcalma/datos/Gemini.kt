package com.mikel.kcalma.datos

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Un alimento reconocido en la foto, con la estimación de la IA por si no está en la base. */
data class Reconocido(
    val nombre: String,
    val busqueda: String,
    val gramos: Double,
    val confianza: String,
    val deLaNota: Boolean,
    val kcal100: Double,
    val proteinas100: Double,
    val hidratos100: Double,
    val grasas100: Double,
)

sealed interface Respuesta<out T> {
    data class Ok<T>(val valor: T) : Respuesta<T>
    data object ClaveMala : Respuesta<Nothing>
    data object Limite : Respuesta<Nothing>
    data object SinConexion : Respuesta<Nothing>
    data class Fallo(val detalle: String) : Respuesta<Nothing>
}

/** Cliente mínimo de la API de Gemini. La IA solo reconoce y estima gramos; la base de datos calcula. */
object Gemini {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
    // El alias apunta siempre al Flash más reciente; si Google lo retira, se prueba el siguiente
    private val MODELOS = listOf("gemini-flash-latest", "gemini-2.5-flash")

    private val INSTRUCCIONES = """
        Eres un nutricionista español. Identifica cada alimento visible en la foto del plato y estima su peso en gramos
        tal como se va a comer (cocinado si está cocinado). Ten en cuenta la nota del usuario: si menciona algo que no
        se ve (aceite, salsa, azúcar…), añádelo como alimento aparte con de_la_nota = true; si corrige algo, hazle caso.
        - nombre: nombre corto en español de España, como en una tabla de alimentos ("Arroz blanco cocido", "Pechuga de pollo a la plancha").
        - busqueda: 1-3 palabras clave para buscarlo en una base de datos española, sin marcas ("arroz cocido", "pollo pechuga").
        - gramos: tu mejor estimación; usa referencias del plato y los cubiertos.
        - confianza: "alta", "media" o "baja".
        - kcal_100g, proteinas_100g, hidratos_100g, grasas_100g: valores típicos por 100 g de ese alimento así preparado.
        Si no hay comida en la foto, devuelve la lista vacía.
    """.trimIndent()

    private val ESQUEMA = JSONObject(
        """
        {"type":"OBJECT","properties":{"alimentos":{"type":"ARRAY","items":{"type":"OBJECT","properties":{
          "nombre":{"type":"STRING"},"busqueda":{"type":"STRING"},"gramos":{"type":"NUMBER"},
          "confianza":{"type":"STRING","enum":["alta","media","baja"]},"de_la_nota":{"type":"BOOLEAN"},
          "kcal_100g":{"type":"NUMBER"},"proteinas_100g":{"type":"NUMBER"},"hidratos_100g":{"type":"NUMBER"},"grasas_100g":{"type":"NUMBER"}},
          "required":["nombre","busqueda","gramos","confianza","de_la_nota","kcal_100g","proteinas_100g","hidratos_100g","grasas_100g"]}}},
         "required":["alimentos"]}
        """
    )

    /** Comprueba la clave con una llamada barata (listar modelos). */
    suspend fun probarClave(clave: String): Respuesta<Unit> = withContext(Dispatchers.IO) {
        llamar("$BASE/models?pageSize=1", clave, null).let { if (it is Respuesta.Ok) Respuesta.Ok(Unit) else it as Respuesta<Unit> }
    }

    suspend fun analizar(clave: String, jpeg: ByteArray, nota: String): Respuesta<List<Reconocido>> = withContext(Dispatchers.IO) {
        val cuerpo = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", INSTRUCCIONES))))
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                .put(JSONObject().put("text", if (nota.isBlank()) "Sin nota." else "Nota del usuario: $nota"))
                .put(JSONObject().put("inline_data", JSONObject()
                    .put("mime_type", "image/jpeg").put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP)))))))
            .put("generationConfig", JSONObject()
                .put("temperature", 0.2)
                .put("responseMimeType", "application/json")
                .put("responseSchema", ESQUEMA))
        var ultimo: Respuesta<JSONObject> = Respuesta.Fallo("sin modelo")
        for (modelo in MODELOS) {
            ultimo = llamar("$BASE/models/$modelo:generateContent", clave, cuerpo.toString())
            if (!(ultimo is Respuesta.Fallo && ultimo.detalle == "404")) break
        }
        when (val r = ultimo) {
            is Respuesta.Ok -> runCatching { Respuesta.Ok(interpretar(r.valor)) }.getOrElse { Respuesta.Fallo("respuesta ilegible") }
            else -> r as Respuesta<List<Reconocido>>
        }
    }

    internal fun interpretar(respuesta: JSONObject): List<Reconocido> {
        val texto = respuesta.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        val lista = JSONObject(texto).getJSONArray("alimentos")
        return (0 until lista.length()).map { lista.getJSONObject(it) }.map { o ->
            Reconocido(
                nombre = o.getString("nombre"),
                busqueda = o.optString("busqueda").ifBlank { o.getString("nombre") },
                gramos = o.optDouble("gramos", 100.0).coerceIn(1.0, 2000.0),
                confianza = o.optString("confianza", "media"),
                deLaNota = o.optBoolean("de_la_nota"),
                kcal100 = o.optDouble("kcal_100g", 0.0),
                proteinas100 = o.optDouble("proteinas_100g", 0.0),
                hidratos100 = o.optDouble("hidratos_100g", 0.0),
                grasas100 = o.optDouble("grasas_100g", 0.0),
            )
        }.filter { it.gramos > 0 }
    }

    private fun llamar(url: String, clave: String, cuerpo: String?): Respuesta<JSONObject> {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 10000
            c.readTimeout = 60000
            c.setRequestProperty("x-goog-api-key", clave) // en cabecera, nunca en la URL
            if (cuerpo != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(cuerpo.toByteArray()) }
            }
            when (val codigo = c.responseCode) {
                200 -> Respuesta.Ok(JSONObject(c.inputStream.bufferedReader().use { it.readText() }))
                400, 401, 403 -> {
                    val error = c.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    if (error.contains("API_KEY_INVALID") || error.contains("API key not valid") || codigo != 400) Respuesta.ClaveMala
                    else Respuesta.Fallo("400")
                }
                429 -> Respuesta.Limite
                else -> Respuesta.Fallo(codigo.toString())
            }
        } catch (e: java.io.IOException) {
            Respuesta.SinConexion
        } finally {
            c.disconnect()
        }
    }
}
