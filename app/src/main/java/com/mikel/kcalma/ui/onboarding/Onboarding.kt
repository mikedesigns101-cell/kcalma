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
    val campos = remember { CamposDatos(null) }
    val datosValidos = campos.validos
    fun aplicarDatos() { perfil = campos.aplicar(perfil) }

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
                BloqueDatos(perfil, campos) { perfil = it }
            }
            2 -> Paso(p, onAtras = { paso-- }, onBoton = { paso++ }) {
                BloqueTrabajo(perfil) { perfil = it }
            }
            3 -> Paso(p, onAtras = { paso-- }, onBoton = { paso++ }) {
                BloquePasos(perfil) { perfil = it }
            }
            4 -> Paso(p, onAtras = { paso-- }, onBoton = { paso++ }) {
                BloqueEjercicio(perfil) { perfil = it }
            }
            5 -> Paso(p, onAtras = { paso-- }, boton = "Calcular mi plan", onBoton = { paso++ }) {
                BloqueObjetivo(perfil) { perfil = it }
            }
            6 -> Paso(p, onAtras = { paso-- }, boton = "¡Vamos!", onBoton = { paso++ }) { TuPlan(perfil) }
            else -> Paso(p, onAtras = { paso-- }, boton = "Terminar", onBoton = { onTerminar(perfil) },
                secundario = "Saltar por ahora", onSecundario = { onTerminar(perfil) }) {
                BloqueComida(perfil) { perfil = it }
            }
        }
    }
}

internal fun Set<String>.alternar(x: String) = if (x in this) this - x else this + x

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
internal fun Contador(etiqueta: String, valor: Int, onCambio: (Int) -> Unit) {
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
internal fun TuPlan(perfil: Perfil) {
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
        if (plan.correccion != 0) LineaDesglose("Ajuste por tu progreso real", (if (plan.correccion > 0) "+" else "−") + miles(kotlin.math.abs(plan.correccion)))
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
internal fun LineaDesglose(texto: String, valor: String, fuerte: Boolean = false, color: Color? = null) {
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
internal fun resaltado(antes: String, fuerte: String, despues: String) = buildAnnotatedString {
    append(antes)
    withStyle(SpanStyle(color = KcalmaTheme.colores.acento, fontWeight = FontWeight.SemiBold)) { append(fuerte) }
    append(despues)
}

@Composable
internal fun textoRitmo(p: Perfil) = run {
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

internal fun kg(x: Double): String {
    val s = if (x == Math.floor(x)) "%.0f".format(Locale.US, x) else "%.2f".format(Locale.US, x).trimEnd('0')
    return (if (x > 0) "+" else "") + s.replace('.', ',').replace("-", "−")
}

internal fun aproximada(f: LocalDate): String {
    val parte = when (f.dayOfMonth) { in 1..10 -> "principios"; in 11..20 -> "mediados"; else -> "finales" }
    val mes = f.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES"))
    val anio = if (f.year != LocalDate.now().year) " de ${f.year}" else ""
    return "$parte de $mes$anio"
}

internal fun miles(n: Int): String = "%,d".format(Locale.US, n).replace(',', '.').replace("-", "−")
