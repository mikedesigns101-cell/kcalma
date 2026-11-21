package com.mikel.kcalma.dominio

/**
 * Pasta, arroz, legumbres y cereales secos ganan agua al cocerlos. Si se pesan ya hechos, los gramos
 * cocidos se dividen por [factor] para calcular con los datos en seco, que son los de la etiqueta.
 * Factores medios de cocción (peso cocido / peso en seco).
 */
object Coccion {
    private val TIPOS = listOf(
        2.4 to listOf("pasta", "espagueti", "spaghetti", "macarron", "tallarin", "fideo", "fideua", "penne", "fusil", "tagliatelle",
            "linguine", "lazo", "helice", "plumas", "caracol", "estrella", "maravilla", "tiburon"),
        2.7 to listOf("arroz", "basmati"),
        2.4 to listOf("lenteja", "garbanzo", "alubia", "judion", "frijol", "habas secas", "soja texturizada"),
        2.5 to listOf("cuscus", "quinoa", "bulgur", "mijo"),
    )

    /** Palabras de algo que ya está hecho o que no se cuece. */
    private val NO = listOf(
        "cocid", "hervid", "cocinad", "conserva", "bote", "frit", "plancha", "asad", "guis", "tortita", "harina", "bebida",
        "galleta", "snack", "crema", "salsa", "ensalada", "precocid", "vasito", "microondas", "sopa", "inflad", "hinchad",
        "copos", "barrita", "con leche", "tres delicias", "a la cubana", "rellen", "fresca", "fresco", "verde",
        "pan", "hojaldre", "brisa", "quebrada", "pizza", "cacahuete", "sesamo", "tahin", "untar", "curry", "miso", "dientes",
        "chips", "tortilla", "colin", "cracker", "corteza", "vitalday", "almendra", "avellana", "chocolate", "mantequilla", "de te", "cacao",
    )

    /** Peso cocido / peso en seco, o null si el alimento no es algo seco que se cueza. */
    fun factor(a: Alimento): Double? {
        if (a.kcal < 250) return null // los secos rondan 330-390 kcal; los ya hechos, 100-160
        val n = " " + normalizar(a.nombre) + " "
        if (NO.any { n.contains(" $it") }) return null
        return TIPOS.firstOrNull { (_, l) -> l.any { n.contains(" $it") } }?.first
    }
}
