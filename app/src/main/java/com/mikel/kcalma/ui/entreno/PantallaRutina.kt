package com.mikel.kcalma.ui.entreno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Ejercicio
import com.mikel.kcalma.dominio.Ejercicios
import com.mikel.kcalma.dominio.Formato
import com.mikel.kcalma.dominio.Progresion
import com.mikel.kcalma.dominio.Rutina
import com.mikel.kcalma.dominio.Rutinas
import com.mikel.kcalma.dominio.Sesion
import com.mikel.kcalma.dominio.historial
import com.mikel.kcalma.dominio.ultimasSeries
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.Etiqueta
import com.mikel.kcalma.ui.componentes.FilaChips
import com.mikel.kcalma.ui.componentes.Segmentado
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.theme.KcalmaTheme

/** Una rutina: músculos, días, ejercicios con el último peso, y Empezar. */
@Composable
fun PantallaRutina(
    r: Rutina,
    sesiones: List<Sesion>,
    onAtras: () -> Unit,
    onEmpezar: () -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
    onDias: (Set<Int>) -> Unit,
    onEjercicio: (Ejercicio) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val (p, s) = r.musculos()
    val original = Rutinas.predefinidas.firstOrNull { it.id == r.id }
    val lateral = Modifier.padding(horizontal = MARGEN)
    Box(Modifier.fillMaxSize().fondoKcalma()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item { Box(lateral) { BarraSuperior("‹", onAtras, "") { BotonTexto("Editar", onEditar) } } }
            item {
                Column(lateral) {
                    Text(r.nombre, style = t.tituloGrande, color = c.texto)
                    Text(detalleRutina(r), style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 2.dp))
                    if (r.descripcion.isNotEmpty()) Text(r.descripcion, style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 6.dp))
                }
            }
            item {
                Column(lateral.padding(top = 14.dp).fillMaxWidth().vidrio(RoundedCornerShape(22.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MapasCuerpo(p, s, 92.dp)
                    Box(Modifier.padding(top = 10.dp)) {
                        FilaChips { (p.map { it.nombre } + s.map { "${it.nombre} (poco)" }).forEach { Etiqueta(it) } }
                    }
                }
            }
            item {
                Rotulo("Qué días la haces", lateral)
                Row(lateral.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in 1..7) {
                        val on = d in r.dias
                        Box(
                            Modifier.weight(1f).vidrio(RoundedCornerShape(14.dp), resaltado = on)
                                .clickable { onDias(if (on) r.dias - d else r.dias + d) }.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(DIAS[d - 1], style = t.tituloTarjeta, color = if (on) c.texto else c.texto3) }
                    }
                }
            }
            item { Rotulo(when (r.formato) { Formato.SERIES -> "Ejercicios"; Formato.INTERVALOS -> "Por turnos, una ronda cada uno"; Formato.COMPLEJO -> "Cada ronda, seguidos y sin soltar el peso"; else -> "Cada vuelta" }, lateral) }
            itemsIndexed(r.bloques) { _, b ->
                val ej = Ejercicios.porId(b.ejercicio) ?: return@itemsIndexed
                val ultima = sesiones.ultimasSeries(ej.id)
                Row(
                    lateral.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).clickable { onEjercicio(ej) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FiguraEjercicio(ej, Modifier.size(46.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(ej.nombre, style = t.tituloTarjeta, color = c.texto)
                        Text(
                            if (r.formato == Formato.SERIES) "${b.series} × ${b.repsMin}–${b.repsMax}${if (ej.porLado) " por lado" else ""} · descanso ${b.descanso} s"
                            else if (r.formato == Formato.INTERVALOS) "${r.trabajo} s de trabajo · ${r.pausa} s de descanso"
                            else "${b.repsMin} reps${if (ej.porLado) " por lado" else ""}",
                            style = t.pequeno, color = c.texto2,
                        )
                    }
                    if (ultima.isNotEmpty()) {
                        val m = ultima.maxBy { it.kg }
                        Text(
                            if (ej.conPeso) "${Progresion.kg(m.kg)} × ${m.reps}" else "× ${ultima.maxOf { it.reps }}",
                            style = t.pequeno, color = c.acento,
                        )
                    }
                }
            }
            item {
                when {
                    !r.predefinida -> BotonTexto("Borrar rutina", onBorrar, lateral.padding(top = 8.dp), c.rojo)
                    original != null && original != r.copy(dias = original.dias) -> BotonTexto("Volver a la original", onBorrar, lateral.padding(top = 8.dp))
                }
            }
        }
        BotonPrincipal(
            "Empezar", onEmpezar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = MARGEN, vertical = 12.dp),
        )
    }
}

