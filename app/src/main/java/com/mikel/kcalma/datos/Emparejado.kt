package com.mikel.kcalma.datos

import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.normalizar
import kotlin.math.abs
import kotlin.math.ln

/**
 * Elige qué alimento de la base corresponde a lo que ha reconocido la IA. La base usa nombres tipo
 * BEDCA («Patata, hervida») y la IA habla natural («patatas cocidas»), así que se compara por raíces de
 * palabra con sinónimos de cocina y se desempata por cercanía a las kcal que estima la IA.
 */
object Emparejado {
    private val SINONIMOS = mapOf(
        "cocid" to listOf("herv"), "herv" to listOf("cocid"),
        "asad" to listOf("horne"), "horne" to listOf("asad"),
    )
    /** Palabras que matizan (cocción, color…): pueden faltar en el nombre de la base. El resto son ingredientes y deben estar. */
    private val BLANDAS = setOf("cocid", "herv", "asad", "horne", "frit", "planc", "guisa", "estof", "salte",
        "cocin", "crudo", "cruda", "vapor", "blanc", "integ", "natur", "fresc", "enter", "casei", "media", "grand", "peque")
    private val VACIAS = setOf("de", "del", "la", "el", "los", "las", "con", "y", "en", "a", "al")

    /** Raíces de las palabras con sentido: «patatas cocidas» → [patat, cocid]. */
    fun raices(texto: String): List<String> =
        normalizar(texto).split(' ').filter { it.length > 1 && it !in VACIAS }.map { it.take(5) }.distinct()

    /** Consultas de más estricta a más amplia, para el buscador de la base. */
    fun consultas(busqueda: String, nombre: String): List<String> {
        val r = raices(busqueda).ifEmpty { raices(nombre) }
        return listOfNotNull(r.joinToString(" "), r.take(2).joinToString(" ").takeIf { r.size > 2 }, r.firstOrNull())
            .filter { it.isNotBlank() }.distinct()
    }

    /** Verduras: poca diferencia absoluta vale; el resto, que no se aleje más de ~2 veces. */
    fun razonable(a: Alimento, kcalIa: Double) = kcalIa <= 0 || abs(a.kcal - kcalIa) < 25 || a.kcal / kcalIa in 0.45..2.2

    /** El mejor candidato: más palabras en común y, a igualdad, kcal más cercanas a la estimación. */
    fun elegir(candidatos: List<Alimento>, busqueda: String, kcalIa: Double): Alimento? {
        val r = raices(busqueda)
        fun coincidencias(a: Alimento): Int {
            val nombre = " " + normalizar(a.nombre) + " "
            return r.count { raiz -> (listOf(raiz) + SINONIMOS[raiz].orEmpty()).any { nombre.contains(" $it") } }
        }
        fun distancia(a: Alimento) = if (kcalIa > 0 && a.kcal > 0) abs(ln(a.kcal / kcalIa)) else 1.0
        fun tieneIngredientes(a: Alimento): Boolean {
            val nombre = " " + normalizar(a.nombre) + " "
            return r.filter { it !in BLANDAS }.all { nombre.contains(" $it") }
        }
        return candidatos.filter { razonable(it, kcalIa) && coincidencias(it) > 0 && tieneIngredientes(it) }
            .sortedWith(compareByDescending<Alimento> { coincidencias(it) }.thenBy { distancia(it) })
            .firstOrNull()
    }
}
