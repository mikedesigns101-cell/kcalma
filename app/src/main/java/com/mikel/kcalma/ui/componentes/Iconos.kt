package com.mikel.kcalma.ui.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.ui.theme.KcalmaTheme

/** Un trazo del icono en coordenadas 24×24 (datos de path SVG). */
class Trazo(val d: String, val alfa: Float = 1f)

/** Iconos propios de Kcalma: trazo redondeado de 1,8 sobre una rejilla de 24. Generado desde las maquetas. */
enum class Ic(val trazos: List<Trazo>) {
    ALERTA(listOf(Trazo("M12 4l9 15.5H3z"), Trazo("M12 10v4M12 17h.01"))),
    ATRAS(listOf(Trazo("M14.5 5.5L8 12l6.5 6.5"))),
    AVISOS(listOf(Trazo("M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"), Trazo("M10 20.5a2 2 0 0 0 4 0"))),
    BUSCAR(listOf(Trazo("M4.5,11a6.5,6.5 0 1,0 13,0a6.5,6.5 0 1,0 -13,0"), Trazo("M16 16l4 4"))),
    CAMARA(listOf(Trazo("M4 8h3l1.5-2.5h7L17 8h3v11H4z"), Trazo("M8.5,13.5a3.5,3.5 0 1,0 7,0a3.5,3.5 0 1,0 -7,0"))),
    CARRITO(listOf(Trazo("M3.5 4.5h2.5l2 11h10.5l2-8H7"), Trazo("M8.2,19.5a1.3,1.3 0 1,0 2.6,0a1.3,1.3 0 1,0 -2.6,0"), Trazo("M15.7,19.5a1.3,1.3 0 1,0 2.6,0a1.3,1.3 0 1,0 -2.6,0"))),
    CENA(listOf(Trazo("M19.5 14.5A8 8 0 0 1 9.5 4.5a8 8 0 1 0 10 10z"), Trazo("M17 3.5v3M15.5 5h3"))),
    CERRAR(listOf(Trazo("M6 6l12 12M18 6L6 18"))),
    CHAT(listOf(Trazo("M4.5 5.5h15v10.5h-8.5l-4.5 3.5v-3.5h-2z"), Trazo("M8.5 10.75h.01M12 10.75h.01M15.5 10.75h.01"))),
    ENVIAR(listOf(Trazo("M4 11.5l16-7-6.5 16-2.5-6.5z"), Trazo("M11 14l9-9.5"))),
    CHECK(listOf(Trazo("M5 12.5l4.5 4.5L19 7.5"))),
    CHISPAS(listOf(Trazo("M12 3.5l1.6 4.4 4.4 1.6-4.4 1.6L12 15.5l-1.6-4.4L6 9.5l4.4-1.6z"), Trazo("M18.5 15l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z"))),
    COMIDA(listOf(Trazo("M7,12.5a5,5 0 1,0 10,0a5,5 0 1,0 -10,0"), Trazo("M3.5 4v4.5a1.5 1.5 0 0 0 3 0V4M5 9v11"), Trazo("M20.5 4c-1.7 0-2.5 2-2.5 5h2.5v11"))),
    COPIA(listOf(Trazo("M12 4v11M7.5 10.5L12 15l4.5-4.5"), Trazo("M4.5 15.5v3h15v-3"))),
    CORAZON(listOf(Trazo("M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.4 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z"))),
    DESAYUNO(listOf(Trazo("M4 9h12v4.5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5V9z"), Trazo("M16 10h1.5a2.5 2.5 0 0 1 0 5H16"), Trazo("M8 3.5c0 1.5 1.5 1.5 1.5 3M12 3.5c0 1.5 1.5 1.5 1.5 3"))),
    DIANA(listOf(Trazo("M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0"), Trazo("M7.5,12a4.5,4.5 0 1,0 9,0a4.5,4.5 0 1,0 -9,0"), Trazo("M11,12a1,1 0 1,0 2,0a1,1 0 1,0 -2,0"))),
    ENSALADA(listOf(Trazo("M3.5 11h17a8.5 8.5 0 0 1-17 0z"), Trazo("M8 11c0-2.5 1.5-4 3.5-4.5M13 11c.5-2.5 2-3.5 4-3.5"))),
    ESCANER(listOf(Trazo("M4 8V5h3M17 5h3v3M20 16v3h-3M7 19H4v-3"), Trazo("M8 9v6M11 9v6M13.5 9v6M16 9v6"))),
    ESTRELLA(listOf(Trazo("M12 3.5l2.6 5.3 5.8.8-4.2 4.1 1 5.8L12 16.8l-5.2 2.7 1-5.8-4.2-4.1 5.8-.8z"))),
    FRUTA(listOf(Trazo("M12 7.5c-1.5-1.2-5-1.5-6.3 1.5-1.3 3 .2 7.2 2.3 9.3 1.3 1.3 2.6 1.2 4 .5 1.4.7 2.7.8 4-.5 2.1-2.1 3.6-6.3 2.3-9.3-1.3-3-4.8-2.7-6.3-1.5z"), Trazo("M12 7.5c0-2 1-3.5 3-4"))),
    GALERIA(listOf(Trazo("M6.5,4.5h11a3,3 0 0 1 3,3v9a3,3 0 0 1 -3,3h-11a3,3 0 0 1 -3,-3v-9a3,3 0 0 1 3,-3z"), Trazo("M7.2,10a1.8,1.8 0 1,0 3.6,0a1.8,1.8 0 1,0 -3.6,0"), Trazo("M20.5 16l-5-5-8.5 8.5"))),
    HOY(listOf(Trazo("M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0", 0.35f), Trazo("M12 3.5a8.5 8.5 0 0 1 8.5 8.5"), Trazo("M9,12a3,3 0 1,0 6,0a3,3 0 1,0 -6,0"))),
    HUEVO(listOf(Trazo("M12 3.5c3.5 0 6 5.5 6 9.5a6 6 0 0 1-12 0c0-4 2.5-9.5 6-9.5z"))),
    IDEAS(listOf(Trazo("M9.5 18h5M10.5 21h3"), Trazo("M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2V16h5v-.1c0-.8.4-1.5 1-2A6 6 0 0 0 12 3z"))),
    INFO(listOf(Trazo("M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0"), Trazo("M12 11v5.5M12 7.8h.01"))),
    KETTLEBELL(listOf(Trazo("M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"), Trazo("M7 15a5 5 0 0 0 10 0c0-2.5-2-4.5-5-4.5S7 12.5 7 15z"))),
    TROFEO(listOf(Trazo("M8 4h8v5a4 4 0 0 1-8 0z"), Trazo("M8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7"))),
    SUBIR(listOf(Trazo("M12 19V5M6 11l6-6 6 6"))),
    CASA(listOf(Trazo("M4 11l8-6.5 8 6.5M6.5 9.5V19h11V9.5"))),
    ARRIBA(listOf(Trazo("M6 15l6-6 6 6"))),
    ABAJO(listOf(Trazo("M6 9l6 6 6-6"))),
    PLAY(listOf(Trazo("M8 5.5v13l10-6.5z"))),
    LAPIZ(listOf(Trazo("M4 20l1-4.5L15.5 5a2.1 2.1 0 0 1 3 3L8 18.5z"))),
    LLAVE(listOf(Trazo("M4,15a4,4 0 1,0 8,0a4,4 0 1,0 -8,0"), Trazo("M11 12l8.5-8.5M16.5 7l2 2M14.5 9l1.5 1.5"))),
    MAS(listOf(Trazo("M12 5v14M5 12h14"))),
    MENOS(listOf(Trazo("M5 12h14"))),
    MENU(listOf(Trazo("M4.5,12a1,1 0 1,0 2,0a1,1 0 1,0 -2,0"), Trazo("M11,12a1,1 0 1,0 2,0a1,1 0 1,0 -2,0"), Trazo("M17.5,12a1,1 0 1,0 2,0a1,1 0 1,0 -2,0"))),
    MERIENDA(listOf(Trazo("M4 15.5h16l-1.2 3.2a2 2 0 0 1-1.9 1.3H7.1a2 2 0 0 1-1.9-1.3z"), Trazo("M5 12.5c0-4 3.1-7 7-7s7 3 7 7z"), Trazo("M4 15.5h16"))),
    PALETA(listOf(Trazo("M12 3.5a8.5 8.5 0 0 0 0 17c1.2 0 1.8-.8 1.8-1.7 0-1.3-1.3-1.6-1.3-2.8 0-1 .8-1.7 1.8-1.7h2.4a3.8 3.8 0 0 0 3.8-3.8c0-3.9-3.8-7-8.5-7z"), Trazo("M6.8,11a1,1 0 1,0 2,0a1,1 0 1,0 -2,0"), Trazo("M9.5,7.5a1,1 0 1,0 2,0a1,1 0 1,0 -2,0"), Trazo("M13.8,7.8a1,1 0 1,0 2,0a1,1 0 1,0 -2,0"))),
    PAPELERA(listOf(Trazo("M4.5 7h15M9.5 7V4.5h5V7M6.5 7l1 13h9l1-13"))),
    PASOS(listOf(Trazo("M8 3.5c1.7 0 2.5 1.8 2.5 4s-.6 4.5-2.5 4.5-2.5-2.3-2.5-4.5.8-4 2.5-4zM6 14.5h4.5v2a2.3 2.3 0 0 1-4.5 0z"), Trazo("M16 7.5c1.7 0 2.5 1.8 2.5 4s-.6 4.5-2.5 4.5-2.5-2.3-2.5-4.5.8-4 2.5-4zM14 18.5h4.5v.5a2.3 2.3 0 0 1-4.5 0z"))),
    PERFIL(listOf(Trazo("M8.2,8.5a3.8,3.8 0 1,0 7.6,0a3.8,3.8 0 1,0 -7.6,0"), Trazo("M4.5 20c1.4-3.6 4.2-5.2 7.5-5.2s6.1 1.6 7.5 5.2"))),
    PESA(listOf(Trazo("M6.5 8v8M17.5 8v8M4 10v4M20 10v4M6.5 12h11"))),
    PESCADO(listOf(Trazo("M3 12c3-4.5 8.5-6 13-2l4.5-3v10L16 14c-4.5 4-10 2.5-13-2z"), Trazo("M7.2,11a0.8,0.8 0 1,0 1.6,0a0.8,0.8 0 1,0 -1.6,0"))),
    PESO(listOf(Trazo("M8.5,4h7a4.5,4.5 0 0 1 4.5,4.5v7a4.5,4.5 0 0 1 -4.5,4.5h-7a4.5,4.5 0 0 1 -4.5,-4.5v-7a4.5,4.5 0 0 1 4.5,-4.5z"), Trazo("M8.5 10.5a5 5 0 0 1 7 0"), Trazo("M12 10.5l1.3-1.6"))),
    PROGRESO(listOf(Trazo("M4 18l5-6 4 3 7-9"), Trazo("M15 6h5v5"))),
    RACHA(listOf(Trazo("M12 21c3.6 0 6-2.4 6-5.8 0-3.6-2.6-5.4-3.6-8.7-.3 2-1.4 3-2.4 3.5C12 7 10.6 4.6 8.8 3c.3 3.4-2.8 5.8-2.8 10.2C6 18.6 8.4 21 12 21z"))),
    RAYO(listOf(Trazo("M13 3L5 13.5h6L10 21l8-10.5h-6z"))),
    RECETA(listOf(Trazo("M8,3.5h8a3,3 0 0 1 3,3v11a3,3 0 0 1 -3,3h-8a3,3 0 0 1 -3,-3v-11a3,3 0 0 1 3,-3z"), Trazo("M8.5 8.5h7M8.5 12h7M8.5 15.5h4"))),
    REGLA(listOf(Trazo("M5,8h14a2,2 0 0 1 2,2v4a2,2 0 0 1 -2,2h-14a2,2 0 0 1 -2,-2v-4a2,2 0 0 1 2,-2z"), Trazo("M7 8v3M11 8v4M15 8v3M19 8v2"))),
    RELOJ(listOf(Trazo("M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0"), Trazo("M12 7.5V12l3 2"))),
    SARTEN(listOf(Trazo("M3.5,12a6.5,6.5 0 1,0 13,0a6.5,6.5 0 1,0 -13,0"), Trazo("M16.3 10.5l5-1.5"), Trazo("M7.8,12a2.2,2.2 0 1,0 4.4,0a2.2,2.2 0 1,0 -4.4,0"))),
    VASO(listOf(Trazo("M6.5 4h11l-1.5 16h-8z"), Trazo("M7 9.5h10"))),
    VOZ(listOf(Trazo("M12,3h0a3,3 0 0 1 3,3v5a3,3 0 0 1 -3,3h0a3,3 0 0 1 -3,-3v-5a3,3 0 0 1 3,-3z"), Trazo("M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21"))),
}

