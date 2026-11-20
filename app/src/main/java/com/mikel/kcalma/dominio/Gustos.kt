package com.mikel.kcalma.dominio

/**
 * Categorías de gustos que ofrece la app. Cada una lleva las raíces de palabra que la delatan en el nombre
 * de una receta o de un ingrediente, para que «no me gustan las setas» quite también los champiñones.
 * Las raíces de menos de 4 letras solo valen como palabra entera.
 */
data class CategoriaGusto(val nombre: String, val emoji: String, val raices: List<String>)

val CATEGORIAS_GUSTO = listOf(
    CategoriaGusto("Setas", "🍄", listOf("seta", "champi", "boletu", "niscal", "shiitake", "portobello", "hongo", "trufa", "rebozuelo")),
    CategoriaGusto("Pescado", "🐟", listOf("pescad", "merluz", "bacala", "salmon", "atun", "bonito", "sardin", "dorada", "lubina", "gallo", "rape", "jurel", "caballa", "boqueron", "anchoa", "emperador", "pez", "lenguado", "trucha", "panga", "abadejo", "rodaballo")),
    CategoriaGusto("Marisco", "🦐", listOf("marisc", "gamba", "langostin", "mejillon", "almeja", "chirla", "berberech", "calamar", "sepia", "pulpo", "cangrej", "necora", "bogavante", "navaja", "vieira", "chipiron", "surimi", "cigala")),
    CategoriaGusto("Cerdo", "🐖", listOf("cerdo", "jamon", "chorizo", "lomo", "panceta", "bacon", "beicon", "salchichon", "morcilla", "costilla", "secreto", "presa", "fuet", "sobrasada", "lacon")),
    CategoriaGusto("Ternera", "🐄", listOf("ternera", "vaca", "buey", "entrecot", "solomillo", "hamburgues", "carne picada")),
    CategoriaGusto("Cordero", "🐑", listOf("cordero", "lechazo")),
    CategoriaGusto("Pollo y pavo", "🍗", listOf("pollo", "pavo", "pechuga", "contramuslo")),
    CategoriaGusto("Vísceras", "🫀", listOf("higado", "riñon", "rinon", "callos", "sesos", "molleja", "corazon")),
    CategoriaGusto("Huevo", "🥚", listOf("huevo", "tortilla", "revuelto")),
    CategoriaGusto("Queso", "🧀", listOf("queso", "mozzarella", "parmesano", "requeson")),
    CategoriaGusto("Leche y yogur", "🥛", listOf("leche", "yogur", "kefir", "nata", "bechamel")),
    CategoriaGusto("Legumbres", "🫘", listOf("lenteja", "garbanzo", "alubia", "judias blancas", "fabada", "haba", "hummus", "soja")),
    CategoriaGusto("Arroz", "🍚", listOf("arroz", "paella", "risotto")),
    CategoriaGusto("Pasta", "🍝", listOf("pasta", "macarron", "espagueti", "spaghetti", "fideo", "lasaña", "lasana", "tallarin", "cuscus", "canelon")),
    CategoriaGusto("Pan", "🥖", listOf("pan", "tostada", "bocadillo", "sandwich", "sándwich", "pita", "wrap", "tortita de trigo")),
    CategoriaGusto("Cebolla", "🧅", listOf("cebolla", "cebolleta", "puerro", "chalota")),
    CategoriaGusto("Ajo", "🧄", listOf("ajo")),
    CategoriaGusto("Pimiento", "🫑", listOf("pimiento", "pimenton", "piquillo", "pisto")),
    CategoriaGusto("Tomate", "🍅", listOf("tomate", "gazpacho", "salmorejo", "pisto")),
    CategoriaGusto("Coles y brócoli", "🥦", listOf("brocoli", "brecol", "coliflor", "col", "repollo", "lombarda", "coles", "kale")),
    CategoriaGusto("Espinacas y acelgas", "🥬", listOf("espinaca", "acelga")),
    CategoriaGusto("Berenjena y calabacín", "🍆", listOf("berenjena", "calabacin")),
    CategoriaGusto("Aguacate", "🥑", listOf("aguacate", "guacamole")),
    CategoriaGusto("Aceitunas", "🫒", listOf("aceituna", "oliva negra")),
    CategoriaGusto("Frutos secos", "🥜", listOf("nuez", "nueces", "almendra", "avellana", "cacahuete", "pistacho", "anacardo", "pipas")),
    CategoriaGusto("Picante", "🌶️", listOf("picante", "guindilla", "chile", "jalapeño", "jalapeno", "cayena", "brava")),
)

object Gustos {
    /** Categoría cuyo nombre es lo que escribió el usuario («setas», «Pescado»…), o null. */
    fun categoriaDe(termino: String): CategoriaGusto? {
        val n = normalizar(termino).removeSuffix("s")
        if (n.isEmpty()) return null
        return CATEGORIAS_GUSTO.firstOrNull { normalizar(it.nombre).removeSuffix("s") == n }
    }

    /**
     * Raíces a buscar: las de la categoría si es su nombre; si no, la raíz conocida que encaje con lo escrito
     * («champiñones» → «champi») o, si no hay, el propio término.
     */
    fun raices(terminos: Set<String>): Set<String> = terminos.flatMap { t ->
        val n = normalizar(t)
        categoriaDe(t)?.raices?.map(::normalizar)
            ?: CATEGORIAS_GUSTO.flatMap { it.raices }.map(::normalizar).filter { coincide(" $n ", it) }.ifEmpty { null }
            ?: listOf(if (n.length > 5) n.dropLast(1) else n)
    }.filter { it.isNotEmpty() }.toSet()

    /** ¿El texto (nombre de receta, ingredientes…) menciona alguno de esos gustos? */
    fun menciona(texto: String, terminos: Set<String>): Boolean {
        if (terminos.isEmpty()) return false
        val n = " " + normalizar(texto) + " "
        return raices(terminos).any { r -> coincide(n, r) }
    }

    /** Para el prompt de la IA: «setas (seta, champi, boletu…)». */
    fun describir(terminos: Set<String>): String = terminos.joinToString { t ->
        val c = categoriaDe(t)
        if (c == null) t else "${c.nombre.lowercase()} (${c.raices.joinToString()})"
    }

    private fun coincide(texto: String, raiz: String): Boolean {
        val palabras = " " + texto.trim() + " "
        return if (raiz.length < 4 || ' ' in raiz) palabras.contains(" $raiz ") || palabras.contains(" ${raiz}s ") || (' ' in raiz && palabras.contains(" $raiz"))
        else palabras.contains(" $raiz")
    }
}
