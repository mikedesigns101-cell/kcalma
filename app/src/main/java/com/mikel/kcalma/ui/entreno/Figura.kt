package com.mikel.kcalma.ui.entreno

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.mikel.kcalma.dominio.Ejercicio
import com.mikel.kcalma.dominio.Material
import com.mikel.kcalma.dominio.Musculo
import com.mikel.kcalma.dominio.Patron
import com.mikel.kcalma.ui.theme.Colores
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/*
 * Figura propia que hace cada ejercicio en bucle, vista de lado y mirando a la derecha.
 * Mundo de 200 × 215 con el suelo en y = 205. Ángulos en grados: 0 = abajo, 90 = adelante, 180 = arriba.
 * Las piernas van por cinemática inversa hasta los tobillos (los pies no se despegan); los brazos son
 * relativos al tronco o van a un punto fijo (la barra de dominadas, el suelo en las flexiones).
 * Las poses salen del prototipo de las maquetas (poses.py).
 */

private const val MUSLO = 45f
private const val TIBIA = 45f
private const val TRONCO = 55f
private const val BRAZO = 30f
private const val ANTEBRAZO = 28f
private const val SUELO = 205f

class Pose(
    val cadera: Offset,
    val tronco: Float = 180f,
    val pies: Pair<Offset, Offset> = Offset(100f, 200f) to Offset(100f, 200f),
    /** Ángulos fijos de muslo y tibia (sustituyen a [pies]). */
    val piernas: Pair<Float, Float>? = null,
    val signoRodilla: Float = 1f,
    val pieA: Float = 90f,
    val pieB: Float = 90f,
    /** Brazo y antebrazo relativos al tronco (con el tronco vertical, son absolutos). */
    val brazos: Pair<Float, Float> = 0f to 0f,
    /** Punto fijo de la mano (sustituye a [brazos]). */
    val mano: Offset? = null,
    val signoCodo: Float = 1f,
)

class Escena(val tipo: String, val v: List<Float>)
class Movimiento(val a: Pose, val b: Pose, val escena: List<Escena>)

