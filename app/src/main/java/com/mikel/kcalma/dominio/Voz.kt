package com.mikel.kcalma.dominio

/**
 * Interpreta lo dictado en el buscador: «200 gramos de arroz hervido», «dos huevos», «medio plátano».
 * Separa la cantidad del alimento para buscar solo el alimento y añadir la cantidad dicha.
 */
object Voz {
    /** [gramos] si se dijo un peso; si no, [unidades] (raciones o piezas). */
    data class Cantidad(val gramos: Double? = null, val unidades: Double? = null) {
        val etiqueta: String
            get() = gramos?.let { "${decimal(it)} g" } ?: unidades?.let { u -> if (u == 1.0) "1 ración" else "${decimal(u)} raciones" } ?: ""

        /** La entrada que se añadiría de [a] con esta cantidad. */
        fun entrada(a: Alimento, comida: ComidaDia): Entrada {
            gramos?.let { return Entrada(0, "", comida, a, it, Unidad.GRAMOS, it) }
            val u = unidades ?: 1.0
            val racion = a.racionValida
            return if (racion != null) Entrada(0, "", comida, a, u * racion, Unidad.RACION, u)
            else Entrada(0, "", comida, a, u * 100, Unidad.GRAMOS, u * 100)
        }
    }

    data class Dictado(val texto: String, val cantidad: Cantidad?)

    private val NUMEROS = mapOf(
        "un" to 1.0, "una" to 1.0, "uno" to 1.0, "medio" to .5, "media" to .5, "dos" to 2.0, "tres" to 3.0,
        "cuatro" to 4.0, "cinco" to 5.0, "seis" to 6.0, "siete" to 7.0, "ocho" to 8.0, "nueve" to 9.0, "diez" to 10.0,
        "cien" to 100.0, "ciento" to 100.0, "doscientos" to 200.0, "trescientos" to 300.0, "cuatrocientos" to 400.0, "quinientos" to 500.0,
    )
    private const val NUM = "(\\d+(?:[.,]\\d+)?|un|una|uno|medio|media|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|cien|ciento|doscientos|trescientos|cuatrocientos|quinientos)"
    private val PESO = Regex("$NUM\\s*(kilos?|kg|gramos?|gr|g)\\b(\\s+de)?")
    private val PIEZAS = Regex("^$NUM\\s+(de\\s+)?")

    private fun numero(s: String) = NUMEROS[s] ?: s.replace(',', '.').toDoubleOrNull()

    fun interpretar(frase: String): Dictado {
        var t = frase.lowercase().trim().replace(Regex("\\s+"), " ")
        PESO.find(t)?.let { m ->
            val n = numero(m.groupValues[1]) ?: return@let
            val kilos = m.groupValues[2].startsWith("k")
            t = t.removeRange(m.range).replace(Regex("\\s+"), " ").trim()
            return Dictado(t, Cantidad(gramos = if (kilos) n * 1000 else n))
        }
        PIEZAS.find(t)?.let { m ->
            val n = numero(m.groupValues[1]) ?: return@let
            t = t.removeRange(m.range).trim()
            // «dos huevos» → «huevo»: el buscador encuentra mejor el singular
            if (n > 1) t = t.split(' ').joinToString(" ") { w -> if (w.length > 3 && w.endsWith("os") || w.length > 3 && w.endsWith("as")) w.dropLast(1) else w }
            return Dictado(t, Cantidad(unidades = n))
        }
        return Dictado(t, null)
    }

    private fun decimal(x: Double) = if (x % 1.0 == 0.0) x.toLong().toString() else x.toString().replace('.', ',')
}
