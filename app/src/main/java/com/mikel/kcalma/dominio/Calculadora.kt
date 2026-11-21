package com.mikel.kcalma.dominio

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Resultado del cálculo, con el desglose que se enseña al usuario. */
data class Plan(
    val reposo: Int,
    val actividad: Int,
    val ejercicio: Int,
    /** Corrección aceptada en el ajuste semanal según el progreso real. */
    val correccion: Int,
    val gasto: Int,
    /** Negativo = déficit, positivo = superávit. */
    val ajuste: Int,
    val kcal: Int,
    val proteinaG: Int,
    val hidratosG: Int,
    val grasasG: Int,
    /** kg por semana (negativo = perder). Cero en recomposición. */
    val kgSemana: Double,
    /** Si se ha aplicado el mínimo de seguridad. */
    val limitadoPorMinimo: Boolean,
    /** Si las kcal las ha puesto el usuario a mano. */
    val manual: Boolean = false,
)

object Calculadora {

    const val KCAL_POR_KG_GRASA = 7700.0
    const val MINIMO_HOMBRE = 1500
    const val MINIMO_MUJER = 1200

    fun reposo(p: Perfil): Double {
        val grasa = p.grasaPct
        return if (grasa != null && grasa in 3.0..60.0) {
            // Katch-McArdle, con la masa magra
            370 + 21.6 * p.pesoKg * (1 - grasa / 100)
        } else {
            // Mifflin-St Jeor
            10 * p.pesoKg + 6.25 * p.alturaCm - 5 * p.edad + if (p.sexo == Sexo.HOMBRE) 5 else -161
        }
    }

    fun ejercicioDiario(p: Perfil): Double =
        (p.tipoEjercicio.met - 1) * p.pesoKg * p.duracion.horas * p.diasEjercicio / 7.0

    /** kg por semana según objetivo y ritmo, con el tope del 1 % del peso al perder. */
    fun kgSemana(p: Perfil): Double = when (p.objetivo) {
        Objetivo.PERDER -> {
            val elegido = when (p.ritmo) { Ritmo.SUAVE -> 0.25; Ritmo.NORMAL -> 0.5; Ritmo.RAPIDO -> 0.75 }
            -min(elegido, p.pesoKg * 0.01)
        }
        Objetivo.RECOMPOSICION -> 0.0
        Objetivo.GANAR -> when (p.ritmo) { Ritmo.SUAVE -> 0.1; Ritmo.NORMAL -> 0.2; Ritmo.RAPIDO -> 0.35 }
    }

    fun calcular(p: Perfil): Plan {
        val reposo = reposo(p)
        val conActividad = reposo * (p.trabajo.factor + p.pasos.extra)
        val ejercicio = ejercicioDiario(p)
        val gasto = conActividad + ejercicio + p.correccionGasto

        val kgSemana = kgSemana(p)
        val ajuste = when (p.objetivo) {
            Objetivo.PERDER -> kgSemana * KCAL_POR_KG_GRASA / 7
            Objetivo.RECOMPOSICION -> -gasto * 0.10
            Objetivo.GANAR -> when (p.ritmo) { Ritmo.SUAVE -> 150.0; Ritmo.NORMAL -> 250.0; Ritmo.RAPIDO -> 400.0 }
        }
        val minimo = if (p.sexo == Sexo.HOMBRE) MINIMO_HOMBRE else MINIMO_MUJER
        val bruto = redondear10(gasto + ajuste)
        val kcal = p.kcalManual ?: max(bruto, minimo)

        // Proteína y grasa por kg; si hay obesidad se usa el peso objetivo para no disparar la proteína
        val imc = p.pesoKg / ((p.alturaCm / 100.0) * (p.alturaCm / 100.0))
        val pesoRef = if (imc >= 30) p.pesoObjetivoKg else p.pesoKg
        val proteina = (if (p.objetivo == Objetivo.GANAR) 1.8 else 2.0) * pesoRef
        var grasa = 0.8 * pesoRef
        var hidratos = (kcal - proteina * 4 - grasa * 9) / 4
        if (hidratos < 50) {
            // Plan muy bajo en calorías: se recorta grasa hasta 0,6 g/kg antes que dejar sin hidratos
            grasa = 0.6 * pesoRef
            hidratos = max(0.0, (kcal - proteina * 4 - grasa * 9) / 4)
        }

        // El desglose que ve el usuario tiene que sumar exactamente el total
        val reposoR = reposo.roundToInt()
        val actividadR = (conActividad - reposo).roundToInt()
        val ejercicioR = ejercicio.roundToInt()
        val gastoR = reposoR + actividadR + ejercicioR + p.correccionGasto
        return Plan(
            reposo = reposoR,
            actividad = actividadR,
            ejercicio = ejercicioR,
            correccion = p.correccionGasto,
            gasto = gastoR,
            ajuste = kcal - gastoR,
            kcal = kcal,
            proteinaG = proteina.roundToInt(),
            hidratosG = hidratos.roundToInt(),
            grasasG = grasa.roundToInt(),
            kgSemana = kgSemana,
            limitadoPorMinimo = p.kcalManual == null && kcal > bruto,
            manual = p.kcalManual != null,
        )
    }

    /** Fecha aproximada en la que se llega al peso objetivo, o null si no aplica. */
    fun fechaObjetivo(p: Perfil, hoy: LocalDate = LocalDate.now()): LocalDate? {
        val semanal = kgSemana(p)
        val diferencia = p.pesoObjetivoKg - p.pesoKg
        if (semanal == 0.0 || diferencia == 0.0 || (diferencia > 0) != (semanal > 0)) return null
        return hoy.plusWeeks(ceil(abs(diferencia / semanal)).toLong())
    }

    private fun redondear10(x: Double) = (x / 10).roundToInt() * 10
}
