package com.mikel.kcalma.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class RegistroTest {
    private val ahora = LocalDateTime.of(2026, 11, 24, 22, 30, 0)
    private val lineas = listOf(
        "24/11 20:00:00  ERROR Foto: IOException: sin red",
        "24/11 22:10:00  Algo viejo",
        "24/11 22:16:00  Foto preparada",
        "24/11 22:20:00  CIERRE: java.lang.IllegalStateException",
        "\tat com.mikel.kcalma.X",
        "24/11 22:29:00  Informe: Hoy",
    )

    @Test fun `solo los ultimos 15 minutos y el ultimo error anterior`() {
        val (recientes, anterior) = Registro.recientes(lineas, ahora)
        assertEquals(4, recientes.size)
        assertEquals("24/11 22:16:00  Foto preparada", recientes.first())
        assertEquals("24/11 20:00:00  ERROR Foto: IOException: sin red", anterior)
    }

    @Test fun `sin actividad reciente`() {
        val (recientes, anterior) = Registro.recientes(lineas.take(2), ahora)
        assertEquals(0, recientes.size)
        assertEquals("24/11 20:00:00  ERROR Foto: IOException: sin red", anterior)
        assertNull(Registro.recientes(emptyList(), ahora).second)
    }

    @Test fun `en enero las lineas de diciembre son del ano pasado`() {
        val enero = LocalDateTime.of(2027, 1, 1, 0, 5, 0)
        val (recientes, _) = Registro.recientes(listOf("31/12 23:40:00  Vieja", "31/12 23:55:00  Reciente"), enero)
        assertEquals(listOf("31/12 23:55:00  Reciente"), recientes)
    }
}
