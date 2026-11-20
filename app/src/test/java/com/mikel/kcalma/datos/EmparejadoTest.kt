package com.mikel.kcalma.datos

import com.mikel.kcalma.dominio.Alimento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmparejadoTest {
    private fun a(nombre: String, kcal: Double) = Alimento(nombre = nombre, kcal = kcal, proteinas = 0.0, hidratos = 0.0, grasas = 0.0)

    private val patatas = listOf(
        a("Patata, asada", 205.5), a("Patata, cruda", 72.5), a("Patata, frita con aceite s/e, sin sal", 288.8),
        a("Patata, hervida", 74.9), a("Patatas, fritas, chips", 536.2),
    )

    @Test fun patatasCocidasEsPatataHervida() {
        assertEquals("Patata, hervida", Emparejado.elegir(patatas, "patatas cocidas", 80.0)?.nombre)
        assertEquals(listOf("patat cocid", "patat"), Emparejado.consultas("patatas cocidas", "Patatas cocidas"))
    }

    @Test fun kcalMuyDistintasNoSirven() {
        assertNull(Emparejado.elegir(listOf(a("Caldo de pollo", 8.0)), "pollo pechuga", 165.0))
    }

    @Test fun tortillaDePatatasNoEsFrancesa() {
        val c = listOf(a("Tortilla, a la francesa", 196.0), a("Tortilla de patatas con cebolla", 160.0))
        assertEquals("Tortilla de patatas con cebolla", Emparejado.elegir(c, "tortilla patatas", 180.0)?.nombre)
        assertNull(Emparejado.elegir(c.take(1), "tortilla patatas", 180.0))
    }

    @Test fun arrozBlancoCocidoAceptaArrozHervido() {
        assertEquals("Arroz, hervido", Emparejado.elegir(listOf(a("Arroz, hervido", 130.0)), "arroz blanco cocido", 130.0)?.nombre)
    }
}
