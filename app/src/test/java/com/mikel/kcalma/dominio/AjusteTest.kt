package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AjusteTest {
    private val hoy = LocalDate.of(2026, 11, 21)
    private val perfil = Perfil(pesoKg = 80.0)
    private val plan = Calculadora.calcular(perfil)

    private fun dias(kcal: Double, n: Int = 21) = (1..n).associate { hoy.minusDays(it.toLong()).toString() to Nutrientes(kcal = kcal) }
    private fun pesos(vararg p: Pair<Int, Double>) = p.map { (haceDias, kg) -> Pesada(hoy.minusDays(haceDias.toLong()).toString(), kg) }

    @Test
    fun siBajaMenosDeLoPrevistoProponeBajar() {
        // Come lo del plan y no baja nada: su gasto real es lo que come, ~500 kcal menos de lo estimado
        val p = Ajuste.proponer(perfil, pesos(21 to 80.0, 14 to 80.0, 7 to 80.0, 0 to 80.0), dias(plan.kcal.toDouble()), hoy)
        assertNotNull(p)
        assertEquals(-Ajuste.TOPE, p!!.nuevaCorreccion) // con tope
        assertTrue(p.kcalNueva < p.kcalActual)
    }

    @Test
    fun siCuadraNoPropone() {
        // Come el plan y baja justo lo previsto (0,5 kg/semana)
        val p = Ajuste.proponer(perfil, pesos(21 to 81.5, 14 to 81.0, 7 to 80.5, 0 to 80.0), dias(plan.kcal.toDouble()), hoy)
        assertNull(p)
    }

    @Test
    fun conPocosDatosNoPropone() {
        assertNull(Ajuste.proponer(perfil, pesos(21 to 80.0, 0 to 80.0), dias(plan.kcal.toDouble()), hoy))
        assertNull(Ajuste.proponer(perfil, pesos(21 to 80.0, 14 to 80.0, 0 to 80.0), dias(plan.kcal.toDouble(), 10), hoy))
        // Días apuntados a medias no cuentan
        assertNull(Ajuste.proponer(perfil, pesos(21 to 80.0, 14 to 80.0, 0 to 80.0), dias(300.0), hoy))
        assertNull(Ajuste.proponer(perfil.copy(kcalManual = 2000), pesos(21 to 80.0, 14 to 80.0, 0 to 80.0), dias(plan.kcal.toDouble()), hoy))
    }

    @Test
    fun laCorreccionCambiaElPlan() {
        val corregido = Calculadora.calcular(perfil.copy(correccionGasto = -200))
        assertEquals(plan.gasto - 200, corregido.gasto)
        assertEquals(plan.kcal - 200, corregido.kcal)
    }
}
