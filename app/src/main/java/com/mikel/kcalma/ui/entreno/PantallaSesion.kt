package com.mikel.kcalma.ui.entreno

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.mikel.kcalma.dominio.Fase
import com.mikel.kcalma.dominio.BloqueRutina
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.AjustesEntreno
import com.mikel.kcalma.dominio.Ejercicio
import com.mikel.kcalma.dominio.Ejercicios
import com.mikel.kcalma.dominio.Formato
import com.mikel.kcalma.dominio.Material
import com.mikel.kcalma.dominio.Progresion
import com.mikel.kcalma.dominio.Rutina
import com.mikel.kcalma.dominio.Serie
import com.mikel.kcalma.dominio.Sesion
import com.mikel.kcalma.dominio.historial
import com.mikel.kcalma.dominio.ultimasSeries
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.delay

/** Orden guardado de la serie [i] del bloque [b]: así un ejercicio repetido en dos bloques no se pisa. */
private fun orden(b: Int, i: Int) = b * 100 + i

private fun reloj(seg: Long): String = "%d:%02d".format(seg / 60, seg % 60)

private fun avisar(contexto: Context, pitar: Boolean) {
    runCatching {
        contexto.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
    }
    if (pitar) runCatching {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tg.release() }, 400)
    }
}

/** El reloj de pared, refrescado cada [cada] ms mientras la pantalla está abierta. */
@Composable
private fun ahora(cada: Long = 250): Long {
    var t by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { t = System.currentTimeMillis(); delay(cada) } }
    return t
}

/** Valores que se están escribiendo en una serie. */
private class Fila(kg: String, reps: String, hecha: Boolean) {
    var kg by mutableStateOf(kg)
    var reps by mutableStateOf(reps)
    var hecha by mutableStateOf(hecha)
}

/**
 * Sesión en curso. Cada serie se guarda al marcarla; si Android cierra la app, se sigue desde "Continuar".
 * Atrás pregunta si terminar, descartar o salir y seguir luego.
 */
