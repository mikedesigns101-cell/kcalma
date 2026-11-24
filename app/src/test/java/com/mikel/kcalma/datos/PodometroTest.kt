package com.mikel.kcalma.datos

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PodometroTest {
    private val hoy = LocalDate.of(2026, 11, 25)

    @Test fun `la primera lectura solo marca el punto de partida`() {
        val (dias, ultimo) = Podometro.repartir(emptyMap(), -1, null, 53_000, hoy)
        assertEquals(emptyMap<String, Long>(), dias)
        assertEquals(53_000L, ultimo)
    }

    @Test fun `los pasos nuevos del mismo dia se suman a hoy`() {
        val (dias, ultimo) = Podometro.repartir(mapOf("2026-11-25" to 1_000L), 53_000, "2026-11-25", 55_500, hoy)
        assertEquals(3_500L, dias["2026-11-25"])
        assertEquals(55_500L, ultimo)
    }

    @Test fun `si el movil se reinicio el contador vuelve a empezar`() {
        val (dias, ultimo) = Podometro.repartir(mapOf("2026-11-25" to 1_000L), 53_000, "2026-11-25", 400, hoy)
        assertEquals(1_400L, dias["2026-11-25"])
        assertEquals(400L, ultimo)
    }

    @Test fun `varios dias sin leer se reparten entre esos dias`() {
        val (dias, _) = Podometro.repartir(mapOf("2026-11-23" to 2_000L), 10_000, "2026-11-23", 19_001, hoy)
        assertEquals(5_000L, dias["2026-11-23"])
        assertEquals(3_000L, dias["2026-11-24"])
        assertEquals(3_001L, dias["2026-11-25"])
    }

    @Test fun `sin pasos nuevos no cambia nada`() {
        val antes = mapOf("2026-11-25" to 1_000L)
        assertEquals(antes, Podometro.repartir(antes, 500, "2026-11-25", 500, hoy).first)
    }
}
