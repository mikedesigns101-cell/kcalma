package com.mikel.kcalma.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.ALERGENOS
import com.mikel.kcalma.dominio.CATEGORIAS_GUSTO
import com.mikel.kcalma.dominio.Gustos
import com.mikel.kcalma.dominio.Calculadora
import com.mikel.kcalma.dominio.Duracion
import com.mikel.kcalma.dominio.Objetivo
import com.mikel.kcalma.dominio.Pasos
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.dominio.Ritmo
import com.mikel.kcalma.dominio.SUPERMERCADOS
import com.mikel.kcalma.dominio.Sexo
import com.mikel.kcalma.dominio.TipoEjercicio
import com.mikel.kcalma.dominio.Trabajo
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.CabeceraPaso
import com.mikel.kcalma.ui.componentes.CampoNumero
import com.mikel.kcalma.ui.componentes.ChipAnadir
import com.mikel.kcalma.ui.componentes.ChipSeleccion
import com.mikel.kcalma.ui.componentes.Etiqueta
import com.mikel.kcalma.ui.componentes.EstadoChip
import com.mikel.kcalma.ui.componentes.FilaChips
import com.mikel.kcalma.ui.componentes.Nota
import com.mikel.kcalma.ui.componentes.Opcion
import com.mikel.kcalma.ui.componentes.Segmentado
import com.mikel.kcalma.ui.componentes.TituloPaso
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/*
 * Bloques de preguntas del onboarding. Se usan en el onboarding y al editar el perfil.
 */

/** Textos de los campos numéricos, aparte del perfil para poder dejarlos vacíos mientras se escribe. */
class CamposDatos(inicial: Perfil?) {
    private fun txt(x: Double) = if (x == Math.floor(x)) x.toInt().toString() else x.toString().replace('.', ',')
    var edad by mutableStateOf(inicial?.edad?.toString() ?: "")
    var altura by mutableStateOf(inicial?.alturaCm?.toString() ?: "")
    var peso by mutableStateOf(inicial?.pesoKg?.let(::txt) ?: "")
    var pesoObj by mutableStateOf(inicial?.pesoObjetivoKg?.let(::txt) ?: "")
    var grasa by mutableStateOf(inicial?.grasaPct?.let(::txt) ?: "")

    private fun num(s: String) = s.replace(',', '.').toDoubleOrNull()
    val validos: Boolean get() = num(edad)?.let { it in 14.0..100.0 } == true &&
        num(altura)?.let { it in 120.0..230.0 } == true &&
        num(peso)?.let { it in 35.0..300.0 } == true &&
        num(pesoObj)?.let { it in 35.0..300.0 } == true &&
        (grasa.isEmpty() || num(grasa)?.let { it in 3.0..60.0 } == true)

    fun aplicar(p: Perfil) = p.copy(
        edad = num(edad)!!.toInt(), alturaCm = num(altura)!!.toInt(),
        pesoKg = num(peso)!!, pesoObjetivoKg = num(pesoObj)!!, grasaPct = num(grasa),
    )
}

@Composable
fun BloqueDatos(perfil: Perfil, campos: CamposDatos, onCambio: (Perfil) -> Unit) {
    TituloPaso("Empecemos por ti", "Con esto calculamos lo que gasta tu cuerpo en reposo.")
    Segmentado(listOf("Hombre", "Mujer"), perfil.sexo.ordinal) { onCambio(perfil.copy(sexo = Sexo.entries[it])) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNumero("Edad", campos.edad, "años", { campos.edad = it }, Modifier.weight(1f), ejemplo = "30")
        CampoNumero("Altura", campos.altura, "cm", { campos.altura = it }, Modifier.weight(1f), ejemplo = "175")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNumero("Peso actual", campos.peso, "kg", { campos.peso = it }, Modifier.weight(1f), ejemplo = "80", decimales = true)
        CampoNumero("Peso objetivo", campos.pesoObj, "kg", { campos.pesoObj = it }, Modifier.weight(1f), ejemplo = "75", decimales = true)
    }
    CampoNumero("% de grasa corporal (opcional)", campos.grasa, "%", { campos.grasa = it }, Modifier.fillMaxWidth(), ejemplo = "No lo sé", decimales = true)
    Nota(resaltado("Si conoces tu % de grasa (báscula de bioimpedancia, plicómetro…) el cálculo es ", "más preciso", ". Si no, no pasa nada."))
}

