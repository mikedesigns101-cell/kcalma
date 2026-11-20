package com.mikel.kcalma.datos

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mikel.kcalma.dominio.Perfil
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
        )
    }

    suspend fun guardar(perfil: Perfil) {
        context.almacen.edit { p ->
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

    private inline fun <reified E : Enum<E>> enumOr(nombre: String?, defecto: E): E =
        enumValues<E>().firstOrNull { it.name == nombre } ?: defecto
}
