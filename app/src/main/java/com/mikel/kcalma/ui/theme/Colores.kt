package com.mikel.kcalma.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Paleta completa de Kcalma. Es el ÚNICO sitio con colores: las pantallas usan KcalmaTheme.colores.
 * Para añadir un tema nuevo basta con crear otra instancia de Colores y elegirla en KcalmaTheme.
 */
@Immutable
data class Colores(
    val fondo0: Color,
    val fondo1: Color,
    val fondo2: Color,
    val fondo3: Color,
    val fondo4: Color,
    val borde: Color,
    val texto: Color,
    val texto2: Color,
    val texto3: Color,
    val acento: Color,
    val acento2: Color,
    val sobreAcento: Color,
    val verde: Color,
    val rojo: Color,
    val proteina: Color,
    val hidratos: Color,
    val grasas: Color,
) {
    val degradado: Brush get() = Brush.linearGradient(listOf(acento, acento2))
}

/** Estilo B: oscuro premium con la paleta de Raideo. */
val TemaRaideo = Colores(
    fondo0 = Color(0xFF070C10),
    fondo1 = Color(0xFF0C141A),
    fondo2 = Color(0xFF101A21),
    fondo3 = Color(0xFF16232B),
    fondo4 = Color(0xFF1D2D36),
    borde = Color(0xFF1A2A33),
    texto = Color(0xFFE9F2F6),
    texto2 = Color(0xFF9DB2BD),
    texto3 = Color(0xFF657C88),
    acento = Color(0xFF3AD6F0),
    acento2 = Color(0xFFE05CC0),
    sobreAcento = Color(0xFF06141A),
    verde = Color(0xFF35D987),
    rojo = Color(0xFFF05A63),
    proteina = Color(0xFF3AD6F0),
    hidratos = Color(0xFFE05CC0),
    grasas = Color(0xFFF2BF4B),
)
