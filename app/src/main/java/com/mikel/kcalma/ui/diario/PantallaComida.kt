package com.mikel.kcalma.ui.diario

import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.componentes.fondoKcalma

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaComida(
    comida: ComidaDia,
    entradas: List<Entrada>,
    onAtras: () -> Unit,
    onBuscar: () -> Unit,
    onEscanear: () -> Unit,
    onEditar: (Entrada) -> Unit,
    onBorrar: (Entrada) -> Unit,
    onGuardarHabitual: (String) -> Unit,
    onFoto: () -> Unit,
    onMover: (List<Entrada>, ComidaDia) -> Unit,
    onDeshacer: (Entrada) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val total = entradas.fold(Nutrientes()) { s, e -> s + e.nutrientes }
    var dialogo by rememberSaveable { mutableStateOf(false) }
    // Lo que se va a mover: una entrada o la comida entera
    var mover by remember { mutableStateOf<List<Entrada>?>(null) }
    // Lo último borrado, para poder deshacerlo durante unos segundos
    var borrada by remember { mutableStateOf<Entrada?>(null) }
    // SQLite puede devolver el mismo id a la entrada restaurada: la clave cambia para que la fila no siga deslizada
    var restauradas by remember { mutableStateOf(0) }
    LaunchedEffect(borrada) { if (borrada != null) { delay(5000); borrada = null } }

    Box(Modifier.fillMaxSize()) {

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        BarraSuperior("‹", onAtras, comida.nombre) {
            if (entradas.isNotEmpty()) BotonIcono("⋯", { mover = entradas })
        }

        val forma = RoundedCornerShape(16.dp)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp).vidrio(forma).padding(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${total.kcalInt}", style = t.tituloGrande, color = c.texto)
                Text("kcal", style = t.pequeno, color = c.texto3)
            }
            Text(
                "P ${total.proteinas.roundToInt()} · H ${total.hidratos.roundToInt()} · G ${total.grasas.roundToInt()}",
                style = t.cuerpo, color = c.texto2,
            )
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
            if (entradas.isEmpty()) item {
                Text("Todavía no has apuntado nada en esta comida.", style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(vertical = 10.dp))
            }
            items(entradas, key = { "${it.id}/$restauradas" }) { e -> FilaEntrada(e, { onEditar(e) }, { onBorrar(e); borrada = e }, { mover = listOf(e) }) }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Accion(Ic.BUSCAR, "Buscar", Modifier.weight(1f), onBuscar)
                    Accion(Ic.ESCANER, "Escanear", Modifier.weight(1f), onEscanear)
                    Accion(Ic.CAMARA, "Foto", Modifier.weight(1f), onFoto)
                }
            }
            if (entradas.isNotEmpty()) item {
                Text(
                    "⭐ Guardar como comida habitual", style = t.cuerpo, color = c.acento, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clickable { dialogo = true }.padding(8.dp),
                )
                Text("Desliza un alimento hacia la izquierda para borrarlo · ⋯ para moverlo a otra comida", style = t.diminuto, color = c.texto3,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    borrada?.let { e ->
        val forma = RoundedCornerShape(12.dp)
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth()
                .clip(forma).background(c.fondo4).padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Borrado: ${e.alimento.nombre}", style = t.cuerpo, color = c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            TextButton({ onDeshacer(e); restauradas++; borrada = null }) { Text("DESHACER", style = t.cuerpo, color = c.acento) }
        }
    }
    }

    mover?.let { lista ->
        val todas = lista.size == entradas.size && lista.size > 1
        ModalBottomSheet(
            onDismissRequest = { mover = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.hoja,
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                Text(
                    if (todas) "Todo lo de ${comida.nombre.lowercase()} (${lista.size})" else lista.first().let { "${it.alimento.nombre} · ${it.etiquetaCantidad}" },
                    style = t.cuerpo, color = c.texto2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text("MOVER A", style = t.diminuto, color = c.texto3, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                ComidaDia.entries.forEach { destino ->
                    val aqui = destino == comida
                    OpcionHoja(destino.icono, if (aqui) "${destino.nombre} (aquí)" else destino.nombre, activa = !aqui) {
                        mover = null
                        onMover(lista, destino)
                    }
                }
                if (!todas) OpcionHoja("✎", "Cambiar cantidad") { mover = null; onEditar(lista.first()) }
            }
        }
    }

    if (dialogo) {
        var nombre by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialogo = false },
            containerColor = c.hoja,
            title = { Text("Guardar comida habitual", style = t.titulo, color = c.texto) },
            text = {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.fondo3).padding(12.dp)) {
                    if (nombre.isEmpty()) Text("Ej.: Desayuno de diario", style = t.cuerpo, color = c.texto3)
                    BasicTextField(nombre, { nombre = it.take(40) }, singleLine = true,
                        textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onGuardarHabitual(nombre.trim().ifEmpty { comida.nombre })
                    dialogo = false
                    Toast.makeText(contexto, "Guardada en «Mis comidas»", Toast.LENGTH_SHORT).show()
                }) { Text("Guardar", color = c.acento) }
            },
            dismissButton = { TextButton(onClick = { dialogo = false }) { Text("Cancelar", color = c.texto2) } },
        )
    }
}

@Composable
private fun OpcionHoja(icono: String, texto: String, activa: Boolean = true, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(if (activa) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(c.fondo4), contentAlignment = Alignment.Center) { Text(icono, style = t.cuerpo) }
        Text(texto, style = t.titulo, color = if (activa) c.texto else c.texto3, modifier = Modifier.padding(start = 12.dp))
    }
}

@Suppress("DEPRECATION")
@Composable
private fun FilaEntrada(e: Entrada, onEditar: () -> Unit, onBorrar: () -> Unit, onMas: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val estado = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart) { onBorrar(); true } else false
    })
    val forma = RoundedCornerShape(13.dp)
    SwipeToDismissBox(
        estado,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.padding(bottom = 7.dp),
        backgroundContent = {
            // El cristal deja ver lo de detrás: el fondo rojo solo aparece mientras se desliza
            if (estado.dismissDirection == SwipeToDismissBoxValue.EndToStart) Box(
                Modifier.fillMaxSize().clip(forma).background(c.rojo.copy(alpha = .25f)).padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Icono(Ic.PAPELERA, tamano = 20.dp, color = c.rojo) }
        },
    ) {
        Row(
            Modifier.fillMaxWidth().vidrio(forma)
                .clickable(onClick = onEditar).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(e.alimento.nombre, style = t.tituloTarjeta, color = c.texto, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(e.alimento.subtitulo.ifEmpty { " " }, style = t.pequeno, color = c.texto3, maxLines = 1)
            }
            Text(
                e.etiquetaCantidad, style = t.pequeno, color = c.texto2,
                modifier = Modifier.padding(horizontal = 10.dp).clip(RoundedCornerShape(8.dp)).background(c.fondo4).padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Text("${e.nutrientes.kcalInt}", style = t.tituloTarjeta, color = c.texto)
            Box(
                Modifier.padding(start = 10.dp).size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.fondo4).clickable(onClick = onMas),
                contentAlignment = Alignment.Center,
            ) { Text("⋯", style = t.cuerpo, color = c.texto2) }
        }
    }
}

@Composable
private fun Accion(icono: Ic, texto: String, modifier: Modifier, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(13.dp)
    Column(
        modifier.vidrio(forma).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icono(icono, tamano = 22.dp)
        Text(texto, style = KcalmaTheme.tipo.pequeno, color = c.texto2, modifier = Modifier.padding(top = 3.dp))
    }
}