@Composable
fun BloqueTrabajo(perfil: Perfil, onCambio: (Perfil) -> Unit) {
    TituloPaso("¿Cómo es tu trabajo o tu día?", "Sin contar el ejercicio. Piensa en un día normal.")
    val ops = listOf(
        Triple("💻", "Casi todo sentado", "Oficina, teletrabajo, estudiante, conductor"),
        Triple("🧍", "De pie o moviéndome a ratos", "Dependiente, profesor, peluquero, camarero de barra"),
        Triple("🚶", "Moviéndome casi todo el día", "Camarero de sala, enfermero, repartidor a pie, reponedor"),
        Triple("🏗️", "Trabajo físico duro", "Albañil, mudanzas, campo, almacén cargando peso"),
    )
    ops.forEachIndexed { i, (e, t, d) ->
        Opcion(e, t, d, perfil.trabajo.ordinal == i) { onCambio(perfil.copy(trabajo = Trabajo.entries[i])) }
    }
}

@Composable
fun BloquePasos(perfil: Perfil, onCambio: (Perfil) -> Unit) {
    TituloPaso("¿Cuánto andas al día?", "Si no lo sabes, mira la app Salud o Fit de tu móvil, o elige lo que más se parezca.")
    val ops = listOf(
        Triple("🛋️", "Menos de 5.000 pasos", "Casi todo en coche, poco paseo"),
        Triple("🚶", "5.000 – 8.000 pasos", "Algún paseo, recados andando"),
        Triple("🏃", "8.000 – 12.000 pasos", "Vas andando a casi todo o paseas a diario"),
        Triple("⛰️", "Más de 12.000 pasos", "Andas muchísimo, paseas al perro largo…"),
    )
    ops.forEachIndexed { i, (e, t, d) ->
        Opcion(e, t, d, perfil.pasos.ordinal == i) { onCambio(perfil.copy(pasos = Pasos.entries[i])) }
    }
    Nota(resaltado("📱 Próximamente: ", "leer tus pasos automáticamente", " con Health Connect para ajustar tus calorías cada día."))
}

@Composable
fun BloqueEjercicio(perfil: Perfil, onCambio: (Perfil) -> Unit) {
    TituloPaso("¿Haces ejercicio?", "Lo que haces de verdad, no lo que te gustaría 😉")
    Contador("Días por semana", perfil.diasEjercicio) { onCambio(perfil.copy(diasEjercicio = it.coerceIn(0, 7))) }
    if (perfil.diasEjercicio > 0) {
        Etiqueta("Tipo de ejercicio")
        val ops = listOf(
            Triple("🏋️", "Pesas / gimnasio", "Fuerza, máquinas, crossfit"),
            Triple("🏃", "Cardio", "Correr, bici, natación, spinning"),
            Triple("⚽", "Deporte", "Fútbol, pádel, baloncesto…"),
        )
        ops.forEachIndexed { i, (e, t, d) ->
            Opcion(e, t, d, perfil.tipoEjercicio.ordinal == i) { onCambio(perfil.copy(tipoEjercicio = TipoEjercicio.entries[i])) }
        }
        Etiqueta("Cada sesión dura")
        Segmentado(listOf("30 min", "1 hora", "1,5 h"), perfil.duracion.ordinal) { onCambio(perfil.copy(duracion = Duracion.entries[it])) }
    }
}

@Composable
fun BloqueObjetivo(perfil: Perfil, onCambio: (Perfil) -> Unit) {
    TituloPaso("¿Qué quieres conseguir?", "Puedes cambiarlo cuando quieras.")
    val ops = listOf(
        Triple("📉", "Perder grasa", "Bajar de peso manteniendo el músculo"),
        Triple("🔄", "Recomposición", "Mismo peso, menos grasa y más músculo. Ideal si empiezas en el gimnasio"),
        Triple("💪", "Ganar músculo", "Subir de peso poco a poco, sin coger mucha grasa"),
    )
    ops.forEachIndexed { i, (e, t, d) ->
        Opcion(e, t, d, perfil.objetivo.ordinal == i) { onCambio(perfil.copy(objetivo = Objetivo.entries[i])) }
    }
    if (perfil.objetivo != Objetivo.RECOMPOSICION) {
        Etiqueta("¿A qué ritmo?")
        Segmentado(listOf("Suave", "Normal", "Rápido"), perfil.ritmo.ordinal) { onCambio(perfil.copy(ritmo = Ritmo.entries[it])) }
        Nota(textoRitmo(perfil))
    } else {
        Nota(resaltado("Comerás un ", "10 % por debajo", " de lo que gastas. Tu peso se mantendrá más o menos igual mientras cambia tu cuerpo."))
    }
}

