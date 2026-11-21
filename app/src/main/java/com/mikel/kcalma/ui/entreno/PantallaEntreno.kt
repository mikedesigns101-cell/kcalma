package com.mikel.kcalma.ui.entreno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.AjustesEntreno
import com.mikel.kcalma.dominio.Formato
import com.mikel.kcalma.dominio.Progresion
import com.mikel.kcalma.dominio.Rutina
import com.mikel.kcalma.dominio.Seccion
import com.mikel.kcalma.dominio.Sesion
import com.mikel.kcalma.dominio.volumen
import com.mikel.kcalma.ui.componentes.BurbujaIcono
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.Segmentado
import com.mikel.kcalma.ui.componentes.entrada
import com.mikel.kcalma.ui.componentes.esferaBrillo
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.hoy.BarraInferior
import com.mikel.kcalma.ui.hoy.Pestana
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal val DIAS = listOf("L", "M", "X", "J", "V", "S", "D")
internal val MARGEN = 16.dp

internal fun fechaDe(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/** Rutina que toca hoy: la que tiene hoy entre sus días (la última usada si hay varias). */
fun rutinaDeHoy(rutinas: List<Rutina>, sesiones: List<Sesion>, hoy: LocalDate = LocalDate.now()): Rutina? {
    val d = hoy.dayOfWeek.value
    val candidatas = rutinas.filter { d in it.dias }
    return candidatas.maxByOrNull { r -> sesiones.firstOrNull { it.rutinaId == r.id }?.inicio ?: 0L }
}

/** "55 min" o "1 h 10 min". */
internal fun minutos(m: Int) = if (m < 60) "$m min" else "${m / 60} h${if (m % 60 > 0) " ${m % 60} min" else ""}"

internal fun detalleRutina(r: Rutina): String = when (r.formato) {
    Formato.SERIES -> "${r.bloques.size} ejercicios · ${r.totalSeries} series · ${minutos(r.duracion())}"
    else -> "${r.formato.nombre} · ${minutos(r.minutos)}"
}

@Composable
fun PantallaEntreno(
    seccion: Seccion,
    rutinas: List<Rutina>,
    sesiones: List<Sesion>,
    ajustes: AjustesEntreno,
    onSeccion: (Seccion) -> Unit,
    onAbrir: (Rutina) -> Unit,
    onEmpezar: (Rutina) -> Unit,
    onContinuar: (Sesion) -> Unit,
    onCrear: () -> Unit,
    onKettlebells: (List<Double>) -> Unit,
    onPestana: (Pestana) -> Unit,
    onCamara: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val lateral = Modifier.padding(horizontal = MARGEN)
    val propias = rutinas.filter { it.seccion == seccion && !it.predefinida }
    val hechas = rutinas.filter { it.seccion == seccion && it.predefinida }
    Box(Modifier.fillMaxSize().fondoKcalma()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(top = 12.dp, bottom = 130.dp)) {
            item { Text("Entreno", style = t.tituloGrande, color = c.texto, modifier = lateral.padding(bottom = 12.dp)) }
            item {
                Box(lateral) {
                    Segmentado(Seccion.entries.map { it.nombre }, seccion.ordinal) { onSeccion(Seccion.entries[it]) }
                }
            }
            item { Semana(rutinas, sesiones, lateral.padding(top = 4.dp, bottom = 12.dp)) }
            item {
                Box(lateral.entrada(0)) {
                    if (hayEntrenoHoy(rutinas, sesiones)) TarjetaEntrenoHoy(rutinas, sesiones, onAbrir, onEmpezar, onContinuar)
                    else Text(
                        "Pon días a tus rutinas y aquí verás cada día la que toca. Ábrela y marca los días.",
                        style = t.pequeno, color = c.texto2,
                        modifier = Modifier.fillMaxWidth().vidrio(RoundedCornerShape(20.dp)).padding(14.dp),
                    )
                }
            }
            if (seccion == Seccion.KETTLEBELL) item { Kettlebells(ajustes.kettlebells, onKettlebells, lateral.padding(top = 14.dp)) }
            if (propias.isNotEmpty()) {
                item { Rotulo("Tus rutinas", lateral) }
                items(propias, key = { it.id }) { r -> FilaRutina(r, lateral) { onAbrir(r) } }
            }
            item { Rotulo("Rutinas hechas", lateral) }
            items(hechas, key = { it.id }) { r -> FilaRutina(r, lateral) { onAbrir(r) } }
            item {
                Row(
                    lateral.padding(top = 2.dp).fillMaxWidth().vidrio(RoundedCornerShape(20.dp)).clickable(onClick = onCrear).padding(13.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icono(Ic.MAS, tamano = 17.dp)
                    Text(
                        if (seccion == Seccion.KETTLEBELL) "  Crear rutina de kettlebell" else "  Crear rutina",
                        style = t.tituloTarjeta, color = c.acento,
                    )
                }
            }
            val ultimas = sesiones.filter { it.terminada }.take(4)
            if (ultimas.isNotEmpty()) {
                item { Rotulo("Últimos entrenos", lateral) }
                items(ultimas, key = { "s${it.id}" }) { s -> FilaSesion(s, lateral) }
            }
        }
        BarraInferior(Modifier.align(Alignment.BottomCenter), Pestana.ENTRENO, onPestana, onCamara)
    }
}

/** Hay algo que enseñar en la tarjeta de hoy: un entreno a medias o una rutina con hoy entre sus días. */
fun hayEntrenoHoy(rutinas: List<Rutina>, sesiones: List<Sesion>) =
    sesiones.any { !it.terminada } || rutinaDeHoy(rutinas, sesiones) != null

/** Tarjeta "Hoy toca" (o "Entreno a medias"), en Entreno y en Hoy. */
@Composable
fun TarjetaEntrenoHoy(
    rutinas: List<Rutina>, sesiones: List<Sesion>,
    onAbrir: (Rutina) -> Unit, onEmpezar: (Rutina) -> Unit, onContinuar: (Sesion) -> Unit,
    mapa: Boolean = true,
) {
    val enCurso = sesiones.firstOrNull { !it.terminada }
    val deHoy = rutinaDeHoy(rutinas, sesiones)
    when {
        enCurso != null -> TarjetaHoy(
            "Entreno a medias", enCurso.nombre, "${enCurso.series.count { it.hecha }} series hechas",
            rutinas.firstOrNull { it.id == enCurso.rutinaId }.takeIf { mapa }, "Continuar",
        ) { onContinuar(enCurso) }
        deHoy != null -> {
            val hecha = sesiones.any { it.terminada && it.rutinaId == deHoy.id && fechaDe(it.inicio) == LocalDate.now() }
            TarjetaHoy(
                if (hecha) "Hecho hoy" else "Hoy toca", deHoy.nombre, detalleRutina(deHoy), deHoy.takeIf { mapa },
                if (hecha) "Ver" else "Empezar",
            ) { if (hecha) onAbrir(deHoy) else onEmpezar(deHoy) }
        }
    }
}

@Composable
internal fun Rotulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(), style = KcalmaTheme.tipo.etiqueta, color = KcalmaTheme.colores.texto3,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp, start = 2.dp),
    )
}

