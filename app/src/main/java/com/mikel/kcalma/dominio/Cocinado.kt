package com.mikel.kcalma.dominio

/**
 * Cómo se ha cocinado un alimento. Lo único que cambia son los gramos de aceite que se suman,
 * con reglas fijas por cada 100 g de alimento (aproximación; nunca lo calcula la IA).
 */
enum class Cocinado(val nombre: String, val aceitePor100: Double) {
    TAL_CUAL("Tal cual", 0.0), HERVIDO("Hervido", 0.0), PLANCHA("Plancha", 3.0), HORNO("Horno", 0.0), AIRFRYER("Airfryer", 1.0), FRITO("Frito", 8.0);

    /** Gramos de aceite que se suman a [gramos] de alimento, redondeados a medio gramo. */
    fun aceite(gramos: Double): Double = Math.round(gramos * aceitePor100 / 100 * 2) / 2.0

    /** El aceite como alimento aparte en el diario, para poder quitarlo o corregirlo. */
    fun alimentoAceite() = ACEITE.copy(nombre = "Aceite de oliva (${nombre.lowercase()})")

    companion object {
        /** Aceite de oliva genérico (BEDCA): 899 kcal y 99,9 g de grasa por 100 g. */
        val ACEITE = Alimento(nombre = "Aceite de oliva", generico = true, kcal = 899.0, grasas = 99.9, saturadas = 14.3)

        /**
         * El cocinado con el que empieza un alimento reconocido. Si su nombre en la base ya dice que está frito,
         * a la plancha, rebozado…, sus datos ya lo cuentan y no se suma aceite otra vez.
         */
        fun paraAlimento(nombre: String, detectado: Cocinado): Cocinado {
            val n = nombre.lowercase()
            return if (detectado.aceitePor100 > 0 && listOf("frit", "plancha", "rebozad", "empanad", "saltead", "asad").any { it in n }) TAL_CUAL else detectado
        }

        /** A partir de lo que dice la IA o una frase («a la plancha», «frito», «en airfryer»…). */
        fun de(texto: String?): Cocinado {
            val t = (texto ?: "").lowercase()
            return when {
                "airfryer" in t || "air fryer" in t || "freidora de aire" in t -> AIRFRYER
                "frit" in t || "rebozad" in t || "empanad" in t -> FRITO
                "plancha" in t || "sarten" in t || "sartén" in t || "saltead" in t -> PLANCHA
                "horno" in t || "asad" in t -> HORNO
                "hervid" in t || "cocid" in t || "vapor" in t -> HERVIDO
                else -> TAL_CUAL
            }
        }
    }
}
