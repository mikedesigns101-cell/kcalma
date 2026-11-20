package com.mikel.kcalma.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColores = staticCompositionLocalOf { TemaRaideo }
private val LocalTipografia = staticCompositionLocalOf { Tipografia() }

@Composable
fun KcalmaTheme(colores: Colores = TemaRaideo, contenido: @Composable () -> Unit) {
    val esquema = darkColorScheme(
        primary = colores.acento,
        secondary = colores.acento2,
        background = colores.fondo0,
        surface = colores.fondo2,
        onPrimary = colores.sobreAcento,
        onBackground = colores.texto,
        onSurface = colores.texto,
        error = colores.rojo,
    )
    CompositionLocalProvider(LocalColores provides colores, LocalTipografia provides Tipografia()) {
        MaterialTheme(colorScheme = esquema, content = contenido)
    }
}

object KcalmaTheme {
    val colores: Colores
        @Composable @ReadOnlyComposable get() = LocalColores.current
    val tipo: Tipografia
        @Composable @ReadOnlyComposable get() = LocalTipografia.current
}
