package com.mikel.kcalma.ui.progreso

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.dominio.Pesada
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.hoy.miles
import com.mikel.kcalma.ui.perfil.kgTexto
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val ES = Locale.forLanguageTag("es-ES")

/** Días que se pueden ver hacia atrás (lo que carga App de la base). */
const val DIAS_HISTORIAL = 100L

/** Historial: un mes de días con las kcal frente al objetivo, los macros y el peso apuntado ese día. */
@Composable
fun PantallaHistorial(
    dias: Map<String, Nutrientes>, pesos: List<Pesada>, planKcal: Int,
    onAtras: () -> Unit, onDia: (String) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val hoy = remember { LocalDate.now() }
    val primero = remember(dias) { dias.keys.minOrNull()?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: hoy }
    val meses = remember(primero) { generateSequence(YearMonth.from(hoy)) { it.minusMonths(1) }.takeWhile { !it.isBefore(YearMonth.from(primero)) }.take(4).toList() }
    var mes by rememberSaveable { mutableStateOf(0) }
    val elegido = meses.getOrElse(mes) { meses.first() }
    val limite = hoy.minusDays(DIAS_HISTORIAL)
    val lista = (elegido.lengthOfMonth() downTo 1).map { elegido.atDay(it) }.filter { !it.isAfter(hoy) && !it.isBefore(limite) }
    val pesoDe = remember(pesos) { pesos.associate { it.fecha to it.kg } }

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding()) {
        Box(Modifier.padding(horizontal = 16.dp)) { BarraSuperior("‹", onAtras, "Historial") }
        Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            meses.forEachIndexed { i, m ->
                Text(
                    m.month.getDisplayName(java.time.format.TextStyle.FULL, ES).replaceFirstChar { it.uppercase() },
                    style = t.pequeno, color = if (i == mes) c.texto else c.texto2,
                    modifier = Modifier.vidrio(RoundedCornerShape(12.dp), resaltado = i == mes).clickable { mes = i }.padding(horizontal = 11.dp, vertical = 7.dp),
                )
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)) {
            items(lista, key = { it.toString() }) { d ->
                val n = dias[d.toString()]
                Row(
                    Modifier.padding(bottom = 7.dp).fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).clickable { onDia(d.toString()) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${d.dayOfMonth}", style = t.numero, color = c.texto)
                        Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, ES).uppercase().trimEnd('.'), style = t.diminuto, color = c.texto3)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        if (n == null) {
                            Text(if (d == hoy) "Hoy, sin apuntar todavía" else "Sin apuntar", style = t.tituloTarjeta, color = c.texto2)
                            Text("Toca para añadir lo que comiste", style = t.pequeno, color = c.texto3)
                        } else {
                            Text("${miles(n.kcalInt)} / ${miles(planKcal)} kcal", style = t.tituloTarjeta, color = c.texto)
                            Text("P ${n.proteinas.roundToInt()} · H ${n.hidratos.roundToInt()} · G ${n.grasas.roundToInt()}", style = t.pequeno, color = c.texto3)
                            Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.texto.copy(alpha = .1f))) {
                                val f = (n.kcal / planKcal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
                                Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(if (n.kcal > planKcal * 1.1) c.aviso else c.acento))
                            }
                        }
                    }
                    pesoDe[d.toString()]?.let { Text("${kgTexto(it)} kg", style = t.pequeno, color = c.texto2) }
                }
            }
            item {
                Text(
                    "Se guardan los últimos $DIAS_HISTORIAL días. Toca un día para ver lo que comiste y cambiarlo.",
                    style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Un día del historial: totales, peso de ese día y sus comidas. Se edita con las mismas pantallas que Hoy. */
@Composable
fun PantallaDia(
    fecha: String, entradas: List<Entrada>, planKcal: Int, peso: Double?,
    onAtras: () -> Unit, onComida: (ComidaDia) -> Unit, onAnadir: (ComidaDia) -> Unit, onPeso: (Double) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val titulo = remember(fecha) {
        runCatching { LocalDate.parse(fecha).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)).replaceFirstChar { it.uppercase() } }.getOrDefault(fecha)
    }
    val total = entradas.fold(Nutrientes()) { s, e -> s + e.nutrientes }
    var kg by remember(fecha, peso) { mutableStateOf(peso?.let { kgTexto(it) } ?: "") }

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding()) {
        Box(Modifier.padding(horizontal = 16.dp)) { BarraSuperior("‹", onAtras, titulo) }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)) {
            item {
                Row(Modifier.fillMaxWidth().vidrio(RoundedCornerShape(18.dp), resaltado = true).padding(14.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(miles(total.kcalInt), style = t.tituloGrande, color = c.texto)
                        Text("de ${miles(planKcal)} kcal", style = t.pequeno, color = c.texto3)
                    }
                    Text("P ${total.proteinas.roundToInt()} · H ${total.hidratos.roundToInt()} · G ${total.grasas.roundToInt()}", style = t.cuerpo, color = c.texto2)
                }
            }
            item {
                Row(Modifier.padding(top = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Peso de este día", style = t.cuerpo, color = c.texto2, modifier = Modifier.weight(1f))
                    BasicTextField(
                        kg, { n -> n.replace('.', ',').filter { it.isDigit() || it == ',' }.take(5).let { if (it.count { ch -> ch == ',' } <= 1) kg = it } },
                        singleLine = true, textStyle = t.numero.copy(color = c.texto, textAlign = TextAlign.End), cursorBrush = SolidColor(c.acento),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(64.dp).vidrio(RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 7.dp),
                        decorationBox = { campo -> Box(contentAlignment = Alignment.CenterEnd) { if (kg.isEmpty()) Text("–", style = t.numero, color = c.texto3); campo() } },
                    )
                    Text(" kg", style = t.pequeno, color = c.texto2)
                    val valor = kg.replace(',', '.').toDoubleOrNull()
                    if (valor != null && valor in 30.0..300.0 && valor != peso) Text(
                        "Guardar", style = t.tituloTarjeta, color = c.acento,
                        modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).clickable { onPeso(valor) }.padding(6.dp),
                    )
                }
            }
            ComidaDia.entries.forEach { comida ->
                val suyas = entradas.filter { it.comida == comida }
                item(key = "c${comida.name}") {
                    Row(Modifier.padding(top = 16.dp, bottom = 6.dp, start = 2.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(comida.nombre.uppercase(), style = t.etiqueta, color = c.texto3, modifier = Modifier.weight(1f))
                        if (suyas.isNotEmpty()) Text("${suyas.sumOf { it.nutrientes.kcal }.roundToInt()} kcal", style = t.pequeno, color = c.texto2)
                        Text(
                            "＋ Añadir", style = t.pequeno, color = c.acento,
                            modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).clickable { onAnadir(comida) }.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                }
                items(suyas, key = { it.id }) { e ->
                    Row(
                        Modifier.padding(bottom = 6.dp).fillMaxWidth().vidrio(RoundedCornerShape(14.dp)).clickable { onComida(comida) }.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(e.alimento.nombre, style = t.tituloTarjeta, color = c.texto, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(e.etiquetaCantidad, style = t.pequeno, color = c.texto3)
                        }
                        Text("${e.nutrientes.kcalInt}", style = t.tituloTarjeta, color = c.texto)
                    }
                }
            }
            item {
                Text(
                    "Toca una comida para cambiar cantidades o borrar; «Añadir» apunta en este día.",
                    style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
