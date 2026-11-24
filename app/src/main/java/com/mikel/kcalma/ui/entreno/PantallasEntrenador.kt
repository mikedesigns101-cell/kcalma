package com.mikel.kcalma.ui.entreno

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.Coach
import com.mikel.kcalma.dominio.Cambio
import com.mikel.kcalma.dominio.Cambios
import com.mikel.kcalma.dominio.Ejercicios
import com.mikel.kcalma.dominio.Entrenador
import com.mikel.kcalma.dominio.MensajeChat
import com.mikel.kcalma.dominio.Progresion
import com.mikel.kcalma.dominio.Rutina
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.esferaBrillo
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.theme.KcalmaTheme

/** Entrenos terminados que hacen falta para que el entrenador tenga algo en que basarse. */
const val ENTRENOS_PARA_PROPUESTA = 2

/**
 * Tarjeta del entrenador en la pestaña Entreno. La propuesta se pide a mano (gasta de la clave de Gemini del usuario)
 * y vale para toda la semana.
 */
@Composable
fun TarjetaEntrenador(
    propuesta: Coach.Propuesta?, pendientes: Int, cargando: Boolean, error: String?, entrenos: Int,
    onPedir: () -> Unit, onVer: () -> Unit, modifier: Modifier = Modifier,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column(modifier.fillMaxWidth().vidrio(RoundedCornerShape(20.dp), resaltado = true).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icono(Ic.CHISPAS, tamano = 15.dp)
            Text("  TU ENTRENADOR", style = t.etiqueta, color = c.acento)
        }
        when {
            propuesta != null && pendientes > 0 -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$pendientes ${if (pendientes == 1) "cambio" else "cambios"} para esta semana",
                    style = t.titulo, color = c.texto, modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                BotonPastilla("Ver propuesta", onVer)
            }
            propuesta != null -> Text(
                propuesta.resumen.ifBlank { "Esta semana no hay nada que cambiar." } + " La semana que viene podrás pedir otra propuesta.",
                style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 8.dp),
            )
            entrenos < ENTRENOS_PARA_PROPUESTA -> Text(
                "Cuando tengas $ENTRENOS_PARA_PROPUESTA entrenos apuntados, podrá revisar cómo vas y proponerte cambios en tus rutinas.",
                style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 8.dp),
            )
            else -> {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Revisa tus últimas 4 semanas y te propone hasta 3 cambios. Tú decides cuáles aceptas.",
                        style = t.pequeno, color = c.texto2, modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    if (cargando) Text("Pensando…", style = t.tituloTarjeta, color = c.texto3)
                    else BotonPastilla("Pedir propuesta", onPedir)
                }
                if (error != null) Text(error, style = t.pequeno, color = c.rojo, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** Tarjeta de un cambio propuesto, con Aceptar y No. La frase del cambio la escribe el código; la IA solo pone el motivo. */
@Composable
private fun TarjetaCambio(c0: Cambio, rutina: Rutina?, estado: Int, onAceptar: () -> Unit, onNo: () -> Unit, modifier: Modifier = Modifier) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column(modifier.fillMaxWidth().vidrio(RoundedCornerShape(18.dp), resaltado = estado == 0).padding(12.dp)) {
        val nueva = c0 as? Cambio.Crear
        Text(
            if (nueva != null) "RUTINA NUEVA · ${nueva.seccion.nombre.uppercase()}${if (nueva.dias.isEmpty()) "" else " · " + nueva.dias.sorted().joinToString(" ") { DIAS[it - 1] }}"
            else "CAMBIO PROPUESTO · ${(rutina?.nombre ?: "Rutina").uppercase()}",
            style = t.etiqueta, color = c.acento,
        )
        Text(Cambios.titulo(c0, rutina), style = t.tituloTarjeta, color = c.texto, modifier = Modifier.padding(top = 4.dp))
        nueva?.bloques?.forEach { b ->
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Ejercicios.porId(b.ejercicio)?.nombre ?: b.ejercicio, style = t.pequeno, color = c.texto2, modifier = Modifier.weight(1f).padding(end = 8.dp))
                Text("${b.series} × ${b.repsMin}–${b.repsMax}", style = t.pequeno, color = c.texto)
            }
        }
        if (c0.motivo.isNotBlank()) Text(c0.motivo, style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 3.dp))
        when (estado) {
            0 -> Row(Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                BotonTexto("No", onNo, Modifier.padding(end = 8.dp), color = c.texto2)
                BotonPastilla(if (nueva != null) "Guardar rutina" else "Cambiar", onAceptar)
            }
            1 -> Text(if (nueva != null) "✓ Guardada en tus rutinas" else "✓ Cambiado en tu rutina", style = t.pequeno, color = c.verde, modifier = Modifier.padding(top = 8.dp))
            else -> Text("No se ha cambiado", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Propuesta de la semana: cambios pendientes y los datos en que se basa. */
@Composable
fun PantallaPropuesta(
    propuesta: Coach.Propuesta, rutinas: List<Rutina>, datos: Entrenador.Estadisticas,
    onAtras: () -> Unit, onAceptar: (List<Int>) -> Unit, onDescartar: (List<Int>) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    // Solo los que siguen pudiéndose aplicar (la rutina puede haber cambiado desde que se pidió)
    val pendientes = propuesta.pendientes.filter { Cambios.validar(propuesta.cambios[it], rutinas) != null }
    Box(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = MARGEN, end = MARGEN, bottom = 150.dp)) {
            item { BarraSuperior("‹", onAtras, "Propuesta de la semana") }
            if (propuesta.resumen.isNotBlank()) item { Text(propuesta.resumen, style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(bottom = 12.dp)) }
            if (pendientes.isEmpty()) item {
                Text(
                    "No queda nada pendiente esta semana.", style = t.cuerpo, color = c.texto2,
                    modifier = Modifier.fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(14.dp),
                )
            }
            items(pendientes, key = { it }) { i ->
                val cambio = propuesta.cambios[i]
                TarjetaCambio(
                    cambio, rutinas.firstOrNull { it.id == cambio.rutina }, 0,
                    onAceptar = { onAceptar(listOf(i)) }, onNo = { onDescartar(listOf(i)) }, modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item { Text("Los cambios se apuntan en tus rutinas. Si es una de las hechas, en tu copia: siempre puedes volver a la original.", style = t.pequeno, color = c.texto3) }
            item { Rotulo("En qué se basa") }
            item {
                Column(Modifier.fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Dato("Entrenos en 4 semanas", if (datos.previstos28 > 0) "${datos.entrenos28} de ${datos.previstos28}" else "${datos.entrenos28}")
                    val orden = datos.seriesSemana.entries.sortedByDescending { it.value }
                    if (orden.isNotEmpty()) Dato("Series por semana", orden.take(4).joinToString(" · ") { "${it.key.nombre} ${numero(it.value)}" })
                    if (orden.size > 4) Dato("Las que menos", orden.takeLast(2).joinToString(" · ") { "${it.key.nombre} ${numero(it.value)}" })
                    Dato(
                        "Estancado",
                        datos.estancados.entries.joinToString("\n") { "${Ejercicios.porId(it.key)?.nombre} · ${Progresion.kg(it.value.kg)} kg × ${it.value.reps}" }.ifEmpty { "Nada" },
                    )
                    Dato("Tu objetivo", Entrenador.nombreObjetivo(datos.objetivo))
                }
            }
            item {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Icono(Ic.CHISPAS, tamano = 14.dp)
                    Text(
                        "  Los datos los calcula Kcalma; la IA (Gemini, con tu clave) solo elige y redacta la propuesta.",
                        style = t.pequeno, color = c.texto3,
                    )
                }
            }
        }
        if (pendientes.isNotEmpty()) Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = MARGEN, vertical = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonTexto("Ahora no", { onDescartar(pendientes); onAtras() }, color = c.texto2)
            Box(Modifier.weight(1f)) {
                com.mikel.kcalma.ui.componentes.BotonPrincipal(
                    if (pendientes.size == 1) "Aceptar" else "Aceptar los ${pendientes.size}", { onAceptar(pendientes) },
                )
            }
        }
    }
}

