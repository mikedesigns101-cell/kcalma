package com.mikel.kcalma.dominio

import kotlin.math.roundToInt

/** Lo que se propone para una serie: lo que toca hoy y lo que se hizo la última vez. */
data class SeriePlan(val kg: Double, val reps: Int, val anterior: Serie?)

data class PlanEjercicio(val series: List<SeriePlan>, val sube: Boolean, val nota: String?)

/**
 * Doble progresión, calculada por código (nunca por la IA):
 * - Cada serie se rellena con lo que hiciste la última vez en ese ejercicio, para superarlo.
 * - Cuando todas las series llegan arriba del rango de repeticiones, se sube el peso y se vuelve abajo del rango.
 * - Kettlebell: se sube a la siguiente que tengas. Sin peso: solo se suben repeticiones.
 */
object Progresion {

    /** [ultima]: series de la última sesión en la que se hizo este ejercicio (cualquier rutina). */
    fun proponer(ej: Ejercicio, b: BloqueRutina, ultima: List<Serie>, kettlebells: List<Double> = emptyList()): PlanEjercicio {
        val hechas = ultima.filter { it.hecha }.sortedBy { it.orden }
        if (hechas.isEmpty()) {
            val kg = if (ej.material == Material.KETTLEBELL) kettlebells.sorted().getOrNull(kettlebells.size / 2) ?: 0.0 else 0.0
            return PlanEjercicio(List(b.series) { SeriePlan(kg, b.repsMin, null) }, false, null)
        }
        val completo = hechas.size >= b.series && hechas.take(b.series).all { it.reps >= b.repsMax }
        val kgUltimo = hechas.maxOf { it.kg }
        if (completo && ej.conPeso) {
            val nuevo = subir(ej, kgUltimo, kettlebells)
            if (nuevo > kgUltimo) return PlanEjercicio(
                List(b.series) { SeriePlan(nuevo, b.repsMin, hechas.getOrNull(it)) }, true,
                "Llegaste a ${b.repsMax} en todas las series: hoy sube a ${kg(nuevo)} kg.",
            )
        }
        val plan = List(b.series) { i ->
            val a = hechas.getOrNull(i) ?: hechas.last()
            SeriePlan(a.kg, a.reps.coerceAtLeast(if (completo) b.repsMax else b.repsMin), hechas.getOrNull(i))
        }
        return PlanEjercicio(plan, false, null)
    }

    /** Peso siguiente: la siguiente kettlebell que se tiene o el salto del ejercicio. */
    fun subir(ej: Ejercicio, kg: Double, kettlebells: List<Double>): Double = when {
        !ej.conPeso -> kg
        ej.material == Material.KETTLEBELL -> kettlebells.filter { it > kg }.minOrNull() ?: kg
        else -> kg + ej.salto
    }

    /** Texto bajo el ejercicio durante la sesión: qué hace falta para subir la próxima vez. */
    fun meta(ej: Ejercicio, b: BloqueRutina, kg: Double, kettlebells: List<Double>): String? {
        if (!ej.conPeso) return "Cuando hagas ${b.repsMax} en todas las series, añade dificultad o más series."
        val sig = subir(ej, kg, kettlebells)
        if (sig <= kg) return null
        return "Si haces ${b.repsMax} reps en las ${b.series} series, la próxima vez te propondré ${kg(sig)} kg."
    }

    /**
     * Es récord si ninguna serie anterior tiene a la vez igual o más peso e igual o más repeticiones
     * (p. ej. 80 × 9 cuando lo mejor con 80 kg era 80 × 8).
     */
    fun esRecord(s: Serie, historial: List<Serie>): Boolean =
        s.reps > 0 && historial.none { it.hecha && it.kg >= s.kg && it.reps >= s.reps }

    /** Mejor serie (más peso; a igual peso, más repeticiones). */
    fun mejor(historial: List<Serie>): Serie? = historial.filter { it.hecha && it.reps > 0 }
        .maxWithOrNull(compareBy<Serie> { it.kg }.thenBy { it.reps })

    fun kg(v: Double): String = if (v == v.roundToInt().toDouble()) v.roundToInt().toString() else "%.1f".format(v).replace('.', ',')
}
