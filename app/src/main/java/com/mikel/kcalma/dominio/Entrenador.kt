package com.mikel.kcalma.dominio

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import kotlin.math.roundToInt

/**
 * Entrenador: los números los calcula este código; la IA (Gemini) solo redacta y elige cambios de una lista cerrada,
 * que aquí se validan antes de enseñarlos.
 */
object Entrenador {

    /** Lo que se le cuenta a la IA y se enseña en «En qué se basa». */
    data class Estadisticas(
        val entrenos28: Int,
        val previstos28: Int,
        /** Series hechas por semana (media de 4 semanas) de cada músculo principal. */
        val seriesSemana: Map<Musculo, Double>,
        /** Ejercicios sin mejorar en sus 3 últimas sesiones: id → mejor serie repetida. */
        val estancados: Map<String, Serie>,
        val objetivo: Objetivo,
        /** Mejor serie de la última sesión de cada ejercicio hecho. */
        val marcas: Map<String, Serie> = emptyMap(),
    )

    fun estadisticas(sesiones: List<Sesion>, rutinas: List<Rutina>, perfil: Perfil, hoy: LocalDate = LocalDate.now()): Estadisticas {
        val desde = hoy.minusDays(27).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val recientes = sesiones.filter { it.terminada && it.inicio >= desde }
        val series = mutableMapOf<Musculo, Double>()
        recientes.flatMap { it.series }.filter { it.hecha }.forEach { s ->
            Ejercicios.porId(s.ejercicio)?.principales?.forEach { m -> series[m] = (series[m] ?: 0.0) + 1 }
        }
        val dias = rutinas.flatMap { it.dias }.size
        // Estancado: las 3 últimas sesiones con ese ejercicio tienen la misma mejor serie
        val estancados = mutableMapOf<String, Serie>()
        val hechos = sesiones.filter { it.terminada }.flatMap { s -> s.series.filter { it.hecha && it.reps > 0 }.map { it.ejercicio } }.toSet()
        hechos.forEach { id ->
            val mejores = sesiones.filter { it.terminada }.mapNotNull { s -> Progresion.mejor(s.series.filter { it.ejercicio == id }) }.take(3)
            if (mejores.size == 3 && mejores.all { it.kg == mejores[0].kg && it.reps == mejores[0].reps } && Ejercicios.porId(id)?.conPeso == true) estancados[id] = mejores[0]
        }
        val marcas = hechos.mapNotNull { id -> Progresion.mejor(sesiones.ultimasSeries(id))?.let { id to it } }.toMap()
        return Estadisticas(recientes.size, dias * 4, series.mapValues { it.value / 4.0 }, estancados, perfil.objetivo, marcas)
    }

    fun nombreObjetivo(o: Objetivo) = when (o) { Objetivo.PERDER -> "Perder grasa"; Objetivo.RECOMPOSICION -> "Recomposición"; Objetivo.GANAR -> "Ganar músculo" }

    /** Contexto para la IA: rutinas con sus ids, números calculados y ejercicios que se pueden usar. */
    fun contexto(e: Estadisticas, rutinas: List<Rutina>, nutricion: String): String = buildString {
        appendLine("Objetivo: ${nombreObjetivo(e.objetivo)}.")
        appendLine("Entrenos terminados en 28 días: ${e.entrenos28}${if (e.previstos28 > 0) " de ${e.previstos28} previstos" else ""}.")
        appendLine("Series por semana por músculo: " + e.seriesSemana.entries.sortedByDescending { it.value }
            .joinToString { "${it.key.nombre} ${fmt(it.value)}" }.ifEmpty { "sin datos" } + ".")
        if (e.estancados.isNotEmpty()) appendLine("Estancados (3 sesiones igual): " + e.estancados.entries.joinToString {
            "${Ejercicios.porId(it.key)?.nombre} (${it.key}) ${Progresion.kg(it.value.kg)} kg × ${it.value.reps}"
        } + ".")
        if (e.marcas.isNotEmpty()) appendLine("Última vez en cada ejercicio (mejor serie): " + e.marcas.entries.joinToString {
            "${it.key} ${if (it.value.kg > 0) Progresion.kg(it.value.kg) + " kg × " else "× "}${it.value.reps}"
        } + ".")
        if (nutricion.isNotBlank()) appendLine(nutricion)
        appendLine()
        appendLine("Rutinas (id · nombre · formato · días 1=lunes):")
        rutinas.filter { it.formato == Formato.SERIES }.forEach { r ->
            appendLine("- ${r.id} · ${r.nombre} · ${r.seccion.nombre} · días ${r.dias.sorted().joinToString(",").ifEmpty { "ninguno" }}")
            r.bloques.forEach { b -> appendLine("    ${b.ejercicio}: ${b.series} × ${b.repsMin}-${b.repsMax}, descanso ${b.descanso} s") }
        }
        appendLine()
        Seccion.entries.forEach { s ->
            appendLine("Ejercicios que existen para ${s.nombre} (id = nombre): " + Ejercicios.de(s).joinToString("; ") { "${it.id} = ${it.nombre}" })
        }
    }

