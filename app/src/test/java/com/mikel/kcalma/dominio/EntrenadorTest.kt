package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EntrenadorTest {
    private val torso = Rutinas.predefinidas.first { it.id == "p_torso" }
    private val rutinas = Rutinas.predefinidas

    @Test fun `no se aceptan ejercicios ni rutinas inventados`() {
        assertNull(Cambios.validar(Cambio.Sustituir("p_torso", "press_militar", "press_inventado", ""), rutinas))
        assertNull(Cambios.validar(Cambio.Sustituir("p_no_existe", "press_militar", "press_arnold", ""), rutinas))
        assertNull(Cambios.validar(Cambio.Sustituir("p_torso", "sentadilla", "press_arnold", ""), rutinas))
        assertNotNull(Cambios.validar(Cambio.Sustituir("p_torso", "press_militar", "press_arnold", ""), rutinas))
    }

    @Test fun `numeros fuera de rango se rechazan`() {
        assertNull(Cambios.validar(Cambio.Series("p_torso", "press_banca", 40, ""), rutinas))
        assertNull(Cambios.validar(Cambio.Reps("p_torso", "press_banca", 12, 8, ""), rutinas))
        assertNull(Cambios.validar(Cambio.Descanso("p_torso", "press_banca", 5000, ""), rutinas))
        assertNull(Cambios.validar(Cambio.Dias("p_torso", setOf(0, 9), ""), rutinas))
        assertNull(Cambios.validar(Cambio.Anadir("p_torso", "press_banca", 3, 8, 10, ""), rutinas))
    }

    @Test fun `las rutinas por tiempo no se tocan`() {
        assertNull(Cambios.validar(Cambio.Series("p_kb_emom", "kb_swing", 3, ""), rutinas))
        assertNull(Cambios.validar(Cambio.Quitar("p_kb_tabata", "kb_swing", ""), rutinas))
    }

    @Test fun `en casa no se propone material de gimnasio`() {
        assertNull(Cambios.validar(Cambio.Anadir("p_casa", "press_banca", 3, 8, 10, ""), rutinas))
        assertNotNull(Cambios.validar(Cambio.Anadir("p_casa", "fondos_banco", 3, 8, 10, ""), rutinas))
    }

    @Test fun `aplicar cambia solo lo pedido`() {
        val r = Cambios.aplicar(torso, Cambio.Sustituir("p_torso", "press_militar", "press_arnold", ""))
        assertEquals(torso.bloques.size, r.bloques.size)
        assertEquals(torso.bloques.map { it.series }, r.bloques.map { it.series })
        assertTrue(r.bloques.any { it.ejercicio == "press_arnold" } && r.bloques.none { it.ejercicio == "press_militar" })
        assertEquals(5, Cambios.aplicar(torso, Cambio.Series("p_torso", "press_banca", 5, "")).bloques.first { it.ejercicio == "press_banca" }.series)
        assertEquals(torso.bloques.size - 1, Cambios.aplicar(torso, Cambio.Quitar("p_torso", "curl_biceps", "")).bloques.size)
        assertEquals("Press militar → Press Arnold", Cambios.titulo(Cambio.Sustituir("p_torso", "press_militar", "press_arnold", ""), torso))
        assertEquals("Press de banca: 4 → 5 series", Cambios.titulo(Cambio.Series("p_torso", "press_banca", 5, ""), torso))
    }

    @Test fun `estadisticas cuentan series por semana y estancados`() {
        val hoy = LocalDate.of(2026, 11, 20)
        fun ms(dias: Long) = hoy.minusDays(dias).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        fun sesion(id: Long, dias: Long) = Sesion(id, "p_torso", "Torso", ms(dias), ms(dias) + 3_600_000, List(4) { Serie("press_banca", it, 80.0, 6) })
        val sesiones = listOf(sesion(3, 1), sesion(2, 8), sesion(1, 15), sesion(0, 40))
        val e = Entrenador.estadisticas(sesiones, listOf(torso.copy(dias = setOf(1, 4))), Perfil(), hoy)
        assertEquals(3, e.entrenos28)
        assertEquals(8, e.previstos28)
        assertEquals(3.0, e.seriesSemana[Musculo.PECHO]!!, 0.001)
        assertEquals(80.0, e.estancados["press_banca"]!!.kg, 0.001)
        assertTrue(Entrenador.contexto(e, rutinas, "").contains("press_arnold = Press Arnold"))
    }
}
