package com.mikel.kcalma.datos

import android.content.Context
import android.net.Uri
import com.mikel.kcalma.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Copia de seguridad en un archivo JSON que el usuario guarda donde quiera (Drive, Descargas…).
 * Lleva el perfil, el historial de peso, el diario y los entrenos. La clave de Gemini no va: está cifrada
 * con una llave de este móvil y en otro no serviría.
 */
object Copia {
    suspend fun exportar(contexto: Context, destino: Uri, almacen: AlmacenPerfil, diario: Diario, entrenos: Entrenos) = withContext(Dispatchers.IO) {
        val o = JSONObject()
            .put("app", "kcalma").put("formato", 1).put("version", BuildConfig.VERSION_NAME)
            .put("creada", System.currentTimeMillis())
            .put("perfil", almacen.exportar())
            .put("diario", diario.exportar())
            .put("entreno", entrenos.exportar())
        contexto.contentResolver.openOutputStream(destino, "wt")!!.use { it.write(o.toString().toByteArray()) }
        Registro.anotar("Copia exportada")
    }

    /** Devuelve false si el archivo no es una copia de Kcalma. */
    suspend fun importar(contexto: Context, origen: Uri, almacen: AlmacenPerfil, diario: Diario, entrenos: Entrenos): Boolean = withContext(Dispatchers.IO) {
        val texto = contexto.contentResolver.openInputStream(origen)!!.use { it.readBytes().toString(Charsets.UTF_8) }
        val o = runCatching { JSONObject(texto) }.getOrNull()
        if (o?.optString("app") != "kcalma") return@withContext false
        diario.importar(o.getJSONObject("diario"))
        // Las copias de antes de la 0.8.0 no traen entrenos: los que haya en el móvil se quedan
        o.optJSONObject("entreno")?.let { entrenos.importar(it) }
        almacen.importar(o.getJSONObject("perfil"))
        Registro.anotar("Copia importada (${o.optString("version")})")
        true
    }
}
