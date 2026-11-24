package com.mikel.kcalma.datos

import com.mikel.kcalma.dominio.BloqueRutina
import com.mikel.kcalma.dominio.Cambio
import com.mikel.kcalma.dominio.Ejercicios
import com.mikel.kcalma.dominio.Seccion
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
        val r = o.optString("rutina")
        val m = o.optString("motivo")
        fun t(k: String) = o.getString(k).also { require(it.isNotBlank()) }
        when (o.getString("tipo")) {
            // Una rutina nueva: se arregla lo arreglable (nombres por ids, números al rango, ejercicios que no hay en esa sección fuera)
            "crear" -> {
                val seccion = Seccion.entries.firstOrNull { it.name == o.optString("seccion").uppercase() } ?: Seccion.GIMNASIO
                val posibles = Ejercicios.de(seccion).map { it.id }.toSet()
                val b = o.getJSONArray("bloques")
                val d = o.optJSONArray("dias") ?: JSONArray()
                val bloques = (0 until b.length()).mapNotNull { b.optJSONObject(it) }.mapNotNull { x ->
                    val id = idDe(x.optString("ejercicio")).takeIf { it in posibles } ?: return@mapNotNull null
                    val min = x.optInt("min", 8).coerceIn(1, 60)
                    BloqueRutina(id, x.optInt("series", 3).coerceIn(1, 10), min, x.optInt("max", min).coerceIn(min, 60), x.optInt("descanso", 90).coerceIn(0, 600))
                }.distinctBy { it.ejercicio }.take(12)
                Cambio.Crear(
                    t("nombre").trim().take(30), seccion,
                    (0 until d.length()).map { d.optInt(it) }.filter { it in 1..7 }.toSet(), bloques, m,
                )
            }
            "sustituir" -> Cambio.Sustituir(r, idDe(t("ejercicio")), idDe(t("nuevo")), m)
            "series" -> Cambio.Series(r, idDe(t("ejercicio")), o.getInt("series"), m)
            "repeticiones" -> Cambio.Reps(r, idDe(t("ejercicio")), o.getInt("min"), o.getInt("max"), m)
            "descanso" -> Cambio.Descanso(r, idDe(t("ejercicio")), o.getInt("segundos"), m)
            "anadir" -> Cambio.Anadir(r, idDe(t("nuevo")), o.optInt("series", 3).takeIf { it > 0 } ?: 3, o.optInt("min", 8).takeIf { it > 0 } ?: 8, o.optInt("max", 12).takeIf { it > 0 } ?: 12, m)
            "quitar" -> Cambio.Quitar(r, idDe(t("ejercicio")), m)
            "dias" -> o.getJSONArray("dias").let { d -> Cambio.Dias(r, (0 until d.length()).map { d.getInt(it) }.toSet(), m) }
            else -> null
        }
    }.getOrNull()

    /** La IA a veces pone el nombre del ejercicio en vez de su id: se traduce si coincide con uno del catálogo. */
    private fun idDe(x: String): String {
        val t = x.trim()
        return Ejercicios.porId(t)?.id ?: Ejercicios.todos.firstOrNull { it.nombre.equals(t, ignoreCase = true) }?.id ?: t
    }

    fun json(c: Cambio): JSONObject = JSONObject().put("rutina", c.rutina).put("motivo", c.motivo).apply {
        when (c) {
            is Cambio.Sustituir -> put("tipo", "sustituir").put("ejercicio", c.de).put("nuevo", c.a)
            is Cambio.Series -> put("tipo", "series").put("ejercicio", c.ejercicio).put("series", c.series)
            is Cambio.Reps -> put("tipo", "repeticiones").put("ejercicio", c.ejercicio).put("min", c.min).put("max", c.max)
            is Cambio.Descanso -> put("tipo", "descanso").put("ejercicio", c.ejercicio).put("segundos", c.segundos)
            is Cambio.Anadir -> put("tipo", "anadir").put("nuevo", c.ejercicio).put("series", c.series).put("min", c.min).put("max", c.max)
            is Cambio.Quitar -> put("tipo", "quitar").put("ejercicio", c.ejercicio)
            is Cambio.Dias -> put("tipo", "dias").put("dias", JSONArray(c.dias.sorted()))
            is Cambio.Crear -> put("tipo", "crear").put("nombre", c.nombre).put("seccion", c.seccion.name).put("dias", JSONArray(c.dias.sorted()))
                .put("bloques", JSONArray(c.bloques.map {
                    JSONObject().put("ejercicio", it.ejercicio).put("series", it.series).put("min", it.repsMin).put("max", it.repsMax).put("descanso", it.descanso)
                }))
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
