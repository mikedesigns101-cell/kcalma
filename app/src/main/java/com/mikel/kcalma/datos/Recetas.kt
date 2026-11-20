package com.mikel.kcalma.datos

import android.content.Context
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Gustos
import com.mikel.kcalma.dominio.Ingrediente
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.dominio.Receta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** Recetas españolas de la app (assets/recetas.json) y las que propone la IA. */
object Recetas {
    private var cache: List<Receta>? = null

    private val COMIDAS = mapOf(
        'D' to ComidaDia.DESAYUNO, 'M' to ComidaDia.MEDIA_MANANA, 'C' to ComidaDia.COMIDA,
        'R' to ComidaDia.MERIENDA, 'N' to ComidaDia.CENA,
    )

    suspend fun todas(contexto: Context): List<Receta> = cache ?: withContext(Dispatchers.IO) {
        val texto = contexto.assets.open("recetas.json").use { it.readBytes().toString(Charsets.UTF_8) }
        val lista = JSONArray(texto)
        (0 until lista.length()).map { leer(lista.getJSONObject(it)) }.also { cache = it }
    }

    private fun leer(o: JSONObject): Receta {
        val ing = o.getJSONArray("ing")
        return Receta(
            nombre = o.getString("nombre"),
            emoji = o.optString("emoji", "🍽️"),
            comidas = o.getString("comidas").mapNotNull { COMIDAS[it] }.toSet(),
            minutos = o.optInt("min", 20),
            ingredientes = (0 until ing.length()).map { i ->
                val x = ing.getJSONObject(i)
                Ingrediente(
                    Alimento(
                        nombre = x.getString("n"), generico = true,
                        kcal = x.getDouble("k"), proteinas = x.getDouble("p"), hidratos = x.getDouble("h"), grasas = x.getDouble("gr"),
                    ),
                    x.getDouble("g"),
                )
            },
            pasos = textos(o.getJSONArray("pasos")),
            alergenos = textos(o.getJSONArray("alergenos")).toSet(),
        )
    }

    private fun textos(a: JSONArray) = (0 until a.length()).map { a.getString(it) }

    /** Lo que se le pide a Gemini: comida, kcal y proteína objetivo, alergias y gustos. */
    fun peticion(perfil: Perfil, comida: ComidaDia, kcal: Double, proteina: Double, rapidas: Boolean): String = buildString {
        append("Propón 3 recetas distintas para ${comida.nombre.lowercase()} de unas ${kcal.roundToInt()} kcal ")
        append("y unos ${proteina.roundToInt()} g de proteína cada una.")
        if (rapidas) append(" Que se hagan en 15 minutos o menos.")
        if (perfil.alergias.isNotEmpty()) append(" Alergias e intolerancias (prohibido): ${perfil.alergias.joinToString()}.")
        if (perfil.noMeGusta.isNotEmpty()) append(" No le gusta, no uses nada de esto: ${Gustos.describir(perfil.noMeGusta)}.")
        if (perfil.meEncanta.isNotEmpty()) append(" Le encanta: ${perfil.meEncanta.joinToString()}.")
    }

    /**
     * Convierte las recetas de la IA: cada ingrediente se empareja con la base (que es la que calcula);
     * si no hay uno fiable, se usan los valores de la IA marcados como estimados.
     */
    suspend fun deIa(repo: Repositorio, comida: ComidaDia, recetas: List<JSONObject>): List<Receta> = recetas.mapNotNull { o ->
        runCatching {
            val ing = o.getJSONArray("ingredientes")
            Receta(
                nombre = o.getString("nombre"),
                emoji = o.optString("emoji", "✨"),
                comidas = setOf(comida),
                minutos = o.optInt("minutos", 20),
                ingredientes = (0 until ing.length()).map { i ->
                    val x = ing.getJSONObject(i)
                    val nombre = x.getString("nombre")
                    val kcal100 = x.getDouble("kcal_100g")
                    val base = runCatching { repo.emparejar(x.optString("busqueda", nombre), nombre, kcal100) }.getOrNull()
                    Ingrediente(
                        base ?: Alimento(
                            nombre = nombre, marca = "Estimado por IA", kcal = kcal100,
                            proteinas = x.getDouble("proteinas_100g"), hidratos = x.getDouble("hidratos_100g"), grasas = x.getDouble("grasas_100g"),
                        ),
                        x.getDouble("gramos"),
                    )
                }.filter { it.gramos > 0 },
                pasos = textos(o.getJSONArray("pasos")),
                alergenos = textos(o.getJSONArray("alergenos")).toSet(),
                deIa = true,
            )
        }.getOrNull()
    }
}
