package com.mikel.kcalma.dominio

import kotlin.math.abs
import kotlin.math.roundToInt

data class Ingrediente(val alimento: Alimento, val gramos: Double)

data class Receta(
    val nombre: String,
    val emoji: String,
    val comidas: Set<ComidaDia>,
    val minutos: Int,
    val ingredientes: List<Ingrediente>,
    val pasos: List<String>,
    val alergenos: Set<String>,
    val deIa: Boolean = false,
) {
    val nutrientes: Nutrientes get() = ingredientes.fold(Nutrientes()) { s, i -> s + i.alimento.nutrientes(i.gramos) }

    /** La misma receta con las cantidades multiplicadas, redondeadas a 5 g. */
    fun escalada(factor: Double) = copy(ingredientes = ingredientes.map { i ->
        i.copy(gramos = ((i.gramos * factor) / 5).roundToInt().coerceAtLeast(1) * 5.0)
    })
}

/** Una receta ya ajustada a lo que le queda al usuario para esa comida. */
data class Idea(val receta: Receta, val encaja: Boolean)

data class FiltrosIdeas(val rapidas: Boolean = false, val favoritas: Boolean = false)

object Recomendador {
    /** Reparto orientativo del plan entre las comidas. */
    private val REPARTO = mapOf(
        ComidaDia.DESAYUNO to .22, ComidaDia.MEDIA_MANANA to .10, ComidaDia.COMIDA to .35,
        ComidaDia.MERIENDA to .10, ComidaDia.CENA to .25,
    )

    /** kcal a las que apuntar en esa comida: su parte del plan, sin pasarse de lo que queda (la cena se lleva lo que quede). */
    fun objetivo(comida: ComidaDia, planKcal: Int, restanteKcal: Double): Double {
        val parte = planKcal * (REPARTO[comida] ?: .25)
        // La cena se lleva lo que quede, pero sin pasar del 40 % del plan aunque no se haya apuntado nada más
        val base = if (comida == ComidaDia.CENA) minOf(restanteKcal, planKcal * .40) else minOf(parte, restanteKcal)
        return base.coerceAtLeast(planKcal * .08)
    }

    /** Clave común para comparar alérgenos escritos de formas distintas (la IA puede decir «Leche» o «crustaceo»). */
    private fun alergeno(a: String): String = normalizar(a).let { n ->
        when {
            n.startsWith("lech") || n.startsWith("lact") -> "lactosa"
            n.startsWith("frut") -> "frutos secos"
            n.startsWith("crust") -> "crustaceos"
            n.startsWith("molus") -> "moluscos"
            n.startsWith("altram") -> "altramuces"
            n.startsWith("sulfit") || n.startsWith("sulfur") -> "sulfitos"
            n.startsWith("huev") -> "huevo"
            n.startsWith("cacahu") -> "cacahuete"
            else -> n
        }
    }

    private fun contiene(r: Receta, terminos: Set<String>): Boolean {
        val texto = normalizar(r.nombre + " " + r.ingredientes.joinToString(" ") { it.alimento.nombre })
        return terminos.any { t -> normalizar(t).takeIf { it.length > 2 }?.let { texto.contains(it.take(5)) } == true }
    }

    fun recomendar(
        recetas: List<Receta>, perfil: Perfil, comida: ComidaDia, planKcal: Int,
        restante: Nutrientes, filtros: FiltrosIdeas = FiltrosIdeas(),
    ): List<Idea> {
        val obj = objetivo(comida, planKcal, restante.kcal)
        val alergias = perfil.alergias.map(::alergeno).toSet()
        // Proteína que tocaría en proporción a las kcal de esta comida
        val protObj = if (restante.kcal > 0) (restante.proteinas * obj / restante.kcal).coerceAtLeast(0.0) else 0.0
        return recetas
            .filter { comida in it.comidas || it.deIa }
            .filter { r -> r.alergenos.none { a -> alergeno(a) in alergias } && !contiene(r, perfil.noMeGusta) }
            .filter { !filtros.rapidas || it.minutos <= 15 }
            .filter { !filtros.favoritas || contiene(it, perfil.meEncanta) }
            .map { r ->
                val kcal = r.nutrientes.kcal
                val escalada = if (kcal > 0) r.escalada((obj / kcal).coerceIn(.6, 1.6)) else r
                val n = escalada.nutrientes
                val desvio = abs(n.kcal - obj) / obj
                val faltaProteina = if (protObj > 0) ((protObj - n.proteinas) / protObj).coerceAtLeast(0.0) else 0.0
                val favorita = if (contiene(r, perfil.meEncanta)) .15 else 0.0
                Triple(Idea(escalada, desvio <= .12), desvio + faltaProteina * .5 - favorita, r.deIa)
            }
            .sortedWith(compareByDescending<Triple<Idea, Double, Boolean>> { it.third }.thenBy { it.second })
            .map { it.first }
    }
}