private val MOVIMIENTOS: Map<Patron, Movimiento> = mapOf(
        Patron.SWING to Movimiento(Pose(cadera = Offset(80.0f, 117.0f), tronco = 112.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 43.0f to 43.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 92.0f to 92.0f), listOf()),
        Patron.BISAGRA to Movimiento(Pose(cadera = Offset(80.0f, 117.0f), tronco = 115.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 65.0f to 65.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 0.0f to 0.0f), listOf()),
        Patron.CLEAN to Movimiento(Pose(cadera = Offset(80.0f, 117.0f), tronco = 112.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 43.0f to 43.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 12.0f to 165.0f), listOf()),
        Patron.SNATCH to Movimiento(Pose(cadera = Offset(80.0f, 117.0f), tronco = 112.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 43.0f to 43.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 176.0f to 180.0f), listOf()),
        Patron.SENTADILLA to Movimiento(Pose(cadera = Offset(78.0f, 160.0f), tronco = 140.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 10.0f to 190.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 10.0f to 190.0f), listOf()),
        Patron.GOBLET to Movimiento(Pose(cadera = Offset(80.0f, 162.0f), tronco = 155.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 8.0f to 168.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 8.0f to 168.0f), listOf()),
        Patron.THRUSTER to Movimiento(Pose(cadera = Offset(80.0f, 162.0f), tronco = 155.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 12.0f to 165.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 176.0f to 180.0f), listOf()),
        Patron.ZANCADA to Movimiento(Pose(cadera = Offset(95.0f, 152.0f), tronco = 180.0f, pies = Offset(128.0f, 200.0f) to Offset(62.0f, 200.0f), pieB = 40.0f, brazos = 0.0f to 0.0f), Pose(cadera = Offset(95.0f, 118.0f), tronco = 180.0f, pies = Offset(128.0f, 200.0f) to Offset(62.0f, 200.0f), pieB = 40.0f, brazos = 0.0f to 0.0f), listOf()),
        Patron.GEMELO to Movimiento(Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 0.0f to 0.0f), Pose(cadera = Offset(100.0f, 103.0f), tronco = 180.0f, pies = Offset(100.0f, 192.0f) to Offset(100.0f, 192.0f), pieA = 40.0f, pieB = 40.0f, brazos = 0.0f to 0.0f), listOf()),
        Patron.PRESS_VERTICAL to Movimiento(Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 22.0f to 172.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 176.0f to 180.0f), listOf()),
        Patron.PRESS_BANCA to Movimiento(Pose(cadera = Offset(80.0f, 143.0f), tronco = 90.0f, piernas = -80.0f to 5.0f, brazos = 390.0f to 250.0f), Pose(cadera = Offset(80.0f, 143.0f), tronco = 90.0f, piernas = -80.0f to 5.0f, brazos = 270.0f to 270.0f), listOf(Escena("banco", listOf(30.0f, 150.0f, 165.0f, 158.0f)))),
        Patron.FLEXION to Movimiento(Pose(cadera = Offset(102.0f, 166.0f), tronco = 111.0f, pies = Offset(18.0f, 194.0f) to Offset(18.0f, 194.0f), pieA = -5.0f, pieB = -5.0f, mano = Offset(150.0f, 203.0f), signoCodo = -1.0f), Pose(cadera = Offset(100.0f, 186.0f), tronco = 97.0f, pies = Offset(18.0f, 194.0f) to Offset(18.0f, 194.0f), pieA = -5.0f, pieB = -5.0f, mano = Offset(150.0f, 203.0f), signoCodo = -1.0f), listOf()),
        Patron.TRICEPS to Movimiento(Pose(cadera = Offset(100.0f, 111.0f), tronco = 172.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 8.0f to 125.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 172.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 8.0f to 4.0f), listOf(Escena("polea", listOf(132.0f, 6.0f)))),
        Patron.REMO to Movimiento(Pose(cadera = Offset(82.0f, 120.0f), tronco = 125.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 55.0f to 55.0f), Pose(cadera = Offset(82.0f, 120.0f), tronco = 125.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = -5.0f to 55.0f), listOf()),
        Patron.DOMINADA to Movimiento(Pose(cadera = Offset(100.0f, 143.0f), tronco = 180.0f, piernas = 40.0f to -60.0f, mano = Offset(104.0f, 34.0f), signoCodo = 1.0f), Pose(cadera = Offset(100.0f, 103.0f), tronco = 180.0f, piernas = 40.0f to -60.0f, mano = Offset(106.0f, 34.0f), signoCodo = 1.0f), listOf(Escena("barra", listOf(70.0f, 34.0f, 140.0f, 34.0f)))),
        Patron.JALON to Movimiento(Pose(cadera = Offset(90.0f, 142.0f), tronco = 170.0f, piernas = 90.0f to 0.0f, mano = Offset(106.0f, 32.0f), signoCodo = 1.0f), Pose(cadera = Offset(90.0f, 142.0f), tronco = 165.0f, piernas = 90.0f to 0.0f, mano = Offset(116.0f, 84.0f), signoCodo = 1.0f), listOf(Escena("banco", listOf(70.0f, 148.0f, 112.0f, 156.0f)), Escena("polea", listOf(110.0f, 6.0f)))),
        Patron.CURL to Movimiento(Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 2.0f to 2.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 2.0f to 150.0f), listOf()),
        Patron.PLANCHA to Movimiento(Pose(cadera = Offset(98.0f, 186.0f), tronco = 100.0f, pies = Offset(16.0f, 194.0f) to Offset(16.0f, 194.0f), pieA = -5.0f, pieB = -5.0f, brazos = 80.0f to 170.0f), Pose(cadera = Offset(98.0f, 184.0f), tronco = 100.5f, pies = Offset(16.0f, 194.0f) to Offset(16.0f, 194.0f), pieA = -5.0f, pieB = -5.0f, brazos = 80.0f to 170.0f), listOf()),
        Patron.CRUNCH to Movimiento(Pose(cadera = Offset(90.0f, 193.0f), tronco = 90.0f, pies = Offset(45.0f, 200.0f) to Offset(45.0f, 200.0f), signoRodilla = -1.0f, brazos = 20.0f to 165.0f), Pose(cadera = Offset(90.0f, 193.0f), tronco = 128.0f, pies = Offset(45.0f, 200.0f) to Offset(45.0f, 200.0f), signoRodilla = -1.0f, brazos = 20.0f to 165.0f), listOf()),
        Patron.PUENTE to Movimiento(Pose(cadera = Offset(100.0f, 195.0f), tronco = 90.0f, pies = Offset(72.0f, 200.0f) to Offset(72.0f, 200.0f), signoRodilla = -1.0f, brazos = 0.0f to 0.0f), Pose(cadera = Offset(100.0f, 170.0f), tronco = 63.0f, pies = Offset(72.0f, 200.0f) to Offset(72.0f, 200.0f), signoRodilla = -1.0f, brazos = 27.0f to 27.0f), listOf()),
        Patron.CURL_PIERNA to Movimiento(Pose(cadera = Offset(112.0f, 150.0f), tronco = 90.0f, piernas = -90.0f to -90.0f, brazos = 110.0f to 110.0f), Pose(cadera = Offset(112.0f, 150.0f), tronco = 90.0f, piernas = -90.0f to -190.0f, brazos = 110.0f to 110.0f), listOf(Escena("banco", listOf(30.0f, 157.0f, 175.0f, 165.0f)))),
        Patron.ELEVACION to Movimiento(Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 6.0f to 6.0f), Pose(cadera = Offset(100.0f, 111.0f), tronco = 180.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = 88.0f to 92.0f), listOf()),
        Patron.FONDOS to Movimiento(Pose(cadera = Offset(100.0f, 150.0f), tronco = 172.0f, piernas = 25.0f to -95.0f, mano = Offset(108.0f, 128.0f), signoCodo = -1.0f), Pose(cadera = Offset(100.0f, 124.0f), tronco = 176.0f, piernas = 25.0f to -95.0f, mano = Offset(106.0f, 128.0f), signoCodo = -1.0f), listOf(Escena("barra", listOf(92.0f, 130.0f, 124.0f, 130.0f)))),
        Patron.EXTENSION_PIERNA to Movimiento(Pose(cadera = Offset(90.0f, 142.0f), tronco = 170.0f, piernas = 90.0f to 0.0f, brazos = 10.0f to 10.0f), Pose(cadera = Offset(90.0f, 142.0f), tronco = 170.0f, piernas = 90.0f to 80.0f, brazos = 10.0f to 10.0f), listOf(Escena("banco", listOf(70.0f, 148.0f, 112.0f, 156.0f)))),
        Patron.ESCALADOR to Movimiento(Pose(cadera = Offset(102.0f, 166.0f), tronco = 111.0f, pies = Offset(18.0f, 194.0f) to Offset(18.0f, 194.0f), signoRodilla = -1.0f, pieA = -5.0f, pieB = -5.0f, mano = Offset(150.0f, 203.0f), signoCodo = -1.0f), Pose(cadera = Offset(102.0f, 166.0f), tronco = 111.0f, pies = Offset(88.0f, 192.0f) to Offset(18.0f, 194.0f), signoRodilla = -1.0f, pieA = 60.0f, pieB = -5.0f, mano = Offset(150.0f, 203.0f), signoCodo = -1.0f), listOf()),
        Patron.SALTO to Movimiento(Pose(cadera = Offset(84.0f, 155.0f), tronco = 145.0f, pies = Offset(100.0f, 200.0f) to Offset(100.0f, 200.0f), brazos = -30.0f to -20.0f), Pose(cadera = Offset(100.0f, 96.0f), tronco = 180.0f, pies = Offset(100.0f, 186.0f) to Offset(100.0f, 186.0f), pieA = 30.0f, pieB = 30.0f, brazos = 172.0f to 180.0f), listOf()),
)

