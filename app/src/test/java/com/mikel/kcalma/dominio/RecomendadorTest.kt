package com.mikel.kcalma.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecomendadorTest {
    private fun receta(nombre: String, kcal100: Double, gramos: Double, alergenos: Set<String> = emptySet(), min: Int = 20) = Receta(
        nombre, "🍽️", setOf(ComidaDia.CENA), min,
        listOf(Ingrediente(Alimento(nombre, kcal = kcal100, proteinas = 20.0), gramos)), emptyList(), alergenos,
    )

    private val merluza = receta("Merluza", 100.0, 400.0, setOf("Pescado"))
    private val pollo = receta("Pollo", 100.0, 600.0, min = 10)

    @Test
    fun quitaLoQueDaAlergia() {
        val ideas = Recomendador.recomendar(listOf(merluza, pollo), Perfil(alergias = setOf("Pescado")), ComidaDia.CENA, 2000, Nutrientes(500.0, 40.0))
        assertEquals(listOf("Pollo"), ideas.map { it.receta.nombre })
    }

    @Test
    fun ajustaLaCantidadALoQueQueda() {
        val idea = Recomendador.recomendar(listOf(merluza), Perfil(), ComidaDia.CENA, 2000, Nutrientes(500.0, 40.0)).single()
        assertEquals(500.0, idea.receta.nutrientes.kcal, 5.0)
        assertTrue(idea.encaja)
    }

    @Test
    fun alergenoDeLaIaConOtroNombre() {
        val ia = receta("Bechamel", 100.0, 500.0, setOf("leche")).copy(deIa = true)
        val ideas = Recomendador.recomendar(listOf(ia), Perfil(alergias = setOf("Lactosa")), ComidaDia.CENA, 2000, Nutrientes(500.0, 40.0))
        assertTrue(ideas.isEmpty())
    }

    @Test
    fun filtroRapidas() {
        val ideas = Recomendador.recomendar(listOf(merluza, pollo), Perfil(), ComidaDia.CENA, 2000, Nutrientes(500.0, 40.0), FiltrosIdeas(rapidas = true))
        assertEquals(listOf("Pollo"), ideas.map { it.receta.nombre })
    }
}
