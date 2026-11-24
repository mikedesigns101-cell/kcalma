package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictadoTest {
    @Test fun `el ejemplo de Mikel`() {
        val p = Voz.separar("He comido 100 gramos de arroz cocido con 300 gramos de pollo a la plancha")
        assertEquals(2, p.size)
        assertEquals("arroz", p[0].texto)
        assertEquals(100.0, p[0].cantidad!!.gramos!!, 0.001)
        assertTrue(p[0].cantidad!!.cocido)
        assertEquals(Cocinado.HERVIDO, p[0].cocinado)
        assertEquals("pollo", p[1].texto)
        assertEquals(300.0, p[1].cantidad!!.gramos!!, 0.001)
        assertEquals(Cocinado.PLANCHA, p[1].cocinado)
    }

    @Test fun `con e y no parten un plato si no sigue una cantidad`() {
        assertEquals(listOf("arroz con leche"), Voz.separar("he cenado arroz con leche").map { it.texto })
        assertEquals(listOf("pan con tomate", "huevo"), Voz.separar("pan con tomate y dos huevos").map { it.texto })
        assertNull(Voz.separar("macarrones con tomate")[0].cantidad)
    }

    @Test fun `comas piezas y cocinado`() {
        val p = Voz.separar("Me he comido dos huevos fritos, 150 g de patatas en airfryer y un yogur")
        assertEquals(listOf("huevo", "patatas", "yogur"), p.map { it.texto })
        assertEquals(2.0, p[0].cantidad!!.unidades!!, 0.001)
        assertEquals(Cocinado.FRITO, p[0].cocinado)
        assertEquals(Cocinado.AIRFRYER, p[1].cocinado)
        assertEquals(Cocinado.TAL_CUAL, p[2].cocinado)
    }

    @Test fun `aceite por cada 100 gramos`() {
        assertEquals(9.0, Cocinado.PLANCHA.aceite(300.0), 0.001)
        assertEquals(1.5, Cocinado.AIRFRYER.aceite(150.0), 0.001)
        assertEquals(12.0, Cocinado.FRITO.aceite(150.0), 0.001)
        assertEquals(0.0, Cocinado.HERVIDO.aceite(500.0), 0.001)
    }
}
