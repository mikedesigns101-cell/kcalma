package com.mikel.kcalma.ui.hoy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Calculadora
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.Idea
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.ui.componentes.BotonCristal
import com.mikel.kcalma.ui.componentes.BurbujaIcono
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.entrada
import com.mikel.kcalma.ui.componentes.esferaBrillo
import com.mikel.kcalma.ui.componentes.icono
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ---------- Datos ----------

private data class Macro(val nombre: String, val actual: Int, val objetivo: Int, val color: Color)

private val MARGEN = 18.dp

/** Lo que ofrece el botón + de la barra. Lo da App; si falta, el + abre la cámara como antes. */
class AccionesMas(
    val buscar: () -> Unit,
    val voz: () -> Unit,
    val foto: () -> Unit,
    val escanear: () -> Unit,
)

val LocalAccionesMas = staticCompositionLocalOf<AccionesMas?> { null }

// ---------- Pantalla ----------

@Composable
fun PantallaHoy(
    perfil: Perfil,
    entradas: List<Entrada>,
    onAbrirComida: (ComidaDia) -> Unit,
    onAnadir: (ComidaDia) -> Unit,
    onCamara: () -> Unit,
    onPestana: (Pestana) -> Unit,
    /** Ideas para la próxima comida, ya ajustadas a lo que queda. */
    ideas: List<Idea> = emptyList(),
    tituloIdeas: String = "Ideas para tu comida",
    onIdea: (Idea) -> Unit = {},
    onVerIdeas: () -> Unit = {},
    aviso: (@Composable () -> Unit)? = null,
    /** Kcal de ejercicio extra que se suman hoy (la mitad de lo que se mueve de más). */
    ejercicio: Int = 0,
    detalleEjercicio: String? = null,
    /** Propuesta semanal de ajuste del plan. */
    tarjeta: (@Composable () -> Unit)? = null,
    onPeso: () -> Unit = {},
    onAvisos: () -> Unit = {},
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val plan = remember(perfil) { Calculadora.calcular(perfil) }
    val comido = entradas.fold(Nutrientes()) { s, e -> s + e.nutrientes }
    val macros = listOf(
        Macro("Proteína", comido.proteinas.roundToInt(), plan.proteinaG, c.proteina),
        Macro("Hidratos", comido.hidratos.roundToInt(), plan.hidratosG, c.hidratos),
        Macro("Grasas", comido.grasas.roundToInt(), plan.grasasG, c.grasas),
    )
    val total = plan.kcal + ejercicio
    val progreso = if (total > 0) (comido.kcalInt.toFloat() / total).coerceIn(0f, 1f) else 0f
    val acciones = LocalAccionesMas.current
    val lateral = Modifier.padding(horizontal = MARGEN)
    var orden = 0
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 130.dp),
        ) {
            if (aviso != null) item { Box(lateral) { aviso() } }
            item { Box(lateral) { Cabecera(onPeso, onAvisos) } }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Pompa(restantes = total - comido.kcalInt, progreso = progreso)
                    Row(Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        DatoResumen("Objetivo", miles(plan.kcal))
                        DatoResumen("Comido", miles(comido.kcalInt))
                        DatoResumen("Ejercicio", "+$ejercicio", if (ejercicio > 0) c.verde else c.texto)
                    }
                }
            }
            if (detalleEjercicio != null) item {
                Text(
                    detalleEjercicio, style = t.pequeno, color = c.texto3, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN).padding(top = 6.dp),
                )
            }
            item { Box(lateral.entrada(orden++)) { Saludo() } }
            item {
                Box(lateral.entrada(orden++)) {
                    Buscador(
                        onBuscar = { acciones?.buscar?.invoke() ?: onAnadir(ComidaDia.segunHora(LocalTime.now().hour)) },
                        onVoz = { acciones?.voz?.invoke() ?: onAnadir(ComidaDia.segunHora(LocalTime.now().hour)) },
                        onCamara = onCamara,
                    )
                }
            }
            item { Box(lateral.entrada(orden++)) { FilaMacros(macros) } }
            if (tarjeta != null) item { Box(lateral) { tarjeta() } }
            items(ComidaDia.entries.chunked(2)) { par ->
                Row(lateral.entrada(orden++).padding(bottom = 9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    par.forEach { cd ->
                        TarjetaComida(cd, entradas.filter { it.comida == cd }, { onAbrirComida(cd) }, { onAnadir(cd) }, Modifier.weight(1f))
                    }
                    if (par.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (ideas.isNotEmpty()) {
                item { Box(lateral.padding(top = 8.dp)) { TituloSeccion(tituloIdeas, "Ver más", onVerIdeas) } }
                item { FilaSugerencias(ideas.take(5), onIdea) }
            }
        }
        BarraInferior(Modifier.align(Alignment.BottomCenter), Pestana.HOY, onPestana, onCamara)
    }
}

@Composable
private fun Cabecera(onPeso: () -> Unit, onAvisos: () -> Unit) {
    val c = KcalmaTheme.colores
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BotonCristal(Ic.PESO, onPeso)
        Text("Kcalma", style = KcalmaTheme.tipo.marca, color = c.texto, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        BotonCristal(Ic.AVISOS, onAvisos)
    }
}

@Composable
private fun Saludo() {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val (saludo, fecha) = remember {
        val h = LocalTime.now().hour
        val s = when (h) { in 6..13 -> "Buenos días"; in 14..20 -> "Buenas tardes"; else -> "Buenas noches" }
        s to LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES")))
            .replaceFirstChar { it.uppercase() }
    }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(saludo, style = t.tituloPaso, color = c.texto)
        Text("$fecha · ¿Qué has comido?", style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Buscador(onBuscar: () -> Unit, onVoz: () -> Unit, onCamara: () -> Unit) {
    val c = KcalmaTheme.colores
    Row(
        Modifier.fillMaxWidth().vidrio(RoundedCornerShape(28.dp)).clickable(onClick = onBuscar)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icono(Ic.BUSCAR, tamano = 18.dp, color = c.texto2)
        Text("Busca un alimento…", style = KcalmaTheme.tipo.cuerpo, color = c.texto3, modifier = Modifier.weight(1f).padding(start = 9.dp))
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onVoz), contentAlignment = Alignment.Center) {
            Icono(Ic.VOZ, tamano = 19.dp, color = c.texto2)
        }
        Box(
            Modifier.padding(start = 4.dp).size(40.dp).shadow(10.dp, CircleShape, ambientColor = c.acento, spotColor = c.acento)
                .esferaBrillo().clickable(onClick = onCamara),
            contentAlignment = Alignment.Center,
        ) { Icono(Ic.CAMARA, tamano = 19.dp, color = c.sobreAcento) }
    }
}

@Composable
private fun DatoResumen(etiqueta: String, valor: String, color: Color = KcalmaTheme.colores.texto) {
    val c = KcalmaTheme.colores
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(etiqueta, style = KcalmaTheme.tipo.pequeno, color = c.texto3)
        Text(valor, style = KcalmaTheme.tipo.numero, color = color)
    }
}

