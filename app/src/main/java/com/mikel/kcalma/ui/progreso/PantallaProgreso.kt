package com.mikel.kcalma.ui.progreso

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Calculadora
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.dominio.Pesada
import com.mikel.kcalma.ui.componentes.Segmentado
import com.mikel.kcalma.ui.hoy.BarraInferior
import com.mikel.kcalma.ui.hoy.Pestana
import com.mikel.kcalma.ui.onboarding.miles
import com.mikel.kcalma.ui.perfil.fechaCorta
import com.mikel.kcalma.ui.perfil.kgTexto
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val ES = Locale.forLanguageTag("es-ES")

/** Pestaña Progreso: peso frente al ritmo del plan, calorías de la semana, proteína y racha. */
@Composable
fun PantallaProgreso(
    perfil: Perfil,
    pesos: List<Pesada>,
    /** Totales por día de los últimos ~100 días. */
    dias: Map<String, Nutrientes>,
    onPestana: (Pestana) -> Unit,
    onCamara: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val plan = remember(perfil) { Calculadora.calcular(perfil) }
    var periodo by rememberSaveable { mutableIntStateOf(1) }
    val hoy = LocalDate.now()
    val desde = when (periodo) { 0 -> hoy.minusMonths(1); 1 -> hoy.minusMonths(3); else -> LocalDate.MIN }
    val enPeriodo = pesos.filter { runCatching { !LocalDate.parse(it.fecha).isBefore(desde) }.getOrDefault(false) }
        .ifEmpty { listOfNotNull(pesos.lastOrNull()) }

    Box(Modifier.fillMaxSize().background(c.fondo0)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp).padding(top = 14.dp, bottom = 130.dp),
        ) {
            Text("Tu progreso", style = t.marca, color = c.texto)
            Spacer(Modifier.height(12.dp))
            Segmentado(listOf("1 mes", "3 meses", "Todo"), periodo) { periodo = it }
            Spacer(Modifier.height(12.dp))

            // ---- Peso ----
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Peso", style = t.titulo, color = c.texto2, modifier = Modifier.weight(1f))
                    Text("objetivo ${kgTexto(perfil.pesoObjetivoKg)} kg", style = t.pequeno, color = c.texto2)
                }
                val actual = pesos.lastOrNull()?.kg ?: perfil.pesoKg
                val primera = enPeriodo.firstOrNull()
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(kgTexto(actual), style = t.numeroGrande, color = c.texto)
                    val extra = if (primera != null && primera.fecha != enPeriodo.last().fecha) {
                        val d = actual - primera.kg
                        " kg · ${if (d > 0) "+" else if (d < 0) "−" else "±"}${kgTexto(abs(d))} kg desde el ${fechaCorta(primera.fecha)}"
                    } else " kg"
                    Text(extra, style = t.pequeno, color = c.texto3, modifier = Modifier.padding(bottom = 7.dp, start = 4.dp))
                }
                if (enPeriodo.size >= 2) {
                    GraficaPeso(enPeriodo, Calculadora.kgSemana(perfil))
                    val dias0 = ChronoUnit.DAYS.between(LocalDate.parse(enPeriodo.first().fecha), LocalDate.parse(enPeriodo.last().fecha))
                    val esperado = enPeriodo.first().kg + Calculadora.kgSemana(perfil) * dias0 / 7.0
                    val dif = enPeriodo.last().kg - esperado
                    val vaBajando = Calculadora.kgSemana(perfil) <= 0
                    val (texto, color) = when {
                        abs(dif) < 0.3 -> "justo a tu ritmo" to c.verde
                        (dif < 0) == vaBajando -> "por delante del plan" to c.verde
                        else -> "un poco por detrás del plan" to c.aviso
                    }
                    val fecha = Calculadora.fechaObjetivo(perfil)
                    Text(buildAnnotatedString {
                        append("Línea discontinua: el ritmo de tu plan. Vas ")
                        withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(texto) }
                        append(".")
                        if (fecha != null) {
                            append(" Llegarás hacia ")
                            withStyle(SpanStyle(color = c.texto, fontWeight = FontWeight.SemiBold)) {
                                append(fecha.month.getDisplayName(TextStyle.FULL, ES) + if (fecha.year != hoy.year) " de ${fecha.year}" else "")
                            }
                            append(".")
                        }
                    }, style = t.pequeno, color = c.texto2)
                } else {
                    Text("Apunta tu peso desde Perfil cada pocos días y aquí verás cómo bajas frente al ritmo de tu plan.",
                        style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 8.dp))
                }
            }

            // ---- Calorías de la semana ----
            val lunes = hoy.with(DayOfWeek.MONDAY)
            val semana = (0..6).map { lunes.plusDays(it.toLong()) }
            val conDatos = semana.filter { !it.isAfter(hoy) && dias[it.toString()] != null }
            val media = conDatos.map { dias[it.toString()]!!.kcal }.average().takeIf { !it.isNaN() }?.roundToInt()
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Calorías esta semana", style = t.titulo, color = c.texto2, modifier = Modifier.weight(1f))
                    Text("media ${media?.let { miles(it) } ?: "—"} / ${miles(plan.kcal)}", style = t.pequeno, color = c.texto2)
                }
                val tope = maxOf(plan.kcal * 1.3, semana.maxOf { dias[it.toString()]?.kcal ?: 0.0 })
                Box(Modifier.fillMaxWidth().height(100.dp).padding(top = 10.dp)) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
                        semana.forEach { d ->
                            val kcal = dias[d.toString()]?.kcal ?: 0.0
                            val color = when {
                                kcal <= 0 -> c.fondo4
                                kcal > plan.kcal * 1.1 -> c.aviso
                                else -> c.acento
                            }
                            Box(
                                Modifier.weight(1f).fillMaxHeight((kcal / tope).toFloat().coerceIn(.04f, 1f))
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(color),
                            )
                        }
                    }
                    val linea = c.acento2
                    Canvas(Modifier.fillMaxSize()) {
                        val y = size.height * (1 - (plan.kcal / tope).toFloat())
                        drawLine(linea, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    semana.forEach { d ->
                        Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, ES).uppercase(), style = t.diminuto,
                            color = if (d == hoy) c.acento else c.texto3, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ---- Proteína y racha ----
            val ultimos7 = (0..6).map { hoy.minusDays(it.toLong()).toString() }.mapNotNull { dias[it] }
            val proteina = ultimos7.map { it.proteinas }.average().takeIf { !it.isNaN() }?.roundToInt()
            var racha = 0
            var d = if (dias[hoy.toString()] != null) hoy else hoy.minusDays(1)
            while (dias[d.toString()] != null) { racha++; d = d.minusDays(1) }
            Tarjeta {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Proteína media", style = t.pequeno, color = c.texto2)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(proteina?.toString() ?: "—", style = t.campo, color = c.texto)
                            Text(" / ${plan.proteinaG} g", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Días apuntados", style = t.pequeno, color = c.texto2)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("🔥 $racha", style = t.campo, color = c.texto)
                            Text(if (racha == 1) " seguido" else " seguidos", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }
                }
            }
        }
        BarraInferior(Modifier.align(Alignment.BottomCenter), Pestana.PROGRESO, onPestana, onCamara)
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(forma).background(c.fondo2).border(1.dp, c.borde, forma).padding(14.dp)) {
        contenido()
    }
}

