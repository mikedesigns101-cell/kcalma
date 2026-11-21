package com.mikel.kcalma.dominio

import kotlin.math.roundToInt

/**
 * Ejercicio extra del día (PLAN.md §0, 0.5.0: «la mitad»). El plan ya cuenta los pasos que el usuario dijo
 * que suele dar; solo lo que camine de más suma, y solo la mitad, porque los pasos del móvil sobrestiman.
 * Los entrenamientos ya están en la media diaria del plan.
 */
object Actividad {
    /** Pasos que el plan ya da por hechos según la respuesta del onboarding. */
    fun pasosDelPlan(p: Pasos): Int = when (p) {
        Pasos.MENOS_5K -> 4000
        Pasos.DE_5_A_8K -> 6500
        Pasos.DE_8_A_12K -> 10000
        Pasos.MAS_12K -> 13500
    }

    /** ~0,5 kcal por kg y 1.000 pasos (unas 40 kcal cada 1.000 pasos con 80 kg). */
    fun kcalExtra(perfil: Perfil, pasos: Long): Int {
        val extra = (pasos - pasosDelPlan(perfil.pasos)).coerceAtLeast(0)
        return (extra / 1000.0 * 0.5 * perfil.pesoKg).roundToInt()
    }

    /** Lo que se suma al objetivo de hoy: la mitad, redondeado a 10. */
    fun suma(perfil: Perfil, pasos: Long): Int = (kcalExtra(perfil, pasos) / 2.0 / 10).roundToInt() * 10
}
