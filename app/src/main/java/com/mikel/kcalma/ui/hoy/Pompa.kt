package com.mikel.kcalma.ui.hoy

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * El orbe de Hoy, como una pompa con líquido dentro: el nivel es lo comido, el anillo lo mismo y la cifra las kcal que quedan.
 * Flota, el líquido se mece con dos olas y suben burbujas. Al tocarla chapotea. Sin animaciones se queda quieta.
 */
@Composable
fun Pompa(restantes: Int, progreso: Float, modifier: Modifier = Modifier, tamano: Dp = 206.dp) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val mov = KcalmaTheme.movimiento
    val alcance = rememberCoroutineScope()

    // Al abrir: el anillo y el líquido suben desde cero y la cifra cuenta; después, cada cambio se anima
    // La subida desde cero solo la primera vez (al volver con el scroll no se repite)
    var vista by rememberSaveable { mutableStateOf(!mov) }
    val nivel = remember { Animatable(if (vista) progreso else 0f) }
    val cifra = remember { Animatable(if (vista) restantes.toFloat() else 0f) }
    LaunchedEffect(progreso) { if (mov) nivel.animateTo(progreso, tween(1400, easing = FastOutSlowInEasing)) else nivel.snapTo(progreso); vista = true }
    LaunchedEffect(restantes) { if (mov) cifra.animateTo(restantes.toFloat(), tween(1300, easing = FastOutSlowInEasing)) else cifra.snapTo(restantes.toFloat()) }
    // Golpe al tocar: un muelle que rebota hasta 0
    val golpe = remember { Animatable(0f) }

    val fase: State<Float>
    val flota: State<Float>
    val mece: State<Float>
    if (mov) {
        val inf = rememberInfiniteTransition(label = "pompa")
        fase = inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "olas")
        flota = inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flota")
        mece = inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "mece")
    } else {
        fase = remember { mutableStateOf(.15f) }; flota = remember { mutableStateOf(0f) }; mece = remember { mutableStateOf(0f) }
    }

    Box(
        modifier.size(tamano + 36.dp)
            .graphicsLayer {
                translationY = flota.value * 4.dp.toPx()
                scaleX = 1f + golpe.value * .07f
                scaleY = 1f - golpe.value * .07f
            }
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                if (mov) alcance.launch { golpe.snapTo(1f); golpe.animateTo(0f, spring(dampingRatio = .28f, stiffness = 260f)) }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centro = center
            val r = tamano.toPx() / 2f
            // Halo de luz alrededor
            drawCircle(Brush.radialGradient(listOf(c.luz1, c.luz2.copy(alpha = c.luz2.alpha * .6f), c.luz1.copy(alpha = 0f)), centro, size.minDimension / 2f), size.minDimension / 2f, centro)
            // Cuerpo de la esfera: oscuro y algo transparente
            drawCircle(Brush.radialGradient(listOf(c.cielo.copy(alpha = .75f), c.fondo0.copy(alpha = .55f)), centro, r), r, centro)

            // Líquido recortado a la esfera
            val dentro = r - 9.dp.toPx()
            val circulo = Path().apply { addOval(androidx.compose.ui.geometry.Rect(centro, dentro)) }
            clipPath(circulo) {
                rotate(mece.value * 3f, centro) {
                    val alto = centro.y + dentro - nivel.value * dentro * 2f
                    val amp = (4.dp.toPx() + abs(golpe.value) * 12.dp.toPx()) * (if (nivel.value in .02f..0.98f) 1f else .3f)
                    val ancho = dentro * 2f
                    val f = fase.value * 2f * PI.toFloat()
                    drawPath(
                        ola(centro.x - dentro, ancho, alto + 3.dp.toPx(), amp * .8f, f * -1f + 1.3f, ancho * .9f, size.height),
                        c.acento2.copy(alpha = .28f),
                    )
                    drawPath(
                        ola(centro.x - dentro, ancho, alto, amp, f, ancho * 1.1f, size.height),
                        Brush.verticalGradient(
                            listOf(c.acento.copy(alpha = .62f), c.acento2.copy(alpha = .48f), c.acento2.copy(alpha = .26f)),
                            startY = alto - amp, endY = centro.y + dentro,
                        ),
                    )
                    // Línea brillante de la superficie
                    drawPath(ola(centro.x - dentro, ancho, alto, amp, f, ancho * 1.1f, null), c.brillo.copy(alpha = .35f), style = Stroke(1.2.dp.toPx()))
                    // Burbujas que suben dentro del líquido
                    if (mov && nivel.value > .08f) for (i in 0 until 3) {
                        val p = (fase.value * 1.75f + i / 3f) % 1f
                        val x = centro.x + (i - 1) * dentro * .32f + sin((p * 6f + i) * 1.3f) * 4.dp.toPx()
                        val y = centro.y + dentro * .9f - p * (centro.y + dentro * .9f - alto)
                        drawCircle(c.brillo.copy(alpha = .55f * (1f - p)), 2.6.dp.toPx(), Offset(x, y), style = Stroke(1.dp.toPx()))
                    }
                }
            }

            // Borde de cristal y reflejos
            drawCircle(c.bordeBrillo.copy(alpha = .45f), r, centro, style = Stroke(1.dp.toPx()))
            drawCircle(Brush.radialGradient(listOf(c.luz1.copy(alpha = 0f), c.luz1.copy(alpha = 0f), c.luz1), centro, r), r, centro, style = Stroke(10.dp.toPx()))
            brillo(centro, r, c.brillo)

            // Anillo de progreso por fuera del líquido
            val g = 5.dp.toPx()
            val ar = r - 3.dp.toPx()
            val esquina = Offset(centro.x - ar, centro.y - ar)
            val caja = Size(ar * 2f, ar * 2f)
            drawArc(c.bordeTenue.copy(alpha = .12f), 0f, 360f, false, esquina, caja, style = Stroke(g))
            val pr = nivel.value.coerceIn(0f, 1f)
            if (pr > 0f) rotate(-90f, centro) {
                val degr = Brush.sweepGradient(0f to c.acento, pr to c.acento2, (pr + 1f) / 2f to c.acento2, 1f to c.acento, center = centro)
                drawArc(degr, 0f, 360f * pr, false, esquina, caja, alpha = .3f, style = Stroke(g * 2.4f, cap = StrokeCap.Round))
                drawArc(degr, 0f, 360f * pr, false, esquina, caja, style = Stroke(g, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(miles(cifra.value.toInt()), style = t.numeroGrande, color = c.texto)
            Text(if (restantes >= 0) "kcal restantes" else "kcal de más", style = t.pequeno, color = c.texto2)
        }
    }
}

/** Ola senoidal de [x0] a [x0]+[ancho] a la altura [y]; cerrada hacia abajo hasta [fondo] (o solo la línea si es null). */
private fun ola(x0: Float, ancho: Float, y: Float, amp: Float, fase: Float, largo: Float, fondo: Float?): Path = Path().apply {
    val paso = 6f
    var x = x0 - paso
    moveTo(x, y + amp * sin(fase + (x - x0) / largo * 2f * PI.toFloat()))
    while (x <= x0 + ancho + paso) {
        x += paso
        lineTo(x, y + amp * sin(fase + (x - x0) / largo * 2f * PI.toFloat()))
    }
    if (fondo != null) { lineTo(x, fondo); lineTo(x0 - paso, fondo); close() }
}

/** Reflejo especular arriba a la izquierda y brillo suave abajo, como una pompa de jabón. */
private fun DrawScope.brillo(centro: Offset, r: Float, col: androidx.compose.ui.graphics.Color) {
    val c0 = Offset(centro.x - r * .42f, centro.y - r * .52f)
    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = .7f), col.copy(alpha = .1f), col.copy(alpha = 0f)), c0, r * .13f), r * .13f, c0)
    rotate(-24f, Offset(centro.x - r * .2f, centro.y - r * .68f)) {
        drawOval(
            Brush.verticalGradient(listOf(col.copy(alpha = .12f), col.copy(alpha = 0f)), startY = centro.y - r * .86f, endY = centro.y - r * .5f),
            topLeft = Offset(centro.x - r * .62f, centro.y - r * .86f), size = Size(r * .9f, r * .4f),
        )
    }
}

internal fun miles(n: Int): String = "%,d".format(java.util.Locale.US, n).replace(',', '.')
