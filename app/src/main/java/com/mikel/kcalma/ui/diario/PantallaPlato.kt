package com.mikel.kcalma.ui.diario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Ingrediente
import com.mikel.kcalma.dominio.Plato
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlin.math.roundToInt

/** Estado del plato que se está creando o editando; vive en App mientras se buscan ingredientes. */
class EdicionPlato(val id: Long = 0, nombre: String = "", raciones: Int = 4, ingredientes: List<Ingrediente> = emptyList()) {
    var nombre by mutableStateOf(nombre)
    var raciones by mutableIntStateOf(raciones)
    val ingredientes = mutableStateListOf<Ingrediente>().apply { addAll(ingredientes) }
    fun plato() = Plato(id, nombre.trim(), raciones, ingredientes.toList())
    val valido get() = nombre.isNotBlank() && ingredientes.isNotEmpty()

    companion object {
        fun de(p: Plato) = EdicionPlato(p.id, p.nombre, p.raciones, p.ingredientes)
    }
}

@Composable
fun PantallaPlato(e: EdicionPlato, onAtras: () -> Unit, onAnadirIngrediente: () -> Unit, onGuardar: () -> Unit, onBorrar: (() -> Unit)?) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var cambiando by remember { mutableStateOf<Int?>(null) }
    var borrar by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(14.dp)
    val tarjeta = Modifier.fillMaxWidth().clip(forma).background(c.fondo2).border(1.dp, c.borde, forma)

    Column(Modifier.fillMaxSize().background(c.fondo0).statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
        BarraSuperior("‹", onAtras, if (e.id > 0) "Editar plato" else "Nuevo plato") {
            if (onBorrar != null) Text("Borrar", style = t.pequeno, color = c.rojo, modifier = Modifier.clickable { borrar = true }.padding(8.dp))
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
            item {
                Column(tarjeta.padding(12.dp)) {
                    Text("Nombre", style = t.pequeno, color = c.texto2)
                    Box(Modifier.padding(top = 4.dp)) {
                        if (e.nombre.isEmpty()) Text("Lentejas de casa", style = t.titulo, color = c.texto3)
                        BasicTextField(
                            e.nombre, { e.nombre = it.take(40) }, singleLine = true,
                            textStyle = t.titulo.copy(color = c.texto), cursorBrush = SolidColor(c.acento), modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item { MiniTitulo("Ingredientes (la olla entera)", Modifier.padding(top = 10.dp)) }
            itemsIndexed(e.ingredientes) { i, ing ->
                Row(
                    Modifier.fillMaxWidth().clickable { cambiando = i }.padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ing.alimento.nombre, style = t.cuerpo, color = c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(ing.alimento.subtitulo.ifEmpty { " " }, style = t.diminuto, color = c.texto3, maxLines = 1)
                    }
                    Text("${ing.gramos.roundToInt()} g", style = t.cuerpo, color = c.texto2)
                }
                Box(Modifier.fillMaxWidth().size(1.dp).background(c.borde))
            }
            item {
                Text(
                    "+ Añadir ingrediente", style = t.tituloTarjeta, color = c.acento,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAnadirIngrediente).padding(vertical = 12.dp, horizontal = 4.dp),
                )
            }
            item {
                Row(tarjeta.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Para cuántas raciones da?", style = t.cuerpo, color = c.texto, modifier = Modifier.weight(1f))
                    Paso("−") { if (e.raciones > 1) e.raciones-- }
                    Text("${e.raciones}", style = t.numero, color = c.texto, modifier = Modifier.padding(horizontal = 12.dp))
                    Paso("+") { if (e.raciones < 30) e.raciones++ }
                }
            }
            if (e.ingredientes.isNotEmpty()) item {
                val n = e.plato().porRacion
                Text(
                    "Cada ración: ${n.kcalInt} kcal · P ${n.proteinas.roundToInt()} · H ${n.hidratos.roundToInt()} · G ${n.grasas.roundToInt()}",
                    style = t.cuerpo, color = c.texto2, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        BotonPrincipal("Guardar plato", onClick = onGuardar, modifier = Modifier.padding(bottom = 12.dp), activo = e.valido)
    }

    cambiando?.let { i ->
        val ing = e.ingredientes.getOrNull(i) ?: return@let
        var texto by remember(i) { mutableStateOf(ing.gramos.roundToInt().toString()) }
        AlertDialog(
            onDismissRequest = { cambiando = null },
            containerColor = c.fondo2,
            title = { Text(ing.alimento.nombre, style = t.titulo, color = c.texto) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Gramos en la olla", style = t.pequeno, color = c.texto2)
                    BasicTextField(
                        texto, { texto = it.filter(Char::isDigit).take(5) }, singleLine = true,
                        textStyle = t.titulo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.fondo3).padding(12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    texto.toDoubleOrNull()?.takeIf { it > 0 }?.let { e.ingredientes[i] = ing.copy(gramos = it) }
                    cambiando = null
                }) { Text("Guardar", color = c.acento) }
            },
            dismissButton = {
                TextButton(onClick = { e.ingredientes.removeAt(i); cambiando = null }) { Text("Quitar", color = c.rojo) }
            },
        )
    }
    if (borrar && onBorrar != null) AlertDialog(
        onDismissRequest = { borrar = false },
        containerColor = c.fondo2,
        title = { Text("¿Borrar «${e.nombre}»?", style = t.titulo, color = c.texto) },
        text = { Text("Lo ya apuntado en el diario no cambia.", style = t.cuerpo, color = c.texto2) },
        confirmButton = { TextButton(onClick = { borrar = false; onBorrar() }) { Text("Borrar", color = c.rojo) } },
        dismissButton = { TextButton(onClick = { borrar = false }) { Text("Cancelar", color = c.texto2) } },
    )
}

@Composable
private fun Paso(simbolo: String, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(c.fondo4).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(simbolo, style = KcalmaTheme.tipo.numero, color = c.acento) }
}
