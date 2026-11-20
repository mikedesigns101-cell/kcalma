package com.mikel.kcalma.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.ui.theme.KcalmaTheme

/** Botón grande con el degradado de la marca. */
@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, activo: Boolean = true) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(15.dp)
    Box(
        modifier.fillMaxWidth().height(52.dp).alpha(if (activo) 1f else .35f)
            .shadow(if (activo) 14.dp else 0.dp, forma, ambientColor = c.acento, spotColor = c.acento)
            .clip(forma).background(c.degradado)
            .clickable(enabled = activo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(texto, style = KcalmaTheme.tipo.titulo, color = c.sobreAcento) }
}

/** Barra de arriba de cada paso: botón atrás y progreso. */
@Composable
fun CabeceraPaso(progreso: Float, onAtras: () -> Unit) {
    val c = KcalmaTheme.colores
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.fondo2)
                .border(1.dp, c.borde, RoundedCornerShape(10.dp)).clickable(onClick = onAtras),
            contentAlignment = Alignment.Center,
        ) { Text("‹", style = KcalmaTheme.tipo.marca, color = c.texto2) }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(c.fondo3)) {
            Box(Modifier.fillMaxWidth(progreso).height(5.dp).clip(CircleShape).background(c.degradado))
        }
    }
}

@Composable
fun TituloPaso(titulo: String, subtitulo: String? = null) {
    val c = KcalmaTheme.colores
    Text(titulo, style = KcalmaTheme.tipo.tituloPaso, color = c.texto)
    if (subtitulo != null) {
        Text(subtitulo, style = KcalmaTheme.tipo.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
    } else {
        Spacer(Modifier.height(16.dp))
    }
}

/** Opción grande seleccionable, con emoji, título y ejemplos. */
@Composable
fun Opcion(emoji: String, titulo: String, detalle: String, seleccionada: Boolean, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(14.dp)
    val borde = if (seleccionada) BorderStroke(1.5.dp, c.degradado) else BorderStroke(1.dp, c.borde)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(forma).background(c.fondo2).border(borde, forma)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (seleccionada) c.acento.copy(alpha = .14f) else c.fondo4),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, style = KcalmaTheme.tipo.titulo) }
        Column(Modifier.padding(start = 11.dp)) {
            Text(titulo, style = KcalmaTheme.tipo.tituloTarjeta, color = c.texto)
            Text(detalle, style = KcalmaTheme.tipo.pequeno, color = c.texto3)
        }
    }
}

/** Selector de varias opciones en una fila (Hombre / Mujer, Suave / Normal / Rápido…). */
@Composable
fun Segmentado(opciones: List<String>, seleccionado: Int, onSeleccion: (Int) -> Unit) {
    val c = KcalmaTheme.colores
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp)).background(c.fondo2)
            .border(1.dp, c.borde, RoundedCornerShape(12.dp)).padding(3.dp)
    ) {
        opciones.forEachIndexed { i, texto ->
            val on = i == seleccionado
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (on) c.fondo4 else c.fondo2)
                    .clickable { onSeleccion(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(texto, style = KcalmaTheme.tipo.cuerpo, color = if (on) c.texto else c.texto2) }
        }
    }
}

/** Campo numérico en tarjeta: "Edad  30 años". Vacío muestra el texto de ejemplo. */
@Composable
fun CampoNumero(
    etiqueta: String, valor: String, unidad: String, onCambio: (String) -> Unit,
    modifier: Modifier = Modifier, ejemplo: String = "", decimales: Boolean = false, error: Boolean = false,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val forma = RoundedCornerShape(14.dp)
    // Toda la tarjeta es el campo: tocar en cualquier parte abre el teclado
    BasicTextField(
        value = valor,
        onValueChange = { nuevo ->
            val limpio = nuevo.replace('.', ',').filter { it.isDigit() || (decimales && it == ',') }
            if (limpio.length <= 5 && limpio.count { it == ',' } <= 1) onCambio(limpio)
        },
        textStyle = t.campo.copy(color = c.texto),
        singleLine = true,
        cursorBrush = SolidColor(c.acento),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimales) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier.padding(bottom = 8.dp),
        decorationBox = { campo ->
            Column(
                Modifier.fillMaxWidth().clip(forma).background(c.fondo2)
                    .border(1.dp, if (error) c.rojo else c.borde, forma).padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(etiqueta, style = t.pequeno, color = if (error) c.rojo else c.texto3)
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.width(IntrinsicSize.Max)) {
                        if (valor.isEmpty()) Text(ejemplo, style = t.campo, color = c.texto3)
                        campo()
                    }
                    // Con un texto de ejemplo como "No lo sé" la unidad sobra
                    if (valor.isNotEmpty() || ejemplo.firstOrNull()?.isDigit() == true) {
                        Text(" $unidad", style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
            }
        },
    )
}

enum class EstadoChip { NEUTRO, NO, SI }

@Composable
fun ChipSeleccion(texto: String, estado: EstadoChip, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val tinta = when (estado) { EstadoChip.NEUTRO -> c.texto2; EstadoChip.NO -> c.rojo; EstadoChip.SI -> c.verde }
    val fondo = if (estado == EstadoChip.NEUTRO) c.fondo2 else tinta.copy(alpha = .13f)
    val borde = if (estado == EstadoChip.NEUTRO) c.borde else tinta.copy(alpha = .4f)
    val prefijo = if (estado == EstadoChip.NO) "✕ " else ""
    val forma = RoundedCornerShape(10.dp)
    Text(
        "$prefijo$texto", style = KcalmaTheme.tipo.cuerpo, color = tinta,
        modifier = Modifier.clip(forma).background(fondo).border(1.dp, borde, forma)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Chip para escribir y añadir un alimento a una lista. */
@Composable
fun ChipAnadir(onAnadir: (String) -> Unit) {
    val c = KcalmaTheme.colores
    var texto by remember { mutableStateOf("") }
    val forma = RoundedCornerShape(10.dp)
    Box(
        Modifier.clip(forma).background(c.fondo2).border(1.dp, c.borde, forma).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (texto.isEmpty()) Text("+ Añadir", style = KcalmaTheme.tipo.cuerpo, color = c.acento)
        BasicTextField(
            value = texto,
            onValueChange = { if (it.length <= 30) texto = it },
            textStyle = KcalmaTheme.tipo.cuerpo.copy(color = c.texto),
            singleLine = true,
            cursorBrush = SolidColor(c.acento),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                val limpio = texto.trim().replaceFirstChar { it.uppercase() }
                if (limpio.isNotEmpty()) onAnadir(limpio)
                texto = ""
            }),
            modifier = Modifier.width(90.dp),
        )
    }
}

/** Recuadro discontinuo con una explicación. */
@Composable
fun Nota(texto: AnnotatedString, modifier: Modifier = Modifier) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(12.dp)
    Text(
        texto, style = KcalmaTheme.tipo.pequeno, color = c.texto3,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp).clip(forma).background(c.fondo1)
            .border(1.dp, c.borde, forma).padding(horizontal = 11.dp, vertical = 9.dp),
    )
}

@Composable
fun Etiqueta(texto: String) {
    Text(
        texto.uppercase(), style = KcalmaTheme.tipo.etiqueta, color = KcalmaTheme.colores.texto2,
        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilaChips(contenido: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { contenido() }
}
