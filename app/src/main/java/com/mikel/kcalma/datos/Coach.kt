package com.mikel.kcalma.datos

import com.mikel.kcalma.dominio.Cambio
import org.json.JSONArray
import org.json.JSONObject

/** Paso de los cambios del entrenador a JSON y vuelta (lo que devuelve Gemini y lo que se guarda). */
object Coach {
    /** Propuesta de una semana. [resueltos]: índices de los cambios ya aceptados o descartados. */
    data class Propuesta(val semana: String, val resumen: String, val cambios: List<Cambio>, val resueltos: Set<Int> = emptySet()) {
        val pendientes get() = cambios.indices.filter { it !in resueltos }
    }

    /** null si faltan campos. No comprueba que la rutina o el ejercicio existan: eso lo hace Cambios.validar. */
    fun cambioDe(o: JSONObject): Cambio? = runCatching {
        val r = o.getString("rutina")
        val m = o.optString("motivo")
        fun t(k: String) = o.getString(k).also { require(it.isNotBlank()) }
        when (o.getString("tipo")) {
            "sustituir" -> Cambio.Sustituir(r, t("ejercicio"), t("nuevo"), m)
            "series" -> Cambio.Series(r, t("ejercicio"), o.getInt("series"), m)
            "repeticiones" -> Cambio.Reps(r, t("ejercicio"), o.getInt("min"), o.getInt("max"), m)
            "descanso" -> Cambio.Descanso(r, t("ejercicio"), o.getInt("segundos"), m)
            "anadir" -> Cambio.Anadir(r, t("nuevo"), o.optInt("series", 3).takeIf { it > 0 } ?: 3, o.optInt("min", 8).takeIf { it > 0 } ?: 8, o.optInt("max", 12).takeIf { it > 0 } ?: 12, m)
            "quitar" -> Cambio.Quitar(r, t("ejercicio"), m)
            "dias" -> o.getJSONArray("dias").let { d -> Cambio.Dias(r, (0 until d.length()).map { d.getInt(it) }.toSet(), m) }
            else -> null
        }
    }.getOrNull()

    fun json(c: Cambio): JSONObject = JSONObject().put("rutina", c.rutina).put("motivo", c.motivo).apply {
        when (c) {
            is Cambio.Sustituir -> put("tipo", "sustituir").put("ejercicio", c.de).put("nuevo", c.a)
            is Cambio.Series -> put("tipo", "series").put("ejercicio", c.ejercicio).put("series", c.series)
            is Cambio.Reps -> put("tipo", "repeticiones").put("ejercicio", c.ejercicio).put("min", c.min).put("max", c.max)
            is Cambio.Descanso -> put("tipo", "descanso").put("ejercicio", c.ejercicio).put("segundos", c.segundos)
            is Cambio.Anadir -> put("tipo", "anadir").put("nuevo", c.ejercicio).put("series", c.series).put("min", c.min).put("max", c.max)
            is Cambio.Quitar -> put("tipo", "quitar").put("ejercicio", c.ejercicio)
            is Cambio.Dias -> put("tipo", "dias").put("dias", JSONArray(c.dias.sorted()))
        }
    }

    fun json(p: Propuesta): String = JSONObject().put("semana", p.semana).put("resumen", p.resumen)
        .put("cambios", JSONArray(p.cambios.map { json(it) })).put("resueltos", JSONArray(p.resueltos.sorted())).toString()

    fun propuestaDe(s: String): Propuesta? = if (s.isBlank()) null else runCatching {
        val o = JSONObject(s)
        val c = o.getJSONArray("cambios")
        val r = o.optJSONArray("resueltos") ?: JSONArray()
        Propuesta(
            o.getString("semana"), o.optString("resumen"),
            (0 until c.length()).mapNotNull { cambioDe(c.getJSONObject(it)) },
            (0 until r.length()).map { r.getInt(it) }.toSet(),
        )
    }.getOrNull()
}
