package com.mikel.kcalma.dominio

import com.mikel.kcalma.dominio.Competicion.Lista
import com.mikel.kcalma.dominio.Competicion.Marca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CompeticionTest {
    private fun m(apodo: String, cercania: Int, dias: Int, entrenos: Int, racha: Int, pasos: Long) =
        Marca(apodo.lowercase(), apodo, "2026-11", cercania, dias, entrenos, racha, pasos)

    @Test fun `mis cifras del mes salen del diario los entrenos y los pasos`() {
        val hoy = LocalDate.of(2026, 11, 10)
        val dias = mapOf(
            "2026-11-01" to Nutrientes(kcal = 2000.0), "2026-11-02" to Nutrientes(kcal = 1800.0), "2026-11-03" to Nutrientes(kcal = 2200.0),
            "2026-11-05" to Nutrientes(kcal = 2000.0), "2026-10-31" to Nutrientes(kcal = 5000.0), "2026-11-11" to Nutrientes(kcal = 2000.0),
        )
        val entrenos = listOf(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 10, 30))
        val pasos = mapOf("2026-11-01" to 8000L, "2026-11-10" to 4000L, "2026-10-31" to 99999L)
        val mia = Competicion.miMarca("abc", "Mikel", hoy, dias, 2000, entrenos, pasos)
        assertEquals(4, mia.diasApuntados)
        assertEquals(95, mia.cercania) // 100, 90, 90 y 100
        assertEquals(3, mia.racha)     // 1, 2 y 3 de noviembre
        assertEquals(2, mia.entrenos)
        assertEquals(12000L, mia.pasos)
        assertFalse(mia.puntuaCercania)
    }

    @Test fun `kilos perdidos desde la primera pesada, sin dar el peso`() {
        assertNull(Competicion.kgPerdidos(emptyList()))
        assertNull(Competicion.kgPerdidos(listOf(Pesada("2026-10-01", 85.0))))
        // Da igual el orden en que lleguen: cuenta la más antigua contra la más reciente
        assertEquals(3.2, Competicion.kgPerdidos(listOf(Pesada("2026-11-20", 81.8), Pesada("2026-10-01", 85.0), Pesada("2026-11-01", 83.0)))!!, 0.001)
        assertEquals(-1.5, Competicion.kgPerdidos(listOf(Pesada("2026-10-01", 70.0), Pesada("2026-11-01", 71.5)))!!, 0.001)
    }

    @Test fun `cercania de un dia`() {
        assertEquals(100.0, Competicion.cercaniaDia(2000.0, 2000), 0.001)
        assertEquals(75.0, Competicion.cercaniaDia(2500.0, 2000), 0.001)
        assertEquals(0.0, Competicion.cercaniaDia(5000.0, 2000), 0.001)
    }

    @Test fun `listas con empates y pocos dias`() {
        val marcas = listOf(m("Ane", 91, 20, 14, 21, 254_900), m("Mikel", 94, 18, 9, 18, 212_300), m("Jon", 83, 12, 14, 7, 281_400), m("Iker", 99, 3, 6, 9, 148_000))
        val cerca = Competicion.lista(Lista.CERCANIA, marcas)
        assertEquals(listOf("Mikel", "Ane", "Jon", "Iker"), cerca.map { it.marca.apodo })
        assertEquals("pocos días", cerca.last().valor)
        val entrenos = Competicion.lista(Lista.ENTRENOS, marcas)
        assertEquals(listOf(1, 1, 3, 4), entrenos.map { it.posicion })
        assertEquals("281.400", Competicion.lista(Lista.PASOS, marcas).first().valor)
    }

    @Test fun `general da 3 2 y 1 puntos por lista`() {
        val marcas = listOf(m("Ane", 91, 20, 14, 21, 254_900), m("Mikel", 94, 18, 9, 18, 212_300), m("Jon", 83, 12, 11, 7, 281_400))
        val general = Competicion.general(marcas)
        // Ane: 2 + 3 + 3 + 2 = 10 · Mikel: 3 + 1 + 2 + 1 = 7 · Jon: 1 + 2 + 1 + 3 = 7
        assertEquals("Ane", general[0].marca.apodo)
        assertEquals("10 puntos", general[0].valor)
        assertEquals(listOf(1, 2, 2), general.map { it.posicion })
        // Quien no ha hecho nada no puntúa aunque quede «tercero»
        assertEquals("0 puntos", Competicion.general(listOf(m("Solo", 0, 0, 0, 0, 0))).single().valor)
    }

    @Test fun `el codigo del grupo cifra y solo ese codigo descifra`() {
        val codigo = CifradoGrupo.normalizar(CifradoGrupo.codigoNuevo())!!
        assertEquals(codigo, CifradoGrupo.normalizar(CifradoGrupo.bonito(codigo).lowercase() + " "))
        assertNull(CifradoGrupo.normalizar("hola"))
        assertNull(CifradoGrupo.normalizar("KCAL-0000-1111-OOOO"))
        val llave = CifradoGrupo.llave(codigo)
        val cifrado = CifradoGrupo.cifrar(llave, "{\"a\":\"Mikel\",\"p\":212300}")
        assertFalse(cifrado.contains("Mikel"))
        assertEquals("{\"a\":\"Mikel\",\"p\":212300}", CifradoGrupo.descifrar(llave, cifrado))
        val otro = CifradoGrupo.normalizar(CifradoGrupo.codigoNuevo())!!
        assertNull(CifradoGrupo.descifrar(CifradoGrupo.llave(otro), cifrado))
        assertNull(CifradoGrupo.descifrar(llave, cifrado.dropLast(4) + "AAAA"))
        assertNull(CifradoGrupo.descifrar(llave, "no es base64"))
        assertNotEquals(CifradoGrupo.id(codigo), CifradoGrupo.id(otro))
        assertTrue(Regex("[0-9a-f]{16}").matches(CifradoGrupo.id(codigo)))
    }
}
