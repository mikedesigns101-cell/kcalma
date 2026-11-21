package com.mikel.kcalma.ui.hoy

import com.mikel.kcalma.ui.componentes.vidrio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Propuesta
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.util.Locale
import kotlin.math.abs

/** Propuesta semanal de ajuste: nunca cambia nada sin que el usuario acepte. */
@Composable
fun TarjetaAjuste(p: Propuesta, onAceptar: () -> Unit, onAhoraNo: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val forma = RoundedCornerShape(16.dp)
    val menor = p.gastoReal < p.gastoEstimado
    val kg = "%.1f".format(Locale.US, abs(p.cambioKg)).replace('.', ',')
    Column(
        Modifier.fillMaxWidth().padding(bottom = 14.dp).vidrio(forma, resaltado = true).padding(14.dp),
    ) {
        Text(if (menor) "📊 Tu gasto real es algo menor" else "📊 Tu gasto real es algo mayor", style = t.tituloTarjeta, color = c.texto)
        Text(buildAnnotatedString {
            append("En ${p.semanas} ${if (p.semanas == 1) "semana" else "semanas"} (${p.dias} días apuntados) has comido una media de ")
            withStyle(SpanStyle(color = c.texto)) { append("${miles(p.mediaKcal)} kcal") }
            append(" y ")
            append(if (abs(p.cambioKg) < 0.05) "tu peso no ha cambiado" else if (p.cambioKg < 0) "has bajado " else "has subido ")
            if (abs(p.cambioKg) >= 0.05) withStyle(SpanStyle(color = c.texto)) { append("$kg kg") }
            append(". Tu gasto real ronda las ")
            withStyle(SpanStyle(color = c.texto)) { append("${miles(p.gastoReal)} kcal") }
            append(", no ${miles(p.gastoEstimado)}.")
        }, style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 4.dp))
        Text(buildAnnotatedString {
            append("Para seguir a tu ritmo: ")
            withStyle(SpanStyle(color = c.acento)) { append("${miles(p.kcalNueva)} kcal/día") }
            append(" (ahora ${miles(p.kcalActual)}).")
        }, style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonPrincipal("Cambiar a ${miles(p.kcalNueva)}", onAceptar, Modifier.weight(1f))
            val f = RoundedCornerShape(13.dp)
            Text(
                "Ahora no", style = t.titulo, color = c.texto2, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(f).border(1.dp, c.borde, f).clickable(onClick = onAhoraNo).padding(vertical = 13.dp),
            )
        }
    }
}

