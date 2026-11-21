package com.mikel.kcalma.ui.ideas

import com.mikel.kcalma.ui.componentes.vidrio

import com.mikel.kcalma.ui.componentes.fondoKcalma

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.FiltrosIdeas
import com.mikel.kcalma.dominio.Idea
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.dominio.Receta
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.diario.SelectorComida
import com.mikel.kcalma.ui.hoy.BarraInferior
import com.mikel.kcalma.ui.hoy.Pestana
import com.mikel.kcalma.ui.onboarding.miles
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlin.math.roundToInt

/** Estado de la petición de ideas a la IA. */
sealed interface EstadoIa {
    data object Nada : EstadoIa
    data object Cargando : EstadoIa
    data class Error(val texto: String) : EstadoIa
}

/** Pestaña Ideas: recetas ajustadas a lo que queda del día, con filtros y más ideas con IA. */
@Composable
fun PantallaIdeas(
    comida: ComidaDia,
    restante: Nutrientes,
    ideas: List<Idea>,
    filtros: FiltrosIdeas,
    estadoIa: EstadoIa,
    onComida: (ComidaDia) -> Unit,
    onFiltros: (FiltrosIdeas) -> Unit,
    onAbrir: (Idea) -> Unit,
    onMasIa: () -> Unit,
    onPestana: (Pestana) -> Unit,
    onCamara: () -> Unit,
    /** Cosas pendientes en la lista de la compra. */
    enLista: Int = 0,
    onLista: () -> Unit = {},
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Box(Modifier.fillMaxSize().fondoKcalma()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp).padding(top = 14.dp, bottom = 130.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ideas para ${articulo(comida)}${comida.nombre.lowercase()}", style = t.marca, color = c.texto, modifier = Modifier.weight(1f))
                Text(
                    if (enLista > 0) "🛒 $enLista" else "🛒", style = t.cuerpo, color = c.texto,
                    modifier = Modifier.tarjeta().clickable(onClick = onLista).padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().tarjeta().padding(14.dp)) {
                Text("Te quedan hoy", style = t.pequeno, color = c.texto2)
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = c.texto, fontSize = 22.sp)) { append("${miles(restante.kcal.coerceAtLeast(0.0).roundToInt())} kcal ") }
                    withStyle(SpanStyle(color = c.texto3)) {
                        append("· P ${restante.proteinas.coerceAtLeast(0.0).roundToInt()} g · H ${restante.hidratos.coerceAtLeast(0.0).roundToInt()} g · G ${restante.grasas.coerceAtLeast(0.0).roundToInt()} g")
                    }
                }, style = t.numero)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SelectorComida(comida, onComida)
                Filtro("⏱ Rápidas", filtros.rapidas) { onFiltros(filtros.copy(rapidas = !filtros.rapidas)) }
                Filtro("♥ Me encanta", filtros.favoritas) { onFiltros(filtros.copy(favoritas = !filtros.favoritas)) }
            }
            Spacer(Modifier.height(12.dp))
            if (ideas.isEmpty()) {
                Text(
                    "No hay recetas con estos filtros. Quita alguno o pide ideas a la IA.",
                    style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            ideas.forEach { idea -> TarjetaIdea(idea) { onAbrir(idea) } }
            Spacer(Modifier.height(4.dp))
            when (estadoIa) {
                EstadoIa.Cargando -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = c.acento, strokeWidth = 2.dp)
                    Text("  Pensando recetas…", style = t.cuerpo, color = c.texto2)
                }
                else -> {
                    if (estadoIa is EstadoIa.Error) Text(estadoIa.texto, style = t.pequeno, color = c.aviso, modifier = Modifier.padding(bottom = 8.dp))
                    val forma = RoundedCornerShape(14.dp)
                    Text(
                        "✨ Más ideas con IA", style = t.titulo, color = c.acento,
                        modifier = Modifier.fillMaxWidth().clip(forma).border(1.dp, c.acento.copy(alpha = .45f), forma)
                            .clickable(onClick = onMasIa).padding(vertical = 13.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        BarraInferior(Modifier.align(Alignment.BottomCenter), Pestana.IDEAS, onPestana, onCamara)
    }
}

private fun articulo(c: ComidaDia) = if (c == ComidaDia.MEDIA_MANANA) "" else "la "

@Composable
private fun Modifier.tarjeta(): Modifier {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(16.dp)
    return vidrio(forma)
}

@Composable
private fun Filtro(texto: String, activo: Boolean, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(10.dp)
    Text(
        texto, style = KcalmaTheme.tipo.cuerpo, color = if (activo) c.acento else c.texto2,
        modifier = Modifier.clip(forma).background(if (activo) c.acento.copy(alpha = .12f) else c.fondo2)
            .border(1.dp, if (activo) c.acento.copy(alpha = .45f) else c.borde, forma)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
fun TarjetaIdea(idea: Idea, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val r = idea.receta
    val n = r.nutrientes
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).tarjeta().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(c.fondo4), contentAlignment = Alignment.Center) {
            Text(r.emoji, fontSize = 22.sp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.nombre, style = t.tituloTarjeta, color = c.texto, modifier = Modifier.weight(1f, fill = false))
                if (idea.encaja) Text(
                    "encaja", style = t.diminuto, color = c.verde,
                    modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(8.dp)).background(c.verde.copy(alpha = .13f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(detalle(r), style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 2.dp))
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = c.texto)) { append("${miles(n.kcalInt)} kcal") }
                append(" · P ${n.proteinas.roundToInt()} · H ${n.hidratos.roundToInt()} · G ${n.grasas.roundToInt()}")
            }, style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

private fun detalle(r: Receta) = listOfNotNull(
    "${r.minutos} min",
    if (r.deIa) "idea de la IA" else null,
    if (r.alergenos.isEmpty()) "sin alérgenos" else null,
).joinToString(" · ")

/** Detalle de una idea: ingredientes con cantidades, pasos y cómo queda el día si se añade. */
@Composable
fun DetalleIdea(idea: Idea, comida: ComidaDia, restante: Nutrientes, planKcal: Int, onAtras: () -> Unit, onAnadir: () -> Unit, onLista: () -> Unit = {}) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val r = idea.receta
    val n = r.nutrientes
    Box(Modifier.fillMaxSize().fondoKcalma()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 150.dp),
        ) {
            Text("‹", style = t.tituloGrande, color = c.texto, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onAtras).padding(horizontal = 10.dp))
            Text(r.emoji, fontSize = 44.sp, modifier = Modifier.padding(top = 4.dp))
            Text(r.nombre, style = t.tituloPaso, color = c.texto, modifier = Modifier.padding(top = 6.dp))
            Text(
                "${miles(n.kcalInt)} kcal · P ${n.proteinas.roundToInt()} g · H ${n.hidratos.roundToInt()} g · G ${n.grasas.roundToInt()} g · ${r.minutos} min",
                style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 4.dp),
            )
            if (r.alergenos.isNotEmpty()) Text("Contiene: ${r.alergenos.joinToString()}", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().tarjeta().padding(14.dp)) {
                Row {
                    Text("Ingredientes", style = t.titulo, color = c.texto, modifier = Modifier.weight(1f))
                    Text("1 persona · en crudo", style = t.pequeno, color = c.texto3)
                }
                r.ingredientes.forEach { i ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(i.alimento.nombre, style = t.cuerpo, color = c.texto)
                            if (i.alimento.marca != null) Text(i.alimento.marca, style = t.diminuto, color = c.aviso)
                        }
                        Text("${i.gramos.roundToInt()} g", style = t.cuerpo, color = c.texto2)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().tarjeta().padding(14.dp)) {
                Text("Cómo se hace", style = t.titulo, color = c.texto)
                r.pasos.forEachIndexed { k, p ->
                    Text("${k + 1}. $p", style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(c.fondo1).navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            val final = planKcal - restante.kcal + n.kcal
            Text(buildAnnotatedString {
                append("Si la añades, terminas el día en ")
                withStyle(SpanStyle(color = if (final > planKcal * 1.05) c.aviso else c.verde)) { append("${miles(final.roundToInt())} kcal") }
                append(" de ${miles(planKcal)}")
            }, style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(bottom = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val f = RoundedCornerShape(13.dp)
                Text(
                    "🛒 A la lista", style = t.titulo, color = c.texto2,
                    modifier = Modifier.padding(end = 8.dp).clip(f).border(1.dp, c.borde, f).clickable(onClick = onLista).padding(horizontal = 14.dp, vertical = 13.dp),
                )
                BotonPrincipal("Añadir a ${articulo(comida)}${comida.nombre.lowercase()}", onAnadir, Modifier.weight(1f))
            }
        }
    }
}
