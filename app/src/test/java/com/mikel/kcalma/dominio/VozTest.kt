package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VozTest {
    @Test
    fun separaGramosDelAlimento() {
        val d = Voz.interpretar("200 gramos de arroz hervido")
        assertEquals("arroz hervido", d.texto)
        assertEquals(200.0, d.cantidad?.gramos)
        assertEquals(500.0, Voz.interpretar("medio kilo de patatas").cantidad?.gramos)
        assertEquals(150.0, Voz.interpretar("Yogur griego 150 g").cantidad?.gramos)
        assertEquals("yogur griego", Voz.interpretar("Yogur griego 150 g").texto)
    }

    @Test
    fun piezasYSingular() {
        val d = Voz.interpretar("dos huevos")
        assertEquals("huevo", d.texto)
        assertEquals(2.0, d.cantidad?.unidades)
        assertEquals(.5, Voz.interpretar("medio plátano").cantidad?.unidades)
        assertNull(Voz.interpretar("lentejas").cantidad)
    }
}
