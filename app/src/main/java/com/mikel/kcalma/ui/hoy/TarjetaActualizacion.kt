package com.mikel.kcalma.ui.hoy

import com.mikel.kcalma.ui.componentes.vidrio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.VersionNueva
import com.mikel.kcalma.ui.theme.KcalmaTheme

/** Aviso de versión nueva arriba de «Hoy». [progreso] = null mientras no se está descargando. */
@Composable
fun TarjetaActualizacion(v: VersionNueva, progreso: Float?, onActualizar: () -> Unit, onOcultar: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val forma = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).vidrio(forma, resaltado = true).padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✨", style = t.titulo)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text("Nueva versión ${v.nombre}", style = t.tituloTarjeta, color = c.texto)
                if (v.notas.isNotBlank()) Text(v.notas, style = t.pequeno, color = c.texto3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (progreso == null) "Actualizar" else "${(progreso * 100).toInt()} %",
                style = t.cuerpo, color = c.sobreAcento,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.degradado)
                    .clickable(enabled = progreso == null, onClick = onActualizar).padding(horizontal = 12.dp, vertical = 7.dp),
            )
            if (progreso == null) Text("✕", style = t.cuerpo, color = c.texto3, modifier = Modifier.clickable(onClick = onOcultar).padding(8.dp))
        }
        if (progreso != null) Box(Modifier.padding(top = 8.dp, end = 4.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(c.fondo4)) {
            Box(Modifier.fillMaxWidth(progreso.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(c.degradado))
        }
    }
}
