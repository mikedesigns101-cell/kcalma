package com.mikel.kcalma.ui.componentes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.BuildConfig
import com.mikel.kcalma.datos.Actualizador
import com.mikel.kcalma.datos.GitHub
import com.mikel.kcalma.datos.Registro
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.launch

/** Lo que se puede mandar desde las burbujas: un fallo (con registro) o una propuesta (solo el texto). */
enum class TipoEnvio(val etiqueta: String, val prefijo: String) { FALLO("informe", "Informe"), PROPUESTA("propuesta", "Propuesta") }

/**
 * Manda un fallo o una propuesta como incidencia del repositorio.
 * Con el token de la compilación va directo; sin token, o si falla, se abre GitHub en el navegador con todo escrito.
 * Devuelve true si se mandó directo.
 */
suspend fun enviarInforme(contexto: Context, tipo: TipoEnvio, titulo: String, descripcion: String, pantalla: String): Boolean {
    Registro.anotar("${tipo.prefijo}: $titulo")
    val cuerpo = if (tipo == TipoEnvio.FALLO) "Pantalla: $pantalla\n" + Registro.informe(descripcion)
    else "$descripcion\n\nPantalla: $pantalla\nKcalma ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    val bloque = if (tipo == TipoEnvio.FALLO) "```\n${cuerpo.takeLast(60000)}\n```" else cuerpo
    // El título de una propuesta es su primera frase, para verlas de un vistazo en la lista
    val cabecera = if (tipo == TipoEnvio.FALLO) titulo else descripcion.lineSequence().first().take(70)
    val ok = GitHub.incidencia("${tipo.prefijo}: $cabecera", bloque, tipo.etiqueta)
    if (!ok) abrirEnGithub(contexto, "${tipo.prefijo}: $cabecera", if (tipo == TipoEnvio.FALLO) "```\n${cuerpo.takeLast(5500)}\n```" else cuerpo.take(5500), tipo.etiqueta)
    return ok
}

/** Plan B: abre una incidencia nueva en el navegador con todo ya escrito (GitHub corta las URL muy largas). */
private fun abrirEnGithub(contexto: Context, titulo: String, cuerpo: String, etiqueta: String) {
    val url = Uri.parse("https://github.com/${Actualizador.REPOSITORIO}/issues/new").buildUpon()
        .appendQueryParameter("title", titulo)
        .appendQueryParameter("body", cuerpo)
        .appendQueryParameter("labels", etiqueta)
        .build()
    runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Toast.makeText(contexto, "No hay navegador para abrir GitHub", Toast.LENGTH_SHORT).show() }
}

/** Enlace «Mandar informe» para las pantallas de error: abre la hoja con el fallo ya descrito. */
@Composable
fun EnlaceInforme(titulo: String, descripcion: String, modifier: Modifier = Modifier) {
    var abierta by remember { mutableStateOf(false) }
    Text(
        "🐞 Mandar informe", style = KcalmaTheme.tipo.cuerpo, color = KcalmaTheme.colores.acento, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().clickable { abierta = true }.padding(10.dp),
    )
    if (abierta) HojaInforme(TipoEnvio.FALLO, titulo, titulo, descripcion) { abierta = false }
}

/** Las dos burbujas que van en una esquina de todas las pantallas: 💡 proponer una idea y 🐞 avisar de un fallo. */
@Composable
fun BurbujasInforme(pantalla: String, modifier: Modifier = Modifier) {
    var abierta by remember { mutableStateOf<TipoEnvio?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Burbuja("💡") { abierta = TipoEnvio.PROPUESTA }
        Burbuja("🐞") { abierta = TipoEnvio.FALLO }
    }
    abierta?.let { HojaInforme(it, pantalla, pantalla, "") { abierta = null } }
}

@Composable
private fun Burbuja(emoji: String, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).vidrio(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(emoji, style = KcalmaTheme.tipo.cuerpo)
    }
}

/** Hoja «Reportar un fallo» o «Proponer una idea»: el texto del usuario + lo que se adjunta solo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaInforme(tipo: TipoEnvio, titulo: String, pantalla: String, inicial: String, onCerrar: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val fallo = tipo == TipoEnvio.FALLO
    var texto by remember { mutableStateOf(inicial) }
    var enviando by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { if (!enviando) onCerrar() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.hoja,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding()) {
            Text(if (fallo) "Reportar un fallo" else "Proponer una idea", style = t.tituloPaso, color = c.texto)
            Text(
                if (fallo) "Cuéntalo con tus palabras. Lo leeremos y lo arreglaremos en la próxima versión."
                else "¿Qué te gustaría que hiciera Kcalma? Las leemos todas.",
                style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            BasicTextField(
                texto, { texto = it.take(2000) },
                textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).vidrio(RoundedCornerShape(16.dp)).padding(12.dp),
                decorationBox = { campo ->
                    Box {
                        if (texto.isEmpty()) Text(if (fallo) "Qué ha pasado y qué esperabas que pasara…" else "Tu idea…", style = t.cuerpo, color = c.texto3)
                        campo()
                    }
                },
            )
            Text("SE ENVÍA TAMBIÉN", style = t.etiqueta, color = c.texto3, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            Column(Modifier.fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Adjunto("Pantalla", pantalla)
                if (fallo) {
                    Adjunto("Versión y móvil", "${BuildConfig.VERSION_NAME} · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
                    Adjunto("Registro", "Lo que ha hecho la app en los últimos ${Registro.MINUTOS} minutos, sin fotos")
                } else Adjunto("Versión", BuildConfig.VERSION_NAME)
            }
            Text(
                (if (fallo) "" else "Sin registro ni datos del móvil: solo tu idea. ") + "Se publica en el GitHub de Kcalma, que es público.",
                style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 8.dp),
            )
            BotonPrincipal(
                if (enviando) "Enviando…" else if (fallo) "Enviar" else "Enviar idea", activo = !enviando && texto.isNotBlank(),
                modifier = Modifier.padding(top = 16.dp),
                onClick = {
                    enviando = true
                    alcance.launch {
                        val directo = enviarInforme(contexto, tipo, titulo, texto.trim(), pantalla)
                        if (directo) Toast.makeText(contexto, if (fallo) "Informe enviado. ¡Gracias!" else "Idea enviada. ¡Gracias!", Toast.LENGTH_SHORT).show()
                        enviando = false
                        onCerrar()
                    }
                },
            )
        }
    }
}

@Composable
private fun Adjunto(nombre: String, valor: String) {
    val c = KcalmaTheme.colores
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(nombre, style = KcalmaTheme.tipo.pequeno, color = c.texto2, modifier = Modifier.padding(end = 12.dp))
        Text(valor, style = KcalmaTheme.tipo.pequeno, color = c.texto, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
    }
}
