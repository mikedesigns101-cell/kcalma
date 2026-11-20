package com.mikel.kcalma.ui.hoy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.mikel.kcalma.dominio.Calculadora
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ---------- Datos ----------

private data class Macro(val nombre: String, val actual: Int, val objetivo: Int, val color: Color)
private data class Sugerencia(val origen: String, val nombre: String, val kcal: Int, val p: Int, val h: Int)

private val sugerencias = listOf(
    Sugerencia("Mercadona · 15 min", "Pollo a la plancha con arroz y pimientos", 620, 52, 68),
    Sugerencia("Lidl · 10 min", "Ensalada de garbanzos con atún y huevo", 590, 46, 55),
    Sugerencia("Dia · 20 min", "Salmón al horno con patata y brócoli", 610, 44, 58),
)

private val MARGEN = 18.dp

// ---------- Pantalla ----------

@Composable
fun PantallaHoy(
    perfil: Perfil,
    entradas: List<Entrada>,
    onAbrirComida: (ComidaDia) -> Unit,
    onAnadir: (ComidaDia) -> Unit,
    onCamara: () -> Unit,
    onPestana: (Pestana) -> Unit,
    aviso: (@Composable () -> Unit)? = null,
) {
    val c = KcalmaTheme.colores
    val plan = remember(perfil) { Calculadora.calcular(perfil) }
    val comido = entradas.fold(Nutrientes()) { s, e -> s + e.nutrientes }
    val macros = listOf(
        Macro("Proteína", comido.proteinas.roundToInt(), plan.proteinaG, c.proteina),
        Macro("Hidratos", comido.hidratos.roundToInt(), plan.hidratosG, c.hidratos),
        Macro("Grasas", comido.grasas.roundToInt(), plan.grasasG, c.grasas),
    )
    val lateral = Modifier.padding(horizontal = MARGEN)
    val contexto = LocalContext.current
    val pronto: (String) -> Unit = { que ->
        Toast.makeText(contexto, "$que llega en una próxima versión", Toast.LENGTH_SHORT).show()
    }
    Box(Modifier.fillMaxSize().background(c.fondo0)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 130.dp),
        ) {
            if (aviso != null) item { Box(lateral) { aviso() } }
            item { Column(lateral) { Cabecera(perfil.pesoKg) } }
            item { Column(lateral) { Anillo(objetivo = plan.kcal, comido = comido.kcalInt, ejercicio = 0) } }
            item { Box(lateral) { FilaMacros(macros) } }
            item { Box(lateral) { TituloSeccion("Ideas para tu comida", "Ver más") { pronto("El recomendador de comidas") } } }
            item { FilaSugerencias { pronto("El recomendador de comidas") } }
            items(ComidaDia.entries) { cd ->
                Box(lateral) { TarjetaComida(cd, entradas.filter { it.comida == cd }, { onAbrirComida(cd) }, { onAnadir(cd) }) }
            }
        }
        BarraInferior(Modifier.align(Alignment.BottomCenter), Pestana.HOY, onPestana, onCamara)
    }
}

@Composable
private fun Modifier.tarjeta(): Modifier {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(14.dp)
    return clip(forma).background(c.fondo2).border(1.dp, c.borde, forma)
}

@Composable
private fun Cabecera(pesoKg: Double) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(c.degradado),
            contentAlignment = Alignment.Center,
        ) { Text("K", style = t.titulo, color = c.sobreAcento) }
        Spacer(Modifier.width(8.dp))
        Text("Kcalma", style = t.marca, color = c.texto)
        Spacer(Modifier.weight(1f))
        Text(
            "⚖️ " + "%.1f".format(Locale.US, pesoKg).removeSuffix(".0").replace('.', ',') + " kg",
            style = t.pequeno, color = c.texto2,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(c.fondo2)
                .border(1.dp, c.borde, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
    val fecha = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES")))
            .replaceFirstChar { it.uppercase() }
    }
    Text(fecha, style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
}

@Composable
private fun Anillo(objetivo: Int, comido: Int, ejercicio: Int) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val total = objetivo + ejercicio
    val progreso = (comido.toFloat() / total).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            val grosor = 14.dp.toPx()
            val esquina = Offset(grosor, grosor)
            val arco = Size(size.width - grosor * 2, size.height - grosor * 2)
            drawArc(c.fondo3, 0f, 360f, false, esquina, arco, style = Stroke(grosor))
            rotate(-90f) {
                // Vuelve a cian al final para que el remate redondo del inicio no salga magenta
                val degradado = Brush.sweepGradient(
                    0f to c.acento, progreso to c.acento2, (progreso + 1f) / 2f to c.acento2, 1f to c.acento,
                )
                // Halo suave, como el resplandor de la maqueta
                drawArc(degradado, 0f, 360f * progreso, false, esquina, arco, alpha = .22f,
                    style = Stroke(grosor * 2f, cap = StrokeCap.Round))
                drawArc(degradado, 0f, 360f * progreso, false, esquina, arco,
                    style = Stroke(grosor, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(miles(total - comido), style = t.numeroGrande, color = c.texto)
            Text("kcal restantes", style = t.cuerpo, color = c.texto2)
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp), horizontalArrangement = Arrangement.SpaceAround) {
        DatoResumen("Objetivo", miles(objetivo))
        DatoResumen("Comido", miles(comido))
        DatoResumen("Ejercicio", "+$ejercicio")
    }
}

@Composable
private fun DatoResumen(etiqueta: String, valor: String) {
    val c = KcalmaTheme.colores
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(etiqueta, style = KcalmaTheme.tipo.pequeno, color = c.texto3)
        Text(valor, style = KcalmaTheme.tipo.numero, color = c.texto)
    }
}

