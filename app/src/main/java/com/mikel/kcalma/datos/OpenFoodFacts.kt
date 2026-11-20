package com.mikel.kcalma.datos

import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.gramosDeCantidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

/** Resultado de buscar un código de barras. */
sealed interface Busqueda {
    data class Encontrado(val alimento: Alimento) : Busqueda
    data object NoExiste : Busqueda
    data object SinConexion : Busqueda
}

/** Consulta un código en Open Food Facts cuando no está en el móvil. Sin clave ni cuenta. */
object OpenFoodFacts {
    private const val CAMPOS = "product_name,product_name_es,brands,quantity,serving_size,serving_quantity,nutriments"

    suspend fun buscar(codigo: String): Busqueda = withContext(Dispatchers.IO) {
        val conexion = URL("https://world.openfoodfacts.org/api/v2/product/$codigo?fields=$CAMPOS")
            .openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 6000
            conexion.readTimeout = 8000
            conexion.setRequestProperty("User-Agent", "Kcalma/0.1 (Android)")
            if (conexion.responseCode == 404) return@withContext Busqueda.NoExiste
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            interpretar(codigo, JSONObject(texto))?.let { Busqueda.Encontrado(it) } ?: Busqueda.NoExiste
        } catch (e: FileNotFoundException) {
            Busqueda.NoExiste
        } catch (e: Exception) {
            Busqueda.SinConexion
        } finally {
            conexion.disconnect()
        }
    }

    internal fun interpretar(codigo: String, json: JSONObject): Alimento? {
        if (json.optInt("status") != 1) return null
        val p = json.optJSONObject("product") ?: return null
        val n = p.optJSONObject("nutriments") ?: return null
        fun num(clave: String) = if (n.has(clave)) n.optDouble(clave).takeUnless { it.isNaN() } else null
        val kcal = num("energy-kcal_100g") ?: num("energy_100g")?.div(4.184) ?: return null
        val nombre = p.optString("product_name_es").ifBlank { p.optString("product_name") }.ifBlank { return null }
        val racionG = p.optString("serving_quantity").toDoubleOrNull()
        return Alimento(
            nombre = nombre.trim(),
            marca = p.optString("brands").substringBefore(',').trim().ifBlank { null },
            codigoBarras = codigo,
            racion = p.optString("serving_size").ifBlank { null },
            racionG = racionG,
            envaseG = gramosDeCantidad(p.optString("quantity")),
            kcal = kcal,
            proteinas = num("proteins_100g") ?: 0.0,
            hidratos = num("carbohydrates_100g") ?: 0.0,
            grasas = num("fat_100g") ?: 0.0,
            azucares = num("sugars_100g"),
            saturadas = num("saturated-fat_100g"),
            fibra = num("fiber_100g"),
            sal = num("salt_100g"),
        )
    }
}
