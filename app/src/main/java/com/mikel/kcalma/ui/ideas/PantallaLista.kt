package com.mikel.kcalma.ui.ideas

import com.mikel.kcalma.ui.componentes.fondoKcalma

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.EstadoLista
import com.mikel.kcalma.datos.ListaCompra
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.diario.MiniTitulo
import com.mikel.kcalma.ui.theme.KcalmaTheme

@Composable
fun PantallaLista(lista: EstadoLista, onAtras: () -> Unit, onMarcar: (String) -> Unit, onVaciar: () -> Unit, onCompartir: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        BarraSuperior("‹", onAtras, "Lista de la compra") {
            if (lista.items.isNotEmpty()) Text(
                "Compartir", style = t.pequeno, color = c.acento,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.fondo3).clickable(onClick = onCompartir).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        if (lista.items.isEmpty()) {
            Text(
                "Está vacía. Abre una receta en Ideas y pulsa «🛒 A la lista»: se suman sus ingredientes y lo repetido se junta.",
                style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(vertical = 12.dp),
            )
            return@Column
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Text("De ${lista.recetas.size} ${if (lista.recetas.size == 1) "receta" else "recetas"}: ${lista.recetas.joinToString()}", style = t.pequeno, color = c.texto2)
            }
            ListaCompra.SECCIONES.forEach { s ->
                val l = lista.items.filter { it.seccion == s }
                if (l.isNotEmpty()) {
                    item { MiniTitulo(s, Modifier.padding(top = 8.dp)) }
                    items(l, key = { it.nombre }) { i ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onMarcar(i.nombre) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val forma = RoundedCornerShape(6.dp)
                            Box(
                                Modifier.size(20.dp).clip(forma).background(if (i.hecho) c.verde else c.fondo0)
                                    .border(1.5.dp, if (i.hecho) c.verde else c.texto3, forma),
                                contentAlignment = Alignment.Center,
                            ) { if (i.hecho) Text("✓", style = t.diminuto, color = c.sobreAcento) }
                            Text(
                                i.nombre, style = t.cuerpo, color = if (i.hecho) c.texto3 else c.texto,
                                textDecoration = if (i.hecho) TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                            Text(ListaCompra.gramosTexto(i.gramos), style = t.cuerpo, color = c.texto2)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.borde))
                    }
                }
            }
            item {
                Text(
                    "Vaciar lista", style = t.cuerpo, color = c.rojo, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).clickable(onClick = onVaciar).padding(8.dp),
                )
            }
        }
    }
}
