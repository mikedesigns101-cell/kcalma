package com.mikel.kcalma.datos

import android.content.Context
import com.mikel.kcalma.dominio.Alimento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Punto único de acceso a los alimentos: catálogo, productos guardados y Open Food Facts. */
class Repositorio(contexto: Context) {
    val catalogo = Catalogo(contexto)
    val diario = Diario(contexto)

    /** [supermercado]: null = todos, "" = solo genéricos. */
    suspend fun buscar(consulta: String, supermercado: String?): List<Alimento> = withContext(Dispatchers.IO) {
        val propios = if (supermercado == null) diario.buscarProductos(consulta) else emptyList()
        propios + catalogo.buscar(consulta, supermercado)
    }

    /** Orden: productos ya guardados, catálogo y, si no está, Open Food Facts (se guarda en el móvil). */
    suspend fun porCodigo(codigo: String): Busqueda = withContext(Dispatchers.IO) {
        diario.productoPorCodigo(codigo)?.let { return@withContext Busqueda.Encontrado(it) }
        catalogo.porCodigo(codigo)?.let { return@withContext Busqueda.Encontrado(it) }
        OpenFoodFacts.buscar(codigo).also { if (it is Busqueda.Encontrado) diario.guardarProducto(it.alimento) }
    }
}