@Composable
private fun FilaMacros(macros: List<Macro>) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        macros.forEach { m ->
            Column(Modifier.weight(1f).vidrio(RoundedCornerShape(16.dp)).padding(horizontal = 11.dp, vertical = 9.dp)) {
                Text(m.nombre, style = t.diminuto, color = c.texto2)
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)) {
                    Text("${m.actual}", style = t.numero, color = c.texto)
                    Text(" / ${m.objetivo} g", style = t.diminuto, color = c.texto3, modifier = Modifier.padding(bottom = 2.dp))
                }
                BarraProgreso(if (m.objetivo > 0) m.actual.toFloat() / m.objetivo else 0f, m.color)
            }
        }
    }
}

@Composable
private fun BarraProgreso(valor: Float, color: Color) {
    val v = valor.coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(KcalmaTheme.colores.fondo3)) {
        Box(
            Modifier.fillMaxWidth(v).height(4.dp)
                .shadow(6.dp, CircleShape, ambientColor = color, spotColor = color)
                .clip(CircleShape).background(color)
        )
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
private fun FilaSugerencias(ideas: List<Idea>, onClick: (Idea) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    LazyRow(
        modifier = Modifier.padding(bottom = 16.dp),
        contentPadding = PaddingValues(horizontal = MARGEN),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(ideas) { idea ->
            val s = idea.receta
            val n = s.nutrientes
            Column(
                Modifier.width(220.dp).vidrio(RoundedCornerShape(20.dp)).clickable { onClick(idea) }.padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.emoji}  ${s.minutos} MIN", style = t.diminuto, color = c.texto3, modifier = Modifier.weight(1f))
                    if (idea.encaja) Chip("✓ Encaja", fondo = c.verde.copy(alpha = .13f), tinta = c.verde)
                }
                Text(
                    s.nombre, style = t.tituloTarjeta, color = c.texto, maxLines = 2, minLines = 2,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Chip("${n.kcalInt} kcal")
                    Chip("P ${n.proteinas.roundToInt()}")
                    Chip("H ${n.hidratos.roundToInt()}")
                }
            }
        }
    }
}

