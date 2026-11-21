package com.mikel.kcalma.datos

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mikel.kcalma.dominio.BloqueRutina
import com.mikel.kcalma.dominio.Formato
import com.mikel.kcalma.dominio.Rutina
import com.mikel.kcalma.dominio.Rutinas
import com.mikel.kcalma.dominio.Seccion
import com.mikel.kcalma.dominio.Serie
import com.mikel.kcalma.dominio.Sesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Ajustes del entreno: las kettlebells que se tienen. */
data class AjustesEntreno(val kettlebells: List<Double> = listOf(12.0, 16.0, 24.0))

/**
 * Entrenos (entreno.db): rutinas del usuario, sesiones y series. Cada serie se guarda al marcarla,
 * así que una sesión a medias sobrevive a que Android cierre la app.
 */
class Entrenos(contexto: Context) : SQLiteOpenHelper(contexto, "entreno.db", null, 1) {
    private val cambios = MutableStateFlow(0)
    private fun avisar() { cambios.value++ }

    override fun onCreate(bd: SQLiteDatabase) {
        bd.execSQL("CREATE TABLE rutinas (id TEXT PRIMARY KEY, datos TEXT NOT NULL, creado INTEGER)")
        bd.execSQL("CREATE TABLE sesiones (id INTEGER PRIMARY KEY, rutina TEXT, nombre TEXT, inicio INTEGER, fin INTEGER)")
        bd.execSQL("CREATE TABLE series (id INTEGER PRIMARY KEY, sesion INTEGER, ejercicio TEXT, orden INTEGER, kg REAL, reps INTEGER, hecha INTEGER)")
        bd.execSQL("CREATE INDEX idx_series_ej ON series(ejercicio)")
        bd.execSQL("CREATE TABLE ajustes (clave TEXT PRIMARY KEY, valor TEXT)")
    }

    override fun onUpgrade(bd: SQLiteDatabase, antes: Int, ahora: Int) {}

    // ---------- Ajustes ----------

    fun ajustes(): Flow<AjustesEntreno> = cambios.map { leerAjustes() }.flowOn(Dispatchers.IO)

    fun leerAjustes(): AjustesEntreno {
        val m = mutableMapOf<String, String>()
        readableDatabase.rawQuery("SELECT clave, valor FROM ajustes", null).use { c -> while (c.moveToNext()) m[c.getString(0)] = c.getString(1) }
        val d = AjustesEntreno()
        return AjustesEntreno(
            m["kettlebells"]?.split(",")?.mapNotNull { it.toDoubleOrNull() } ?: d.kettlebells,
        )
    }

    fun guardarAjuste(clave: String, valor: String) {
        writableDatabase.insertWithOnConflict("ajustes", null, ContentValues().apply { put("clave", clave); put("valor", valor) }, SQLiteDatabase.CONFLICT_REPLACE)
        avisar()
    }

    fun guardarKettlebells(kg: List<Double>) = guardarAjuste("kettlebells", kg.sorted().joinToString(","))

    // ---------- Rutinas ----------

    /** Las hechas (o la copia del usuario si la editó) y luego las suyas. */
    fun rutinas(): Flow<List<Rutina>> = cambios.map { leerRutinas() }.flowOn(Dispatchers.IO)

    fun leerRutinas(): List<Rutina> {
        val propias = mutableListOf<Rutina>()
        readableDatabase.rawQuery("SELECT datos FROM rutinas ORDER BY creado", null).use { c ->
            while (c.moveToNext()) runCatching { propias += rutinaDe(JSONObject(c.getString(0))) }
        }
        // Las hechas, en su orden (con la copia del usuario si la cambió; vacía = oculta), y luego las suyas
        val porId = propias.associateBy { it.id }
        return Rutinas.predefinidas.mapNotNull { p -> porId[p.id]?.takeIf { it.bloques.isNotEmpty() } ?: p.takeIf { p.id !in porId } } +
            propias.filter { !it.predefinida }
    }

    /** Guarda una rutina. Si es una de las hechas, se guarda la copia editada con su mismo id. Devuelve el id. */
    fun guardarRutina(r: Rutina): String {
        val id = r.id.ifEmpty { "u${System.currentTimeMillis()}" }
        val v = ContentValues().apply { put("id", id); put("datos", json(r.copy(id = id)).toString()); put("creado", System.currentTimeMillis()) }
        val bd = writableDatabase
        if (bd.update("rutinas", ContentValues().apply { put("datos", v.getAsString("datos")) }, "id = ?", arrayOf(id)) == 0) bd.insert("rutinas", null, v)
        avisar()
        return id
    }

    fun borrarRutina(id: String) {
        writableDatabase.delete("rutinas", "id = ?", arrayOf(id))
        avisar()
    }

    // ---------- Sesiones ----------

    fun empezar(r: Rutina): Long {
        val id = writableDatabase.insert("sesiones", null, ContentValues().apply {
            put("rutina", r.id); put("nombre", r.nombre); put("inicio", System.currentTimeMillis()); put("fin", 0)
        })
        avisar()
        return id
    }

