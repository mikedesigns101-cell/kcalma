package com.mikel.kcalma.datos

import android.content.Context
import com.mikel.kcalma.dominio.Alimento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Punto único de acceso a los alimentos: catálogo, productos guardados y Open Food Facts. */
class Repositorio(contexto: Context) {
    val catalogo = Catalogo(contexto)
    val diario = Diario(contexto)
    val entrenos = Entrenos(contexto)

    /** [supermercado]: null = todos, "" = solo genéricos. */
    suspend fun buscar(consulta: String, supermercado: String?): List<Alimento> = withContext(Dispatchers.IO) {
        val propios = if (supermercado == null) diario.buscarProductos(consulta) else emptyList()
        // Los de la comunidad van al final y solo si ese código no lo tiene ya el usuario ni el catálogo
        val delCatalogo = catalogo.buscar(consulta, supermercado)
        val vistos = (propios + delCatalogo).mapNotNull { it.codigoBarras }.toSet()
        val comunidad = if (supermercado == null) diario.buscarComunidad(consulta).filter { it.codigoBarras !in vistos } else emptyList()
        propios + delCatalogo + comunidad
    }

    /**
     * Alimento de la base que corresponde a lo que propone la IA: genéricos primero (de la consulta más
     * estricta a la más amplia) y, si no, cualquier producto. Null si nada encaja.
     */
    suspend fun emparejar(busqueda: String, nombre: String, kcal100: Double): Alimento? {
        // Si el usuario ya corrigió antes este mismo alimento, vale su elección
        withContext(Dispatchers.IO) { runCatching { diario.preferido(busqueda) }.getOrNull() }?.let { return it }
        val consultas = Emparejado.consultas(busqueda, nombre)
        return consultas.firstNotNullOfOrNull { q -> Emparejado.elegir(buscar(q, "").take(30), "$busqueda $nombre", kcal100) }
            ?: consultas.firstNotNullOfOrNull { q -> Emparejado.elegir(buscar(q, null).take(30), "$busqueda $nombre", kcal100) }
    }

    /** Orden: productos ya guardados, catálogo y, si no está, Open Food Facts (se guarda en el móvil). */
    suspend fun porCodigo(codigo: String): Busqueda = withContext(Dispatchers.IO) {
        diario.productoPorCodigo(codigo)?.let { return@withContext Busqueda.Encontrado(it) }
        catalogo.porCodigo(codigo)?.let { return@withContext Busqueda.Encontrado(it) }
        val off = OpenFoodFacts.buscar(codigo).also { if (it is Busqueda.Encontrado) diario.guardarProducto(it.alimento) }
        // La comunidad solo cuenta si no está en el móvil, en el catálogo ni en Open Food Facts: nunca pisa datos oficiales
        if (off is Busqueda.Encontrado) off else diario.comunidadPorCodigo(codigo)?.let { Busqueda.Encontrado(it) } ?: off
    }
}