/** Tira de la semana: qué toca cada día y qué días ya se entrenó. */
@Composable
private fun Semana(rutinas: List<Rutina>, sesiones: List<Sesion>, modifier: Modifier) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val hoy = LocalDate.now()
    val lunes = hoy.minusDays(hoy.dayOfWeek.value - 1L)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (i in 0..6) {
            val dia = lunes.plusDays(i.toLong())
            val hecha = sesiones.firstOrNull { it.terminada && fechaDe(it.inicio) == dia }
            val toca = rutinas.firstOrNull { (i + 1) in it.dias }
            val esHoy = dia == hoy
            Column(
                Modifier.weight(1f).vidrio(RoundedCornerShape(14.dp), resaltado = esHoy).padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(DIAS[i], style = t.diminuto, color = c.texto3)
                if (hecha != null) Icono(Ic.CHECK, tamano = 15.dp, modifier = Modifier.padding(top = 2.dp))
                else Text(
                    toca?.nombre ?: "—", style = t.diminuto, color = if (esHoy) c.texto else c.texto2,
                    maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaHoy(etiqueta: String, nombre: String, detalle: String, rutina: Rutina?, boton: String, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column(Modifier.fillMaxWidth().vidrio(RoundedCornerShape(22.dp), resaltado = true).clickable(onClick = onClick).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(etiqueta, style = t.pequeno, color = c.texto2)
                Text(nombre, style = t.tituloPaso, color = c.texto)
                Text(detalle, style = t.pequeno, color = c.texto2)
            }
            BotonPastilla(boton, onClick)
        }
        if (rutina != null) {
            val (p, s) = rutina.musculos()
            MapasCuerpo(p, s, 62.dp, Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
        }
    }
}

/** Botón pequeño con el degradado (Empezar, Continuar…). */
@Composable
internal fun BotonPastilla(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = KcalmaTheme.colores
    Box(
        modifier.height(38.dp).esferaBrillo(RoundedCornerShape(19.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(texto, style = KcalmaTheme.tipo.tituloTarjeta, color = c.sobreAcento) }
}

@Composable
internal fun FilaRutina(r: Rutina, modifier: Modifier, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val (p, s) = r.musculos()
    Row(
        modifier.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MapaMini(p, s, 20.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(r.nombre, style = t.tituloTarjeta, color = c.texto)
            if (r.descripcion.isNotEmpty()) Text(r.descripcion, style = t.pequeno, color = c.texto2, maxLines = 2)
            Text(
                detalleRutina(r) + if (r.dias.isNotEmpty()) " · " + r.dias.sorted().joinToString("") { DIAS[it - 1] } else "",
                style = t.pequeno, color = c.acento,
            )
        }
        Text("›", style = t.titulo, color = c.texto3)
    }
}

@Composable
private fun FilaSesion(s: Sesion, modifier: Modifier) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val f = fechaDe(s.inicio)
    val min = ((s.fin - s.inicio) / 60000).toInt()
    Row(
        modifier.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BurbujaIcono(Ic.PESA, tamano = 34.dp, icon = 17.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(s.nombre, style = t.tituloTarjeta, color = c.texto)
            Text(
                "${DIAS[f.dayOfWeek.value - 1]} ${f.dayOfMonth}/${f.monthValue} · ${s.series.count { it.hecha }} series · ${minutos(min.coerceAtLeast(1))}",
                style = t.pequeno, color = c.texto2,
            )
        }
        val v = s.volumen()
        if (v > 0) Text("${Progresion.kg(Math.round(v).toDouble())} kg", style = t.pequeno, color = c.texto2)
    }
}

/** Las kettlebells que tiene el usuario: solo se proponen esas. */
@Composable
private fun Kettlebells(tiene: List<Double>, onCambio: (List<Double>) -> Unit, modifier: Modifier) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column(modifier) {
        Text("TUS KETTLEBELLS", style = t.etiqueta, color = c.texto3, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
        listOf(listOf(4.0, 6.0, 8.0, 10.0, 12.0, 14.0), listOf(16.0, 20.0, 24.0, 28.0, 32.0, 40.0)).forEach { fila ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { kg ->
                    val on = kg in tiene
                    Box(
                        Modifier.weight(1f).vidrio(RoundedCornerShape(14.dp), resaltado = on)
                            .clickable { onCambio(if (on) tiene - kg else (tiene + kg).sorted()) }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(Progresion.kg(kg), style = t.numero, color = if (on) c.texto else c.texto3) }
                }
            }
        }
        Text(
            "Solo te propondré estas. Para subir, la siguiente que tengas.", style = t.pequeno, color = c.texto3,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp),
        )
        Spacer(Modifier.width(0.dp))
    }
}