    /** Guarda (o corrige) una serie de la sesión en curso. */
    fun guardarSerie(sesion: Long, s: Serie) {
        val bd = writableDatabase
        val v = ContentValues().apply { put("kg", s.kg); put("reps", s.reps); put("hecha", if (s.hecha) 1 else 0) }
        val n = bd.update("series", v, "sesion = ? AND ejercicio = ? AND orden = ?", arrayOf("$sesion", s.ejercicio, "${s.orden}"))
        if (n == 0) bd.insert("series", null, v.apply { put("sesion", sesion); put("ejercicio", s.ejercicio); put("orden", s.orden) })
        avisar()
    }

    fun terminar(sesion: Long) {
        writableDatabase.execSQL("DELETE FROM series WHERE sesion = ? AND hecha = 0", arrayOf(sesion))
        writableDatabase.update("sesiones", ContentValues().apply { put("fin", System.currentTimeMillis()) }, "id = ?", arrayOf("$sesion"))
        avisar()
    }

    fun descartar(sesion: Long) {
        writableDatabase.delete("series", "sesion = ?", arrayOf("$sesion"))
        writableDatabase.delete("sesiones", "id = ?", arrayOf("$sesion"))
        avisar()
    }

    /** Todas las sesiones, de la más reciente a la más antigua. */
    fun sesiones(): Flow<List<Sesion>> = cambios.map { leerSesiones() }.flowOn(Dispatchers.IO)

    fun leerSesiones(): List<Sesion> {
        val series = mutableMapOf<Long, MutableList<Serie>>()
        readableDatabase.rawQuery("SELECT sesion, ejercicio, orden, kg, reps, hecha FROM series ORDER BY orden", null).use { c ->
            while (c.moveToNext()) series.getOrPut(c.getLong(0)) { mutableListOf() } += Serie(c.getString(1), c.getInt(2), c.getDouble(3), c.getInt(4), c.getInt(5) == 1)
        }
        val out = mutableListOf<Sesion>()
        readableDatabase.rawQuery("SELECT id, rutina, nombre, inicio, fin FROM sesiones ORDER BY inicio DESC", null).use { c ->
            while (c.moveToNext()) out += Sesion(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4), series[c.getLong(0)] ?: emptyList())
        }
        return out
    }

    // ---------- Copia de seguridad ----------

    fun exportar(): JSONObject {
        val o = JSONObject()
        listOf("rutinas", "sesiones", "series", "ajustes").forEach { t ->
            val filas = JSONArray()
            readableDatabase.query(t, null, null, null, null, null, null).use { c ->
                while (c.moveToNext()) {
                    val f = JSONObject()
                    for (i in 0 until c.columnCount) when (c.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> f.put(c.getColumnName(i), c.getLong(i))
                        Cursor.FIELD_TYPE_FLOAT -> f.put(c.getColumnName(i), c.getDouble(i))
                        Cursor.FIELD_TYPE_STRING -> f.put(c.getColumnName(i), c.getString(i))
                        else -> {}
                    }
                    filas.put(f)
                }
            }
            o.put(t, filas)
        }
        return o
    }

    fun importar(o: JSONObject) {
        val bd = writableDatabase
        bd.beginTransaction()
        try {
            listOf("rutinas", "sesiones", "series", "ajustes").forEach { t ->
                bd.delete(t, null, null)
                val filas = o.optJSONArray(t) ?: return@forEach
                for (i in 0 until filas.length()) {
                    val f = filas.getJSONObject(i)
                    val v = ContentValues()
                    f.keys().forEach { k ->
                        when (val x = f.get(k)) {
                            is Int -> v.put(k, x.toLong()); is Long -> v.put(k, x)
                            is Number -> v.put(k, x.toDouble())
                            else -> v.put(k, x.toString())
                        }
                    }
                    bd.insert(t, null, v)
                }
            }
            bd.setTransactionSuccessful()
        } finally { bd.endTransaction() }
        avisar()
    }

    companion object {
        fun json(r: Rutina): JSONObject = JSONObject()
            .put("id", r.id).put("nombre", r.nombre).put("seccion", r.seccion.name).put("formato", r.formato.name)
            .put("minutos", r.minutos).put("dias", JSONArray(r.dias.sorted())).put("descripcion", r.descripcion)
            .put("bloques", JSONArray(r.bloques.map {
                JSONObject().put("e", it.ejercicio).put("s", it.series).put("min", it.repsMin).put("max", it.repsMax).put("d", it.descanso)
            }))

        fun rutinaDe(o: JSONObject): Rutina {
            val b = o.getJSONArray("bloques")
            val d = o.optJSONArray("dias") ?: JSONArray()
            return Rutina(
                id = o.getString("id"), nombre = o.getString("nombre"),
                seccion = Seccion.entries.firstOrNull { it.name == o.optString("seccion") } ?: Seccion.GIMNASIO,
                bloques = (0 until b.length()).map { b.getJSONObject(it) }.map {
                    BloqueRutina(it.getString("e"), it.optInt("s", 3), it.optInt("min", 8), it.optInt("max", 10), it.optInt("d", 120))
                },
                formato = Formato.entries.firstOrNull { it.name == o.optString("formato") } ?: Formato.SERIES,
                minutos = o.optInt("minutos"),
                dias = (0 until d.length()).map { d.getInt(it) }.toSet(),
                descripcion = o.optString("descripcion"),
            )
        }
    }
}
