package com.mikel.kcalma.ui.componentes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.Actualizador
import com.mikel.kcalma.datos.Registro
import com.mikel.kcalma.ui.theme.KcalmaTheme

/**
 * Abre una incidencia nueva en el repositorio de GitHub con el informe ya escrito.
 * El usuario lo ve antes de enviarlo y puede borrar lo que quiera.
 */
fun mandarInforme(contexto: Context, titulo: String, descripcion: String) {
    Registro.anotar("Informe: $titulo")
    // GitHub corta las URL muy largas: el informe se limita a lo último del registro
    val cuerpo = Registro.informe(descripcion).takeLast(5500)
    val url = Uri.parse("https://github.com/${Actualizador.REPOSITORIO}/issues/new").buildUpon()
        .appendQueryParameter("title", "Informe: $titulo")
        .appendQueryParameter("body", "```\n$cuerpo\n```")
        .appendQueryParameter("labels", "informe")
        .build()
    runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, url)) }
        .onFailure { Toast.makeText(contexto, "No hay navegador para abrir GitHub", Toast.LENGTH_SHORT).show() }
}

/** Enlace «Mandar informe» para las pantallas de error. */
@Composable
fun EnlaceInforme(titulo: String, descripcion: String, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    Text(
        "🐞 Mandar informe", style = KcalmaTheme.tipo.cuerpo, color = KcalmaTheme.colores.acento, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().clickable { mandarInforme(contexto, titulo, descripcion) }.padding(10.dp),
    )
}
