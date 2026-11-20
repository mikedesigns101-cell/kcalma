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

private const val PASOS_TOTALES = 7

@Composable
fun Onboarding(onTerminar: (Perfil) -> Unit) {
    var paso by rememberSaveable { mutableIntStateOf(0) }
    var perfil by remember { mutableStateOf(Perfil()) }
    // Los campos de texto se guardan aparte para poder dejarlos vacíos mientras se escribe
    var edad by rememberSaveable { mutableStateOf("") }
    var altura by rememberSaveable { mutableStateOf("") }
    var peso by rememberSaveable { mutableStateOf("") }
    var pesoObj by rememberSaveable { mutableStateOf("") }
    var grasa by rememberSaveable { mutableStateOf("") }

    fun num(s: String) = s.replace(',', '.').toDoubleOrNull()
    val datosValidos = num(edad)?.let { it in 14.0..100.0 } == true &&
        num(altura)?.let { it in 120.0..230.0 } == true &&
        num(peso)?.let { it in 35.0..300.0 } == true &&
        num(pesoObj)?.let { it in 35.0..300.0 } == true &&
        (grasa.isEmpty() || num(grasa)?.let { it in 3.0..60.0 } == true)

    fun aplicarDatos() {
        perfil = perfil.copy(
            edad = num(edad)!!.toInt(), alturaCm = num(altura)!!.toInt(),
            pesoKg = num(peso)!!, pesoObjetivoKg = num(pesoObj)!!, grasaPct = num(grasa),
        )
    }

    BackHandler(enabled = paso > 0) { paso-- }

    val c = KcalmaTheme.colores
    AnimatedContent(
        targetState = paso,
        transitionSpec = {
            val dir = if (targetState > initialState) 1 else -1
            (slideInHorizontally { it * dir / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 4 } + fadeOut())
        },
        modifier = Modifier.fillMaxSize().background(c.fondo0),
        label = "onboarding",
    ) { p ->
        when (p) {
            0 -> Bienvenida { paso = 1 }
            1 -> Paso(p, onAtras = { paso-- }, boton = "Siguiente", botonActivo = datosValidos, onBoton = { aplicarDatos(); paso++ }) {
                TituloPaso("Empecemos por ti", "Con esto calculamos lo que gasta tu cuerpo en reposo.")
                Segmentado(listOf("Hombre", "Mujer"), perfil.sexo.ordinal) { perfil = perfil.copy(sexo = Sexo.entries[it]) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoNumero("Edad", edad, "años", { edad = it }, Modifier.weight(1f), ejemplo = "30")
                    CampoNumero("Altura", altura, "cm", { altura = it }, Modifier.weight(1f), ejemplo = "175")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoNumero("Peso actual", peso, "kg", { peso = it }, Modifier.weight(1f), ejemplo = "80", decimales = true)
                    CampoNumero("Peso objetivo", pesoObj, "kg", { pesoObj = it }, Modifier.weight(1f), ejemplo = "75", decimales = true)
                }
                CampoNumero("% de grasa corporal (opcional)", grasa, "%", { grasa = it }, Modifier.fillMaxWidth(), ejemplo = "No lo sé", decimales = true)
                Nota(resaltado("Si conoces tu % de grasa (báscula de bioimpedancia, plicómetro…) el cálculo es ", "más preciso", ". Si no, no pasa nada."))
            }
            2 -> Paso(p, onAtras = { paso-- }, onBoton = { paso++ }) {
                TituloPaso("¿Cómo es tu trabajo o tu día?", "Sin contar el ejercicio. Piensa en un día normal.")
                val ops = listOf(
                    Triple("💻", "Casi todo sentado", "Oficina, teletrabajo, estudiante, conductor"),
                    Triple("🧍", "De pie o moviéndome a ratos", "Dependiente, profesor, peluquero, camarero de barra"),
                    Triple("🚶", "Moviéndome casi todo el día", "Camarero de sala, enfermero, repartidor a pie, reponedor"),
                    Triple("🏗️", "Trabajo físico duro", "Albañil, mudanzas, campo, almacén cargando peso"),
                )
                ops.forEachIndexed { i, (e, t, d) ->
                    Opcion(e, t, d, perfil.trabajo.ordinal == i) { perfil = perfil.copy(trabajo = Trabajo.entries[i]) }
                }
            }
            3 -> Paso(p, onAtras = { paso-- }, onBoton = { paso++ }) {
                TituloPaso("¿Cuánto andas al día?", "Si no lo sabes, mira la app Salud o Fit de tu móvil, o elige lo que más se parezca.")
                val ops = listOf(
                    Triple("🛋️", "Menos de 5.000 pasos", "Casi todo en coche, poco paseo"),
                    Triple("🚶", "5.000 – 8.000 pasos", "Algún paseo, recados andando"),
                    Triple("🏃", "8.000 – 12.000 pasos", "Vas andando a casi todo o paseas a diario"),
                    Triple("⛰️", "Más de 12.000 pasos", "Andas muchísimo, paseas al perro largo…"),
                )
                ops.forEachIndexed { i, (e, t, d) ->
                    Opcion(e, t, d, perfil.pasos.ordinal == i) { perfil = perfil.copy(pasos = Pasos.entries[i]) }
                }
                Nota(resaltado("📱 Próximamente: ", "leer tus pasos automáticamente", " con Health Connect para ajustar tus calorías cada día."))
            }
            4 -> Paso(p, onAtras = { paso-- }, onBoton = { paso++ }) {
                TituloPaso("¿Haces ejercicio?", "Lo que haces de verdad, no lo que te gustaría 😉")
                Contador("Días por semana", perfil.diasEjercicio) { perfil = perfil.copy(diasEjercicio = it.coerceIn(0, 7)) }
                if (perfil.diasEjercicio > 0) {
                    Etiqueta("Tipo de ejercicio")
                    val ops = listOf(
                        Triple("🏋️", "Pesas / gimnasio", "Fuerza, máquinas, crossfit"),
                        Triple("🏃", "Cardio", "Correr, bici, natación, spinning"),
                        Triple("⚽", "Deporte", "Fútbol, pádel, baloncesto…"),
                    )
                    ops.forEachIndexed { i, (e, t, d) ->
                        Opcion(e, t, d, perfil.tipoEjercicio.ordinal == i) { perfil = perfil.copy(tipoEjercicio = TipoEjercicio.entries[i]) }
                    }
                    Etiqueta("Cada sesión dura")
                    Segmentado(listOf("30 min", "1 hora", "1,5 h"), perfil.duracion.ordinal) { perfil = perfil.copy(duracion = Duracion.entries[it]) }
                }
            }
            5 -> Paso(p, onAtras = { paso-- }, boton = "Calcular mi plan", onBoton = { paso++ }) {
                TituloPaso("¿Qué quieres conseguir?", "Puedes cambiarlo cuando quieras.")
                val ops = listOf(
                    Triple("📉", "Perder grasa", "Bajar de peso manteniendo el músculo"),
                    Triple("🔄", "Recomposición", "Mismo peso, menos grasa y más músculo. Ideal si empiezas en el gimnasio"),
                    Triple("💪", "Ganar músculo", "Subir de peso poco a poco, sin coger mucha grasa"),
                )
                ops.forEachIndexed { i, (e, t, d) ->
                    Opcion(e, t, d, perfil.objetivo.ordinal == i) { perfil = perfil.copy(objetivo = Objetivo.entries[i]) }
                }
                if (perfil.objetivo != Objetivo.RECOMPOSICION) {
                    Etiqueta("¿A qué ritmo?")
                    Segmentado(listOf("Suave", "Normal", "Rápido"), perfil.ritmo.ordinal) { perfil = perfil.copy(ritmo = Ritmo.entries[it]) }
                    Nota(textoRitmo(perfil))
                } else {
                    Nota(resaltado("Comerás un ", "10 % por debajo", " de lo que gastas. Tu peso se mantendrá más o menos igual mientras cambia tu cuerpo."))
                }
            }
            6 -> Paso(p, onAtras = { paso-- }, boton = "¡Vamos!", onBoton = { paso++ }) { TuPlan(perfil) }
            else -> Paso(p, onAtras = { paso-- }, boton = "Terminar", onBoton = { onTerminar(perfil) },
                secundario = "Saltar por ahora", onSecundario = { onTerminar(perfil) }) {
                TituloPaso("¿Qué no puedes o no quieres comer?", "Lo usamos para que las ideas de comida te encajen.")
                Etiqueta("Alergias e intolerancias")
                FilaChips {
                    ALERGENOS.forEach { a ->
                        val sel = a in perfil.alergias
                        ChipSeleccion(a, if (sel) EstadoChip.NO else EstadoChip.NEUTRO) {
                            perfil = perfil.copy(alergias = perfil.alergias.alternar(a))
                        }
                    }
                }
                Etiqueta("No me gusta")
                FilaChips {
                    perfil.noMeGusta.forEach { a ->
                        ChipSeleccion(a, EstadoChip.NO) { perfil = perfil.copy(noMeGusta = perfil.noMeGusta - a) }
                    }
                    ChipAnadir { perfil = perfil.copy(noMeGusta = perfil.noMeGusta + it) }
                }
                Etiqueta("Me encanta")
                FilaChips {
                    perfil.meEncanta.forEach { a ->
                        ChipSeleccion("♥ $a", EstadoChip.SI) { perfil = perfil.copy(meEncanta = perfil.meEncanta - a) }
                    }
                    ChipAnadir { perfil = perfil.copy(meEncanta = perfil.meEncanta + it) }
                }
                Etiqueta("Dónde compras")
                FilaChips {
                    SUPERMERCADOS.forEach { s ->
                        val sel = s in perfil.supermercados
                        ChipSeleccion(s, if (sel) EstadoChip.SI else EstadoChip.NEUTRO) {
                            perfil = perfil.copy(supermercados = perfil.supermercados.alternar(s))
                        }
                    }
                }
            }
        }
    }
}

