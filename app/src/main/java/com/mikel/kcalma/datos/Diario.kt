package com.mikel.kcalma.datos

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.Unidad
import com.mikel.kcalma.dominio.normalizar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Comida habitual guardada para repetirla con un toque. */
data class Habitual(val id: Long, val nombre: String, val entradas: List<Entrada>)

/**
 * Datos del usuario (diario.db): lo que come, sus comidas habituales y los productos
 * traídos de Open Food Facts. Cada fila guarda una copia completa del alimento.
 */
class Diario(contexto: Context) : SQLiteOpenHelper(contexto, "diario.db", null, 2) {

    /** Sube cada vez que algo cambia, para que las pantallas vuelvan a leer. */
    private val cambios = MutableStateFlow(0)

    override fun onCreate(bd: SQLiteDatabase) {
        val alimento = "nombre TEXT NOT NULL, marca TEXT, supermercado TEXT, codigo_barras TEXT, generico INTEGER, " +
            "racion TEXT, racion_g REAL, envase_g REAL, kcal REAL, proteinas REAL, hidratos REAL, grasas REAL, " +
            "azucares REAL, saturadas REAL, fibra REAL, sal REAL"
        bd.execSQL("CREATE TABLE entradas (id INTEGER PRIMARY KEY, fecha TEXT NOT NULL, comida INTEGER NOT NULL, gramos REAL, unidad TEXT, cantidad REAL, creado INTEGER, $alimento)")
        bd.execSQL("CREATE INDEX idx_fecha ON entradas(fecha)")
        bd.execSQL("CREATE TABLE productos (id INTEGER PRIMARY KEY, busca TEXT, $alimento)")
        bd.execSQL("CREATE UNIQUE INDEX idx_producto_codigo ON productos(codigo_barras)")
        bd.execSQL("CREATE TABLE habituales (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, creado INTEGER)")
        bd.execSQL("CREATE TABLE habitual_items (id INTEGER PRIMARY KEY, habitual_id INTEGER, gramos REAL, unidad TEXT, cantidad REAL, $alimento)")
    }

    override fun onUpgrade(bd: SQLiteDatabase, antes: Int, ahora: Int) {
        // Migraciones siempre sin borrar datos del usuario.
        if (antes < 2) corregirGenericos(bd)
    }

    /** v2: lo ya apuntado con genéricos de BEDCA que tenían datos erróneos (ver datos/scripts/07_correcciones.py). */
    private fun corregirGenericos(bd: SQLiteDatabase) {
        CORRECCIONES.forEach { (nombre, v) ->
            val campos = listOf("kcal", "proteinas", "hidratos", "grasas", "fibra").zip(v.toList()).filter { it.second != null }
            val sql = "UPDATE %s SET " + campos.joinToString { "${it.first} = ?" } + " WHERE generico = 1 AND nombre = ?"
            val args = campos.map { it.second as Any } + nombre
            listOf("entradas", "habitual_items").forEach { tabla -> bd.execSQL(sql.format(tabla), args.toTypedArray()) }
        }
    }

    // ---------- Copia de seguridad ----------

    private val TABLAS = listOf("entradas", "productos", "habituales", "habitual_items")

