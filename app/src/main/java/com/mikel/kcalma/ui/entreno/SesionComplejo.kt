package com.mikel.kcalma.ui.entreno

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
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.AjustesEntreno
import com.mikel.kcalma.dominio.Ejercicio
import com.mikel.kcalma.dominio.Ejercicios
import com.mikel.kcalma.dominio.Material
import com.mikel.kcalma.dominio.Progresion
import com.mikel.kcalma.dominio.Rutina
import com.mikel.kcalma.dominio.Serie
import com.mikel.kcalma.dominio.Sesion
import com.mikel.kcalma.dominio.ultimasSeries
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.theme.KcalmaTheme

private fun mmss(ms: Long): String = (ms.coerceAtLeast(0) / 1000).let { "%d:%02d".format(it / 60, it % 60) }

/**
 * Complex: todos los ejercicios seguidos sin soltar el peso son una ronda. Se ven todos a la vez, un toque
 * apunta la ronda entera y empieza el descanso; al acabar el descanso arranca sola la siguiente.
 */
@Composable
internal fun SesionComplejo(
    sesion: Sesion, rutina: Rutina, sesiones: List<Sesion>, ajustes: AjustesEntreno, reloj: Long,
    onSerie: (Serie) -> Unit, onFicha: (Ejercicio) -> Unit, avisar: () -> Unit, onTerminar: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val bloques = rutina.bloques.filter { Ejercicios.porId(it.ejercicio) != null }
    if (bloques.isEmpty()) return
    val ejercicios = bloques.map { Ejercicios.porId(it.ejercicio)!! }
    val hechas = sesion.series.filter { it.hecha }.map { it.orden }.distinct().size
    val total = rutina.rondas.coerceAtLeast(1)
    val acabado = hechas >= total
    // Un solo peso para todo el complex: el ya usado en esta sesión o el que propone la progresión del primer ejercicio con peso
    val conPeso = ejercicios.firstOrNull { it.conPeso }
    var kg by rememberSaveable(sesion.id) {
        mutableStateOf(
            sesion.series.firstOrNull()?.kg ?: conPeso?.let { ej ->
                val b = bloques.first { it.ejercicio == ej.id }
                Progresion.proponer(ej, b.copy(series = 1), sesiones.ultimasSeries(ej.id, sin = sesion.id), ajustes.kettlebells).series.first().kg
            } ?: 0.0
        )
    }
    var kgTexto by rememberSaveable(sesion.id) { mutableStateOf(if (kg > 0) Progresion.kg(kg) else "") }
    // Cuándo empezó la ronda en curso y cuándo acaba el descanso (0 = no se está descansando)
    var inicioRonda by rememberSaveable(sesion.id) { mutableLongStateOf(if (hechas == 0) sesion.inicio else System.currentTimeMillis()) }
    var finDescanso by rememberSaveable(sesion.id) { mutableLongStateOf(0L) }
    var tiempos by rememberSaveable(sesion.id) { mutableStateOf(listOf<Long>()) }
    val descansando = finDescanso > 0 && !acabado
    LaunchedEffect(descansando, reloj >= finDescanso) {
        if (descansando && reloj >= finDescanso) { inicioRonda = finDescanso; finDescanso = 0; avisar() }
    }
    fun apuntar(ronda: Int, hecha: Boolean) = bloques.forEachIndexed { i, b ->
        onSerie(Serie(b.ejercicio, ronda, if (ejercicios[i].conPeso) kg else 0.0, b.repsMin, hecha))
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = MARGEN, end = MARGEN, bottom = 130.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Text(
                    if (acabado) "¡Hecho! $total ${if (total == 1) "ronda" else "rondas"}" else "Ronda ${hechas + 1} de $total · sin soltar el peso",
                    style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                Row(Modifier.padding(top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (k in 0 until total.coerceAtMost(30)) Box(
                        Modifier.size(10.dp).clip(CircleShape).background(if (k < hechas) c.acento else if (k == hechas && !descansando) c.acento.copy(alpha = .45f) else c.texto.copy(alpha = .12f))
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth().vidrio(RoundedCornerShape(20.dp), resaltado = descansando).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (acabado) "Total" else if (descansando) "Descansa" else "Esta ronda", style = t.pequeno, color = if (descansando) c.acento else c.texto2)
                        Text(
                            when {
                                acabado -> mmss((if (sesion.fin > 0) sesion.fin else reloj) - sesion.inicio)
                                descansando -> mmss(finDescanso - reloj + 999)
                                else -> mmss(reloj - inicioRonda)
                            },
                            style = t.numeroGrande, color = c.texto,
                        )
                    }
                    if (conPeso != null) Column(horizontalAlignment = Alignment.End) {
                        Text(if (conPeso.material == Material.KETTLEBELL) "Kettlebell" else "Peso", style = t.pequeno, color = c.texto2, modifier = Modifier.padding(bottom = 4.dp))
                        val opciones = if (conPeso.material == Material.KETTLEBELL) ajustes.kettlebells.sorted() else emptyList()
                        if (opciones.isNotEmpty()) Text(
                            "${Progresion.kg(kg)} kg", style = t.numero, color = c.texto,
                            modifier = Modifier.vidrio(RoundedCornerShape(12.dp), resaltado = true).clickable { kg = opciones.firstOrNull { it > kg } ?: opciones.first() }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) else Row(verticalAlignment = Alignment.CenterVertically) {
                            CeldaPeso(kgTexto) { kgTexto = it; kg = it.replace(',', '.').toDoubleOrNull() ?: 0.0 }
                            Text(" kg", style = t.pequeno, color = c.texto2)
                        }
                    }
                }
            }
            itemsIndexed(bloques) { i, b ->
                val ej = ejercicios[i]
                Row(
                    Modifier.padding(top = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).clickable { onFicha(ej) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(c.texto.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = t.pequeno, color = c.texto2)
                    }
                    FiguraEjercicio(ej, Modifier.padding(start = 8.dp).size(44.dp))
                    Text(ej.nombre, style = t.tituloTarjeta, color = c.texto, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                    Text("${b.repsMin} reps${if (ej.porLado) "\npor lado" else ""}", style = t.numero, color = c.texto, textAlign = TextAlign.End)
                }
            }
            if (tiempos.isNotEmpty()) item {
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    tiempos.forEachIndexed { i, ms ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ronda ${hechas - tiempos.size + i + 1}", style = t.pequeno, color = c.texto2)
                            Text(mmss(ms), style = t.pequeno, color = c.texto)
                        }
                    }
                }
            }
            item {
                if (hechas > 0 && !acabado) Text(
                    "Quitar la última ronda", style = t.pequeno, color = c.texto3,
                    modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp)).clickable {
                        apuntar(hechas - 1, false); tiempos = tiempos.dropLast(1); finDescanso = 0; inicioRonda = System.currentTimeMillis()
                    }.padding(6.dp),
                )
                Text(
                    "Un toque por ronda. Toca un ejercicio para ver su técnica.", style = t.pequeno, color = c.texto3, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = MARGEN, vertical = 12.dp)) {
            when {
                acabado -> BotonPrincipal("Guardar entreno", onTerminar)
                descansando -> BotonPrincipal("Saltar el descanso", { inicioRonda = System.currentTimeMillis(); finDescanso = 0 })
                else -> BotonPrincipal(
                    if (hechas + 1 >= total) "Última ronda hecha" else if (rutina.pausa > 0) "Ronda hecha · descansar ${mmss(rutina.pausa * 1000L)}" else "Ronda hecha",
                    {
                        val ahora = System.currentTimeMillis()
                        apuntar(hechas, true)
                        tiempos = tiempos + (ahora - inicioRonda)
                        if (hechas + 1 < total && rutina.pausa > 0) finDescanso = ahora + rutina.pausa * 1000L else inicioRonda = ahora
                    },
                )
            }
        }
    }
}
