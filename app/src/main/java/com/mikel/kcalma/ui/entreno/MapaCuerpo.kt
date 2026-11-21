package com.mikel.kcalma.ui.entreno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Musculo
import com.mikel.kcalma.ui.theme.KcalmaTheme

/** Mitad izquierda del cuerpo (caja 18..82 × 0..182); la derecha es su espejo sobre x = 50. Dibujo propio. */
private object Silueta {
    private fun p(d: String): Path = PathParser().parsePathString(d).toPath()
    private fun ovalo(l: Float, t: Float, r: Float, b: Float) = Path().apply { addOval(Rect(l, t, r, b)) }

    val resto: List<Path> = listOf(
        ovalo(42.5f, 3.5f, 57.5f, 22.5f), p("M46 22h8v6h-8z"), ovalo(19.3f, 86.5f, 25.7f, 95.5f),
        p("M37 172h8l1 6h-11z"), ovalo(37f, 129.8f, 46f, 136.2f),
    )
    val frente: Map<Musculo, Path> = mapOf(
        Musculo.TRAPECIO to p("M46 27c-3 2-6 3-9 4l9 1z"),
        Musculo.HOMBRO to p("M38 31c-7 0-12 4-13 12 2 4 5 4 7 2 1-6 3-10 6-12z"),
        Musculo.PECHO to p("M49 33H39c-5 3-6 9-5 14 4 4 11 4 15 2z"),
        Musculo.BICEPS to p("M25 46c-2 6-2 12 0 17 3 0 5-3 6-8 0-5-1-8-3-9z"),
        Musculo.ANTEBRAZO to p("M24 65c-2 7-3 15-3 21h4c2-6 4-14 4-21z"),
        Musculo.ABDOMEN to p("M49 52h-7c-1 8-1 18 1 26l6 2z"),
        Musculo.OBLICUOS to p("M40 52c-4 4-5 14-3 22l5 4c-2-10-2-18-1-26z"),
        Musculo.CUADRICEPS to p("M47 86c-7 0-12 6-13 16 0 12 2 22 5 28h6c2-10 3-24 3-38z"),
        Musculo.ADUCTORES to p("M49.4 87l-.6 6c0 11-1 21-2 27 2-4 3-14 3-24z"),
        Musculo.GEMELOS to p("M38 138c-2 10-1 22 1 32h4c1-10 2-22 1-32z"),
    )
    val espalda: Map<Musculo, Path> = mapOf(
        Musculo.TRAPECIO to p("M49 22c-4 4-9 7-12 10 4 4 8 8 12 11z"),
        Musculo.HOMBRO to p("M37 31c-7 0-11 4-12 12 2 4 5 4 7 2 1-6 3-10 6-12z"),
        Musculo.DORSAL to p("M48 46c-4-4-8-7-12-9-2 8-1 17 2 24 3 4 7 6 10 7z"),
        Musculo.TRICEPS to p("M25 46c-2 6-2 12 0 17 3 0 5-3 6-8 0-5-1-8-3-9z"),
        Musculo.ANTEBRAZO to p("M24 65c-2 7-3 15-3 21h4c2-6 4-14 4-21z"),
        Musculo.LUMBAR to p("M49 70h-5c-1 4-1 8 0 12l5 1z"),
        Musculo.GLUTEO to p("M49 85c-7-2-13 2-14 9 0 6 5 10 14 9z"),
        Musculo.ISQUIOS to p("M47 106c-7 0-11 4-12 12 0 6 2 10 4 13h6c2-7 3-17 3-23z"),
        Musculo.GEMELOS to p("M38 136c-3 6-3 14 0 20 2 2 5 1 6-3 1-7 0-13-1-17z"),
    )
}

/** Un lado del cuerpo con los músculos trabajados: principales con el degradado del tema, secundarios en suave. */
@Composable
fun MapaCuerpo(
    principales: Set<Musculo>,
    secundarios: Set<Musculo>,
    espalda: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = KcalmaTheme.colores
    val partes = if (espalda) Silueta.espalda else Silueta.frente
    Canvas(modifier.aspectRatio(64f / 182f)) {
        val e = size.width / 64f
        val degradado = Brush.linearGradient(listOf(c.acento, c.acento2), Offset(18f, 0f), Offset(82f, 182f))
        val fino = Stroke(.6f)
        fun DrawScope.mitad() {
            Silueta.resto.forEach { drawPath(it, c.texto.copy(alpha = .05f)); drawPath(it, c.texto.copy(alpha = .12f), style = fino) }
            partes.forEach { (m, path) ->
                when (m) {
                    in principales -> {
                        drawPath(path, c.acento.copy(alpha = .35f), style = Stroke(2.2f))
                        drawPath(path, degradado)
                    }
                    in secundarios -> {
                        drawPath(path, c.acento.copy(alpha = .38f))
                        drawPath(path, c.acento.copy(alpha = .5f), style = fino)
                    }
                    else -> {
                        drawPath(path, c.texto.copy(alpha = .08f))
                        drawPath(path, c.texto.copy(alpha = .16f), style = fino)
                    }
                }
            }
        }
        scale(e, e, Offset.Zero) {
            translate(-18f, 0f) {
                mitad()
                scale(-1f, 1f, Offset(50f, 0f)) { mitad() }
            }
        }
    }
}

/** Frente y espalda juntos, con su rótulo debajo si [rotulos]. */
@Composable
fun MapasCuerpo(principales: Set<Musculo>, secundarios: Set<Musculo>, ancho: Dp, modifier: Modifier = Modifier, rotulos: Boolean = true) {
    val c = KcalmaTheme.colores
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(ancho / 6), verticalAlignment = Alignment.Bottom) {
        listOf(false, true).forEach { esp ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MapaCuerpo(principales, secundarios, esp, Modifier.width(ancho))
                if (rotulos) Text(
                    if (esp) "Espalda" else "Frente", style = KcalmaTheme.tipo.diminuto, color = c.texto3,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Mapa compacto para listas: los dos lados juntos y sin rótulos. */
@Composable
fun MapaMini(principales: Set<Musculo>, secundarios: Set<Musculo>, ancho: Dp = 22.dp) =
    MapasCuerpo(principales, secundarios, ancho, rotulos = false)
