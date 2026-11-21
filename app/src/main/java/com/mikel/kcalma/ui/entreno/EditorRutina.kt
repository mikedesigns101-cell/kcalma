package com.mikel.kcalma.ui.entreno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.BloqueRutina
import com.mikel.kcalma.dominio.Ejercicios
import com.mikel.kcalma.dominio.Formato
import com.mikel.kcalma.dominio.Rutina
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.Segmentado
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.text.Normalizer

private fun sinTildes(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

/**
 * Crear o editar una rutina. Editar una de las hechas guarda tu copia (con su mismo id).
 * Orden con flechas ↑ ↓ (sin arrastrar, más fiable con una mano).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditorRutina(inicial: Rutina, onAtras: () -> Unit, onGuardar: (Rutina) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var r by remember { mutableStateOf(inicial) }
    var anadir by remember { mutableStateOf(false) }
    val lateral = Modifier.padding(horizontal = MARGEN)
    fun bloques(f: (MutableList<BloqueRutina>) -> Unit) { r = r.copy(bloques = r.bloques.toMutableList().also(f)) }
    val (p, s) = r.musculos()
    Box(Modifier.fillMaxSize().fondoKcalma()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item { Box(lateral) { BarraSuperior("✕", onAtras, if (inicial.id.isEmpty()) "Nueva rutina" else "Editar rutina") } }
            item {
                BasicTextField(
                    r.nombre, { r = r.copy(nombre = it.take(30)) }, singleLine = true,
                    textStyle = t.tituloPaso.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                    modifier = lateral.fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(14.dp),
                    decorationBox = { campo -> Box { if (r.nombre.isEmpty()) Text("Nombre de la rutina", style = t.tituloPaso, color = c.texto3); campo() } },
                )
            }
            item {
                Column(lateral.padding(top = 12.dp)) {
                    Segmentado(com.mikel.kcalma.dominio.Seccion.entries.map { it.nombre }, r.seccion.ordinal) {
                        r = r.copy(seccion = com.mikel.kcalma.dominio.Seccion.entries[it])
                    }
                    Segmentado(Formato.entries.map { it.nombre }, r.formato.ordinal) {
                        val f = Formato.entries[it]
                        r = r.copy(formato = f, minutos = if (f != Formato.SERIES && r.minutos == 0) 12 else r.minutos)
                    }
                    if (r.formato != Formato.SERIES) Paso("Minutos en total", r.minutos, 1, 60) { r = r.copy(minutos = it) }
                    Text(
                        when (r.formato) {
                            Formato.SERIES -> "Series con descanso; te propongo el peso según lo que hiciste."
                            Formato.EMOM -> "Cada minuto empiezas un ejercicio, en orden. Lo que sobra del minuto, descansas."
                            Formato.AMRAP -> "Tantas vueltas como puedas en el tiempo; apuntas cada vuelta."
                        },
                        style = t.pequeno, color = c.texto3, modifier = Modifier.padding(start = 2.dp, top = 2.dp),
                    )
                }
            }
            item {
                Rotulo("Días", lateral)
                Row(lateral.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in 1..7) {
                        val on = d in r.dias
                        Box(
                            Modifier.weight(1f).vidrio(RoundedCornerShape(14.dp), resaltado = on)
                                .clickable { r = r.copy(dias = if (on) r.dias - d else r.dias + d) }.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(DIAS[d - 1], style = t.tituloTarjeta, color = if (on) c.texto else c.texto3) }
                    }
                }
            }
            if (r.bloques.isNotEmpty()) item {
                Box(lateral.padding(top = 12.dp).fillMaxWidth().vidrio(RoundedCornerShape(20.dp)).padding(10.dp), contentAlignment = Alignment.Center) {
                    MapasCuerpo(p, s, 54.dp)
                }
            }
            item { Rotulo("Ejercicios", lateral) }
            itemsIndexed(r.bloques) { i, b ->
                val ej = Ejercicios.porId(b.ejercicio) ?: return@itemsIndexed
                Column(lateral.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FiguraEjercicio(ej, Modifier.size(40.dp))
                        Text(ej.nombre, style = t.tituloTarjeta, color = c.texto, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        MiniBoton(Ic.ARRIBA, i > 0) { bloques { l -> l.add(i - 1, l.removeAt(i)) } }
                        MiniBoton(Ic.ABAJO, i < r.bloques.lastIndex) { bloques { l -> l.add(i + 1, l.removeAt(i)) } }
                        MiniBoton(Ic.PAPELERA, true) { bloques { l -> l.removeAt(i) } }
                    }
                    if (r.formato == Formato.SERIES) {
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Paso("Series", b.series, 1, 10, Modifier.weight(1f)) { v -> bloques { it[i] = b.copy(series = v) } }
                            Paso("Reps mín.", b.repsMin, 1, 50, Modifier.weight(1f)) { v -> bloques { it[i] = b.copy(repsMin = v, repsMax = maxOf(v, b.repsMax)) } }
                            Paso("Reps máx.", b.repsMax, 1, 50, Modifier.weight(1f)) { v -> bloques { it[i] = b.copy(repsMax = v, repsMin = minOf(v, b.repsMin)) } }
                        }
                        Paso("Descanso (s)", b.descanso, 0, 600, Modifier.padding(top = 6.dp), salto = 15) { v -> bloques { it[i] = b.copy(descanso = v) } }
                    } else {
                        Paso("Reps por vuelta", b.repsMin, 1, 50, Modifier.padding(top = 6.dp)) { v -> bloques { it[i] = b.copy(repsMin = v, repsMax = v) } }
                    }
                }
            }
            item {
                Row(
                    lateral.fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).clickable { anadir = true }.padding(13.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icono(Ic.MAS, tamano = 17.dp)
                    Text("  Añadir ejercicio", style = t.tituloTarjeta, color = c.acento)
                }
            }
        }
        BotonPrincipal(
            "Guardar", { onGuardar(r.copy(nombre = r.nombre.trim())) },
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = MARGEN, vertical = 12.dp),
            activo = r.nombre.isNotBlank() && r.bloques.isNotEmpty() && (r.formato == Formato.SERIES || r.minutos > 0),
        )
    }
    if (anadir) {
        var busca by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { anadir = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.hoja,
        ) {
            val lista = Ejercicios.de(r.seccion).filter { sinTildes(it.nombre).contains(sinTildes(busca.trim())) }
            Column(Modifier.padding(horizontal = MARGEN)) {
                BasicTextField(
                    busca, { busca = it }, singleLine = true,
                    textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                    modifier = Modifier.fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).padding(12.dp),
                    decorationBox = { campo -> Box { if (busca.isEmpty()) Text("Buscar ejercicio", style = t.cuerpo, color = c.texto3); campo() } },
                )
                LazyColumn(Modifier.heightIn(max = 520.dp).padding(top = 8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(lista, key = { it.id }) { ej ->
                        Row(
                            Modifier.padding(bottom = 6.dp).fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).clickable {
                                bloques { it.add(BloqueRutina(ej.id, repsMin = if (r.formato == Formato.SERIES) 8 else 10, repsMax = if (r.formato == Formato.SERIES) 12 else 10, descanso = if (ej.conPeso) 120 else 60)) }
                                anadir = false
                            }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MapaMini(ej.principales, ej.secundarios, 16.dp)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(ej.nombre, style = t.tituloTarjeta, color = c.texto)
                                Text(ej.material.nombre + " · " + ej.principales.joinToString { it.nombre }, style = t.pequeno, color = c.texto2)
                            }
                            Icono(Ic.MAS, tamano = 16.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBoton(ic: Ic, activo: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 4.dp).size(32.dp).vidrio(CircleShape).clickable(enabled = activo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icono(ic, tamano = 15.dp, color = if (activo) KcalmaTheme.colores.texto2 else KcalmaTheme.colores.texto3.copy(alpha = .4f)) }
}

/** − valor + con su rótulo. */
@Composable
private fun Paso(rotulo: String, valor: Int, min: Int, max: Int, modifier: Modifier = Modifier, salto: Int = 1, onCambio: (Int) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Row(modifier.fillMaxWidth().vidrio(RoundedCornerShape(14.dp)).padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("−", style = t.titulo, color = c.texto2, modifier = Modifier.clickable { onCambio((valor - salto).coerceAtLeast(min)) }.padding(horizontal = 8.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$valor", style = t.numero, color = c.texto)
            Text(rotulo, style = t.diminuto, color = c.texto3, maxLines = 1)
        }
        Text("+", style = t.titulo, color = c.texto2, modifier = Modifier.clickable { onCambio((valor + salto).coerceAtMost(max)) }.padding(horizontal = 8.dp))
    }
}