private fun Set<String>.alternar(x: String) = if (x in this) this - x else this + x

/** Plantilla común: cabecera con progreso, contenido con scroll y botón abajo. */
@Composable
private fun Paso(
    numero: Int,
    onAtras: () -> Unit,
    boton: String = "Siguiente",
    botonActivo: Boolean = true,
    onBoton: () -> Unit,
    secundario: String? = null,
    onSecundario: () -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        CabeceraPaso(numero.toFloat() / PASOS_TOTALES, onAtras)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), content = contenido)
        Spacer(Modifier.height(12.dp))
        BotonPrincipal(boton, onBoton, activo = botonActivo)
        if (secundario != null) {
            Text(
                secundario, style = KcalmaTheme.tipo.pequeno, color = KcalmaTheme.colores.texto3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSecundario).padding(vertical = 12.dp),
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Bienvenida(onEmpezar: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.shadow(28.dp, RoundedCornerShape(24.dp), ambientColor = c.acento2, spotColor = c.acento2)
                    .size(80.dp).clip(RoundedCornerShape(24.dp)).background(c.degradado),
                contentAlignment = Alignment.Center,
            ) { Text("K", style = t.tituloGrande, color = c.sobreAcento) }
            Spacer(Modifier.height(22.dp))
            Text("Kcalma", style = t.tituloGrande, color = c.texto)
            Text(
                "Come lo que te gusta y pierde peso sin agobios. Te lo cuadramos nosotros.",
                style = t.cuerpo, color = c.texto2, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp).width(260.dp),
            )
            Spacer(Modifier.height(26.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("📸  Haz una foto y sabrás lo que comes", "🎯  Calorías y macros hechas a tu medida", "🛒  Ideas con productos de tu súper")
                    .forEach { Text(it, style = t.cuerpo, color = c.texto2) }
            }
        }
        BotonPrincipal("Empezar · 2 minutos", onEmpezar)
        Text("Tus datos se quedan en tu móvil", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun Contador(etiqueta: String, valor: Int, onCambio: (Int) -> Unit) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(forma).background(c.fondo2).border(1.dp, c.borde, forma)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta.uppercase(), style = KcalmaTheme.tipo.etiqueta, color = c.texto2)
            Text(if (valor == 0) "Ninguno" else "$valor", style = KcalmaTheme.tipo.campo, color = c.texto)
        }
        listOf("−" to -1, "+" to 1).forEach { (s, d) ->
            Box(
                Modifier.padding(start = 8.dp).size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.fondo4)
                    .clickable { onCambio(valor + d) },
                contentAlignment = Alignment.Center,
            ) { Text(s, style = KcalmaTheme.tipo.campo, color = c.acento) }
        }
    }
}

