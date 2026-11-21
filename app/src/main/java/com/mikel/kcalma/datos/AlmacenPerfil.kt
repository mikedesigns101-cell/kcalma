package com.mikel.kcalma.datos

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.dominio.Pesada
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.almacen: DataStore<Preferences> by preferencesDataStore(name = "perfil")

/** Guarda el perfil en el propio móvil. Devuelve null mientras no se haya hecho el onboarding. */
class AlmacenPerfil(private val context: Context) {

    private object K {
        val sexo = stringPreferencesKey("sexo")
        val edad = intPreferencesKey("edad")
        val altura = intPreferencesKey("altura_cm")
        val peso = doublePreferencesKey("peso_kg")
        val pesoObjetivo = doublePreferencesKey("peso_objetivo_kg")
        val grasa = doublePreferencesKey("grasa_pct")
        val trabajo = stringPreferencesKey("trabajo")
        val pasos = stringPreferencesKey("pasos")
        val dias = intPreferencesKey("dias_ejercicio")
        val tipo = stringPreferencesKey("tipo_ejercicio")
        val duracion = stringPreferencesKey("duracion")
        val objetivo = stringPreferencesKey("objetivo")
        val ritmo = stringPreferencesKey("ritmo")
        val alergias = stringSetPreferencesKey("alergias")
        val noMeGusta = stringSetPreferencesKey("no_me_gusta")
        val meEncanta = stringSetPreferencesKey("me_encanta")
        val supers = stringSetPreferencesKey("supermercados")
        val kcalManual = intPreferencesKey("kcal_manual")
        val correccion = intPreferencesKey("correccion_gasto")
        /** Fecha de la última propuesta de ajuste (aceptada o no): se propone como mucho una vez por semana. */
        val ultimaPropuesta = stringPreferencesKey("ultima_propuesta")
        /** Historial de peso: «aaaa-mm-dd=kg;…», ordenado por fecha. */
        val pesos = stringPreferencesKey("pesos")
        /** Apariencia: tema de color y animaciones. */
        val tema = stringPreferencesKey("tema")
        val animaciones = booleanPreferencesKey("animaciones")
    }

    val perfil: Flow<Perfil?> = context.almacen.data.map { p ->
        val sexo = p[K.sexo] ?: return@map null
        val base = Perfil()
        Perfil(
            sexo = enumOr(sexo, base.sexo),
            edad = p[K.edad] ?: base.edad,
            alturaCm = p[K.altura] ?: base.alturaCm,
            pesoKg = p[K.peso] ?: base.pesoKg,
            pesoObjetivoKg = p[K.pesoObjetivo] ?: base.pesoObjetivoKg,
            grasaPct = p[K.grasa],
            trabajo = enumOr(p[K.trabajo], base.trabajo),
            pasos = enumOr(p[K.pasos], base.pasos),
            diasEjercicio = p[K.dias] ?: base.diasEjercicio,
            tipoEjercicio = enumOr(p[K.tipo], base.tipoEjercicio),
            duracion = enumOr(p[K.duracion], base.duracion),
            objetivo = enumOr(p[K.objetivo], base.objetivo),
            ritmo = enumOr(p[K.ritmo], base.ritmo),
            alergias = p[K.alergias] ?: emptySet(),
            noMeGusta = p[K.noMeGusta] ?: emptySet(),
            meEncanta = p[K.meEncanta] ?: emptySet(),
            supermercados = p[K.supers] ?: emptySet(),
            kcalManual = p[K.kcalManual],
            correccionGasto = p[K.correccion] ?: 0,
        )
    }

    val pesos: Flow<List<Pesada>> = context.almacen.data.map { leerPesos(it[K.pesos]) }

    val ultimaPropuesta: Flow<String?> = context.almacen.data.map { it[K.ultimaPropuesta] }

    /** Tema elegido en Apariencia (null = el de por defecto). */
    val tema: Flow<String?> = context.almacen.data.map { it[K.tema] }
    val animaciones: Flow<Boolean> = context.almacen.data.map { it[K.animaciones] ?: true }