/** Línea del peso real (degradado) y, discontinua, la del ritmo del plan desde la primera pesada. */
@Composable
private fun GraficaPeso(pesos: List<Pesada>, kgSemana: Double) {
    val c = KcalmaTheme.colores
    val inicio = LocalDate.parse(pesos.first().fecha)
    val xs = pesos.map { ChronoUnit.DAYS.between(inicio, LocalDate.parse(it.fecha)).toFloat() }
    val total = xs.last().coerceAtLeast(1f)
    val planFinal = pesos.first().kg + kgSemana * total / 7.0
    val valores = pesos.map { it.kg } + planFinal
    val min = valores.min() - 0.3
    val max = valores.max() + 0.3
    val degradado = c.degradado
    val gris = c.texto3
    val punto = c.acento2
    Canvas(Modifier.fillMaxWidth().height(96.dp).padding(vertical = 8.dp)) {
        fun x(v: Float) = v / total * size.width
        fun y(kg: Double) = (size.height * (1 - (kg - min) / (max - min))).toFloat()
        drawLine(gris, Offset(0f, y(pesos.first().kg)), Offset(size.width, y(planFinal)), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        val path = Path().apply { pesos.forEachIndexed { i, p -> if (i == 0) moveTo(x(xs[i]), y(p.kg)) else lineTo(x(xs[i]), y(p.kg)) } }
        drawPath(path, degradado, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(punto, 4.dp.toPx(), Offset(x(xs.last()), y(pesos.last().kg)))
    }
}
