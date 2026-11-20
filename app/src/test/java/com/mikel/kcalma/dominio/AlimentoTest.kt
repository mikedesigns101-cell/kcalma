package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlimentoTest {
    @Test fun normalizaIgualQuePython() {
        assertEquals("cafe bonka", normalizar("Café Bonka®"))
        assertEquals("pina", normalizar("piña"))
        assertEquals("yogur griego 0", normalizar("  Yogur GRIEGO 0%  "))
    }

    @Test fun gramosDelEnvase() {
        assertEquals(500.0, gramosDeCantidad("500 g")!!, 0.0)
        assertEquals(1000.0, gramosDeCantidad("1 kg")!!, 0.0)
        assertEquals(1000.0, gramosDeCantidad("1 l")!!, 0.0)
        assertEquals(330.0, gramosDeCantidad("33 cl")!!, 0.0)
        assertEquals(200.0, gramosDeCantidad("200g")!!, 0.0)
        assertNull(gramosDeCantidad("6 x 125 g"))
        assertNull(gramosDeCantidad(null))
    }

    @Test fun nutrientesPorCantidad() {
        val yogur = Alimento("Yogur", kcal = 120.0, proteinas = 6.0, racionG = 125.0)
        assertEquals(150, yogur.nutrientes(125.0).kcalInt)
        assertEquals(125.0, yogur.gramosRapidos, 0.0)
        assertEquals(100.0, yogur.copy(racionG = 1.0).gramosRapidos, 0.0)
        assertEquals("7,5", decimal(7.5))
        assertEquals("150", decimal(150.0))
    }
}