@Composable
private fun FilaMacros(macros: List<Macro>) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        macros.forEach { m ->
            Column(Modifier.weight(1f).tarjeta().padding(10.dp)) {
                Text(m.nombre, style = t.pequeno, color = c.texto2)
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp, bottom = 7.dp)) {
                    Text("${m.actual}", style = t.numero, color = c.texto)
                    Text(" / ${m.objetivo} g", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(bottom = 2.dp))
                }
                BarraProgreso(m.actual.toFloat() / m.objetivo, m.color)
            }
        }
    }
}

@Composable
private fun BarraProgreso(valor: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(KcalmaTheme.colores.fondo4)) {
        Box(Modifier.fillMaxWidth(valor.coerceIn(0f, 1f)).height(5.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun TituloSeccion(titulo: String, accion: String, onAccion: () -> Unit) {
    val c = KcalmaTheme.colores
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(titulo, style = KcalmaTheme.tipo.titulo, color = c.texto)
        Spacer(Modifier.weight(1f))
        Text(accion, style = KcalmaTheme.tipo.pequeno, color = c.acento, modifier = Modifier.clickable(onClick = onAccion).padding(4.dp))
    }
}

@Composable
private fun FilaSugerencias(onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    LazyRow(
        modifier = Modifier.padding(bottom = 16.dp),
        contentPadding = PaddingValues(horizontal = MARGEN),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(sugerencias) { s ->
            val forma = RoundedCornerShape(16.dp)
            Column(
                Modifier.width(220.dp).clip(forma).background(c.fondo2)
                    .border(BorderStroke(1.dp, Brush.linearGradient(listOf(c.acento.copy(alpha = .5f), c.acento2.copy(alpha = .5f)))), forma)
                    .clickable(onClick = onClick).padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.origen.uppercase(), style = t.diminuto, color = c.texto3, modifier = Modifier.weight(1f))
                    Chip("✓ Cuadra", fondo = c.verde.copy(alpha = .13f), tinta = c.verde)
                }
                Text(
                    s.nombre, style = t.tituloTarjeta, color = c.texto, maxLines = 2, minLines = 2,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Chip("${s.kcal} kcal")
                    Chip("P ${s.p}")
                    Chip("H ${s.h}")
                }
            }
        }
    }
}

@Composable
private fun Chip(
    texto: String,
    fondo: Color = KcalmaTheme.colores.fondo4,
    tinta: Color = KcalmaTheme.colores.texto2,
) {
    Text(
        texto, style = KcalmaTheme.tipo.diminuto, color = tinta, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(fondo).padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun TarjetaComida(comida: ComidaDia, entradas: List<Entrada>, onAbrir: () -> Unit, onAnadir: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val kcal = if (entradas.isEmpty()) null else entradas.sumOf { it.nutrientes.kcal }.roundToInt()
    val detalle = if (entradas.isEmpty()) null else entradas.joinToString(", ") { it.alimento.nombre }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).tarjeta().clickable(onClick = onAbrir).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(c.fondo4), contentAlignment = Alignment.Center) {
            Text(comida.icono, style = t.titulo)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(comida.nombre, style = t.tituloTarjeta, color = c.texto)
            Text(
                detalle ?: "Sin registrar", style = t.pequeno, color = c.texto3,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (kcal != null) Text("$kcal", style = t.tituloTarjeta, color = c.texto, modifier = Modifier.padding(end = 10.dp))
        run {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, c.borde, RoundedCornerShape(10.dp)).clickable(onClick = onAnadir),
                contentAlignment = Alignment.Center,
            ) { Text("+", style = t.numero, color = c.acento) }
        }
    }
}