    suspend fun guardarTema(clave: String) { context.almacen.edit { it[K.tema] = clave } }
    suspend fun guardarAnimaciones(si: Boolean) { context.almacen.edit { it[K.animaciones] = si } }

    /** Acepta (con [correccion]) o descarta (null) la propuesta de la semana. */
    suspend fun responderPropuesta(correccion: Int?) {
        context.almacen.edit { p ->
            p[K.ultimaPropuesta] = LocalDate.now().toString()
            if (correccion != null) p[K.correccion] = correccion
        }
    }

    /**
     * Guarda el perfil. Si el peso ha cambiado respecto a la última pesada (o es la primera vez),
     * lo apunta con la fecha de hoy; [forzarPesada] lo apunta aunque sea el mismo.
     */
    suspend fun guardar(perfil: Perfil, forzarPesada: Boolean = false) {
        context.almacen.edit { p ->
            val pesos = leerPesos(p[K.pesos])
            if (forzarPesada || pesos.lastOrNull()?.kg != perfil.pesoKg) {
                val hoy = LocalDate.now().toString()
                p[K.pesos] = escribirPesos(pesos.filter { it.fecha != hoy } + Pesada(hoy, perfil.pesoKg))
            }
            perfil.kcalManual?.let { p[K.kcalManual] = it } ?: p.remove(K.kcalManual)
            p[K.correccion] = perfil.correccionGasto
            p[K.sexo] = perfil.sexo.name
            p[K.edad] = perfil.edad
            p[K.altura] = perfil.alturaCm
            p[K.peso] = perfil.pesoKg
            p[K.pesoObjetivo] = perfil.pesoObjetivoKg
            perfil.grasaPct?.let { p[K.grasa] = it } ?: p.remove(K.grasa)
            p[K.trabajo] = perfil.trabajo.name
            p[K.pasos] = perfil.pasos.name
            p[K.dias] = perfil.diasEjercicio
            p[K.tipo] = perfil.tipoEjercicio.name
            p[K.duracion] = perfil.duracion.name
            p[K.objetivo] = perfil.objetivo.name
            p[K.ritmo] = perfil.ritmo.name
            p[K.alergias] = perfil.alergias
            p[K.noMeGusta] = perfil.noMeGusta
            p[K.meEncanta] = perfil.meEncanta
            p[K.supers] = perfil.supermercados
        }
    }

    /** Todas las preferencias del perfil en JSON, con su tipo, para la copia de seguridad. */
    suspend fun exportar(): JSONObject {
        val o = JSONObject()
        context.almacen.data.first().asMap().forEach { (k, v) ->
            o.put(k.name, when (v) {
                is Set<*> -> JSONObject().put("t", "set").put("v", JSONArray(v.toList()))
                is Int -> JSONObject().put("t", "int").put("v", v)
                is Double -> JSONObject().put("t", "double").put("v", v)
                else -> JSONObject().put("t", "str").put("v", v.toString())
            })
        }
        return o
    }

    suspend fun importar(o: JSONObject) {
        context.almacen.edit { p ->
            p.clear()
            o.keys().forEach { k ->
                val e = o.getJSONObject(k)
                when (e.getString("t")) {
                    "set" -> p[stringSetPreferencesKey(k)] = e.getJSONArray("v").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                    "int" -> p[intPreferencesKey(k)] = e.getInt("v")
                    "double" -> p[doublePreferencesKey(k)] = e.getDouble("v")
                    else -> p[stringPreferencesKey(k)] = e.getString("v")
                }
            }
        }
    }

    private fun leerPesos(texto: String?): List<Pesada> = texto.orEmpty().split(';').mapNotNull { t ->
        val (f, kg) = t.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        kg.toDoubleOrNull()?.let { Pesada(f, it) }
    }.sortedBy { it.fecha }

    private fun escribirPesos(l: List<Pesada>) = l.sortedBy { it.fecha }.takeLast(1000).joinToString(";") { "${it.fecha}=${it.kg}" }

    private inline fun <reified E : Enum<E>> enumOr(nombre: String?, defecto: E): E =
        enumValues<E>().firstOrNull { it.name == nombre } ?: defecto
}
