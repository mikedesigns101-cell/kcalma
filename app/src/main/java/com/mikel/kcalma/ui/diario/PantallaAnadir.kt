package com.mikel.kcalma.ui.diario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.Habitual
import com.mikel.kcalma.datos.Repositorio
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.Unidad
import java.time.LocalDate
import androidx.compose.ui.text.input.KeyboardType
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val TODOS = "Todos"
private const val GENERICOS = "Genéricos"
private const val MIS_COMIDAS = "Mis comidas"

@Composable
fun PantallaAnadir(
    comida: ComidaDia,
    supermercados: List<String>,
    repo: Repositorio,
    onCambiarComida: (ComidaDia) -> Unit,
    onCerrar: () -> Unit,
    onAbrir: (Alimento) -> Unit,
    onAnadirRapido: (Alimento) -> Unit,
    onHabitual: (Habitual) -> Unit,
    onEscanear: (() -> Unit)?,
    titulo: String = "Añadir",
    selectorComida: Boolean = true,
    /** Atajos del diario (repetir, lo de siempre, solo calorías); null en la foto. */
    onAnadirEntrada: ((Entrada) -> Unit)? = null,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var consulta by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf(TODOS) }
    var resultados by remember { mutableStateOf<List<Alimento>>(emptyList()) }
    var buscando by remember { mutableStateOf(false) }
    var recientes by remember { mutableStateOf<List<Alimento>>(emptyList()) }
    var habituales by remember { mutableStateOf<List<Habitual>>(emptyList()) }
    var ultima by remember { mutableStateOf<List<Entrada>>(emptyList()) }
    var frecuentes by remember { mutableStateOf<List<Entrada>>(emptyList()) }
    var dialogoKcal by remember { mutableStateOf(false) }
    val foco = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { recientes = repo.diario.recientes(); habituales = repo.diario.habituales() }
    }
    LaunchedEffect(comida) {
        if (onAnadirEntrada == null) return@LaunchedEffect
        val hoy = LocalDate.now()
        withContext(Dispatchers.IO) {
            ultima = repo.diario.ultimaComida(comida, hoy.toString())
            frecuentes = repo.diario.frecuentes(comida, hoy.minusDays(45).toString())
        }
    }
    LaunchedEffect(consulta, filtro) {
        if (consulta.isBlank() || filtro == MIS_COMIDAS) { resultados = emptyList(); return@LaunchedEffect }
        delay(150) // espera a que deje de escribir
        buscando = true
        val superFiltro = when (filtro) { TODOS -> null; GENERICOS -> ""; else -> filtro }
        resultados = repo.buscar(consulta, superFiltro)
        buscando = false
    }

    if (dialogoKcal && onAnadirEntrada != null) DialogoKcal({ dialogoKcal = false }) { nombre, kcal, prot ->
        dialogoKcal = false
        val a = Alimento(nombre = nombre, racion = "1 ración", racionG = 100.0, kcal = kcal, proteinas = prot)
        onAnadirEntrada(Entrada(0, "", comida, a, 100.0, Unidad.RACION, 1.0))
    }
    Column(Modifier.fillMaxSize().background(c.fondo0).statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
        BarraSuperior("✕", onCerrar, titulo) { if (selectorComida) SelectorComida(comida, onCambiarComida) }

        // Buscador con borde degradado y botón de escáner
        val forma = RoundedCornerShape(14.dp)
        Row(
            Modifier.fillMaxWidth().clip(forma).background(c.fondo2).border(BorderStroke(1.5.dp, c.degradado), forma)
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔍", style = t.cuerpo)
            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (consulta.isEmpty()) Text("Busca un alimento o marca", style = t.titulo, color = c.texto3)
                BasicTextField(
                    consulta, { consulta = it.take(60) }, singleLine = true,
                    textStyle = t.titulo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().focusRequester(foco),
                )
            }
            if (consulta.isNotEmpty()) {
                Text("✕", style = t.cuerpo, color = c.texto3, modifier = Modifier.clickable { consulta = "" }.padding(8.dp))
            }
            if (onEscanear != null) Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(c.fondo4).clickable(onClick = onEscanear),
                contentAlignment = Alignment.Center,
            ) { Text("▥", style = t.titulo, color = c.texto) }
        }

        val filtros = listOf(TODOS) + supermercados + listOf(GENERICOS, MIS_COMIDAS)
        LazyRow(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filtros) { f -> ChipFiltro(f, f == filtro) { filtro = f } }
        }

        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            when {
                filtro == MIS_COMIDAS -> {
                    item { MiniTitulo("Comidas habituales") }
                    if (habituales.isEmpty()) item {
                        Text(
                            "Aún no tienes ninguna. Desde una comida del día, pulsa «Guardar como comida habitual».",
                            style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(habituales) { h -> FilaHabitual(h) { onHabitual(h) } }
                }
                consulta.isBlank() -> {
                    if (onAnadirEntrada != null) {
                        if (ultima.isNotEmpty()) item {
                            val cuando = cuandoFue(ultima.first().fecha)
                            FilaHabitual(Habitual(-1, "Repetir tu ${comida.nombre.lowercase()} de $cuando", ultima), "↻") {
                                onHabitual(Habitual(-1, "Tu ${comida.nombre.lowercase()} de $cuando", ultima))
                            }
                        }
                        item { FilaAtajo("⚡", "Añadir solo calorías", "Para cuando comes fuera y no sabes qué lleva") { dialogoKcal = true } }
                        if (frecuentes.isNotEmpty()) {
                            item { MiniTitulo("Sueles tomar en ${articuloComida(comida)}${comida.nombre.lowercase()}", Modifier.padding(top = 6.dp)) }
                            items(frecuentes) { e -> FilaAlimento(e.alimento, { onAbrir(e.alimento) }, { onAnadirEntrada(e) }, habitual = e) }
                        }
                    }
                    val otros = recientes.filter { a -> frecuentes.none { it.alimento.nombre == a.nombre && it.alimento.marca == a.marca } }
                    if (otros.isNotEmpty()) {
                        item { MiniTitulo("Recientes") }
                        items(otros) { a -> FilaAlimento(a, { onAbrir(a) }, { onAnadirRapido(a) }) }
                    } else if (recientes.isEmpty() && ultima.isEmpty()) item {
                        Text("Escribe para buscar entre casi 80.000 alimentos de supermercados españoles, o escanea un código de barras.",
                            style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
                else -> {
                    item { MiniTitulo("Resultados") }
                    if (resultados.isEmpty() && !buscando) item {
                        Text("Sin resultados. Prueba con menos palabras o escanea el código de barras.",
                            style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(resultados) { a -> FilaAlimento(a, { onAbrir(a) }, { onAnadirRapido(a) }) }
                }
            }
        }
    }
    LaunchedEffect(Unit) { if (consulta.isEmpty()) runCatching { foco.requestFocus() } }
}

@Composable
fun SelectorComida(comida: ComidaDia, onCambiar: (ComidaDia) -> Unit) {
    val c = KcalmaTheme.colores
    var abierto by remember { mutableStateOf(false) }
    Box {
        Text(
            "${comida.icono} ${comida.nombre} ▾", style = KcalmaTheme.tipo.cuerpo, color = c.acento,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.acento.copy(alpha = .1f))
                .clickable { abierto = true }.padding(horizontal = 10.dp, vertical = 7.dp),
        )
        DropdownMenu(abierto, { abierto = false }, containerColor = c.fondo3) {
            ComidaDia.entries.forEach { cd ->
                DropdownMenuItem(
                    text = { Text("${cd.icono}  ${cd.nombre}", style = KcalmaTheme.tipo.cuerpo, color = if (cd == comida) c.acento else c.texto) },
                    onClick = { onCambiar(cd); abierto = false },
                )
            }
        }
    }
}

@Composable
private fun ChipFiltro(texto: String, activo: Boolean, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(10.dp)
    Text(
        texto, style = KcalmaTheme.tipo.cuerpo, color = if (activo) c.acento else c.texto2,
        modifier = Modifier.clip(forma).background(if (activo) c.acento.copy(alpha = .12f) else c.fondo2)
            .border(1.dp, if (activo) c.acento.copy(alpha = .45f) else c.borde, forma)
            .clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Composable
private fun FilaHabitual(h: Habitual, emoji: String = "⭐", onAnadir: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val kcal = h.entradas.sumOf { it.nutrientes.kcal }.roundToInt()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(13.dp)).background(c.fondo2)
            .border(1.dp, c.borde, RoundedCornerShape(13.dp)).clickable(onClick = onAnadir).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = t.titulo, color = c.acento)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(h.nombre, style = t.tituloTarjeta, color = c.texto)
            Text(h.entradas.joinToString(", ") { it.alimento.nombre }, style = t.pequeno, color = c.texto3, maxLines = 1)
        }
        Text("$kcal", style = t.tituloTarjeta, color = c.texto)
        Spacer(Modifier.width(10.dp))
        Text("+", style = t.numero, color = c.acento)
    }
}

@Composable
private fun FilaAtajo(emoji: String, titulo: String, detalle: String, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(13.dp)).background(c.fondo2)
            .border(1.dp, c.borde, RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = t.titulo)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(titulo, style = t.tituloTarjeta, color = c.texto)
            Text(detalle, style = t.pequeno, color = c.texto3, maxLines = 1)
        }
        Text("+", style = t.numero, color = c.acento)
    }
}

