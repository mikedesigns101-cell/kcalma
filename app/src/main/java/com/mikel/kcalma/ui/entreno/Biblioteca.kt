package com.mikel.kcalma.ui.entreno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Ejercicio
import com.mikel.kcalma.dominio.Ejercicios
import com.mikel.kcalma.dominio.Material
import com.mikel.kcalma.dominio.Musculo
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.text.Normalizer

internal fun sinAcentos(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

/** Todos los ejercicios: buscador y filtros por músculo y material. Al tocar uno se abre su ficha. */
@Composable
fun PantallaBiblioteca(onAtras: () -> Unit, onEjercicio: (Ejercicio) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var busca by rememberSaveable { mutableStateOf("") }
    var musculo by rememberSaveable { mutableStateOf<Musculo?>(null) }
    var material by rememberSaveable { mutableStateOf<Material?>(null) }
    val todos = Ejercicios.todos
    val lista = remember(busca, musculo, material) {
        val q = sinAcentos(busca.trim())
        todos.filter { e ->
            (q.isEmpty() || sinAcentos(e.nombre).contains(q)) &&
                (musculo == null || musculo in e.principales || musculo in e.secundarios) &&
                (material == null || e.material == material)
        }.sortedWith(compareBy({ musculo != null && musculo !in it.principales }, { sinAcentos(it.nombre) }))
    }
    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding()) {
        Box(Modifier.padding(horizontal = MARGEN)) { BarraSuperior("‹", onAtras, "Ejercicios") }
        BasicTextField(
            busca, { busca = it }, singleLine = true,
            textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
            modifier = Modifier.padding(horizontal = MARGEN).fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).padding(12.dp),
            decorationBox = { campo ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icono(Ic.BUSCAR, tamano = 16.dp, color = c.texto3)
                    Box(Modifier.padding(start = 8.dp)) { if (busca.isEmpty()) Text("Buscar entre ${todos.size} ejercicios", style = t.cuerpo, color = c.texto3); campo() }
                }
            },
        )
        Chips(listOf<Musculo?>(null) + Musculo.entries, musculo, { it?.nombre ?: "Todos" }) { musculo = it }
        Chips(listOf<Material?>(null) + Material.entries, material, { it?.nombre ?: "Todo material" }) { material = it }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = MARGEN, end = MARGEN, top = 4.dp, bottom = 120.dp)) {
            item { Text("${lista.size} ${if (lista.size == 1) "ejercicio" else "ejercicios"}", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(bottom = 6.dp)) }
            items(lista, key = { it.id }) { e ->
                Row(
                    Modifier.padding(bottom = 6.dp).fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).clickable { onEjercicio(e) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MapaMini(e.principales, e.secundarios, 18.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(e.nombre, style = t.tituloTarjeta, color = c.texto)
                        Text(e.material.nombre + " · " + e.principales.joinToString { it.nombre }, style = t.pequeno, color = c.texto2)
                    }
                    Text("›", style = t.titulo, color = c.texto3)
                }
            }
        }
    }
}

@Composable
private fun <T> Chips(opciones: List<T>, elegido: T, nombre: (T) -> String, onElegir: (T) -> Unit) {
    val c = KcalmaTheme.colores
    LazyRow(
        Modifier.padding(top = 8.dp), contentPadding = PaddingValues(horizontal = MARGEN),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(opciones) { o ->
            val on = o == elegido
            Text(
                nombre(o), style = KcalmaTheme.tipo.pequeno, color = if (on) c.texto else c.texto2,
                modifier = Modifier.vidrio(RoundedCornerShape(12.dp), resaltado = on).clickable { onElegir(o) }.padding(horizontal = 11.dp, vertical = 7.dp),
            )
        }
    }
}
