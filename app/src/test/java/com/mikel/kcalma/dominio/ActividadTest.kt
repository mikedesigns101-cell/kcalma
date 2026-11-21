package com.mikel.kcalma.dominio

import com.mikel.kcalma.datos.ListaCompra
import org.junit.Assert.assertEquals
import org.junit.Test

class ActividadTest {
    @Test
    fun soloSumaLaMitadDeLoQueCaminaDeMas() {
        val p = Perfil(pesoKg = 80.0, pasos = Pasos.DE_5_A_8K) // el plan ya cuenta 6.500
        assertEquals(0, Actividad.suma(p, 5000))
        assertEquals(0, Actividad.suma(p, 6500))
        // 10.000 pasos: 3.500 de más · 40 kcal/1.000 = 140 → la mitad, 70
        assertEquals(70, Actividad.suma(p, 10000))
    }

    @Test
    fun listaPorSecciones() {
        assertEquals("Pescado y carne", ListaCompra.seccion("Pechuga de pollo"))
        assertEquals("Verdura y fruta", ListaCompra.seccion("Pimiento rojo"))
        assertEquals("Despensa", ListaCompra.seccion("Tomate frito"))
        assertEquals("Lácteos y huevos", ListaCompra.seccion("Huevo"))
        assertEquals("Despensa", ListaCompra.seccion("Arroz"))
        assertEquals("Lenteja", ListaCompra.nombreCorto("Lenteja, seca, cruda"))
    }
}