private fun articuloComida(c: ComidaDia) = if (c == ComidaDia.DESAYUNO) "el " else "la "

/** «ayer», «el martes», «el 3 nov». */
private fun cuandoFue(fecha: String): String {
    val f = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return fecha
    val dias = java.time.temporal.ChronoUnit.DAYS.between(f, LocalDate.now())
    val es = java.util.Locale.forLanguageTag("es-ES")
    return when {
        dias <= 1L -> "ayer"
        dias < 7L -> "el " + f.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, es)
        else -> "el ${f.dayOfMonth} " + f.month.getDisplayName(java.time.format.TextStyle.SHORT, es).trimEnd('.')
    }
}

/** Calorías sin desglose (y proteína si se sabe), con un nombre opcional. */
@Composable
private fun DialogoKcal(onCerrar: () -> Unit, onAnadir: (String, Double, Double) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var nombre by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var prot by remember { mutableStateOf("") }
    val k = kcal.toDoubleOrNull()?.takeIf { it > 0 }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = c.fondo2,
        title = { Text("Añadir solo calorías", style = t.titulo, color = c.texto) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Campo("¿Qué era? (opcional)", nombre, KeyboardType.Text) { nombre = it.take(40) }
                Campo("Calorías (kcal)", kcal, KeyboardType.Number) { kcal = it.filter { ch -> ch.isDigit() }.take(4) }
                Campo("Proteína en g (opcional)", prot, KeyboardType.Number) { prot = it.filter { ch -> ch.isDigit() }.take(3) }
            }
        },
        confirmButton = {
            Text(
                "Añadir", style = t.titulo, color = if (k != null) c.acento else c.texto3,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = k != null) {
                    if (k != null) onAnadir(nombre.trim().ifEmpty { "Calorías rápidas" }, k, prot.toDoubleOrNull() ?: 0.0)
                }.padding(10.dp),
            )
        },
        dismissButton = {
            Text(
                "Cancelar", style = t.titulo, color = c.texto2,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCerrar).padding(10.dp),
            )
        },
    )
}

@Composable
private fun Campo(etiqueta: String, valor: String, tipo: KeyboardType, onCambio: (String) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column {
        Text(etiqueta, style = t.pequeno, color = c.texto2)
        BasicTextField(
            valor, onCambio, singleLine = true,
            textStyle = t.titulo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
            keyboardOptions = KeyboardOptions(keyboardType = tipo),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).background(c.fondo3)
                .border(1.dp, c.borde, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
