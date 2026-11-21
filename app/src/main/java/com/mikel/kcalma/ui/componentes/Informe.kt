package com.mikel.kcalma.ui.componentes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
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
import com.mikel.kcalma.datos.Registro
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manda el informe como incidencia del repositorio con la etiqueta «informe».
 * Con el token de la compilación va directo; sin token, o si falla, se abre GitHub en el navegador con todo escrito.
 * Devuelve true si se mandó directo.
 */
suspend fun enviarInforme(contexto: Context, titulo: String, descripcion: String, pantalla: String): Boolean {
    Registro.anotar("Informe: $titulo")
    val cuerpo = "Pantalla: $pantalla\n" + Registro.informe(descripcion)
    val ok = BuildConfig.GITHUB_INFORMES.isNotEmpty() && withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://api.github.com/repos/${Actualizador.REPOSITORIO}/issues").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 15000; c.readTimeout = 20000
            c.doOutput = true
            c.setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_INFORMES}")
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val json = JSONObject()
                .put("title", "Informe: $titulo")
                .put("body", "```\n${cuerpo.takeLast(60000)}\n```")
                .put("labels", JSONArray().put("informe"))
            c.outputStream.use { it.write(json.toString().toByteArray()) }
            val codigo = c.responseCode
            c.disconnect()
            if (codigo != 201) Registro.anotar("Informe no enviado: HTTP $codigo")
            codigo == 201
        }.getOrElse { Registro.error("Enviar informe", it); false }
    }
    if (!ok) abrirEnGithub(contexto, titulo, cuerpo)
    return ok
}

/** Plan B: abre una incidencia nueva en el navegador con el informe ya escrito. */
private fun abrirEnGithub(contexto: Context, titulo: String, cuerpo: String) {
    // GitHub corta las URL muy largas: el informe se limita a lo último del registro
    val url = Uri.parse("https://github.com/${Actualizador.REPOSITORIO}/issues/new").buildUpon()
        .appendQueryParameter("title", "Informe: $titulo")
        .appendQueryParameter("body", "```\n${cuerpo.takeLast(5500)}\n```")
        .appendQueryParameter("labels", "informe")
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
    if (abierta) HojaInforme(titulo, titulo, descripcion) { abierta = false }
}

/** Burbuja pequeña 🐞 que va en una esquina de todas las pantallas. */
@Composable
fun BurbujaInforme(pantalla: String, modifier: Modifier = Modifier) {
    var abierta by remember { mutableStateOf(false) }
    Box(
        modifier.size(34.dp).vidrio(CircleShape).clickable { abierta = true },
        contentAlignment = Alignment.Center,
    ) { Text("🐞", style = KcalmaTheme.tipo.cuerpo) }
    if (abierta) HojaInforme(pantalla, pantalla, "") { abierta = false }
}

/** Hoja «Reportar un fallo»: qué ha pasado + lo que se adjunta solo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaInforme(titulo: String, pantalla: String, inicial: String, onCerrar: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var texto by remember { mutableStateOf(inicial) }
    var enviando by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { if (!enviando) onCerrar() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.hoja,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding()) {
            Text("Reportar un fallo", style = t.tituloPaso, color = c.texto)
            Text("Cuéntalo con tus palabras. Lo leeremos y lo arreglaremos en la próxima versión.", style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            BasicTextField(
                texto, { texto = it },
                textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).vidrio(RoundedCornerShape(16.dp)).padding(12.dp),
                decorationBox = { campo -> Box { if (texto.isEmpty()) Text("Qué ha pasado y qué esperabas que pasara…", style = t.cuerpo, color = c.texto3); campo() } },
            )
            Text("SE ADJUNTA SOLO", style = t.etiqueta, color = c.texto3, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            Column(Modifier.fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Adjunto("Pantalla", pantalla)
                Adjunto("Versión y móvil", "${BuildConfig.VERSION_NAME} · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
                Adjunto("Registro", "Los últimos pasos y errores de la app, sin fotos")
            }
            Text("Se publica como incidencia en el repositorio de Kcalma en GitHub, que es público.", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 8.dp))
            BotonPrincipal(
                if (enviando) "Enviando…" else "Enviar", activo = !enviando && texto.isNotBlank(),
                modifier = Modifier.padding(top = 16.dp),
                onClick = {
                    enviando = true
                    alcance.launch {
                        val directo = enviarInforme(contexto, titulo, texto.trim(), pantalla)
                        if (directo) Toast.makeText(contexto, "Informe enviado. ¡Gracias!", Toast.LENGTH_SHORT).show()
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
