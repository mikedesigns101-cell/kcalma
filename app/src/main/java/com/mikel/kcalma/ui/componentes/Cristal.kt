package com.mikel.kcalma.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.delay

/** Fondo de las pantallas: cielo oscuro con luces de color difuminadas, que es lo que hace que se note el cristal. */
@Composable
fun Modifier.fondoKcalma(): Modifier {
    val c = KcalmaTheme.colores
    return drawBehind {
        val w = size.width
        val h = size.height
        drawRect(c.fondo0)
        drawRect(Brush.radialGradient(listOf(c.cielo, c.fondo0), Offset(w * .5f, h * .12f), w * 1.15f))
        fun luz(color: androidx.compose.ui.graphics.Color, x: Float, y: Float, r: Float) =
            drawRect(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), Offset(w * x, h * y), w * r))
        luz(c.luz1, .5f, .2f, .55f)
        luz(c.luz2, .05f, .6f, .55f)
        luz(c.luz1, .98f, .75f, .5f)
        luz(c.luz2, .5f, 1.05f, .7f)
    }
}

/** Panel de cristal esmerilado: relleno translúcido, borde con brillo arriba a la izquierda y reflejo superior. */
@Composable
fun Modifier.vidrio(forma: Shape = RoundedCornerShape(20.dp), resaltado: Boolean = false): Modifier {
    val c = KcalmaTheme.colores
    val borde = if (resaltado) Brush.linearGradient(listOf(c.acento, c.acento2.copy(alpha = .6f), c.acento))
    else Brush.linearGradient(listOf(c.bordeBrillo, c.bordeTenue, c.bordeTenue, c.bordeBrillo.copy(alpha = .4f)))
    return clip(forma)
        .background(Brush.linearGradient(listOf(c.vidrioAlto, c.vidrioBajo)))
        .background(Brush.verticalGradient(0f to c.vidrioBajo, .5f to c.vidrioBajo.copy(alpha = 0f)))
        .border(if (resaltado) 1.5.dp else 1.dp, borde, forma)
}

/** Esfera de cristal con el degradado del tema y un brillo arriba (botón + y botones principales redondos). */
@Composable
fun Modifier.esferaBrillo(forma: Shape = CircleShape): Modifier {
    val c = KcalmaTheme.colores
    return clip(forma).background(Brush.linearGradient(listOf(c.acento, c.acento2)))
        .drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(c.brillo.copy(alpha = .55f), c.brillo.copy(alpha = .1f), c.brillo.copy(alpha = 0f)),
                    Offset(size.minDimension * .45f, size.height * .2f), size.minDimension * .7f,
                )
            )
        }
        .border(1.dp, c.bordeBrillo, forma)
}

/** Burbuja de cristal redonda con un icono dentro (iconos de comidas, ajustes…). */
@Composable
fun BurbujaIcono(icono: Ic, tamano: Dp = 36.dp, icon: Dp = 18.dp, onClick: (() -> Unit)? = null) {
    val c = KcalmaTheme.colores
    Box(
        Modifier.size(tamano).clip(CircleShape)
            .background(Brush.radialGradient(listOf(c.vidrioAlto, c.vidrioBajo), Offset.Unspecified))
            .border(1.dp, Brush.linearGradient(listOf(c.bordeBrillo, c.bordeTenue)), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Icono(icono, tamano = icon) }
}

/** Botón redondo de cristal con icono gris (atrás, cerrar, peso, avisos…). */
@Composable
fun BotonCristal(icono: Ic, onClick: () -> Unit, modifier: Modifier = Modifier, tamano: Dp = 40.dp) {
    Box(
        modifier.size(tamano).vidrio(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icono(icono, tamano = tamano * .46f, color = KcalmaTheme.colores.texto2) }
}

/**
 * Entrada en cascada: el elemento [orden] aparece subiendo 14 dp con un fundido, 50 ms después del anterior.
 * Sin animaciones aparece directamente.
 */
@Composable
fun Modifier.entrada(orden: Int): Modifier {
    val movimiento = KcalmaTheme.movimiento
    val p = remember { Animatable(if (movimiento) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (p.value < 1f) {
            delay(60L * orden.coerceAtMost(8))
            p.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 14.dp.toPx()
    }
}