@Composable
private fun Chip(
    texto: String,
    fondo: Color = KcalmaTheme.colores.fondo3,
    tinta: Color = KcalmaTheme.colores.texto2,
) {
    Text(
        texto, style = KcalmaTheme.tipo.diminuto, color = tinta, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(fondo).padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun TarjetaComida(comida: ComidaDia, entradas: List<Entrada>, onAbrir: () -> Unit, onAnadir: () -> Unit, modifier: Modifier) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val kcal = if (entradas.isEmpty()) null else entradas.sumOf { it.nutrientes.kcal }.roundToInt()
    Row(
        modifier.vidrio(RoundedCornerShape(18.dp)).clickable(onClick = onAbrir).padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BurbujaIcono(comida.icono(), tamano = 34.dp, icon = 17.dp)
        Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
            Text(comida.nombre, style = t.tituloTarjeta, color = c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (kcal != null) "$kcal kcal" else "Sin registrar", style = t.pequeno, color = c.texto2, maxLines = 1)
        }
        Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onAnadir), contentAlignment = Alignment.Center) {
            Icono(Ic.MAS, tamano = 16.dp, color = if (kcal == null) c.acento else c.texto3)
        }
    }
}

// ---------- Barra inferior ----------

/** Pestañas de la barra de abajo. */
enum class Pestana { HOY, PROGRESO, IDEAS, PERFIL }

/** Dónde estaba la burbuja de la barra, para que al cambiar de pestaña se deslice desde ahí. */
private object MemoriaBarra { var hueco = 0f }

