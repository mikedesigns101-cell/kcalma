package com.mikel.kcalma.dominio

import com.mikel.kcalma.datos.Catalogo
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

    @Test
    fun cocido() {
        val d = Voz.interpretar("250 gramos de espaguetis cocidos")
        assertEquals("espaguetis", d.texto)
        assertEquals(true, d.cantidad?.cocido)
        assertEquals(true, Voz.interpretar("200 gramos de arroz hervido").cantidad?.cocido)
        val e = d.cantidad!!.entrada(Alimento("Espaguetis", kcal = 360.0), ComidaDia.entries.first())
        assertEquals(Unidad.COCIDO, e.unidad)
        assertEquals(250.0, e.cantidad, 0.0)
        assertEquals(250 / 2.4, e.gramos, 0.01)
        // Un genérico ya cocido no se convierte
        val hecha = d.cantidad!!.entrada(Alimento("Pasta alimenticia, hervida", kcal = 125.0), ComidaDia.entries.first())
        assertEquals(Unidad.GRAMOS, hecha.unidad)
    }

    @Test
    fun deteccionDeCocidos() {
        assertEquals(2.4, Coccion.factor(Alimento("Macarrones", kcal = 350.0)))
        assertEquals(2.7, Coccion.factor(Alimento("Arroz basmati", kcal = 350.0)))
        assertEquals(2.4, Coccion.factor(Alimento("Lenteja pardina", kcal = 310.0)))
        assertNull(Coccion.factor(Alimento("Lenteja, hervida", kcal = 116.0)))
        assertNull(Coccion.factor(Alimento("Pasta de cacahuete", kcal = 600.0)))
        assertNull(Coccion.factor(Alimento("Tortitas de arroz", kcal = 380.0)))
    }

    @Test
    fun pluralesEnElBuscador() {
        assertEquals("macarron", Catalogo.singular("macarrones"))
        assertEquals("espagueti", Catalogo.singular("espaguetis"))
        assertEquals("lenteja", Catalogo.singular("lentejas"))
        assertEquals("arroz", Catalogo.singular("arroz"))
    }
}