@Composable
fun BloqueComida(perfil: Perfil, onCambio: (Perfil) -> Unit) {
    TituloPaso("¿Qué no puedes o no quieres comer?", "Lo usamos para que las ideas de comida te encajen.")
    BloqueAlergias(perfil, onCambio)
    BloqueGustos(perfil, onCambio)
    BloqueSupermercados(perfil, onCambio)
}

@Composable
fun BloqueAlergias(perfil: Perfil, onCambio: (Perfil) -> Unit) {
    Etiqueta("Alergias e intolerancias")
    FilaChips {
        ALERGENOS.forEach { a ->
            val sel = a in perfil.alergias
            ChipSeleccion(a, if (sel) EstadoChip.NO else EstadoChip.NEUTRO) {
                onCambio(perfil.copy(alergias = perfil.alergias.alternar(a)))
            }
        }
    }
}

@Composable
fun BloqueGustos(perfil: Perfil, onCambio: (Perfil) -> Unit) {
    // Lo escrito a mano que corresponde a una categoría cuenta como esa categoría
    fun de(t: String) = Gustos.categoriaDe(t)?.nombre
    val no = perfil.noMeGusta.mapNotNull(::de).toSet()
    val si = perfil.meEncanta.mapNotNull(::de).toSet()
    Etiqueta("Toca una vez: no me gusta · dos: me encanta")
    FilaChips {
        CATEGORIAS_GUSTO.forEach { cat ->
            val estado = when (cat.nombre) { in no -> EstadoChip.NO; in si -> EstadoChip.SI; else -> EstadoChip.NEUTRO }
            ChipSeleccion("${cat.emoji} ${cat.nombre}", estado) {
                val sinNo = perfil.noMeGusta.filterNot { de(it) == cat.nombre }.toSet()
                val sinSi = perfil.meEncanta.filterNot { de(it) == cat.nombre }.toSet()
                onCambio(
                    when (estado) {
                        EstadoChip.NEUTRO -> perfil.copy(noMeGusta = sinNo + cat.nombre, meEncanta = sinSi)
                        EstadoChip.NO -> perfil.copy(noMeGusta = sinNo, meEncanta = sinSi + cat.nombre)
                        EstadoChip.SI -> perfil.copy(noMeGusta = sinNo, meEncanta = sinSi)
                    }
                )
            }
        }
    }
    // Lo que no está en ninguna categoría se escribe a mano
    val anadir: (String, Boolean) -> Unit = { texto, gusta ->
        val t = de(texto) ?: texto
        onCambio(
            if (gusta) perfil.copy(meEncanta = perfil.meEncanta + t, noMeGusta = perfil.noMeGusta - t)
            else perfil.copy(noMeGusta = perfil.noMeGusta + t, meEncanta = perfil.meEncanta - t)
        )
    }
    Etiqueta("Otros que no me gustan")
    FilaChips {
        perfil.noMeGusta.filter { de(it) == null }.forEach { a ->
            ChipSeleccion(a, EstadoChip.NO) { onCambio(perfil.copy(noMeGusta = perfil.noMeGusta - a)) }
        }
        ChipAnadir { anadir(it, false) }
    }
    Etiqueta("Otros que me encantan")
    FilaChips {
        perfil.meEncanta.filter { de(it) == null }.forEach { a ->
            ChipSeleccion("♥ $a", EstadoChip.SI) { onCambio(perfil.copy(meEncanta = perfil.meEncanta - a)) }
        }
        ChipAnadir { anadir(it, true) }
    }
}

@Composable
fun BloqueSupermercados(perfil: Perfil, onCambio: (Perfil) -> Unit) {
    Etiqueta("Dónde compras")
    FilaChips {
        SUPERMERCADOS.forEach { s ->
            val sel = s in perfil.supermercados
            ChipSeleccion(s, if (sel) EstadoChip.SI else EstadoChip.NEUTRO) {
                onCambio(perfil.copy(supermercados = perfil.supermercados.alternar(s)))
            }
        }
    }
}