@Composable
private fun TuPlan(perfil: Perfil) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val plan = Calculadora.calcular(perfil)
    TituloPaso("Tu plan diario")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(miles(plan.kcal), style = t.numeroPlan.copy(brush = c.degradado))
        Text("kcal al día", style = t.cuerpo, color = c.texto2)
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("${plan.proteinaG} g", "Proteína", c.proteina),
            Triple("${plan.hidratosG} g", "Hidratos", c.hidratos),
            Triple("${plan.grasasG} g", "Grasas", c.grasas),
        ).forEach { (v, n, col) ->
            val forma = RoundedCornerShape(13.dp)
            Column(
                Modifier.weight(1f).clip(forma).background(c.fondo2).border(1.dp, c.borde, forma).padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(v, style = t.campo, color = c.texto)
                Text(n, style = t.diminuto, color = c.texto3)
                Box(Modifier.padding(top = 6.dp).width(44.dp).height(4.dp).clip(CircleShape).background(col))
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    val forma = RoundedCornerShape(13.dp)
    Column(
        Modifier.fillMaxWidth().clip(forma).background(c.fondo1).border(1.dp, c.borde, forma).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LineaDesglose("Tu cuerpo en reposo", miles(plan.reposo))
        LineaDesglose("Trabajo y pasos", "+${miles(plan.actividad)}")
        if (plan.ejercicio > 0) LineaDesglose("Ejercicio (media diaria)", "+${miles(plan.ejercicio)}")
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.borde))
        LineaDesglose("Gastas al día", miles(plan.gasto), fuerte = true)
        val nombreAjuste = when (perfil.objetivo) {
            Objetivo.PERDER -> "Déficit para %s kg/semana".format(kg(plan.kgSemana))
            Objetivo.RECOMPOSICION -> "Déficit suave (−10 %)"
            Objetivo.GANAR -> "Superávit para +%s kg/semana".format(kg(plan.kgSemana))
        }
        LineaDesglose(nombreAjuste, (if (plan.ajuste > 0) "+" else "") + miles(plan.ajuste), color = c.acento)
    }
    Spacer(Modifier.height(10.dp))
    if (plan.limitadoPorMinimo) {
        Nota(resaltado("Hemos subido tu objetivo al ", "mínimo seguro", " de calorías. Bajar más no compensa: perderías músculo y pasarías hambre."))
    }
    Nota(resaltado("Es un ", "punto de partida", ". Si te pesas a menudo, Kcalma aprenderá tu gasto real y lo ajustará cada semana."))
}

