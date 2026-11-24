package com.mikel.kcalma.dominio

import kotlin.math.abs
import kotlin.math.max

/**
 * Alimentos de la comunidad: lo que un usuario registra y llega a los demás. Estas comprobaciones son las mismas
 * que hace el robot de GitHub (comunidad/robot.py); la app las repite al mandar y al recibir, para no fiarse del archivo.
 */
object Compartidos {
    /** Marca con la que se distinguen en las listas y en la ficha. */
    const val ORIGEN = "Comunidad"

    /** GTIN de 8, 12, 13 o 14 cifras con su dígito de control. */
    fun codigoValido(c: String?): Boolean {
        if (c == null || !Regex("\\d{8}|\\d{12,14}").matches(c)) return false
        val cifras = c.map { it - '0' }
        val suma = cifras.dropLast(1).reversed().withIndex().sumOf { (i, d) -> d * if (i % 2 == 0) 3 else 1 }
        return (10 - suma % 10) % 10 == cifras.last()
    }

    /** Números posibles y coherentes entre sí. */
    fun valido(a: Alimento): Boolean {
        val nombre = a.nombre.trim()
        if (nombre.length !in 2..80 || nombre.any { it.code < 32 || it == '<' || it == '>' } || "http" in nombre.lowercase()) return false
        if (!codigoValido(a.codigoBarras)) return false
        if (a.kcal !in 0.0..900.0 || listOf(a.proteinas, a.hidratos, a.grasas).any { it !in 0.0..100.0 }) return false
        if (a.proteinas + a.hidratos + a.grasas > 100.5) return false
        val calculadas = 4 * a.proteinas + 4 * a.hidratos + 9 * a.grasas
        if (abs(calculadas - a.kcal) > max(25.0, 0.20 * max(a.kcal, calculadas))) return false
        if (listOfNotNull(a.azucares, a.saturadas, a.fibra, a.sal).any { it !in 0.0..100.0 }) return false
        if ((a.azucares ?: 0.0) > a.hidratos + 0.5 || (a.saturadas ?: 0.0) > a.grasas + 0.5) return false
        return true
    }
}
