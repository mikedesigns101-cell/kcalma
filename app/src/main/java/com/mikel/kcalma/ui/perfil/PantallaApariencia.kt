package com.mikel.kcalma.ui.perfil

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.ui.componentes.BurbujaIcono
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.theme.KcalmaTheme
import com.mikel.kcalma.ui.theme.Tema

/** Perfil → Apariencia: el tema de color y si hay animaciones. */
@Composable
fun PantallaApariencia(
    tema: Tema,
    animaciones: Boolean,
    onTema: (Tema) -> Unit,
    onAnimaciones: (Boolean) -> Unit,
    onAtras: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        BarraSuperior("‹", onAtras, "Apariencia")
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("TEMA DE COLOR", style = t.etiqueta, color = c.texto3, modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 8.dp))
            Tema.entries.chunked(2).forEach { par ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    par.forEach { tm -> MuestraTema(tm, tm == tema, Modifier.weight(1f)) { onTema(tm) } }
                }
            }
            Text("MOVIMIENTO", style = t.etiqueta, color = c.texto3, modifier = Modifier.padding(start = 2.dp, top = 10.dp, bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().vidrio(RoundedCornerShape(14.dp)).clickable { onAnimaciones(!animaciones) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BurbujaIcono(Ic.CHISPAS, tamano = 32.dp, icon = 16.dp)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("Animaciones", style = t.cuerpo, color = c.texto)
                    Text("La pompa, las transiciones y la entrada de las tarjetas", style = t.pequeno, color = c.texto2)
                }
                Switch(
                    animaciones, onAnimaciones,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = c.texto, checkedTrackColor = c.acento,
                        uncheckedThumbColor = c.texto3, uncheckedTrackColor = c.fondo4, uncheckedBorderColor = c.borde,
                    ),
                )
            }
            Text(
                "Si en Android tienes quitadas las animaciones, Kcalma tampoco las pone.",
                style = t.pequeno, color = c.texto3, modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

/** Muestra de un tema: su cielo con las dos luces y una pompa con el degradado. */
@Composable
private fun MuestraTema(tema: Tema, elegido: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val m = tema.colores
    Column(modifier.vidrio(RoundedCornerShape(18.dp), resaltado = elegido).clickable(onClick = onClick).padding(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                drawRoundRect(m.fondo0, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
                drawRoundRect(
                    Brush.radialGradient(listOf(m.cielo, m.fondo0), Offset(w * .5f, h * .2f), w),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                )
                drawCircle(Brush.radialGradient(listOf(m.luz1, m.luz1.copy(alpha = 0f)), Offset(w * .2f, h * .8f), w * .5f), w * .5f, Offset(w * .2f, h * .8f))
                drawCircle(Brush.radialGradient(listOf(m.luz2, m.luz2.copy(alpha = 0f)), Offset(w * .85f, h * .3f), w * .5f), w * .5f, Offset(w * .85f, h * .3f))
                val r = h * .3f
                val centro = Offset(w / 2f, h / 2f)
                drawCircle(Brush.linearGradient(listOf(m.acento, m.acento2), Offset(centro.x - r, centro.y - r), Offset(centro.x + r, centro.y + r)), r, centro)
                drawCircle(m.bordeBrillo, r, centro, style = Stroke(1.dp.toPx()))
                drawCircle(m.brillo.copy(alpha = .6f), r * .22f, Offset(centro.x - r * .38f, centro.y - r * .42f))
            }
            if (elegido) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).vidrio(RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icono(Ic.CHECK, tamano = 13.dp)
            }
        }
        Text(tema.nombre, style = KcalmaTheme.tipo.tituloTarjeta, color = c.texto, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        Text(tema.descripcion, style = KcalmaTheme.tipo.pequeno, color = c.texto2, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
    }
}