@Composable
internal fun BotonTexto(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color? = null) {
    Text(
        texto, style = KcalmaTheme.tipo.tituloTarjeta, color = color ?: KcalmaTheme.colores.acento,
        modifier = modifier.vidrio(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Ficha de un ejercicio: la figura, cómo se hace, músculos y tu historial. */
@Composable
fun PantallaFichaEjercicio(ej: Ejercicio, sesiones: List<Sesion>, onAtras: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var lento by rememberSaveable { mutableStateOf(false) }
    val lateral = Modifier.padding(horizontal = MARGEN)
    LazyColumn(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Box(lateral) { BarraSuperior("‹", onAtras, "") } }
        item {
            Column(lateral) {
                Text(ej.nombre, style = t.tituloGrande, color = c.texto)
                Text(ej.material.nombre + if (ej.porLado) " · por lado" else "", style = t.pequeno, color = c.texto2)
            }
        }
        item {
            Box(lateral.padding(top = 12.dp).fillMaxWidth().vidrio(RoundedCornerShape(24.dp)).padding(10.dp)) {
                FiguraEjercicio(ej, Modifier.fillMaxWidth().padding(horizontal = 30.dp).aspectRatio(1f), if (lento) .5f else 1f)
                Text(
                    if (lento) "0,5×" else "1×", style = t.tituloTarjeta, color = c.texto,
                    modifier = Modifier.align(Alignment.TopEnd).vidrio(CircleShape).clickable { lento = !lento }.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        item { Box(lateral.padding(top = 12.dp)) { Segmentado(listOf("Cómo se hace", "Músculos", "Tu historial"), pestana) { pestana = it } } }
        when (pestana) {
            0 -> {
                itemsIndexed(ej.pasos) { i, (titulo, texto) ->
                    Row(lateral.padding(bottom = 10.dp)) {
                        Box(Modifier.size(26.dp).vidrio(CircleShape, resaltado = true), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = t.pequeno, color = c.texto)
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(titulo, style = t.tituloTarjeta, color = c.texto)
                            Text(texto, style = t.cuerpo, color = c.texto2)
                        }
                    }
                }
                if (ej.error.isNotEmpty()) item {
                    Column(lateral.fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(12.dp)) {
                        Text("Error típico", style = t.tituloTarjeta, color = c.aviso)
                        Text(ej.error, style = t.cuerpo, color = c.texto2)
                    }
                }
            }
            1 -> item {
                Column(lateral.fillMaxWidth().vidrio(RoundedCornerShape(22.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MapasCuerpo(ej.principales, ej.secundarios, 100.dp)
                    Box(Modifier.padding(top = 10.dp)) {
                        FilaChips { (ej.principales.map { it.nombre } + ej.secundarios.map { "${it.nombre} (poco)" }).forEach { Etiqueta(it) } }
                    }
                }
            }
            else -> {
                val hechas = sesiones.filter { it.terminada && it.series.any { s -> s.ejercicio == ej.id && s.hecha } }
                val mejor = Progresion.mejor(sesiones.historial(ej.id))
                if (mejor != null) item {
                    Row(lateral.padding(bottom = 10.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp), resaltado = true).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.mikel.kcalma.ui.componentes.Icono(com.mikel.kcalma.ui.componentes.Ic.TROFEO, tamano = 20.dp)
                        Text("  Tu mejor marca", style = t.cuerpo, color = c.texto2, modifier = Modifier.weight(1f))
                        Text(if (ej.conPeso) "${Progresion.kg(mejor.kg)} kg × ${mejor.reps}" else "${mejor.reps} reps", style = t.numero, color = c.texto)
                    }
                }
                if (hechas.isEmpty()) item {
                    Text("Aún no lo has hecho. Cuando lo apuntes, aquí verás cada sesión.", style = t.cuerpo, color = c.texto2, modifier = lateral)
                }
                itemsIndexed(hechas.take(20)) { _, s ->
                    val f = fechaDe(s.inicio)
                    Column(lateral.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).padding(12.dp)) {
                        Text("${DIAS[f.dayOfWeek.value - 1]} ${f.dayOfMonth}/${f.monthValue} · ${s.nombre}", style = t.pequeno, color = c.texto3)
                        Text(
                            s.series.filter { it.ejercicio == ej.id && it.hecha }
                                .joinToString("   ") { if (ej.conPeso) "${Progresion.kg(it.kg)}×${it.reps}" else "×${it.reps}" },
                            style = t.cuerpo, color = c.texto,
                        )
                    }
                }
            }
        }
    }
}
