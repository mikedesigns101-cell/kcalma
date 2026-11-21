package com.mikel.kcalma.ui.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalColores = staticCompositionLocalOf { TemaKcalma }
private val LocalTipografia = staticCompositionLocalOf { Tipografia() }
private val LocalMovimiento = staticCompositionLocalOf { true }

/** [animaciones]: lo que eligió el usuario en Apariencia. Si el móvil tiene «Quitar animaciones», manda el móvil. */
@Composable
fun KcalmaTheme(colores: Colores = TemaKcalma, animaciones: Boolean = true, contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val sistema = remember {
        runCatching { Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f) > 0f
    }
    val esquema = darkColorScheme(
        primary = colores.acento,
        secondary = colores.acento2,
        background = colores.fondo0,
        surface = colores.hoja,
        onPrimary = colores.sobreAcento,
        onBackground = colores.texto,
        onSurface = colores.texto,
        error = colores.rojo,
    )
    CompositionLocalProvider(
        LocalColores provides colores,
        LocalTipografia provides Tipografia(),
        LocalMovimiento provides (animaciones && sistema),
    ) {
        MaterialTheme(colorScheme = esquema, content = contenido)
    }
}

object KcalmaTheme {
    val colores: Colores
        @Composable @ReadOnlyComposable get() = LocalColores.current
    val tipo: Tipografia
        @Composable @ReadOnlyComposable get() = LocalTipografia.current
    /** false si hay que quitar las animaciones que se repiten (la pompa, las burbujas…). */
    val movimiento: Boolean
        @Composable @ReadOnlyComposable get() = LocalMovimiento.current
}
