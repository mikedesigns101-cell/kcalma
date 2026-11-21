package com.mikel.kcalma.dominio

import java.text.Normalizer
import kotlin.math.roundToInt

/** Las 5 comidas del día. El orden (ordinal) se guarda en el diario: no reordenar. */
enum class ComidaDia(val icono: String, val nombre: String) {
    DESAYUNO("☕", "Desayuno"),
    MEDIA_MANANA("🍎", "Media mañana"),
    COMIDA("🍽️", "Comida"),
    MERIENDA("🥪", "Merienda"),
    CENA("🌙", "Cena");

    companion object {
        /** La comida que toca según la hora. */
        fun segunHora(hora: Int): ComidaDia = when (hora) {
            in 5..10 -> DESAYUNO
            in 11..12 -> MEDIA_MANANA
            in 13..16 -> COMIDA
            in 17..19 -> MERIENDA
            else -> CENA
        }
    }
}

/**
 * Un alimento con sus valores por 100 g. En el diario se guarda una copia completa,
 * nunca el id del catálogo, porque los ids cambian al regenerar la base de datos.
 */
data class Alimento(
    val nombre: String,
    val marca: String? = null,
    val supermercado: String? = null,
    val codigoBarras: String? = null,
    val generico: Boolean = false,
    val racion: String? = null,
    val racionG: Double? = null,
    val envaseG: Double? = null,
    val kcal: Double = 0.0,
    val proteinas: Double = 0.0,
    val hidratos: Double = 0.0,
    val grasas: Double = 0.0,
    val azucares: Double? = null,
    val saturadas: Double? = null,
    val fibra: Double? = null,
    val sal: Double? = null,
) {
    /** Ración útil: se ignoran raciones absurdas de 1-4 g que vienen de Open Food Facts. */
    val racionValida: Double? get() = racionG?.takeIf { it >= 5 }

    /** Gramos que se añaden con el botón + de la lista. */
    val gramosRapidos: Double get() = racionValida ?: 100.0

    fun nutrientes(gramos: Double) = Nutrientes(
        kcal * gramos / 100, proteinas * gramos / 100, hidratos * gramos / 100, grasas * gramos / 100,
    )

    val subtitulo: String
        get() = listOfNotNull(if (generico) "Genérico" else marca).joinToString(" · ")
}

data class Nutrientes(val kcal: Double = 0.0, val proteinas: Double = 0.0, val hidratos: Double = 0.0, val grasas: Double = 0.0) {
    operator fun plus(o: Nutrientes) = Nutrientes(kcal + o.kcal, proteinas + o.proteinas, hidratos + o.hidratos, grasas + o.grasas)
    val kcalInt get() = kcal.roundToInt()
}

/** Cómo se eligió la cantidad en la ficha. */
/** COCIDO: [Entrada.cantidad] son gramos ya cocinados y [Entrada.gramos] su equivalente en seco (ver [Coccion]). */
enum class Unidad { GRAMOS, RACION, ENVASE, COCIDO }

/** Un alimento apuntado en el diario. */
data class Entrada(
    val id: Long,
    val fecha: String,
    val comida: ComidaDia,
    val alimento: Alimento,
    val gramos: Double,
    val unidad: Unidad,
    val cantidad: Double,
) {
    val nutrientes get() = alimento.nutrientes(gramos)

    /** "150 g", "1 ud", "2 env". */
    val etiquetaCantidad: String
        get() = when (unidad) {
            Unidad.GRAMOS -> "${decimal(gramos)} g"
            Unidad.RACION -> if (cantidad == 1.0) "1 ración" else "${decimal(cantidad)} raciones"
            Unidad.ENVASE -> if (cantidad == 1.0) "1 envase" else "${decimal(cantidad)} envases"
            Unidad.COCIDO -> "${decimal(cantidad)} g cocido"
        }
}

/** Igual que `normalizar` de datos/scripts/comun.py: si no coinciden, la búsqueda no encuentra nada. */
fun normalizar(texto: String): String {
    val sinTildes = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFKD)
        .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
    return sinTildes.replace(Regex("[^a-z0-9]+"), " ").trim()
}

/** Gramos de un texto de envase: "500 g" -> 500, "1 kg" -> 1000, "1 l" -> 1000, "33 cl" -> 330. */
fun gramosDeCantidad(texto: String?): Double? {
    if (texto == null) return null
    val m = Regex("""^\s*(\d+(?:[.,]\d+)?)\s*(kg|g|gr|gramos|l|litro|litros|ml|cl)\b""", RegexOption.IGNORE_CASE)
        .find(texto) ?: return null
    val n = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val g = when (m.groupValues[2].lowercase()) {
        "kg", "l", "litro", "litros" -> n * 1000
        "cl" -> n * 10
        else -> n
    }
    return g.takeIf { it in 5.0..10000.0 }
}

/** 7.5 -> "7,5" · 150.0 -> "150". */
fun decimal(n: Double): String {
    val r = (n * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString().replace('.', ',')
}

/** Emoji aproximado según el nombre, para dar una pista visual en las listas. */
fun emojiDe(nombre: String): String {
    val n = normalizar(nombre)
    val tabla = listOf(
        "yogur" to "🥛", "leche" to "🥛", "kefir" to "🥛", "queso" to "🧀", "huevo" to "🥚",
        "pollo" to "🍗", "pavo" to "🍗", "ternera" to "🥩", "cerdo" to "🥩", "jamon" to "🥓", "chorizo" to "🥓",
        "atun" to "🐟", "salmon" to "🐟", "merluza" to "🐟", "gamba" to "🦐", "pan" to "🍞", "tostada" to "🍞",
        "arroz" to "🍚", "pasta" to "🍝", "macarron" to "🍝", "espagueti" to "🍝", "avena" to "🥣", "cereal" to "🥣",
        "platano" to "🍌", "manzana" to "🍎", "naranja" to "🍊", "fresa" to "🍓", "uva" to "🍇", "pera" to "🍐",
        "tomate" to "🍅", "patata" to "🥔", "zanahoria" to "🥕", "lechuga" to "🥬", "ensalada" to "🥗",
        "pimiento" to "🫑", "brocoli" to "🥦", "aguacate" to "🥑", "guacamole" to "🥑", "aceite" to "🫒",
        "aceituna" to "🫒", "chocolate" to "🍫", "cacao" to "🍫", "galleta" to "🍪", "cafe" to "☕",
        "pizza" to "🍕", "hamburguesa" to "🍔", "cerveza" to "🍺", "vino" to "🍷", "zumo" to "🧃",
        "agua" to "💧", "garbanzo" to "🫘", "lenteja" to "🫘", "alubia" to "🫘", "frutos secos" to "🥜",
        "cacahuete" to "🥜", "almendra" to "🥜", "nuez" to "🥜", "miel" to "🍯", "helado" to "🍦",
    )
    return tabla.firstOrNull { (clave, _) -> n.contains(clave) }?.second ?: "🍽️"
}
