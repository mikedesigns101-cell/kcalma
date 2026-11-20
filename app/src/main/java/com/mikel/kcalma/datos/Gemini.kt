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
    /**
     * Modelos en orden (medido el 20/11/2026 con 5 fotos reales y clave gratuita): Flash Lite reconoce igual
     * que 3.5 Flash en 2-3 s; 3.5 Flash tarda 6-30 s y a veces se agota; el alias Flash suele dar 503.
     * Los dos últimos quedan solo de reserva. gemini-2.5-flash ya no se ofrece a claves nuevas.
     */
    private val MODELOS = listOf<Pair<String, JSONObject?>>(
        "gemini-flash-lite-latest" to null,
        "gemini-3.5-flash" to JSONObject().put("thinkingLevel", "low"),
        "gemini-flash-latest" to JSONObject().put("thinkingLevel", "low"),
    )

    private val INSTRUCCIONES = """
        Eres un nutricionista español experto en estimar raciones a partir de fotos. Identifica cada alimento visible
        en el plato y estima su peso en gramos tal como se va a comer (cocinado si está cocinado).
        Cómo estimar los gramos:
        - Usa referencias: un plato llano mide unos 26 cm, uno de postre 20 cm, un tenedor 19 cm, una cuchara sopera 15 ml.
        - Cuenta las piezas cuando se pueda (3 patatas medianas cocidas ≈ 450 g; 1 huevo ≈ 55 g; 1 filete de pollo ≈ 150 g).
        - Fíjate en la altura del montón, no solo en la superficie que ocupa.
        - Separa los ingredientes que se distinguen (arroz, pollo, verdura) y agrupa lo que es un plato único (lentejas
          estofadas, paella, tortilla de patatas).
        Nota del usuario: si menciona algo que no se ve (aceite, salsa, azúcar…), añádelo aparte con de_la_nota = true;
        si corrige algo (qué es o cuánto hay), hazle caso por encima de lo que veas. No inventes aceite ni salsas que
        no se vean ni se mencionen.
        Campos:
        - nombre: nombre corto en español de España, como en una tabla de alimentos ("Patata cocida", "Arroz blanco cocido").
        - busqueda: el alimento base en 1-2 palabras en singular, sin marcas ni acompañamientos ("patata cocida", "pollo pechuga", "paella", "merluza", "verdura").
        - confianza: "alta" si lo ves claro, "media" si dudas del alimento o la cantidad, "baja" si casi adivinas.
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

    suspend fun analizar(clave: String, jpeg: ByteArray, nota: String): Respuesta<List<Reconocido>> {
        val contenido = JSONArray().put(JSONObject().put("parts", JSONArray()
            .put(JSONObject().put("text", if (nota.isBlank()) "Sin nota." else "Nota del usuario: $nota"))
            .put(JSONObject().put("inline_data", JSONObject()
                .put("mime_type", "image/jpeg").put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP))))))
        return withContext(Dispatchers.IO) {
            cadena(MODELOS, clave, contenido, INSTRUCCIONES, ESQUEMA, 0.2, ::interpretar) { l ->
                l.joinToString { "${it.nombre} ${it.gramos.toInt()} g (${it.confianza})" }
            }
        }
    }

    /** Prueba los modelos en orden hasta que uno responda algo legible. */
    private fun <T> cadena(
        modelos: List<Pair<String, JSONObject?>>, clave: String, contenido: JSONArray,
        instrucciones: String, esquema: JSONObject, temperatura: Double,
        leer: (JSONObject) -> T, resumir: (T) -> String,
    ): Respuesta<T> {
        var ultimo: Respuesta<T> = Respuesta.Fallo("sin modelo")
        for ((modelo, razonamiento) in modelos) {
            val cuerpo = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instrucciones))))
                .put("contents", contenido)
                .put("generationConfig", JSONObject()
                    .put("temperature", temperatura)
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", esquema)
                    .apply { if (razonamiento != null) put("thinkingConfig", razonamiento) })
            val inicio = System.currentTimeMillis()
            val r = llamar("$BASE/models/$modelo:generateContent", clave, cuerpo.toString())
            ultimo = when (r) {
                is Respuesta.Ok -> runCatching { Respuesta.Ok(leer(r.valor)) }.getOrElse {
                    Registro.anotar("Gemini $modelo: respuesta ilegible: ${it.message} · ${r.valor.toString().take(600)}")
                    Respuesta.Fallo("respuesta ilegible")
                }
                Respuesta.ClaveMala -> Respuesta.ClaveMala
                Respuesta.Limite -> Respuesta.Limite
                Respuesta.SinConexion -> Respuesta.SinConexion
                is Respuesta.Fallo -> r
            }
            val detalle = (ultimo as? Respuesta.Ok)?.valor?.let(resumir) ?: resumen(ultimo)
            Registro.anotar("Gemini $modelo en ${System.currentTimeMillis() - inicio} ms: $detalle")
            // Saturado, sin cuota, retirado, tiempo agotado…: se prueba el siguiente. Clave mala o sin red: no
            if (ultimo !is Respuesta.Fallo && ultimo !is Respuesta.Limite) break
        }
        return ultimo
    }

    // ---------- Ideas de comida ----------

    private val INSTRUCCIONES_IDEAS = """
        Eres un nutricionista español. Propón recetas caseras españolas, sencillas y realistas, que encajen en las
        calorías y la proteína que se piden para esa comida (±10 %). Respeta siempre las alergias y lo que no le gusta.
        Usa ingredientes de supermercado español. Gramos en crudo, tal como se pesan al cocinar.
        Campos de cada ingrediente: nombre corto en español de España; busqueda (alimento base en 1-2 palabras en singular,
        sin marcas); gramos; kcal_100g, proteinas_100g, hidratos_100g, grasas_100g típicos de ese ingrediente en crudo.
        pasos: 2-3 frases cortas. emoji: uno que represente el plato. alergenos: de la lista de 14 de la UE que contenga.
    """.trimIndent()

    private val ESQUEMA_IDEAS = JSONObject(
        """
        {"type":"OBJECT","properties":{"recetas":{"type":"ARRAY","items":{"type":"OBJECT","properties":{
          "nombre":{"type":"STRING"},"emoji":{"type":"STRING"},"minutos":{"type":"INTEGER"},
          "ingredientes":{"type":"ARRAY","items":{"type":"OBJECT","properties":{
            "nombre":{"type":"STRING"},"busqueda":{"type":"STRING"},"gramos":{"type":"NUMBER"},
            "kcal_100g":{"type":"NUMBER"},"proteinas_100g":{"type":"NUMBER"},"hidratos_100g":{"type":"NUMBER"},"grasas_100g":{"type":"NUMBER"}},
            "required":["nombre","busqueda","gramos","kcal_100g","proteinas_100g","hidratos_100g","grasas_100g"]}},
          "pasos":{"type":"ARRAY","items":{"type":"STRING"}},"alergenos":{"type":"ARRAY","items":{"type":"STRING"}}},
          "required":["nombre","emoji","minutos","ingredientes","pasos","alergenos"]}}},"required":["recetas"]}
        """
    )

    /** Recetas propuestas por la IA, sin emparejar todavía con la base. */
    suspend fun ideas(clave: String, peticion: String): Respuesta<List<JSONObject>> = withContext(Dispatchers.IO) {
        val contenido = JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", peticion))))
        cadena(MODELOS, clave, contenido, INSTRUCCIONES_IDEAS, ESQUEMA_IDEAS, 0.9, { r ->
            val lista = JSONObject(texto(r)).getJSONArray("recetas")
            (0 until lista.length()).map { lista.getJSONObject(it) }
        }) { l -> l.joinToString { it.optString("nombre") } }
    }

    /** Junta el texto de todas las partes que no son razonamiento y quita un posible ```json. */
    private fun texto(respuesta: JSONObject): String {
        val partes = respuesta.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        return (0 until partes.length()).map { partes.getJSONObject(it) }
            .filter { !it.optBoolean("thought") && it.has("text") }.joinToString("") { it.getString("text") }
            .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    }

    internal fun interpretar(respuesta: JSONObject): List<Reconocido> {
        val partes = respuesta.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        val texto = (0 until partes.length()).map { partes.getJSONObject(it) }
            .filter { !it.optBoolean("thought") && it.has("text") }.joinToString("") { it.getString("text") }
            .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
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

    private fun resumen(r: Respuesta<*>) = when (r) {
        is Respuesta.Ok -> "OK"
        is Respuesta.Fallo -> "fallo ${r.detalle}"
        else -> r.toString()
    }

    private fun llamar(url: String, clave: String, cuerpo: String?): Respuesta<JSONObject> {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 10000
            c.readTimeout = 30000
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
                    else Respuesta.Fallo("400 " + mensajeError(error))
                }
                429 -> Respuesta.Limite
                else -> Respuesta.Fallo("$codigo " + mensajeError(c.errorStream?.bufferedReader()?.use { it.readText() } ?: ""))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Respuesta.Fallo("tiempo agotado")
        } catch (e: java.io.IOException) {
            Respuesta.SinConexion
        } finally {
            c.disconnect()
        }
    }

    private fun mensajeError(cuerpo: String): String =
        runCatching { JSONObject(cuerpo).getJSONObject("error").getString("message") }.getOrDefault(cuerpo).take(200)
}
