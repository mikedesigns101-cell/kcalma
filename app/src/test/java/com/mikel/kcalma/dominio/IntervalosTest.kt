package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalosTest {
    private val tabata = Rutina("t", "Tabata", Seccion.KETTLEBELL, emptyList(), Formato.INTERVALOS, trabajo = 20, pausa = 10, rondas = 8)

    @Test fun `duracion sin descanso tras la ultima ronda`() {
        assertEquals(5_000L + 8 * 20_000 + 7 * 10_000, tabata.duracionIntervalos())
        assertEquals(4, tabata.duracion())
    }

    @Test fun `fases en orden`() {
        assertEquals(Fase.Tipo.PREPARA, tabata.fase(1_000).tipo)
        tabata.fase(6_000).let { assertEquals(Fase.Tipo.TRABAJA, it.tipo); assertEquals(0, it.ronda); assertEquals(19_000, it.queda); assertEquals(0, it.cerradas) }
        tabata.fase(26_000).let { assertEquals(Fase.Tipo.DESCANSA, it.tipo); assertEquals(0, it.ronda); assertEquals(1, it.cerradas) }
        tabata.fase(35_000).let { assertEquals(Fase.Tipo.TRABAJA, it.tipo); assertEquals(1, it.ronda) }
        tabata.fase(tabata.duracionIntervalos() - 1).let { assertEquals(Fase.Tipo.TRABAJA, it.tipo); assertEquals(7, it.ronda) }
        tabata.fase(tabata.duracionIntervalos()).let { assertEquals(Fase.Tipo.FIN, it.tipo); assertEquals(8, it.cerradas) }
    }
}