/** Barra de abajo compartida por las pestañas; el contenido de encima debe dejar ~130 dp libres. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraInferior(modifier: Modifier, actual: Pestana, onPestana: (Pestana) -> Unit, onCamara: () -> Unit) {
    val c = KcalmaTheme.colores
    val mov = KcalmaTheme.movimiento
    val acciones = LocalAccionesMas.current
    var hoja by remember { mutableStateOf(false) }
    val huecos = listOf(Pestana.HOY to 0, Pestana.PROGRESO to 1, Pestana.IDEAS to 3, Pestana.PERFIL to 4)
    val destino = huecos.first { it.first == actual }.second.toFloat()
    val burbuja = remember { Animatable(MemoriaBarra.hueco) }
    LaunchedEffect(destino) {
        MemoriaBarra.hueco = destino
        if (mov) burbuja.animateTo(destino, spring(dampingRatio = .72f, stiffness = 380f)) else burbuja.snapTo(destino)
    }

    // pointerInput vacío: la barra se queda los toques y no pasan a las tarjetas de debajo
    Box(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(68.dp).vidrio(RoundedCornerShape(30.dp))) {
            val hueco = maxWidth / 5
            // Burbuja de cristal bajo la pestaña elegida
            Box(
                Modifier.offset(x = hueco * burbuja.value + 5.dp, y = 7.dp).width(hueco - 10.dp).height(54.dp)
                    .vidrio(RoundedCornerShape(22.dp))
                    .drawBehind { drawRect(c.luz1.copy(alpha = c.luz1.alpha * .5f)) }
            )
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                ElementoBarra(Ic.HOY, "Hoy", actual == Pestana.HOY, Modifier.weight(1f)) { onPestana(Pestana.HOY) }
                ElementoBarra(Ic.PROGRESO, "Progreso", actual == Pestana.PROGRESO, Modifier.weight(1f)) { onPestana(Pestana.PROGRESO) }
                Spacer(Modifier.weight(1f))
                ElementoBarra(Ic.IDEAS, "Ideas", actual == Pestana.IDEAS, Modifier.weight(1f)) { onPestana(Pestana.IDEAS) }
                ElementoBarra(Ic.PERFIL, "Perfil", actual == Pestana.PERFIL, Modifier.weight(1f)) { onPestana(Pestana.PERFIL) }
            }
        }
        val pulsado = remember { MutableInteractionSource() }
        val apretado by pulsado.collectIsPressedAsState()
        val escala by animateFloatAsState(if (apretado) .9f else 1f, spring(dampingRatio = .5f, stiffness = 500f), label = "mas")
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-24).dp).size(62.dp)
                .graphicsLayer { scaleX = escala; scaleY = escala }
                .shadow(20.dp, CircleShape, ambientColor = c.acento, spotColor = c.acento2)
                .esferaBrillo()
                .clickable(pulsado, indication = null) { if (acciones == null) onCamara() else hoja = true },
            contentAlignment = Alignment.Center,
        ) { Icono(Ic.MAS, tamano = 28.dp, color = c.sobreAcento) }
    }

    if (hoja && acciones != null) {
        val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { hoja = false }, sheetState = estado, containerColor = c.hoja) {
            Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 18.dp).navigationBarsPadding()) {
                Text("Añadir", style = KcalmaTheme.tipo.tituloPaso, color = c.texto, modifier = Modifier.padding(bottom = 12.dp))
                OpcionMas(Ic.BUSCAR, "Buscar alimento", "Por nombre o marca") { hoja = false; acciones.buscar() }
                OpcionMas(Ic.CAMARA, "Foto con IA", "Reconoce el plato") { hoja = false; acciones.foto() }
                OpcionMas(Ic.ESCANER, "Escanear código", "Código de barras del envase") { hoja = false; acciones.escanear() }
                OpcionMas(Ic.VOZ, "Dictar", "«200 gramos de arroz cocido»") { hoja = false; acciones.voz() }
            }
        }
    }
}

@Composable
private fun OpcionMas(icono: Ic, titulo: String, detalle: String, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).vidrio(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BurbujaIcono(icono, tamano = 42.dp, icon = 20.dp)
        Column(Modifier.padding(start = 12.dp)) {
            Text(titulo, style = KcalmaTheme.tipo.tituloTarjeta, color = c.texto)
            Text(detalle, style = KcalmaTheme.tipo.pequeno, color = c.texto2)
        }
    }
}

@Composable
private fun ElementoBarra(icono: Ic, texto: String, activo: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    Column(
        modifier.fillMaxHeight().clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        if (activo) Icono(icono, tamano = 22.dp) else Icono(icono, tamano = 22.dp, color = c.texto3)
        Spacer(Modifier.height(3.dp))
        Text(texto, style = KcalmaTheme.tipo.diminuto, color = if (activo) c.texto else c.texto3)
    }
}
