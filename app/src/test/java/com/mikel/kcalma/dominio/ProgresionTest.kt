package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgresionTest {
    private val sentadilla = Ejercicios.porId("sentadilla")!!
    private val swing = Ejercicios.porId("kb_swing")!!
    private val flexiones = Ejercicios.porId("flexiones")!!
    private val b = BloqueRutina("sentadilla", series = 3, repsMin = 6, repsMax = 10)

    private fun series(kg: Double, vararg reps: Int) = reps.mapIndexed { i, r -> Serie("x", i, kg, r) }

    @Test fun `sin historial empieza abajo del rango`() {
        val p = Progresion.proponer(sentadilla, b, emptyList())
        assertEquals(3, p.series.size)
        assertTrue(p.series.all { it.reps == 6 && it.anterior == null })
    }

    @Test fun `el ejemplo de Mikel - 80x5 y luego 80x8, el siguiente dia propone 80x8`() {
        // Día 1: 80 × 5. Día 2: 80 × 8. Lo que cuenta es la última vez.
        val p = Progresion.proponer(sentadilla, b, series(80.0, 8, 8, 8))
        assertFalse(p.sube)
        assertTrue(p.series.all { it.kg == 80.0 && it.reps == 8 })
        assertEquals(8, p.series[0].anterior?.reps)
    }

    @Test fun `cada serie conserva lo suyo`() {
        val p = Progresion.proponer(sentadilla, b, series(80.0, 9, 8, 7))
        assertEquals(listOf(9, 8, 7), p.series.map { it.reps })
    }

    @Test fun `arriba del rango en todas sube 2,5 y vuelve abajo`() {
        val p = Progresion.proponer(sentadilla, b, series(80.0, 10, 10, 10))
        assertTrue(p.sube)
        assertTrue(p.series.all { it.kg == 82.5 && it.reps == 6 })
    }

    @Test fun `si una serie no llega no sube`() {
        assertFalse(Progresion.proponer(sentadilla, b, series(80.0, 10, 10, 9)).sube)
    }

    @Test fun `kettlebell sube a la siguiente que tienes`() {
        val kb = listOf(12.0, 16.0, 24.0)
        val p = Progresion.proponer(swing, b.copy(ejercicio = "kb_swing"), series(16.0, 10, 10, 10), kb)
        assertTrue(p.sube)
        assertEquals(24.0, p.series[0].kg, 0.0)
    }

    @Test fun `kettlebell sin otra mas pesada no sube`() {
        val p = Progresion.proponer(swing, b, series(24.0, 10, 10, 10), listOf(16.0, 24.0))
        assertFalse(p.sube)
        assertEquals(24.0, p.series[0].kg, 0.0)
    }

    @Test fun `sin peso no sube kilos`() {
        val p = Progresion.proponer(flexiones, b, series(0.0, 12, 12, 12))
        assertFalse(p.sube)
        assertEquals(12, p.series[0].reps)
    }

    @Test fun `record con mas repeticiones al mismo peso`() {
        val h = series(80.0, 8, 8, 7)
        assertTrue(Progresion.esRecord(Serie("x", 0, 80.0, 9), h))
        assertFalse(Progresion.esRecord(Serie("x", 0, 80.0, 8), h))
        assertFalse(Progresion.esRecord(Serie("x", 0, 75.0, 8), h))
        assertTrue(Progresion.esRecord(Serie("x", 0, 82.5, 1), h))
    }

    @Test fun `meta explica cuando se sube`() {
        assertEquals("Si haces 10 reps en las 3 series, la próxima vez te propondré 82,5 kg.", Progresion.meta(sentadilla, b, 80.0, emptyList()))
        assertNull(Progresion.meta(swing, b, 24.0, listOf(24.0)))
    }

    @Test fun `ids del catalogo son unicos y las rutinas usan ejercicios que existen`() {
        val ids = Ejercicios.todos.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        Rutinas.predefinidas.forEach { r -> r.bloques.forEach { assertTrue("${r.id}: ${it.ejercicio}", Ejercicios.porId(it.ejercicio) != null) } }
    }
}