/**
 * Dibuja un icono. Sin [color] usa el degradado del tema (cian → magenta en Kcalma).
 * Se pinta con Image y no con Icon, porque Icon tiñe y borraría el degradado.
 */
@Composable
fun Icono(icono: Ic, modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color? = null) {
    val c = KcalmaTheme.colores
    val vector = remember(icono, color, c.acento, c.acento2) {
        val pincel = if (color != null) SolidColor(color)
        else Brush.linearGradient(listOf(c.acento, c.acento2), start = Offset(2f, 2f), end = Offset(22f, 22f))
        ImageVector.Builder(icono.name, 24.dp, 24.dp, 24f, 24f).apply {
            icono.trazos.forEach { t ->
                addPath(
                    pathData = addPathNodes(t.d), fill = null, stroke = pincel, strokeAlpha = t.alfa,
                    strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }
    Image(rememberVectorPainter(vector), contentDescription = null, modifier = modifier.size(tamano))
}

/** Icono de cada comida del día. */
fun com.mikel.kcalma.dominio.ComidaDia.icono(): Ic = when (this) {
    com.mikel.kcalma.dominio.ComidaDia.DESAYUNO -> Ic.DESAYUNO
    com.mikel.kcalma.dominio.ComidaDia.MEDIA_MANANA -> Ic.FRUTA
    com.mikel.kcalma.dominio.ComidaDia.COMIDA -> Ic.COMIDA
    com.mikel.kcalma.dominio.ComidaDia.MERIENDA -> Ic.MERIENDA
    com.mikel.kcalma.dominio.ComidaDia.CENA -> Ic.CENA
}