private fun dir(a: Float): Offset { val r = Math.toRadians(a.toDouble()); return Offset(sin(r).toFloat(), cos(r).toFloat()) }
private fun mas(p: Offset, a: Float, l: Float) = p + dir(a) * l
private fun angulo(p: Offset, q: Offset) = Math.toDegrees(atan2((q.x - p.x).toDouble(), (q.y - p.y).toDouble())).toFloat()

/** Dos segmentos de [o] a [t]: devuelve los ángulos del primero y del segundo. */
private fun ik(o: Offset, t: Offset, l1: Float, l2: Float, s: Float): Pair<Float, Float> {
    val d = min(hypot(t.x - o.x, t.y - o.y), l1 + l2 - .01f)
    val base = angulo(o, t)
    val al = Math.toDegrees(acos(((l1 * l1 + d * d - l2 * l2) / (2 * l1 * d)).coerceIn(-1f, 1f).toDouble())).toFloat()
    val a1 = base + s * al
    return a1 to angulo(mas(o, a1, l1), t)
}

private fun l(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun l(a: Offset, b: Offset, f: Float) = Offset(l(a.x, b.x, f), l(a.y, b.y, f))

private class Puntos(
    val cadera: Offset, val hombro: Offset, val cabeza: Offset,
    val rodilla: List<Offset>, val tobillo: List<Offset>, val pie: List<Offset>,
    val codo: Offset, val mano: Offset, val antebrazo: Float,
)

private fun puntos(m: Movimiento, f: Float): Puntos {
    val a = m.a; val b = m.b
    val h = l(a.cadera, b.cadera, f)
    val t = l(a.tronco, b.tronco, f)
    val sh = mas(h, t, TRONCO)
    val rod = ArrayList<Offset>(); val tob = ArrayList<Offset>(); val pie = ArrayList<Offset>()
    for (i in 0..1) {
        val ap = a.piernas; val bp = b.piernas
        val (mu, ti) = if (ap != null && bp != null) l(ap.first, bp.first, f) to l(ap.second, bp.second, f)
        else ik(h, l(if (i == 0) a.pies.first else a.pies.second, if (i == 0) b.pies.first else b.pies.second, f), MUSLO, TIBIA, a.signoRodilla)
        val r = mas(h, mu, MUSLO); val tb = mas(r, ti, TIBIA)
        rod += r; tob += tb
        pie += mas(tb, if (i == 0) l(a.pieA, b.pieA, f) else l(a.pieB, b.pieB, f), 13f)
    }
    val am = a.mano; val bm = b.mano
    val (br, ab) = if (am != null && bm != null) ik(sh, l(am, bm, f), BRAZO, ANTEBRAZO, a.signoCodo)
    else (l(a.brazos.first, b.brazos.first, f) + t - 180f) to (l(a.brazos.second, b.brazos.second, f) + t - 180f)
    val codo = mas(sh, br, BRAZO)
    return Puntos(h, sh, mas(sh, t, 16f), rod, tob, pie, codo, mas(codo, ab, ANTEBRAZO), ab)
}

private val TRONCO_M = setOf(Musculo.PECHO, Musculo.ABDOMEN, Musculo.OBLICUOS, Musculo.DORSAL, Musculo.LUMBAR, Musculo.TRAPECIO)
private val MUSLO_M = setOf(Musculo.CUADRICEPS, Musculo.ISQUIOS, Musculo.GLUTEO, Musculo.ADUCTORES)
private val BRAZO_M = setOf(Musculo.HOMBRO, Musculo.BICEPS, Musculo.TRICEPS)

/**
 * El ejercicio en bucle. Con las animaciones quitadas se queda quieta en la posición de inicio.
 * [velocidad]: 1 = ritmo normal (unos 2 s por repetición), 0,5 = a cámara lenta.
 */
@Composable
fun FiguraEjercicio(ej: Ejercicio, modifier: Modifier = Modifier, velocidad: Float = 1f) {
    val c = KcalmaTheme.colores
    val m = MOVIMIENTOS[ej.patron] ?: return
    val f = if (KcalmaTheme.movimiento) {
        val tr = rememberInfiniteTransition(label = "figura")
        tr.animateFloat(
            0f, 1f, infiniteRepeatable(tween((950 / velocidad).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f",
        ).value
    } else 0f
    Canvas(modifier) {
        val e = min(size.width / 200f, size.height / 200f)
        translate((size.width - 200f * e) / 2f, (size.height - 200f * e) / 2f - 15f * e) {
            scale(e, e, Offset.Zero) { dibujar(ej, m, puntos(m, f), c) }
        }
    }
}

private fun DrawScope.dibujar(ej: Ejercicio, m: Movimiento, p: Puntos, c: Colores) {
    val degradado = Brush.linearGradient(listOf(c.acento, c.acento2), Offset(40f, 40f), Offset(160f, 200f))
    val apagado = Brush.linearGradient(listOf(c.vidrioAlto, c.vidrioBajo))
    fun relleno(grupo: Set<Musculo>): Brush = when {
        grupo.any { it in ej.principales } -> degradado
        grupo.any { it in ej.secundarios } -> Brush.linearGradient(listOf(c.acento.copy(alpha = .45f), c.acento2.copy(alpha = .35f)))
        else -> apagado
    }
    fun seg(a: Offset, b: Offset, ancho: Float, pincel: Brush, alfa: Float = 1f) {
        drawLine(c.bordeBrillo, a, b, ancho + 2.2f, StrokeCap.Round, alpha = alfa)
        drawLine(c.fondo0, a, b, ancho, StrokeCap.Round, alpha = alfa)
        drawLine(pincel, a, b, ancho, StrokeCap.Round, alpha = alfa)
    }
    // Suelo
    drawOval(c.texto.copy(alpha = .05f), Offset(30f, SUELO - 4f), Size(140f, 9f))
    drawLine(c.texto.copy(alpha = .14f), Offset(18f, SUELO), Offset(182f, SUELO), 1f)
    // Escena: banco, barra de dominadas, polea
    m.escena.forEach { s ->
        val v = s.v
        when (s.tipo) {
            "banco" -> drawLine(c.fondo4, Offset(v[0], v[1]), Offset(v[2], v[3]), 8f, StrokeCap.Round)
            "barra" -> drawLine(c.texto2, Offset(v[0], v[1]), Offset(v[2], v[3]), 4f, StrokeCap.Round)
            "polea" -> {
                drawLine(c.texto3, Offset(v[0], v[1]), p.mano, 1.4f)
                drawCircle(c.texto3, 4f, Offset(v[0], v[1]))
            }
        }
    }
    val brazo = relleno(BRAZO_M); val antebrazo = relleno(setOf(Musculo.ANTEBRAZO))
    val muslo = relleno(MUSLO_M); val tibia = relleno(setOf(Musculo.GEMELOS))
    // Lado lejano, más tenue
    seg(p.cadera, p.rodilla[1], 14f, muslo, .45f); seg(p.rodilla[1], p.tobillo[1], 11f, tibia, .45f)
    seg(p.tobillo[1], p.pie[1], 6.5f, apagado, .45f)
    seg(p.hombro, p.codo, 9f, brazo, .45f); seg(p.codo, p.mano, 7.5f, antebrazo, .45f)
    // Tronco y cabeza
    seg(p.cadera, p.hombro, 22f, relleno(TRONCO_M))
    drawCircle(c.bordeBrillo, 11.2f, p.cabeza)
    drawCircle(c.fondo0, 10f, p.cabeza)
    drawCircle(apagado, 10f, p.cabeza)
    // Lado cercano
    seg(p.cadera, p.rodilla[0], 15f, muslo); seg(p.rodilla[0], p.tobillo[0], 12f, tibia)
    seg(p.tobillo[0], p.pie[0], 7f, apagado)
    seg(p.hombro, p.codo, 10f, brazo); seg(p.codo, p.mano, 8f, antebrazo)
    material(ej, p, c)
}

private fun DrawScope.material(ej: Ejercicio, p: Puntos, c: Colores) {
    val borde = Stroke(1.4f)
    when (ej.material) {
        Material.BARRA -> {
            val centro = if (ej.patron == Patron.PUENTE) p.cadera + Offset(0f, -13f) else p.mano
            drawCircle(c.fondo2, 13f, centro); drawCircle(c.texto2, 13f, centro, style = borde); drawCircle(c.texto2, 2.5f, centro)
        }
        Material.MANCUERNAS -> {
            val d = dir(p.antebrazo + 90f) * 5f
            drawLine(c.texto2, p.mano - d, p.mano + d, 2f)
            drawCircle(c.fondo2, 5f, p.mano - d); drawCircle(c.texto2, 5f, p.mano - d, style = borde)
            drawCircle(c.fondo2, 5f, p.mano + d); drawCircle(c.texto2, 5f, p.mano + d, style = borde)
        }
        Material.KETTLEBELL -> {
            val centro = p.mano + dir(p.antebrazo) * 12f
            drawLine(c.texto2, p.mano, centro, 2.6f, StrokeCap.Round)
            drawCircle(c.fondo2, 10f, centro); drawCircle(c.texto2, 10f, centro, style = borde)
            drawCircle(c.texto.copy(alpha = .3f), 2.6f, centro + Offset(-3f, -4f))
        }
        Material.MAQUINA -> if (ej.patron == Patron.CURL_PIERNA) {
            drawCircle(c.fondo2, 6f, p.tobillo[0]); drawCircle(c.texto2, 6f, p.tobillo[0], style = borde)
        }
        else -> {}
    }
}
