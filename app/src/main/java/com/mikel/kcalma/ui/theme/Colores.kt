package com.mikel.kcalma.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Paleta completa de Kcalma. Es el ÚNICO sitio con colores: las pantallas usan KcalmaTheme.colores.
 * Los temas de cristal tienen fondos translúcidos (fondo1…fondo4, borde) para que se vean las luces de detrás.
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
    val aviso: Color,
    /** Hojas y diálogos: opaco, porque detrás hay otra pantalla. */
    val hoja: Color = fondo1,
    /** Fondo de pantalla: azul noche arriba, casi negro abajo. */
    val cielo: Color = fondo0,
    /** Luces de color difuminadas detrás del cristal. */
    val luz1: Color = Color.Transparent,
    val luz2: Color = Color.Transparent,
    /** Cristal: relleno de arriba y de abajo, borde brillante y borde tenue, reflejo. */
    val vidrioAlto: Color = fondo2,
    val vidrioBajo: Color = fondo2,
    val bordeBrillo: Color = borde,
    val bordeTenue: Color = borde,
    val brillo: Color = Color.Transparent,
    val sombra: Color = Color.Black,
) {
    val degradado: Brush get() = Brush.linearGradient(listOf(acento, acento2))
}

/** Paleta opaca anterior. La usa el widget, que se pinta sobre el fondo de pantalla del móvil. */
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
    aviso = Color(0xFFF2BF4B),
)

private fun cristal(acento: Color, acento2: Color, cielo: Color, verde: Color = Color(0xFF3EE8A6)) = Colores(
    fondo0 = Color(0xFF04080F),
    fondo1 = Color(0x0FFFFFFF),
    fondo2 = Color(0x14FFFFFF),
    fondo3 = Color(0x1AFFFFFF),
    fondo4 = Color(0x1FFFFFFF),
    borde = Color(0x24FFFFFF),
    texto = Color(0xFFEEF4FB),
    texto2 = Color(0xFFB3C2D4),
    texto3 = Color(0xFF7B8CA3),
    acento = acento,
    acento2 = acento2,
    sobreAcento = Color(0xFFFFFFFF),
    verde = verde,
    rojo = Color(0xFFF05A63),
    proteina = acento,
    hidratos = acento2,
    grasas = Color(0xFFF2BF4B),
    aviso = Color(0xFFF2BF4B),
    hoja = Color(0xFF0D1828),
    cielo = cielo,
    luz1 = acento.copy(alpha = .30f),
    luz2 = acento2.copy(alpha = .24f),
    vidrioAlto = Color(0x29FFFFFF),
    vidrioBajo = Color(0x08FFFFFF),
    bordeBrillo = Color(0x99FFFFFF),
    bordeTenue = Color(0x0DFFFFFF),
    brillo = Color(0xF2FFFFFF),
    sombra = Color(0x99000000),
)

val TemaKcalma = cristal(Color(0xFF3AD6F0), Color(0xFFE05CC0), Color(0xFF0B1A2E))
val TemaHielo = cristal(Color(0xFF4FE3F5), Color(0xFF4D7CFF), Color(0xFF0A1830))
val TemaAurora = cristal(Color(0xFF3AD6F0), Color(0xFF3EE8A6), Color(0xFF08201F))
val TemaOrquidea = cristal(Color(0xFFE05CC0), Color(0xFF8F6BFF), Color(0xFF1A1030))

/** Temas que se eligen en Perfil → Apariencia. La clave se guarda en el móvil. */
enum class Tema(val nombre: String, val descripcion: String, val colores: Colores) {
    KCALMA("Kcalma", "Por defecto", TemaKcalma),
    HIELO("Hielo", "Fría y tranquila", TemaHielo),
    AURORA("Aurora", "Fresca", TemaAurora),
    ORQUIDEA("Orquídea", "Más cálida", TemaOrquidea);

    companion object {
        fun de(clave: String?): Tema = entries.firstOrNull { it.name == clave } ?: KCALMA
    }
}
