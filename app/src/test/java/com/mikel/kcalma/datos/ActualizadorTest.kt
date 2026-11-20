package com.mikel.kcalma.datos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActualizadorTest {
    @Test fun comparaVersiones() {
        assertTrue(Actualizador.esMasNueva("0.2.0", "0.1.0"))
        assertTrue(Actualizador.esMasNueva("0.10.0", "0.9.2"))
        assertTrue(Actualizador.esMasNueva("1.0", "0.9.9"))
        assertFalse(Actualizador.esMasNueva("0.1.0", "0.1.0"))
        assertFalse(Actualizador.esMasNueva("0.1.0", "0.2.0"))
    }
}
