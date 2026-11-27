package com.mikel.kcalma.dominio

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Competición mensual entre amigos. Todo lo calcula este código a partir del diario, los entrenos y los pasos;
 * lo único que sale del móvil son el apodo y las cuatro cifras de [Marca].
 */
object Competicion {
    /** Días apuntados que hacen falta en el mes para puntuar en «Cerca del límite». */
    const val DIAS_MINIMOS = 5

    /** Las cifras del mes de una persona. [cercania]: 0-100, media de lo cerca que se queda de su objetivo los días que apunta. */
    data class Marca(
        val miembro: String, val apodo: String, val mes: String,
        val cercania: Int, val diasApuntados: Int, val entrenos: Int, val racha: Int, val pasos: Long,
        /** Detalle que se enseña al tocar a alguien; null si esa persona no lo comparte o aún no ha actualizado. */
        val kgPerdidos: Double? = null, val kcalHoy: Int? = null, val objetivoHoy: Int? = null,
        /** Cuándo se calcularon las cifras del detalle (milisegundos). */
        val momento: Long? = null,
    ) {
        /** Con pocos días apuntados la media no dice nada: no puntúa en esa lista. */
        val puntuaCercania get() = diasApuntados >= DIAS_MINIMOS
    }

    enum class Lista(val titulo: String) { CERCANIA("Cerca del límite"), ENTRENOS("Entrenos"), RACHA("Racha de días"), PASOS("Pasos") }

    data class Puesto(val marca: Marca, val posicion: Int, val valor: String)

    /**
     * Kilos perdidos desde la primera pesada hasta la última (negativo si se ha subido). Solo sale la diferencia,
     * nunca el peso. null con menos de dos pesadas.
     */
    fun kgPerdidos(pesos: List<Pesada>): Double? {
        val orden = pesos.sortedBy { it.fecha }
        if (orden.size < 2) return null
        return Math.round((orden.first().kg - orden.last().kg) * 10) / 10.0
    }

    fun mes(hoy: LocalDate = LocalDate.now()): String = YearMonth.from(hoy).toString()

    /** Lo cerca que se quedó un día: 100 clavando el objetivo, 0 si se pasa o se queda corto en el 100 % o más. */
    fun cercaniaDia(comido: Double, objetivo: Int): Double =
        if (objetivo <= 0) 0.0 else (100 - abs(comido - objetivo) / objetivo * 100).coerceIn(0.0, 100.0)

    /**
     * Mis cifras del mes de [hoy]. [dias]: kcal apuntadas por fecha; [inicios]: comienzo (millis) de cada entreno terminado;
     * [pasos]: pasos por fecha.
     */
    fun miMarca(
        miembro: String, apodo: String, hoy: LocalDate, dias: Map<String, Nutrientes>, objetivoKcal: Int,
        fechasEntrenos: List<LocalDate>, pasos: Map<String, Long>,
    ): Marca {
        val ym = YearMonth.from(hoy)
        val delMes = (1..hoy.dayOfMonth).map { ym.atDay(it) }
        val apuntados = delMes.filter { (dias[it.toString()]?.kcal ?: 0.0) > 0 }
        val cercania = apuntados.map { cercaniaDia(dias[it.toString()]!!.kcal, objetivoKcal) }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
        // Racha: la tira más larga de días seguidos apuntados dentro del mes
        var mejor = 0
        var actual = 0
        delMes.forEach { d -> if (d in apuntados) { actual++; if (actual > mejor) mejor = actual } else actual = 0 }
        return Marca(
            miembro, apodo, ym.toString(), cercania, apuntados.size,
            fechasEntrenos.count { YearMonth.from(it) == ym }, mejor,
            delMes.sumOf { pasos[it.toString()] ?: 0L },
        )
    }

    /** Una lista ordenada. Los empates comparten posición (1, 1, 3…). */
    fun lista(tipo: Lista, marcas: List<Marca>): List<Puesto> {
        fun clave(m: Marca): Long = when (tipo) {
            Lista.CERCANIA -> if (m.puntuaCercania) m.cercania.toLong() else -1L
            Lista.ENTRENOS -> m.entrenos.toLong()
            Lista.RACHA -> m.racha.toLong()
            Lista.PASOS -> m.pasos
        }
        fun texto(m: Marca): String = when (tipo) {
            Lista.CERCANIA -> if (m.puntuaCercania) "${m.cercania} %" else "pocos días"
            Lista.ENTRENOS -> "${m.entrenos}"
            Lista.RACHA -> if (m.racha == 1) "1 día" else "${m.racha} días"
            Lista.PASOS -> "%,d".format(java.util.Locale.US, m.pasos).replace(',', '.')
        }
        val orden = marcas.sortedWith(compareByDescending<Marca> { clave(it) }.thenBy { it.apodo.lowercase() })
        return orden.map { m -> Puesto(m, 1 + orden.count { clave(it) > clave(m) }, texto(m)) }
    }

    /** General: 3, 2 y 1 puntos a las posiciones 1.ª, 2.ª y 3.ª de cada lista. Quien tiene 0 en una lista no puntúa en ella. */
    fun general(marcas: List<Marca>): List<Puesto> {
        val puntos = marcas.associate { it.miembro to 0 }.toMutableMap()
        Lista.entries.forEach { tipo ->
            lista(tipo, marcas).forEach { p ->
                val vale = when (tipo) { Lista.CERCANIA -> p.marca.puntuaCercania; Lista.ENTRENOS -> p.marca.entrenos > 0; Lista.RACHA -> p.marca.racha > 0; Lista.PASOS -> p.marca.pasos > 0 }
                if (vale && p.posicion <= 3) puntos[p.marca.miembro] = puntos.getValue(p.marca.miembro) + (4 - p.posicion)
            }
        }
        val orden = marcas.sortedWith(compareByDescending<Marca> { puntos.getValue(it.miembro) }.thenBy { it.apodo.lowercase() })
        return orden.map { m ->
            val mios = puntos.getValue(m.miembro)
            Puesto(m, 1 + orden.count { puntos.getValue(it.miembro) > mios }, if (mios == 1) "1 punto" else "$mios puntos")
        }
    }
}
