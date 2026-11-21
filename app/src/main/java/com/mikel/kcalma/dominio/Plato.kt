package com.mikel.kcalma.dominio

/**
 * Plato casero del usuario: los ingredientes de la olla entera y para cuántas raciones da.
 * Se apunta como un alimento más, con la ración = olla / raciones.
 */
data class Plato(val id: Long, val nombre: String, val raciones: Int, val ingredientes: List<Ingrediente>) {
    val gramosTotales: Double get() = ingredientes.sumOf { it.gramos }
    val total: Nutrientes get() = ingredientes.fold(Nutrientes()) { s, i -> s + i.alimento.nutrientes(i.gramos) }
    val porRacion: Nutrientes get() = total.let { n -> val r = raciones.coerceAtLeast(1); Nutrientes(n.kcal / r, n.proteinas / r, n.hidratos / r, n.grasas / r) }

    /** El plato como alimento: valores por 100 g de la mezcla y la ración de este plato. */
    val alimento: Alimento
        get() {
            val g = gramosTotales.coerceAtLeast(1.0)
            val n = total
            return Alimento(
                nombre = nombre, marca = MARCA, racion = "1 ración", racionG = g / raciones.coerceAtLeast(1),
                kcal = n.kcal * 100 / g, proteinas = n.proteinas * 100 / g, hidratos = n.hidratos * 100 / g, grasas = n.grasas * 100 / g,
            )
        }

    companion object { const val MARCA = "Plato casero" }
}