private fun numero(v: Double) = if (v == Math.floor(v)) v.toInt().toString() else "%.1f".format(v).replace('.', ',')

@Composable
private fun Dato(nombre: String, valor: String) {
    val c = KcalmaTheme.colores
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(nombre, style = KcalmaTheme.tipo.pequeno, color = c.texto2, modifier = Modifier.padding(end = 12.dp))
        Text(valor, style = KcalmaTheme.tipo.pequeno, color = c.texto, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f, fill = false))
    }
}

private val SUGERENCIAS = listOf("¿Cómo voy este mes?", "Me molesta el hombro, ¿qué cambio?", "¿Cuánta proteína me queda hoy?", "Técnica del peso muerto")

/** Chat con el entrenador. Los cambios que propone salen como tarjeta y solo se aplican al tocar «Cambiar». */
@Composable
fun PantallaChat(
    mensajes: List<MensajeChat>, pensando: Boolean, error: String?, rutinas: List<Rutina>,
    onAtras: () -> Unit, onEnviar: (String) -> Unit, onCambio: (Int, Boolean) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    var texto by rememberSaveable { mutableStateOf("") }
    val lista = rememberLazyListState()
    LaunchedEffect(mensajes.size, pensando) { if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.size) }
    val dictar = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { texto = (texto + " " + it).trim().take(400) }
    }
    fun enviar(s: String) { if (s.isNotBlank() && !pensando) { onEnviar(s.trim()); texto = "" } }

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(Modifier.padding(horizontal = MARGEN)) { BarraSuperior("‹", onAtras, "Entrenador") }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = lista, contentPadding = PaddingValues(horizontal = MARGEN, vertical = 4.dp)) {
            if (mensajes.isEmpty()) item {
                Text(
                    "Pregúntame por tus rutinas, la técnica de un ejercicio o qué cambiar. Conozco tus entrenos y tu plan de comidas; si propongo un cambio, tú decides si se hace.",
                    style = t.cuerpo, color = c.texto2,
                    modifier = Modifier.fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(14.dp),
                )
            }
            itemsIndexed(mensajes) { i, m ->
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = if (m.mio) Alignment.End else Alignment.Start) {
                    Text(
                        m.texto, style = t.cuerpo, color = c.texto,
                        modifier = Modifier.widthIn(max = 300.dp).vidrio(RoundedCornerShape(18.dp), resaltado = m.mio).padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                    m.cambio?.let { cambio ->
                        TarjetaCambio(
                            cambio, rutinas.firstOrNull { it.id == cambio.rutina }, m.estado,
                            onAceptar = { onCambio(i, true) }, onNo = { onCambio(i, false) },
                            modifier = Modifier.padding(top = 6.dp).widthIn(max = 300.dp),
                        )
                    }
                }
            }
            item {
                if (pensando) Text("Pensando…", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                else if (error != null) Text(error, style = t.pequeno, color = c.rojo, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            }
        }
        if (!pensando && texto.isEmpty()) LazyRow(
            contentPadding = PaddingValues(horizontal = MARGEN), horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp),
        ) {
            items(SUGERENCIAS) { s ->
                Text(
                    s, style = t.pequeno, color = c.texto2,
                    modifier = Modifier.vidrio(RoundedCornerShape(12.dp)).clickable { enviar(s) }.padding(horizontal = 11.dp, vertical = 7.dp),
                )
            }
        }
        Row(Modifier.padding(start = MARGEN, end = MARGEN, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                texto, { texto = it.take(400) },
                textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento), maxLines = 4,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp).vidrio(RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { campo -> Box { if (texto.isEmpty()) Text("Pregunta lo que quieras…", style = t.cuerpo, color = c.texto3); campo() } },
            )
            Box(
                Modifier.padding(start = 6.dp).size(44.dp).vidrio(CircleShape).clickable {
                    val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Pregunta a tu entrenador")
                    runCatching { dictar.launch(i) }.onFailure { Toast.makeText(contexto, "Este móvil no tiene dictado por voz", Toast.LENGTH_SHORT).show() }
                },
                contentAlignment = Alignment.Center,
            ) { Icono(Ic.VOZ, tamano = 19.dp, color = c.texto2) }
            Box(
                Modifier.padding(start = 6.dp).size(44.dp).esferaBrillo(CircleShape).clickable(enabled = texto.isNotBlank() && !pensando) { enviar(texto) },
                contentAlignment = Alignment.Center,
            ) { Icono(Ic.ENVIAR, tamano = 18.dp, color = c.sobreAcento) }
        }
    }
}
