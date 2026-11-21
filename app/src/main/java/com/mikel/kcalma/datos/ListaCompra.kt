package com.mikel.kcalma.datos

import android.content.Context
import com.mikel.kcalma.dominio.Receta
import com.mikel.kcalma.dominio.normalizar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class ItemCompra(val nombre: String, val gramos: Double, val seccion: String, val hecho: Boolean = false)

data class EstadoLista(val recetas: List<String> = emptyList(), val items: List<ItemCompra> = emptyList())

/** Lista de la compra hecha con las recetas de Ideas. Se guarda en el móvil; no entra en la copia. */
class ListaCompra(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("lista_compra", Context.MODE_PRIVATE)
    private val _estado = MutableStateFlow(leer())
    val estado: StateFlow<EstadoLista> = _estado

    /** Suma los ingredientes de [receta]; lo repetido se junta. */
    fun anadir(receta: Receta) {
        val e = _estado.value
        val items = e.items.toMutableList()
        receta.ingredientes.forEach { i ->
            val nombre = nombreCorto(i.alimento.nombre)
            val k = items.indexOfFirst { normalizar(it.nombre) == normalizar(nombre) }
            if (k >= 0) items[k] = items[k].copy(gramos = items[k].gramos + i.gramos, hecho = false)
            else items += ItemCompra(nombre, i.gramos, seccion(nombre))
        }
        guardar(EstadoLista((e.recetas - receta.nombre) + receta.nombre, items))
    }

    fun marcar(nombre: String) = guardar(_estado.value.let { e ->
        e.copy(items = e.items.map { if (it.nombre == nombre) it.copy(hecho = !it.hecho) else it })
    })

    fun vaciar() = guardar(EstadoLista())

    /** Texto para compartir (WhatsApp, notas…): solo lo que falta por coger. */
    fun texto(): String = buildString {
        append("🛒 Lista de la compra (Kcalma)\n")
        _estado.value.items.filter { !it.hecho }.groupBy { it.seccion }.toSortedMap(compareBy { SECCIONES.indexOf(it) }).forEach { (s, l) ->
            append("\n$s\n")
            l.forEach { append("• ${it.nombre} · ${gramosTexto(it.gramos)}\n") }
        }
    }

    private fun guardar(e: EstadoLista) {
        _estado.value = e
        val o = JSONObject().put("recetas", JSONArray(e.recetas)).put("items", JSONArray(e.items.map {
            JSONObject().put("n", it.nombre).put("g", it.gramos).put("s", it.seccion).put("h", it.hecho)
        }))
        prefs.edit().putString("lista", o.toString()).apply()
    }

    private fun leer(): EstadoLista = runCatching {
        val o = JSONObject(prefs.getString("lista", null) ?: return EstadoLista())
        val r = o.getJSONArray("recetas")
        val i = o.getJSONArray("items")
        EstadoLista(
            (0 until r.length()).map { r.getString(it) },
            (0 until i.length()).map { i.getJSONObject(it) }.map { ItemCompra(it.getString("n"), it.getDouble("g"), it.getString("s"), it.optBoolean("h")) },
        )
    }.getOrDefault(EstadoLista())

    companion object {
        val SECCIONES = listOf("Pescado y carne", "Verdura y fruta", "Lácteos y huevos", "Despensa")

        private val RAICES = mapOf(
            "Pescado y carne" to listOf(
                "pollo", "pavo", "ternera", "cerdo", "lomo", "jamon", "chorizo", "carne", "solomillo", "conejo", "cordero", "hamburguesa",
                "salchicha", "bacon", "panceta", "pechuga", "muslo", "merluza", "salmon", "atun", "bacalao", "gamba", "langostino",
                "pescado", "sardina", "dorada", "lubina", "boqueron", "calamar", "sepia", "mejillon", "pulpo", "rape", "emperador", "caballa",
            ),
            "Lácteos y huevos" to listOf("leche", "yogur", "queso", "huevo", "nata", "mantequilla", "kefir", "requeson", "clara"),
            "Verdura y fruta" to listOf(
                "tomate", "cebolla", "ajo", "pimiento", "patata", "zanahoria", "lechuga", "espinaca", "calabacin", "berenjena", "brocoli",
                "coliflor", "champinon", "seta", "pepino", "manzana", "platano", "naranja", "fresa", "limon", "aguacate", "fruta",
                "verdura", "puerro", "judia verde", "judias verdes", "guisante", "calabaza", "kiwi", "pera", "uva", "rucula", "perejil",
                "cilantro", "acelga", "col", "lombarda", "esparrago", "alcachofa", "mango", "pina", "melon", "sandia", "arandano", "boniato",
                "batata", "apio", "canonigo", "brote", "ensalada", "frambuesa", "melocoton", "cereza",
            ),
        )

        fun seccion(nombre: String): String {
            val n = " " + normalizar(nombre) + " "
            return RAICES.entries.firstOrNull { (_, l) -> l.any { n.contains(" $it") } }?.key ?: "Despensa"
        }

        /** "Lenteja, seca, cruda" → "Lenteja". */
        fun nombreCorto(nombre: String) = nombre.substringBefore(",").trim().ifEmpty { nombre }

        fun gramosTexto(g: Double) = if (g >= 1000) "%.1f kg".format(g / 1000).replace('.', ',').replace(",0 kg", " kg") else "${g.roundToInt()} g"
    }
}
