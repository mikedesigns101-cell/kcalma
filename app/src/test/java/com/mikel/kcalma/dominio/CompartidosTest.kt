package com.mikel.kcalma.dominio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompartidosTest {
    private val bueno = Alimento(
        nombre = "Hamburguesa de vaca madurada", marca = "Hacendado", codigoBarras = "8480000123459",
        kcal = 250.0, proteinas = 17.5, hidratos = 0.8, grasas = 19.6, saturadas = 8.9, azucares = 0.5, sal = 1.2,
    )

    @Test fun `codigo de barras con su digito de control`() {
        assertTrue(Compartidos.codigoValido("8480000123459"))
        assertTrue(Compartidos.codigoValido("96385074"))
        assertFalse(Compartidos.codigoValido("8480000123450"))
        assertFalse(Compartidos.codigoValido("123"))
        assertFalse(Compartidos.codigoValido(null))
    }

    @Test fun `solo vale lo que tiene numeros coherentes`() {
        assertTrue(Compartidos.valido(bueno))
        assertFalse(Compartidos.valido(bueno.copy(kcal = 20.0)))
        assertFalse(Compartidos.valido(bueno.copy(proteinas = 60.0, grasas = 50.0)))
        assertFalse(Compartidos.valido(bueno.copy(nombre = "Mira http://x.es")))
        assertFalse(Compartidos.valido(bueno.copy(codigoBarras = null)))
        assertFalse(Compartidos.valido(bueno.copy(azucares = 30.0)))
        assertFalse(Compartidos.valido(bueno.copy(kcal = 5000.0)))
    }
}