// ---------- Barra inferior ----------

private enum class Icono { HOY, PROGRESO, IDEAS, PERFIL, CAMARA }

/** Pestañas de la barra de abajo. */
enum class Pestana { HOY, PROGRESO, IDEAS, PERFIL }

/** Barra de abajo compartida por las pestañas; el contenido de encima debe dejar ~130 dp libres. */
@Composable
fun BarraInferior(modifier: Modifier, actual: Pestana, onPestana: (Pestana) -> Unit, onCamara: () -> Unit) {
    val c = KcalmaTheme.colores
    // pointerInput vacío: la barra se queda los toques y no pasan a las tarjetas de debajo
    Box(modifier.fillMaxWidth().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }) {
        Row(
            Modifier.fillMaxWidth().background(c.fondo1)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ElementoBarra(Icono.HOY, "Hoy", actual == Pestana.HOY, Modifier.weight(1f)) { onPestana(Pestana.HOY) }
            ElementoBarra(Icono.PROGRESO, "Progreso", actual == Pestana.PROGRESO, Modifier.weight(1f)) { onPestana(Pestana.PROGRESO) }
            Spacer(Modifier.weight(1f))
            ElementoBarra(Icono.IDEAS, "Ideas", actual == Pestana.IDEAS, Modifier.weight(1f)) { onPestana(Pestana.IDEAS) }
            ElementoBarra(Icono.PERFIL, "Perfil", actual == Pestana.PERFIL, Modifier.weight(1f)) { onPestana(Pestana.PERFIL) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.borde))
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-26).dp)
                .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = c.acento, spotColor = c.acento)
                .size(62.dp).clip(RoundedCornerShape(22.dp)).background(c.degradado).clickable(onClick = onCamara),
            contentAlignment = Alignment.Center,
        ) { DibujoIcono(Icono.CAMARA, c.sobreAcento, 28) }
    }
}

@Composable
private fun ElementoBarra(icono: Icono, texto: String, activo: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val color = if (activo) c.acento else c.texto3
    Column(modifier.fillMaxHeight().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        DibujoIcono(icono, color, 22)
        Spacer(Modifier.height(3.dp))
        Text(texto, style = KcalmaTheme.tipo.diminuto, color = color)
    }
}

/** Iconos de línea dibujados a mano (viewBox 24x24) para no depender de librerías de iconos. */
@Composable
private fun DibujoIcono(icono: Icono, color: Color, tamano: Int) {
    Canvas(Modifier.size(tamano.dp)) {
        val e = size.width / 24f
        val trazo = Stroke(width = 2f * e, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(vararg puntos: Pair<Float, Float>, cerrar: Boolean = false) {
            val path = Path().apply {
                puntos.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * e, y * e) else lineTo(x * e, y * e) }
                if (cerrar) close()
            }
            drawPath(path, color, style = trazo)
        }
        when (icono) {
            Icono.HOY -> p(3f to 11f, 12f to 4f, 21f to 11f, 21f to 21f, 15f to 21f, 15f to 15f, 9f to 15f, 9f to 21f, 3f to 21f, cerrar = true)
            Icono.PROGRESO -> { p(4f to 19f, 4f to 10f); p(10f to 19f, 10f to 5f); p(16f to 19f, 16f to 12f); p(2f to 21f, 22f to 21f) }
            Icono.IDEAS -> {
                drawCircle(color, 6f * e, Offset(12f * e, 9f * e), style = trazo)
                p(9f to 18f, 15f to 18f); p(10f to 21f, 14f to 21f)
            }
            Icono.PERFIL -> {
                drawCircle(color, 4f * e, Offset(12f * e, 8f * e), style = trazo)
                drawArc(color, 180f, 180f, false, Offset(4f * e, 15f * e), Size(16f * e, 12f * e), style = trazo)
            }
            Icono.CAMARA -> {
                p(4f to 8f, 7f to 8f, 9f to 5f, 15f to 5f, 17f to 8f, 20f to 8f, 20f to 19f, 4f to 19f, cerrar = true)
                drawCircle(color, 3.5f * e, Offset(12f * e, 13f * e), style = trazo)
            }
        }
    }
}

private fun miles(n: Int): String = "%,d".format(java.util.Locale.US, n).replace(',', '.')