    /** Todas las tablas en JSON (fila = objeto columna → valor). */
    fun exportar(): org.json.JSONObject {
        val o = org.json.JSONObject()
        TABLAS.forEach { t ->
            val filas = org.json.JSONArray()
            readableDatabase.query(t, null, null, null, null, null, null).use { c ->
                while (c.moveToNext()) {
                    val f = org.json.JSONObject()
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

    /** Sustituye todo el diario por el de la copia, en una sola transacción. */
    fun importar(o: org.json.JSONObject) {
        val bd = writableDatabase
        bd.beginTransaction()
        try {
            TABLAS.forEach { t ->
                bd.delete(t, null, null)
                val filas = o.optJSONArray(t) ?: return@forEach
                for (i in 0 until filas.length()) {
                    val f = filas.getJSONObject(i)
                    val v = ContentValues()
                    f.keys().forEach { k ->
                        when (val x = f.get(k)) {
                            is Int -> v.put(k, x.toLong()); is Long -> v.put(k, x)
                            is Double -> v.put(k, x); is Number -> v.put(k, x.toDouble())
                            else -> v.put(k, x.toString())
                        }
                    }
                    bd.insert(t, null, v)
                }
            }
            bd.setTransactionSuccessful()
        } finally { bd.endTransaction() }
        cambios.value++
    }

    // ---------- Diario ----------

    fun entradasDe(fecha: String): Flow<List<Entrada>> =
        cambios.map { leerEntradas("fecha = ?", arrayOf(fecha), "creado") }.flowOn(Dispatchers.IO)

    /** Totales por día (aaaa-mm-dd) desde [fecha], para Progreso. */
    fun totalesDesde(fecha: String): Flow<Map<String, com.mikel.kcalma.dominio.Nutrientes>> =
        cambios.map {
            leerEntradas("fecha >= ?", arrayOf(fecha), "fecha").groupBy { it.fecha }
                .mapValues { (_, l) -> l.fold(com.mikel.kcalma.dominio.Nutrientes()) { s, e -> s + e.nutrientes } }
        }.flowOn(Dispatchers.IO)

    fun anadir(fecha: String, comida: ComidaDia, alimento: Alimento, gramos: Double, unidad: Unidad, cantidad: Double) {
        val v = valores(alimento).apply {
            put("fecha", fecha); put("comida", comida.ordinal); put("gramos", gramos)
            put("unidad", unidad.name); put("cantidad", cantidad); put("creado", System.currentTimeMillis())
        }
        writableDatabase.insert("entradas", null, v)
        cambios.value++
    }

    fun actualizar(id: Long, gramos: Double, unidad: Unidad, cantidad: Double) {
        val v = ContentValues().apply { put("gramos", gramos); put("unidad", unidad.name); put("cantidad", cantidad) }
        writableDatabase.update("entradas", v, "id = ?", arrayOf(id.toString()))
        cambios.value++
    }

    fun borrar(id: Long) {
        writableDatabase.delete("entradas", "id = ?", arrayOf(id.toString()))
        cambios.value++
    }

    /** Últimos alimentos distintos que se han apuntado. */
    fun recientes(limite: Int = 8): List<Alimento> {
        val vistos = mutableSetOf<String>()
        return leerEntradas(null, null, "creado DESC", 60).map { it.alimento }
            .filter { vistos.add(it.nombre + "|" + it.marca) }.take(limite)
    }

    /** Lo apuntado en [comida] el último día anterior a [antesDe] en que se apuntó algo en ella. */
    fun ultimaComida(comida: ComidaDia, antesDe: String): List<Entrada> {
        val fecha = readableDatabase.rawQuery(
            "SELECT MAX(fecha) FROM entradas WHERE comida = ? AND fecha < ?", arrayOf(comida.ordinal.toString(), antesDe),
        ).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null } ?: return emptyList()
        return leerEntradas("comida = ? AND fecha = ?", arrayOf(comida.ordinal.toString(), fecha), "creado")
    }

    /**
     * Lo que más se repite en [comida] desde [desde] (al menos 2 veces), con la cantidad de la última vez,
     * para añadirlo de un toque tal cual se suele tomar.
     */
    fun frecuentes(comida: ComidaDia, desde: String, limite: Int = 6): List<Entrada> =
        leerEntradas("comida = ? AND fecha >= ?", arrayOf(comida.ordinal.toString(), desde), "creado DESC")
            .groupBy { it.alimento.nombre + "|" + it.alimento.marca }
            .values.filter { it.size >= 2 }
            .sortedByDescending { it.size }
            .take(limite).map { it.first() }

    private fun leerEntradas(donde: String?, args: Array<String>?, orden: String, limite: Int? = null): List<Entrada> =
        readableDatabase.query("entradas", null, donde, args, null, null, orden, limite?.toString()).use { c ->
            generateSequence { if (c.moveToNext()) c.entrada("fecha") else null }.toList()
        }

    // ---------- Comidas habituales ----------

    fun guardarHabitual(nombre: String, entradas: List<Entrada>) {
        val bd = writableDatabase
        bd.beginTransaction()
        try {
            val id = bd.insert("habituales", null, ContentValues().apply {
                put("nombre", nombre); put("creado", System.currentTimeMillis())
            })
            entradas.forEach { e ->
                bd.insert("habitual_items", null, valores(e.alimento).apply {
                    put("habitual_id", id); put("gramos", e.gramos); put("unidad", e.unidad.name); put("cantidad", e.cantidad)
                })
            }
            bd.setTransactionSuccessful()
        } finally { bd.endTransaction() }
        cambios.value++
    }

    fun habituales(): List<Habitual> {
        val bd = readableDatabase
        val cabeceras = bd.query("habituales", arrayOf("id", "nombre"), null, null, null, null, "creado DESC").use { c ->
            generateSequence { if (c.moveToNext()) c.getLong(0) to c.getString(1) else null }.toList()
        }
        return cabeceras.map { (id, nombre) ->
            val items = bd.query("habitual_items", null, "habitual_id = ?", arrayOf(id.toString()), null, null, "id").use { c ->
                generateSequence { if (c.moveToNext()) c.entrada(null) else null }.toList()
            }
            Habitual(id, nombre, items)
        }
    }

    fun borrarHabitual(id: Long) {
        writableDatabase.delete("habitual_items", "habitual_id = ?", arrayOf(id.toString()))
        writableDatabase.delete("habituales", "id = ?", arrayOf(id.toString()))
        cambios.value++
    }

    // ---------- Productos de Open Food Facts guardados ----------

    fun guardarProducto(a: Alimento) {
        val busca = " " + listOfNotNull(a.nombre, a.marca, a.supermercado).joinToString(" ") { normalizar(it) } + " "
        writableDatabase.insertWithOnConflict("productos", null, valores(a).apply { put("busca", busca) }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun productoPorCodigo(codigo: String): Alimento? =
        readableDatabase.query("productos", null, "codigo_barras = ?", arrayOf(codigo), null, null, null, "1")
            .use { if (it.moveToFirst()) it.alimento() else null }

    fun buscarProductos(consulta: String): List<Alimento> {
        val palabras = normalizar(consulta).split(' ').filter { it.isNotEmpty() }.take(5)
        if (palabras.isEmpty()) return emptyList()
        return readableDatabase.query(
            "productos", null, palabras.joinToString(" AND ") { "busca LIKE ?" },
            palabras.map { "% $it%" }.toTypedArray(), null, null, null, "10",
        ).use { c -> generateSequence { if (c.moveToNext()) c.alimento() else null }.toList() }
    }

    // ---------- Conversión ----------

    private fun valores(a: Alimento) = ContentValues().apply {
        put("nombre", a.nombre); put("marca", a.marca); put("supermercado", a.supermercado)
        put("codigo_barras", a.codigoBarras); put("generico", if (a.generico) 1 else 0)
        put("racion", a.racion); put("racion_g", a.racionG); put("envase_g", a.envaseG)
        put("kcal", a.kcal); put("proteinas", a.proteinas); put("hidratos", a.hidratos); put("grasas", a.grasas)
        put("azucares", a.azucares); put("saturadas", a.saturadas); put("fibra", a.fibra); put("sal", a.sal)
    }

    private fun Cursor.s(c: String) = getColumnIndexOrThrow(c).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.d(c: String) = getColumnIndexOrThrow(c).let { if (isNull(it)) null else getDouble(it) }

    private fun Cursor.alimento() = Alimento(
        nombre = s("nombre") ?: "", marca = s("marca"), supermercado = s("supermercado"),
        codigoBarras = s("codigo_barras"), generico = d("generico") == 1.0,
        racion = s("racion"), racionG = d("racion_g"), envaseG = d("envase_g"),
        kcal = d("kcal") ?: 0.0, proteinas = d("proteinas") ?: 0.0, hidratos = d("hidratos") ?: 0.0, grasas = d("grasas") ?: 0.0,
        azucares = d("azucares"), saturadas = d("saturadas"), fibra = d("fibra"), sal = d("sal"),
    )

    /** [columnaFecha] = null para las filas de comidas habituales, que no tienen fecha ni comida. */
    private fun Cursor.entrada(columnaFecha: String?) = Entrada(
        id = getLong(getColumnIndexOrThrow("id")),
        fecha = columnaFecha?.let { s(it) } ?: "",
        comida = if (columnaFecha != null) ComidaDia.entries[getInt(getColumnIndexOrThrow("comida"))] else ComidaDia.COMIDA,
        alimento = alimento(),
        gramos = d("gramos") ?: 0.0,
        unidad = runCatching { Unidad.valueOf(s("unidad") ?: "") }.getOrDefault(Unidad.GRAMOS),
        cantidad = d("cantidad") ?: 0.0,
    )
}

/** Mismos valores que CORRECCIONES en datos/scripts/07_correcciones.py: kcal, proteínas, hidratos, grasas, fibra. */
private data class Valores(val kcal: Double?, val p: Double?, val h: Double?, val g: Double?, val f: Double?) {
    fun toList() = listOf(kcal, p, h, g, f)
}

private val CORRECCIONES = mapOf(
    "Arroz, hervido" to Valores(130.0, 2.7, 28.2, 0.3, 0.4),
    "Garbanzo, hervido" to Valores(148.0, 8.9, 18.7, 2.5, 7.6),
    "Lenteja, hervida" to Valores(116.0, 9.0, 12.2, 0.4, 7.9),
    "Judías blancas, cocidas" to Valores(139.0, 9.7, 18.8, 0.4, 6.3),
    "Garbanzo, en conserva" to Valores(120.0, null, null, null, null),
    "Bonito, enlatado en aceite, escurrido" to Valores(null, null, null, 11.0, null),
    "Cangrejo" to Valores(87.0, 18.1, 0.0, 1.1, null),
    "Nécora" to Valores(87.0, 18.1, 0.0, 1.1, null),
    "Chirla" to Valores(47.0, null, null, null, null),
    "Dorada, cruda" to Valores(133.0, null, null, null, null),
    "Jurel, crudo" to Valores(null, null, null, 5.2, null),
    "Kebab" to Valores(173.0, null, null, null, null),
    "Pimienta, negra" to Valores(251.0, null, null, null, null),
    "Pimienta, blanca" to Valores(296.0, null, null, null, null),
    "Puré de patata y queso, en copos" to Valores(350.0, null, null, null, null),
)
