package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GustosTest {
    @Test
    fun setasQuitaChampinones() {
        assertTrue(Gustos.menciona("Revuelto de champiñones y gambas", setOf("Setas")))
        assertTrue(Gustos.menciona("Revuelto de champiñones y gambas", setOf("setas")))
    }

    @Test
    fun loEscritoAManoSoloQuitaEsaPalabra() {
        assertEquals("Setas", Gustos.categoriaDe("seta")?.nombre)
        assertEquals(null, Gustos.categoriaDe("Champiñones"))
        assertTrue(Gustos.menciona("Revuelto de champiñón", setOf("Champiñones")))
        assertFalse(Gustos.menciona("Leche natural", setOf("Nata")))
        assertTrue(Gustos.menciona("Fresas con nata", setOf("Nata")))
    }

    @Test
    fun noConfundePalabrasParecidas() {
        assertFalse(Gustos.menciona("Panceta con patatas", setOf("Pan")))
        assertFalse(Gustos.menciona("Bizcocho de chocolate", setOf("Marisco")))
        assertTrue(Gustos.menciona("Tostada con pan integral", setOf("Pan")))
    }

    @Test
    fun terminoLibreSinCategoria() {
        assertTrue(Gustos.menciona("Ensalada con remolacha", setOf("Remolacha")))
    }
}
