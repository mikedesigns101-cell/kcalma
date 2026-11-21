package com.mikel.kcalma.dominio

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/** Propuesta semanal de cambiar el plan según lo que de verdad se ha comido y pesado. */
data class Propuesta(
    val dias: Int,
    val mediaKcal: Int,
    /** Cambio de peso en el periodo según la tendencia (negativo = ha bajado). */
    val cambioKg: Double,
    val semanas: Int,
    val gastoReal: Int,
    val gastoEstimado: Int,
    /** Nueva corrección del gasto que se guardaría en el perfil. */
    val nuevaCorreccion: Int,
    val kcalActual: Int,
    val kcalNueva: Int,
)

/**
 * Ajuste automático (PLAN.md §0, 0.5.0): con lo apuntado y las pesadas de las últimas 4 semanas se calcula
 * el gasto real (lo comido + lo perdido × 7.700 kcal/kg) y, si no cuadra con el estimado, se propone el cambio.
 * Nunca se aplica solo.
 */
object Ajuste {
    const val VENTANA_DIAS = 28L
    const val MIN_DIAS = 14
    const val MIN_PESADAS = 3
    /** Por debajo de esta diferencia el plan ya cuadra y no se molesta. */
    const val UMBRAL = 100
    /** Tope de cada propuesta, para cambiar poco a poco. */
    const val TOPE = 300

    fun proponer(perfil: Perfil, pesos: List<Pesada>, dias: Map<String, Nutrientes>, hoy: LocalDate = LocalDate.now()): Propuesta? {
        if (perfil.kcalManual != null) return null
        val plan = Calculadora.calcular(perfil)
        val desde = hoy.minusDays(VENTANA_DIAS)
        // Hoy no cuenta (está a medias) y tampoco los días apuntados a medias (menos de la mitad del plan)
        val completos = dias.filter { (f, n) ->
            val d = runCatching { LocalDate.parse(f) }.getOrNull() ?: return@filter false
            d >= desde && d < hoy && n.kcal >= plan.kcal * 0.5
        }
        if (completos.size < MIN_DIAS) return null
        val enVentana = pesos.mapNotNull { p -> runCatching { LocalDate.parse(p.fecha) }.getOrNull()?.let { it to p.kg } }
            .filter { it.first >= desde && it.first <= hoy }
        if (enVentana.size < MIN_PESADAS) return null
        val x = enVentana.map { ChronoUnit.DAYS.between(desde, it.first).toDouble() }
        if (x.max() - x.min() < 10) return null
        val pendiente = pendiente(x, enVentana.map { it.second }) // kg por día
        val media = completos.values.sumOf { it.kcal } / completos.size
        val gastoReal = media - pendiente * Calculadora.KCAL_POR_KG_GRASA
        val diferencia = (gastoReal - plan.gasto).roundToInt()
        if (abs(diferencia) < UMBRAL) return null
        val cambio = diferencia.coerceIn(-TOPE, TOPE) / 10 * 10
        val nueva = perfil.correccionGasto + cambio
        val kcalNueva = Calculadora.calcular(perfil.copy(correccionGasto = nueva)).kcal
        if (kcalNueva == plan.kcal) return null
        return Propuesta(
            dias = completos.size,
            mediaKcal = media.roundToInt(),
            cambioKg = pendiente * (x.max() - x.min()),
            semanas = ((x.max() - x.min()) / 7).roundToInt().coerceAtLeast(1),
            gastoReal = gastoReal.roundToInt(),
            gastoEstimado = plan.gasto,
            nuevaCorreccion = nueva,
            kcalActual = plan.kcal,
            kcalNueva = kcalNueva,
        )
    }

    /** Pendiente de la recta de mínimos cuadrados: la tendencia, sin fiarse de una sola pesada. */
    internal fun pendiente(x: List<Double>, y: List<Double>): Double {
        val mx = x.average()
        val my = y.average()
        val num = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
        val den = x.sumOf { (it - mx) * (it - mx) }
        return if (den == 0.0) 0.0 else num / den
    }
}
