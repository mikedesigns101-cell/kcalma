package com.mikel.kcalma.datos

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.gramosDeCantidad
import com.mikel.kcalma.dominio.normalizar
import java.io.File

/**
 * Catálogo de alimentos de solo lectura (assets/alimentos.db, generado por
 * datos/scripts/05_bd_movil.py). Se copia al móvil la primera vez y cada vez que
 * cambia VERSION. Nunca se escribe en él: los datos del usuario van en [Diario].
 */
class Catalogo(private val contexto: Context) {
    private var bd: SQLiteDatabase? = null

    /** Subir a la vez que `PRAGMA user_version` del script cuando se regenere la base. */
    private val version = 2

    @Synchronized
    private fun abrir(): SQLiteDatabase {
        bd?.let { return it }
        val archivo = contexto.getDatabasePath("alimentos.db")
        if (!archivo.exists() || versionDe(archivo) != version) copiar(archivo)
        return SQLiteDatabase.openDatabase(archivo.path, null, SQLiteDatabase.OPEN_READONLY).also { bd = it }
    }

    private fun versionDe(archivo: File): Int = try {
        SQLiteDatabase.openDatabase(archivo.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
    } catch (e: Exception) { -1 }

    private fun copiar(archivo: File) {
        archivo.parentFile?.mkdirs()
        val temporal = File(archivo.path + ".tmp")
        contexto.assets.open("alimentos.db").use { entrada -> temporal.outputStream().use { entrada.copyTo(it) } }
        archivo.delete()
        temporal.renameTo(archivo)
    }

    /**
     * Busca por palabras (cada una como principio de palabra en nombre, marca o supermercado).
     * [supermercado] = null para todos; "" para solo genéricos.
     */
    fun buscar(consulta: String, supermercado: String? = null, limite: Int = 40): List<Alimento> {
        val palabras = normalizar(consulta).split(' ').filter { it.isNotEmpty() }.take(5)
        if (palabras.isEmpty()) return emptyList()
        val condiciones = palabras.map { "busca LIKE ?" }.toMutableList()
        val argumentos = palabras.map { "% $it%" }.toMutableList()
        when (supermercado) {
            null -> {}
            "" -> condiciones += "tipo = 'generico'"
            else -> { condiciones += "supermercado = ?"; argumentos += supermercado }
        }
        argumentos += " ${palabras.first()}%"
        val sql = "SELECT * FROM alimentos WHERE ${condiciones.joinToString(" AND ")} " +
            "ORDER BY (busca LIKE ?) DESC, popularidad DESC LIMIT $limite"
        return abrir().rawQuery(sql, argumentos.toTypedArray()).use { c -> generateSequence { if (c.moveToNext()) c.alimento() else null }.toList() }
    }

    fun porCodigo(codigo: String): Alimento? =
        abrir().rawQuery("SELECT * FROM alimentos WHERE codigo_barras = ? LIMIT 1", arrayOf(codigo))
            .use { if (it.moveToFirst()) it.alimento() else null }

    private fun Cursor.alimento(): Alimento {
        fun s(c: String) = getColumnIndexOrThrow(c).let { if (isNull(it)) null else getString(it) }
        fun d(c: String) = getColumnIndexOrThrow(c).let { if (isNull(it)) null else getDouble(it) }
        val cantidad = s("cantidad")
        return Alimento(
            nombre = s("nombre") ?: "",
            marca = s("marca"),
            supermercado = s("supermercado"),
            codigoBarras = s("codigo_barras"),
            generico = s("tipo") == "generico",
            racion = s("racion"),
            racionG = d("racion_g"),
            envaseG = gramosDeCantidad(cantidad),
            kcal = d("kcal_100g") ?: 0.0,
            proteinas = d("proteinas_100g") ?: 0.0,
            hidratos = d("hidratos_100g") ?: 0.0,
            grasas = d("grasas_100g") ?: 0.0,
            azucares = d("azucares_100g"),
            saturadas = d("saturadas_100g"),
            fibra = d("fibra_100g"),
            sal = d("sal_100g"),
        )
    }
}