@Composable
fun PantallaSesion(
    sesion: Sesion,
    rutina: Rutina,
    sesiones: List<Sesion>,
    ajustes: AjustesEntreno,
    onSerie: (Serie) -> Unit,
    onTerminar: () -> Unit,
    onDescartar: () -> Unit,
    onSalir: () -> Unit,
    onFicha: (Ejercicio) -> Unit,
) {
    var preguntar by remember { mutableStateOf(false) }
    BackHandler { preguntar = true }
    val vista = LocalView.current
    DisposableEffect(Unit) { vista.keepScreenOn = true; onDispose { vista.keepScreenOn = false } }
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val reloj = ahora()
    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding()) {
        Box(Modifier.padding(horizontal = MARGEN)) {
            BarraSuperior("✕", { preguntar = true }, rutina.nombre) {
                Text(reloj(((reloj - sesion.inicio) / 1000).coerceAtLeast(0)), style = t.numero, color = c.texto2)
            }
        }
        if (rutina.formato == Formato.SERIES) SesionSeries(sesion, rutina, sesiones, ajustes, reloj, onSerie, onFicha) { preguntar = true }
        else if (rutina.formato == Formato.COMPLEJO) {
            val contexto = LocalContext.current
            SesionComplejo(sesion, rutina, sesiones, ajustes, reloj, onSerie, onFicha, { avisar(contexto, true) }) { preguntar = true }
        }
        else if (rutina.formato == Formato.INTERVALOS) SesionIntervalos(sesion, rutina, sesiones, ajustes, reloj, onSerie) { preguntar = true }
        else SesionTiempo(sesion, rutina, sesiones, ajustes, reloj, onSerie) { preguntar = true }
    }
    if (preguntar) {
        val hechas = sesion.series.count { it.hecha }
        AlertDialog(
            onDismissRequest = { preguntar = false },
            containerColor = c.hoja,
            title = { Text(if (hechas > 0) "¿Terminas el entreno?" else "¿Sales del entreno?", style = t.titulo, color = c.texto) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (hechas > 0) "Llevas $hechas ${if (hechas == 1) "serie" else "series"}. Se guardan las marcadas." else "Aún no has marcado ninguna serie.",
                        style = t.cuerpo, color = c.texto2,
                    )
                    if (hechas > 0) OpcionDialogo("Terminar y guardar", c.acento) { preguntar = false; onTerminar() }
                    OpcionDialogo("Salir y seguir luego", c.texto) { preguntar = false; onSalir() }
                    OpcionDialogo("Descartar entreno", c.rojo) { preguntar = false; onDescartar() }
                    OpcionDialogo("Seguir entrenando", c.texto2) { preguntar = false }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun OpcionDialogo(texto: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        texto, style = KcalmaTheme.tipo.tituloTarjeta, color = color, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().vidrio(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp),
    )
}

// ---------------- Series con descanso ----------------

@Composable
private fun SesionSeries(
    sesion: Sesion, rutina: Rutina, sesiones: List<Sesion>, ajustes: AjustesEntreno, reloj: Long,
    onSerie: (Serie) -> Unit, onFicha: (Ejercicio) -> Unit, onTerminar: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val bloques = rutina.bloques.filter { Ejercicios.porId(it.ejercicio) != null }
    var actual by rememberSaveable { mutableIntStateOf(0) }
    var finDescanso by rememberSaveable { mutableLongStateOf(0L) }
    var avisado by rememberSaveable { mutableStateOf(true) }
    // Planes y filas: se calculan una vez por sesión (al volver, se recupera lo ya marcado)
    val planes = remember(sesion.id) {
        bloques.map { b ->
            val ej = Ejercicios.porId(b.ejercicio)!!
            Progresion.proponer(ej, b, sesiones.ultimasSeries(ej.id, sin = sesion.id), ajustes.kettlebells)
        }
    }
    val filas = remember(sesion.id) {
        mutableStateMapOf<Int, List<Fila>>().apply {
            bloques.forEachIndexed { bi, _ ->
                put(bi, planes[bi].series.mapIndexed { i, sp ->
                    val ya = sesion.series.firstOrNull { it.orden == orden(bi, i) && it.ejercicio == bloques[bi].ejercicio }
                    (ya?.kg ?: sp.kg).let { k -> Fila(if (k > 0) Progresion.kg(k) else "", "${ya?.reps ?: sp.reps}", ya?.hecha == true) }
                })
            }
        }
    }
    if (bloques.isEmpty()) return
    val bi = actual.coerceIn(0, bloques.lastIndex)
    val b = bloques[bi]
    val ej = Ejercicios.porId(b.ejercicio)!!
    val plan = planes[bi]
    val historial = remember(sesion.id, ej.id) { sesiones.historial(ej.id, sin = sesion.id) }
    val restante = ((finDescanso - reloj) / 1000).coerceAtLeast(0)
    LaunchedEffect(restante, avisado) { if (restante == 0L && !avisado) { avisado = true; avisar(contexto, true) } }

    fun guardar(i: Int) {
        val f = filas[bi]!![i]
        onSerie(Serie(ej.id, orden(bi, i), f.kg.replace(',', '.').toDoubleOrNull() ?: 0.0, f.reps.toIntOrNull() ?: 0, f.hecha))
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = MARGEN, end = MARGEN, bottom = 120.dp)) {
            item {
                // Navegación entre ejercicios
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Flecha("‹", bi > 0) { actual = bi - 1 }
                    Text(
                        "Ejercicio ${bi + 1} de ${bloques.size}", style = t.pequeno, color = c.texto2,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                    Flecha("›", bi < bloques.lastIndex) { actual = bi + 1 }
                }
            }
            item {
                Row(
                    Modifier.padding(top = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(22.dp)).clickable { onFicha(ej) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FiguraEjercicio(ej, Modifier.size(72.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(ej.nombre, style = t.tituloPaso, color = c.texto)
                        Text("${b.series} × ${b.repsMin}–${b.repsMax}${if (ej.porLado) " por lado" else ""}", style = t.pequeno, color = c.texto2)
                        Text("Ver cómo se hace ›", style = t.pequeno, color = c.acento, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (plan.nota != null) item {
                Row(Modifier.padding(top = 10.dp).fillMaxWidth().vidrio(RoundedCornerShape(16.dp), resaltado = true).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icono(Ic.SUBIR, tamano = 18.dp)
                    Text("  ${plan.nota}", style = t.pequeno, color = c.texto)
                }
            }
            item {
                Row(Modifier.padding(top = 14.dp, bottom = 6.dp).fillMaxWidth()) {
                    Cabeza("Serie", 0.6f); Cabeza("Anterior", 1.3f); Cabeza(if (ej.conPeso) "kg" else "", 1f); Cabeza("Reps", 1f); Cabeza("", 0.7f)
                }
            }
            filas[bi]!!.forEachIndexed { i, f ->
                item(key = "$bi-$i") {
                    val ant = plan.series[i].anterior
                    val kg = f.kg.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val record = f.hecha && historial.isNotEmpty() && Progresion.esRecord(Serie(ej.id, 0, kg, f.reps.toIntOrNull() ?: 0), historial)
                    Row(
                        Modifier.padding(bottom = 6.dp).fillMaxWidth().vidrio(RoundedCornerShape(14.dp), resaltado = f.hecha).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(.6f), contentAlignment = Alignment.Center) {
                            if (record) Icono(Ic.TROFEO, tamano = 18.dp) else Text("${i + 1}", style = t.numero, color = c.texto2)
                        }
                        Text(
                            when { ant == null -> "—"; ej.conPeso -> "${Progresion.kg(ant.kg)} × ${ant.reps}"; else -> "× ${ant.reps}" },
                            style = t.pequeno, color = c.texto3, textAlign = TextAlign.Center, modifier = Modifier.weight(1.3f),
                        )
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (ej.conPeso) Celda(f.kg, true) { nuevo ->
                                // Las siguientes sin marcar que tenían el mismo peso lo siguen
                                filas[bi]!!.drop(i + 1).filter { !it.hecha && it.kg == f.kg }.forEach { it.kg = nuevo }
                                f.kg = nuevo; if (f.hecha) guardar(i)
                            }
                        }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Celda(f.reps, false) { f.reps = it; if (f.hecha) guardar(i) }
                        }
                        Box(Modifier.weight(.7f), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(34.dp).vidrio(CircleShape, resaltado = f.hecha).clickable {
                                    f.hecha = !f.hecha
                                    guardar(i)
                                    if (f.hecha) { finDescanso = System.currentTimeMillis() + b.descanso * 1000L; avisado = false }
                                    // Al terminar todas las series, pasar al siguiente ejercicio
                                    if (f.hecha && filas[bi]!!.all { it.hecha } && bi < bloques.lastIndex) actual = bi + 1
                                },
                                contentAlignment = Alignment.Center,
                            ) { if (f.hecha) Icono(Ic.CHECK, tamano = 18.dp) }
                        }
                    }
                }
            }
            item {
                val ultimoKg = filas[bi]!!.maxOfOrNull { it.kg.replace(',', '.').toDoubleOrNull() ?: 0.0 } ?: 0.0
                (if (ej.conPeso && ultimoKg <= 0) "Apunta el peso que uses: la próxima vez te lo propondré."
                else Progresion.meta(ej, b, ultimoKg, ajustes.kettlebells))?.let {
                    Text(it, style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = MARGEN, vertical = 12.dp)) {
            if (restante > 0) {
                Row(
                    Modifier.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(20.dp), resaltado = true).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icono(Ic.RELOJ, tamano = 18.dp)
                    Text("  Descanso", style = t.cuerpo, color = c.texto2)
                    Text("  ${reloj(restante)}", style = t.numero, color = c.texto, modifier = Modifier.weight(1f))
                    BotonTexto("+30 s", { finDescanso += 30_000 })
                    Box(Modifier.width(6.dp))
                    BotonTexto("Saltar", { finDescanso = 0; avisado = true })
                }
            }
            BotonPrincipal("Terminar entreno", onTerminar)
        }
    }
}

@Composable
private fun Flecha(s: String, activa: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).vidrio(CircleShape).clickable(enabled = activa, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(s, style = KcalmaTheme.tipo.titulo, color = if (activa) KcalmaTheme.colores.texto else KcalmaTheme.colores.texto3) }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cabeza(s: String, peso: Float) {
    Text(s, style = KcalmaTheme.tipo.diminuto, color = KcalmaTheme.colores.texto3, textAlign = TextAlign.Center, modifier = Modifier.weight(peso))
}

/** Casilla numérica pequeña de la tabla de series. */
@Composable
internal fun CeldaPeso(valor: String, onCambio: (String) -> Unit) = Celda(valor, true, onCambio)

@Composable
private fun Celda(valor: String, decimales: Boolean, onCambio: (String) -> Unit) {
    val c = KcalmaTheme.colores
    BasicTextField(
        value = valor,
        onValueChange = { n ->
            val l = n.replace('.', ',').filter { it.isDigit() || (decimales && it == ',') }
            if (l.length <= 5 && l.count { it == ',' } <= 1) onCambio(l)
        },
        singleLine = true,
        textStyle = KcalmaTheme.tipo.numero.copy(color = c.texto, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(c.acento),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimales) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.width(58.dp).vidrio(RoundedCornerShape(10.dp)).padding(vertical = 7.dp),
        decorationBox = { campo ->
            Box(contentAlignment = Alignment.Center) {
                if (valor.isEmpty()) Text("–", style = KcalmaTheme.tipo.numero, color = c.texto3)
                campo()
            }
        },
    )
}

// ---------------- EMOM y AMRAP ----------------

@Composable
private fun SesionTiempo(
    sesion: Sesion, rutina: Rutina, sesiones: List<Sesion>, ajustes: AjustesEntreno, reloj: Long,
    onSerie: (Serie) -> Unit, onTerminar: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val bloques = rutina.bloques.filter { Ejercicios.porId(it.ejercicio) != null }
    if (bloques.isEmpty()) return
    val total = rutina.minutos.coerceAtLeast(1) * 60_000L
    val pasado = (reloj - sesion.inicio).coerceIn(0, total)
    val acabado = pasado >= total
    val emom = rutina.formato == Formato.EMOM
    // Peso elegido en cada bloque (empieza en lo que propone la progresión)
    val kilos = remember(sesion.id) {
        mutableStateListOf<Double>().apply {
            bloques.forEach { b ->
                val ej = Ejercicios.porId(b.ejercicio)!!
                val ya = sesion.series.firstOrNull { it.ejercicio == b.ejercicio }
                add(ya?.kg ?: Progresion.proponer(ej, b.copy(series = 1), sesiones.ultimasSeries(ej.id, sin = sesion.id), ajustes.kettlebells).series.first().kg)
            }
        }
    }
    fun anotar(b: Int, orden: Int, hecha: Boolean = true) =
        onSerie(Serie(bloques[b].ejercicio, orden, kilos[b], bloques[b].repsMin, hecha))

    val minuto = (pasado / 60_000).toInt()
    val rondas = if (emom) 0 else sesion.series.filter { it.hecha }.map { it.orden }.distinct().size
    // EMOM: al cerrar cada minuto se apunta el ejercicio de ese minuto, con aviso
    var ultimoMinuto by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(minuto, acabado) {
        if (!emom) { if (acabado && ultimoMinuto != -2) { ultimoMinuto = -2; avisar(contexto, true) }; return@LaunchedEffect }
        val cerrados = if (acabado) total.toInt() / 60_000 else minuto
        for (k in 0 until cerrados) {
            if (sesion.series.none { it.orden == k }) anotar(k % bloques.size, k)
        }
        if (ultimoMinuto != -1 && ultimoMinuto != cerrados) avisar(contexto, true)
        ultimoMinuto = cerrados
    }
    val fraccion = if (emom && !acabado) (pasado % 60_000) / 60_000f else pasado / total.toFloat()
    val restante = if (emom && !acabado) 60 - (pasado % 60_000) / 1000 else (total - pasado) / 1000
    val ahoraB = if (emom) minuto % bloques.size else 0

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = MARGEN, end = MARGEN, bottom = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Text(
                    when {
                        acabado -> "¡Hecho!"
                        emom -> "Minuto ${minuto + 1} de ${rutina.minutos}"
                        else -> "Tantas rondas como puedas"
                    },
                    style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 4.dp),
                )
            }
            item { Anillo(fraccion, if (acabado) "0:00" else reloj(restante), if (emom) "este minuto" else "quedan", Modifier.padding(vertical = 14.dp)) }
            if (!emom) item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                    Flecha("−", rondas > 0 && !acabado) { anotarRonda(bloques.size, rondas - 1, false, ::anotar) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("$rondas", style = t.numeroGrande, color = c.texto)
                        Text("rondas", style = t.pequeno, color = c.texto2)
                    }
                    Flecha("+", !acabado) { anotarRonda(bloques.size, rondas, true, ::anotar) }
                }
            }
            bloques.forEachIndexed { i, b ->
                item(key = "b$i") {
                    val ej = Ejercicios.porId(b.ejercicio)!!
                    val toca = emom && !acabado && i == ahoraB
                    Row(
                        Modifier.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp), resaltado = toca).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FiguraEjercicio(ej, Modifier.size(if (toca) 64.dp else 44.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(ej.nombre, style = t.tituloTarjeta, color = c.texto)
                            Text("${b.repsMin} reps${if (ej.porLado) " por lado" else ""}", style = t.pequeno, color = c.texto2)
                        }
                        if (ej.conPeso) {
                            val opciones = if (ej.material == Material.KETTLEBELL) ajustes.kettlebells.sorted() else emptyList()
                            Text(
                                "${Progresion.kg(kilos[i])} kg", style = t.numero, color = c.texto,
                                modifier = Modifier.vidrio(RoundedCornerShape(12.dp), resaltado = true).clickable(enabled = opciones.isNotEmpty()) {
                                    kilos[i] = opciones.firstOrNull { it > kilos[i] } ?: opciones.first()
                                }.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
            if (emom) item {
                Text("Cada minuto se apunta solo. Toca el peso para cambiar de kettlebell.", style = t.pequeno, color = c.texto3, textAlign = TextAlign.Center)
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = MARGEN, vertical = 12.dp)) {
            BotonPrincipal(if (acabado) "Guardar entreno" else "Terminar antes", onTerminar)
        }
    }
}

/** Peso de cada bloque al empezar: el ya apuntado en esta sesión o el que propone la progresión. */
@Composable
private fun kilosIniciales(sesion: Sesion, bloques: List<BloqueRutina>, sesiones: List<Sesion>, ajustes: AjustesEntreno) = remember(sesion.id) {
    mutableStateListOf<Double>().apply {
        bloques.forEach { b ->
            val ej = Ejercicios.porId(b.ejercicio)!!
            val ya = sesion.series.firstOrNull { it.ejercicio == b.ejercicio }
            add(ya?.kg ?: Progresion.proponer(ej, b.copy(series = 1), sesiones.ultimasSeries(ej.id, sin = sesion.id), ajustes.kettlebells).series.first().kg)
        }
    }
}

/**
 * Intervalos (Tabata y parecidos): cuenta atrás, trabajo y descanso con pitido y vibración en cada cambio.
 * Cada ronda toca el siguiente ejercicio y se apunta sola al acabar su trabajo.
 */
@Composable
private fun SesionIntervalos(
    sesion: Sesion, rutina: Rutina, sesiones: List<Sesion>, ajustes: AjustesEntreno, reloj: Long,
    onSerie: (Serie) -> Unit, onTerminar: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val bloques = rutina.bloques.filter { Ejercicios.porId(it.ejercicio) != null }
    if (bloques.isEmpty()) return
    val kilos = kilosIniciales(sesion, bloques, sesiones, ajustes)
    val fase = rutina.fase((reloj - sesion.inicio).coerceAtLeast(0))
    val acabado = fase.tipo == Fase.Tipo.FIN
    val cerradas = if (acabado) rutina.rondas else fase.cerradas
    // Al cerrar el trabajo de cada ronda se apunta su ejercicio (reps 0: por tiempo)
    LaunchedEffect(cerradas) {
        for (k in 0 until cerradas) {
            if (sesion.series.none { it.orden == k }) bloques[k % bloques.size].let { b -> onSerie(Serie(b.ejercicio, k, kilos[k % bloques.size], 0)) }
        }
    }
    // Pitido en cada cambio de fase (no al abrir la pantalla)
    var ultima by rememberSaveable { mutableStateOf("") }
    val clave = "${fase.tipo}${fase.ronda}"
    LaunchedEffect(clave) {
        if (ultima.isNotEmpty() && ultima != clave) avisar(contexto, true)
        ultima = clave
    }
    val seg = ((fase.queda + 999) / 1000)
    val toca = if (fase.tipo == Fase.Tipo.DESCANSA) fase.ronda + 1 else fase.ronda.coerceAtLeast(0)
    val siguiente = when (fase.tipo) {
        Fase.Tipo.PREPARA -> "Primero: ${Ejercicios.porId(bloques[0].ejercicio)!!.nombre}"
        Fase.Tipo.TRABAJA -> if (fase.ronda + 1 < rutina.rondas) "Luego: descansa ${rutina.pausa} s" else "Última ronda"
        Fase.Tipo.DESCANSA -> "Ahora viene: ${Ejercicios.porId(bloques[toca % bloques.size].ejercicio)!!.nombre}"
        Fase.Tipo.FIN -> "Has hecho las ${rutina.rondas} rondas"
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = MARGEN, end = MARGEN, bottom = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Text(
                    if (acabado) "¡Hecho!" else if (fase.tipo == Fase.Tipo.PREPARA) "Prepárate" else "Ronda ${fase.ronda + 1} de ${rutina.rondas}",
                    style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                Anillo(
                    if (acabado) 1f else 1f - fase.queda / fase.largo.toFloat(),
                    if (acabado) "0:00" else if (fase.tipo == Fase.Tipo.PREPARA) "$seg" else reloj(seg),
                    when (fase.tipo) { Fase.Tipo.PREPARA -> "empieza en"; Fase.Tipo.TRABAJA -> "¡TRABAJA!"; Fase.Tipo.DESCANSA -> "descansa"; Fase.Tipo.FIN -> "terminado" },
                    Modifier.padding(vertical = 14.dp),
                )
            }
            item { Text(siguiente, style = t.titulo, color = c.texto, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 10.dp)) }
            item {
                // Rondas hechas: un punto por ronda
                Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (k in 0 until rutina.rondas.coerceAtMost(30)) Box(
                        Modifier.size(10.dp).clip(CircleShape).background(
                            if (k < cerradas) c.acento else if (k == fase.ronda && fase.tipo == Fase.Tipo.TRABAJA) c.acento.copy(alpha = .45f) else c.texto.copy(alpha = .12f)
                        )
                    )
                }
            }
            bloques.forEachIndexed { i, b ->
                item(key = "b$i") {
                    val ej = Ejercicios.porId(b.ejercicio)!!
                    val ahoraEste = !acabado && fase.tipo != Fase.Tipo.PREPARA && i == toca % bloques.size
                    Row(
                        Modifier.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp), resaltado = ahoraEste).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FiguraEjercicio(ej, Modifier.size(if (ahoraEste) 64.dp else 44.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(ej.nombre, style = t.tituloTarjeta, color = c.texto)
                            Text("${rutina.trabajo} s a buen ritmo", style = t.pequeno, color = c.texto2)
                        }
                        if (ej.conPeso) {
                            val opciones = if (ej.material == Material.KETTLEBELL) ajustes.kettlebells.sorted() else emptyList()
                            Text(
                                "${Progresion.kg(kilos[i])} kg", style = t.numero, color = c.texto,
                                modifier = Modifier.vidrio(RoundedCornerShape(12.dp), resaltado = true).clickable(enabled = opciones.isNotEmpty()) {
                                    kilos[i] = opciones.firstOrNull { it > kilos[i] } ?: opciones.first()
                                }.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
            item { Text("Cada ronda se apunta sola. Pita y vibra al cambiar.", style = t.pequeno, color = c.texto3, textAlign = TextAlign.Center) }
        }
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = MARGEN, vertical = 12.dp)) {
            BotonPrincipal(if (acabado) "Guardar entreno" else "Terminar antes", onTerminar)
        }
    }
}

/** AMRAP: una ronda = una serie de cada ejercicio con el mismo orden. Quitar la marca la deja sin hacer. */
private fun anotarRonda(n: Int, ronda: Int, hecha: Boolean, anotar: (Int, Int, Boolean) -> Unit) {
    for (b in 0 until n) anotar(b, ronda, hecha)
}

@Composable
private fun Anillo(fraccion: Float, numero: String, rotulo: String, modifier: Modifier = Modifier) {
    val c = KcalmaTheme.colores
    Box(modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            val g = 14.dp.toPx()
            val r = Size(size.width - g, size.height - g)
            val o = Offset(g / 2, g / 2)
            drawArc(c.texto.copy(alpha = .08f), 0f, 360f, false, o, r, style = Stroke(g))
            drawArc(Brush.sweepGradient(listOf(c.acento, c.acento2, c.acento)), -90f, 360f * (1 - fraccion), false, o, r, style = Stroke(g, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(numero, style = KcalmaTheme.tipo.numeroGrande, color = c.texto)
            Text(rotulo, style = KcalmaTheme.tipo.pequeno, color = c.texto2)
        }
    }
}