@Composable
private fun LineaDesglose(texto: String, valor: String, fuerte: Boolean = false, color: Color? = null) {
    val c = KcalmaTheme.colores
    Row(Modifier.fillMaxWidth()) {
        Text(texto, style = KcalmaTheme.tipo.pequeno, color = if (fuerte) c.texto else c.texto2, modifier = Modifier.weight(1f))
        Text(
            valor, style = KcalmaTheme.tipo.pequeno.copy(fontWeight = FontWeight.SemiBold),
            color = color ?: c.texto,
        )
    }
}

@Composable
private fun resaltado(antes: String, fuerte: String, despues: String) = buildAnnotatedString {
    append(antes)
    withStyle(SpanStyle(color = KcalmaTheme.colores.acento, fontWeight = FontWeight.SemiBold)) { append(fuerte) }
    append(despues)
}

@Composable
private fun textoRitmo(p: Perfil) = run {
    val semanal = Calculadora.kgSemana(p)
    val fecha = Calculadora.fechaObjetivo(p)
    val consejo = when (p.ritmo) {
        Ritmo.SUAVE -> "Muy llevadero, ideal si te cuesta mantener una dieta."
        Ritmo.NORMAL -> "Es el ritmo más fácil de mantener sin pasar hambre."
        Ritmo.RAPIDO -> "Exige más fuerza de voluntad. Mejor si tienes bastante que perder."
    }
    val cuando = fecha?.let { " Llegarías a ${kg(p.pesoObjetivoKg).removePrefix("+")} kg hacia ${aproximada(it)}." } ?: ""
    resaltado("Ritmo ${p.ritmo.name.lowercase()}: ", "${kg(semanal)} kg por semana", ".$cuando $consejo")
}

private fun kg(x: Double): String {
    val s = if (x == Math.floor(x)) "%.0f".format(Locale.US, x) else "%.2f".format(Locale.US, x).trimEnd('0')
    return (if (x > 0) "+" else "") + s.replace('.', ',').replace("-", "−")
}

private fun aproximada(f: LocalDate): String {
    val parte = when (f.dayOfMonth) { in 1..10 -> "principios"; in 11..20 -> "mediados"; else -> "finales" }
    val mes = f.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES"))
    val anio = if (f.year != LocalDate.now().year) " de ${f.year}" else ""
    return "$parte de $mes$anio"
}

private fun miles(n: Int): String = "%,d".format(Locale.US, n).replace(',', '.').replace("-", "−")
