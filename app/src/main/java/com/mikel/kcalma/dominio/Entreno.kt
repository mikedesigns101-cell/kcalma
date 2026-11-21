package com.mikel.kcalma.dominio

/** Músculos del mapa del cuerpo. El nombre de la constante se guarda: no cambiarlo. */
enum class Musculo(val nombre: String) {
    PECHO("Pecho"), HOMBRO("Hombros"), BICEPS("Bíceps"), TRICEPS("Tríceps"), ANTEBRAZO("Antebrazo"),
    ABDOMEN("Abdomen"), OBLICUOS("Oblicuos"), CUADRICEPS("Cuádriceps"), ADUCTORES("Aductores"), GEMELOS("Gemelos"),
    TRAPECIO("Trapecio"), DORSAL("Espalda"), LUMBAR("Lumbar"), GLUTEO("Glúteos"), ISQUIOS("Isquios"),
}

enum class Material(val nombre: String) {
    BARRA("Barra"), MANCUERNAS("Mancuernas"), MAQUINA("Máquina"), POLEA("Polea"), KETTLEBELL("Kettlebell"), CUERPO("Peso corporal"),
}

/** Movimiento base de la figura animada: varios ejercicios comparten el mismo. */
enum class Patron {
    SWING, BISAGRA, SENTADILLA, GOBLET, ZANCADA, PRESS_BANCA, PRESS_VERTICAL, REMO, DOMINADA, JALON, CURL, TRICEPS,
    FLEXION, PLANCHA, CRUNCH, PUENTE, GEMELO, CURL_PIERNA, CLEAN, SNATCH, THRUSTER,
}

/** Sección de la pestaña Entreno. */
enum class Seccion(val nombre: String) { GIMNASIO("Gimnasio"), KETTLEBELL("Kettlebell"), CASA("Casa") }

/** Cómo se hace una rutina: series con descanso, o por tiempo (cada minuto / máximas rondas). */
enum class Formato(val nombre: String) { SERIES("Series"), EMOM("EMOM"), AMRAP("AMRAP") }

data class Ejercicio(
    /** Id estable: el historial lo usa para siempre. No cambiarlo nunca. */
    val id: String,
    val nombre: String,
    val material: Material,
    val patron: Patron,
    val principales: Set<Musculo>,
    val secundarios: Set<Musculo> = emptySet(),
    val pasos: List<Pair<String, String>> = emptyList(),
    val error: String = "",
    /** Cuánto sube el peso al progresar (kg). 0 = sin peso: se progresa en repeticiones. */
    val salto: Double = 2.5,
    /** Repeticiones por lado (zancadas, remo a una mano…). */
    val porLado: Boolean = false,
) {
    val conPeso get() = material != Material.CUERPO
}

/** Un ejercicio dentro de una rutina. En EMOM/AMRAP, [repsMin] son las repeticiones de cada vuelta. */
data class BloqueRutina(
    val ejercicio: String,
    val series: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 10,
    val descanso: Int = 120,
)

data class Rutina(
    /** "p_…" las que vienen hechas; las del usuario, un número. */
    val id: String,
    val nombre: String,
    val seccion: Seccion,
    val bloques: List<BloqueRutina>,
    val formato: Formato = Formato.SERIES,
    /** EMOM/AMRAP: minutos totales. */
    val minutos: Int = 0,
    /** Días de la semana (1 = lunes … 7 = domingo). */
    val dias: Set<Int> = emptySet(),
    val descripcion: String = "",
) {
    val predefinida get() = id.startsWith("p_")

    fun musculos(): Pair<Set<Musculo>, Set<Musculo>> {
        val ej = bloques.mapNotNull { Ejercicios.porId(it.ejercicio) }
        val p = ej.flatMap { it.principales }.toSet()
        return p to (ej.flatMap { it.secundarios }.toSet() - p)
    }

    /** Minutos aproximados: ~40 s por serie más el descanso. */
    fun duracion(): Int = if (formato != Formato.SERIES) minutos
    else bloques.sumOf { it.series * (40 + it.descanso) } / 60

    val totalSeries get() = bloques.sumOf { it.series }
}

/** Una serie apuntada. [fin] de la sesión = millis; 0 si no ha terminado. */
data class Serie(val ejercicio: String, val orden: Int, val kg: Double, val reps: Int, val hecha: Boolean = true)

data class Sesion(
    val id: Long,
    val rutinaId: String,
    val nombre: String,
    val inicio: Long,
    val fin: Long,
    val series: List<Serie>,
) {
    val terminada get() = fin > 0
}

/** Series de la última sesión terminada en la que se hizo [ej] (sin contar la sesión [sin]). */
fun List<Sesion>.ultimasSeries(ej: String, sin: Long = -1): List<Serie> =
    firstOrNull { it.id != sin && it.terminada && it.series.any { s -> s.ejercicio == ej && s.hecha } }
        ?.series?.filter { it.ejercicio == ej && it.hecha } ?: emptyList()

/** Todas las series hechas de [ej] antes (sin la sesión [sin]). */
fun List<Sesion>.historial(ej: String, sin: Long = -1): List<Serie> =
    filter { it.id != sin }.flatMap { s -> s.series.filter { it.ejercicio == ej && it.hecha } }

/** Kilos totales movidos (kg × reps). */
fun Sesion.volumen(): Double = series.filter { it.hecha }.sumOf { it.kg * it.reps }
