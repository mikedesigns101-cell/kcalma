package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalculadoraTest {

    // El ejemplo de la maqueta: hombre, 30 años, 178 cm, 85 kg, sentado, 5-8k pasos, 3 días de pesas de 1 h
    private val mikel = Perfil(
        sexo = Sexo.HOMBRE, edad = 30, alturaCm = 178, pesoKg = 85.0, pesoObjetivoKg = 78.0,
        trabajo = Trabajo.SENTADO, pasos = Pasos.DE_5_A_8K,
        diasEjercicio = 3, tipoEjercicio = TipoEjercicio.PESAS, duracion = Duracion.UNA_HORA,
        objetivo = Objetivo.PERDER, ritmo = Ritmo.NORMAL,
    )

    @Test fun reposoMifflin() = assertEquals(1818, Calculadora.calcular(mikel).reposo)

    @Test fun desgloseDelGasto() {
        val plan = Calculadora.calcular(mikel)
        assertEquals(636, plan.actividad)   // 1817,5 × 0,35
        assertEquals(146, plan.ejercicio)   // 4 × 85 × 1 × 3 / 7
        assertEquals(2600, plan.gasto)
    }

    @Test fun deficitNormalEsMedioKilo() {
        val plan = Calculadora.calcular(mikel)
        assertEquals(2050, plan.kcal)       // 2600 − 550
        assertEquals(-0.5, plan.kgSemana, 1e-9)
    }

    @Test fun macrosCuadranConLasCalorias() {
        val plan = Calculadora.calcular(mikel)
        assertEquals(170, plan.proteinaG)
        assertEquals(68, plan.grasasG)
        val kcalMacros = plan.proteinaG * 4 + plan.hidratosG * 4 + plan.grasasG * 9
        assertTrue(kotlin.math.abs(kcalMacros - plan.kcal) <= 4)
    }

    @Test fun katchMcArdleSiHayGrasa() =
        assertEquals(1839, Calculadora.calcular(mikel.copy(grasaPct = 20.0)).reposo) // 370 + 21,6 × 68

    @Test fun rapidoNoPasaDelUnoPorCiento() {
        val ligera = mikel.copy(pesoKg = 60.0, alturaCm = 165, sexo = Sexo.MUJER, ritmo = Ritmo.RAPIDO)
        assertEquals(-0.6, Calculadora.calcular(ligera).kgSemana, 1e-9)
    }

    @Test fun minimoDeSeguridad() {
        val pequena = Perfil(
            sexo = Sexo.MUJER, edad = 60, alturaCm = 150, pesoKg = 50.0, pesoObjetivoKg = 45.0,
            trabajo = Trabajo.SENTADO, pasos = Pasos.MENOS_5K, diasEjercicio = 0,
            objetivo = Objetivo.PERDER, ritmo = Ritmo.RAPIDO,
        )
        val plan = Calculadora.calcular(pequena)
        assertEquals(Calculadora.MINIMO_MUJER, plan.kcal)
        assertTrue(plan.limitadoPorMinimo)
    }

    @Test fun recomposicionEsMenosDiezPorCiento() =
        assertEquals(2340, Calculadora.calcular(mikel.copy(objetivo = Objetivo.RECOMPOSICION)).kcal)

    @Test fun fechaObjetivo() {
        val hoy = LocalDate.of(2026, 11, 20)
        assertEquals(LocalDate.of(2027, 2, 26), Calculadora.fechaObjetivo(mikel, hoy)) // 7 kg / 0,5 = 14 semanas
        assertNull(Calculadora.fechaObjetivo(mikel.copy(objetivo = Objetivo.RECOMPOSICION), hoy))
    }
}