    private fun fmt(v: Double) = if (v == v.roundToInt().toDouble()) "${v.roundToInt()}" else "%.1f".format(v).replace('.', ',')

    /** Semana ISO actual («2026-W47»), para guardar la propuesta una vez por semana. */
    fun semana(hoy: LocalDate = LocalDate.now()) = "${hoy.get(IsoFields.WEEK_BASED_YEAR)}-W${hoy.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

    fun fecha(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
}

/** Cambios que puede proponer el entrenador. Todos sobre una rutina de series que existe. */
sealed interface Cambio {
    val rutina: String
    val motivo: String

    data class Sustituir(override val rutina: String, val de: String, val a: String, override val motivo: String) : Cambio
    data class Series(override val rutina: String, val ejercicio: String, val series: Int, override val motivo: String) : Cambio
    data class Reps(override val rutina: String, val ejercicio: String, val min: Int, val max: Int, override val motivo: String) : Cambio
    data class Descanso(override val rutina: String, val ejercicio: String, val segundos: Int, override val motivo: String) : Cambio
    data class Anadir(override val rutina: String, val ejercicio: String, val series: Int, val min: Int, val max: Int, override val motivo: String) : Cambio
    data class Quitar(override val rutina: String, val ejercicio: String, override val motivo: String) : Cambio
    data class Dias(override val rutina: String, val dias: Set<Int>, override val motivo: String) : Cambio
    /** Una rutina nueva entera, de series. [rutina] va vacío: aún no existe. */
    data class Crear(
        val nombre: String, val seccion: Seccion, val dias: Set<Int>, val bloques: List<BloqueRutina>, override val motivo: String,
        override val rutina: String = "",
    ) : Cambio
}

object Cambios {
    /** null si el cambio no se puede aplicar tal cual (rutina o ejercicio que no existen, números fuera de rango…). */
    fun validar(c: Cambio, rutinas: List<Rutina>): Cambio? {
        if (c is Cambio.Crear) {
            val posibles = Ejercicios.de(c.seccion).map { it.id }.toSet()
            return c.takeIf {
                c.nombre.isNotBlank() && c.nombre.length <= 30 && c.bloques.size in 2..12 &&
                    c.bloques.map { it.ejercicio }.toSet().size == c.bloques.size &&
                    c.bloques.all { it.ejercicio in posibles && it.series in 1..10 && it.repsMin in 1..60 && it.repsMax in it.repsMin..60 && it.descanso in 0..600 } &&
                    c.dias.all { it in 1..7 } && rutinas.count { !it.predefinida } < 40
            }
        }
        val r = rutinas.firstOrNull { it.id == c.rutina && it.formato == Formato.SERIES } ?: return null
        val tiene = { id: String -> r.bloques.any { it.ejercicio == id } }
        val vale = { id: String -> Ejercicios.de(r.seccion).any { it.id == id } }
        return when (c) {
            is Cambio.Sustituir -> c.takeIf { tiene(c.de) && vale(c.a) && !tiene(c.a) }
            is Cambio.Series -> c.takeIf { tiene(c.ejercicio) && c.series in 1..10 && r.bloques.first { it.ejercicio == c.ejercicio }.series != c.series }
            is Cambio.Reps -> c.takeIf { tiene(c.ejercicio) && c.min in 1..50 && c.max in c.min..50 }
            is Cambio.Descanso -> c.takeIf { tiene(c.ejercicio) && c.segundos in 0..600 }
            is Cambio.Anadir -> c.takeIf { vale(c.ejercicio) && !tiene(c.ejercicio) && c.series in 1..10 && c.min in 1..50 && c.max in c.min..50 && r.bloques.size < 15 }
            is Cambio.Quitar -> c.takeIf { tiene(c.ejercicio) && r.bloques.size > 1 }
            is Cambio.Dias -> c.takeIf { c.dias.isNotEmpty() && c.dias.all { it in 1..7 } && c.dias != r.dias }
            is Cambio.Crear -> null
        }
    }

    fun aplicar(r: Rutina, c: Cambio): Rutina = when (c) {
        is Cambio.Sustituir -> r.copy(bloques = r.bloques.map { if (it.ejercicio == c.de) it.copy(ejercicio = c.a) else it })
        is Cambio.Series -> r.copy(bloques = r.bloques.map { if (it.ejercicio == c.ejercicio) it.copy(series = c.series) else it })
        is Cambio.Reps -> r.copy(bloques = r.bloques.map { if (it.ejercicio == c.ejercicio) it.copy(repsMin = c.min, repsMax = c.max) else it })
        is Cambio.Descanso -> r.copy(bloques = r.bloques.map { if (it.ejercicio == c.ejercicio) it.copy(descanso = c.segundos) else it })
        is Cambio.Anadir -> r.copy(bloques = r.bloques + BloqueRutina(c.ejercicio, c.series, c.min, c.max, if (Ejercicios.porId(c.ejercicio)?.conPeso == true) 120 else 60))
        is Cambio.Quitar -> r.copy(bloques = r.bloques.filter { it.ejercicio != c.ejercicio })
        is Cambio.Dias -> r.copy(dias = c.dias)
        is Cambio.Crear -> r
    }

    /** La rutina que crea un [Cambio.Crear] (sin id: se le pone al guardarla). */
    fun rutinaNueva(c: Cambio.Crear) = Rutina("", c.nombre.trim(), c.seccion, c.bloques, dias = c.dias, descripcion = "Hecha con tu entrenador")

    /** Frase corta del cambio, escrita por el código con los datos de la rutina. */
    fun titulo(c: Cambio, r: Rutina?): String {
        fun n(id: String) = Ejercicios.porId(id)?.nombre ?: id
        val b = { id: String -> r?.bloques?.firstOrNull { it.ejercicio == id } }
        return when (c) {
            is Cambio.Sustituir -> "${n(c.de)} → ${n(c.a)}"
            is Cambio.Series -> "${n(c.ejercicio)}: ${b(c.ejercicio)?.series ?: "?"} → ${c.series} series"
            is Cambio.Reps -> "${n(c.ejercicio)}: ${b(c.ejercicio)?.let { "${it.repsMin}–${it.repsMax}" } ?: "?"} → ${c.min}–${c.max} reps"
            is Cambio.Descanso -> "${n(c.ejercicio)}: descanso ${b(c.ejercicio)?.descanso ?: "?"} → ${c.segundos} s"
            is Cambio.Anadir -> "Añadir ${n(c.ejercicio)}, ${c.series} × ${c.min}–${c.max}"
            is Cambio.Quitar -> "Quitar ${n(c.ejercicio)}"
            is Cambio.Crear -> c.nombre.trim()
            is Cambio.Dias -> "Días: " + c.dias.sorted().joinToString(", ") { listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")[it - 1] }
        }
    }
}

/** Un mensaje del chat con el entrenador. [estado] del cambio: 0 pendiente, 1 aceptado, 2 rechazado. */
data class MensajeChat(val mio: Boolean, val texto: String, val cambio: Cambio? = null, val estado: Int = 0)
