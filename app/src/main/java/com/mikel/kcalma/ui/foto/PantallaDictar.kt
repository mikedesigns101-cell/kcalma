package com.mikel.kcalma.ui.foto

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.Icono
import com.mikel.kcalma.ui.componentes.esferaBrillo
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.diario.SelectorComida
import com.mikel.kcalma.ui.theme.KcalmaTheme

/**
 * Dictar lo que has comido: el micrófono escribe la frase, que se puede corregir a mano antes de reconocer.
 * Al abrirse lanza el dictado directamente.
 */
@Composable
fun PantallaDictar(
    comida: ComidaDia,
    frase: String,
    ocupado: Boolean,
    error: String?,
    conIa: Boolean,
    onFrase: (String) -> Unit,
    onComida: (ComidaDia) -> Unit,
    onReconocer: () -> Unit,
    onCerrar: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val dictar = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { onFrase(it.take(400)) }
    }
    fun escuchar() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Di lo que has comido")
        runCatching { dictar.launch(i) }.onFailure { Toast.makeText(contexto, "Este móvil no tiene dictado por voz. Escríbelo.", Toast.LENGTH_SHORT).show() }
    }
    var lanzado by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!lanzado && frase.isEmpty()) { lanzado = true; escuchar() } }

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
        BarraSuperior("✕", onCerrar, "Dictar comida") { SelectorComida(comida, onComida) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.padding(top = 18.dp).size(92.dp).esferaBrillo(CircleShape).clickable(enabled = !ocupado) { escuchar() },
                contentAlignment = Alignment.Center,
            ) { Icono(Ic.VOZ, tamano = 38.dp, color = c.sobreAcento) }
            Text(
                if (frase.isEmpty()) "Toca y di lo que has comido" else "Toca para dictarlo otra vez",
                style = t.titulo, color = c.texto, modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Por ejemplo: «He comido 100 gramos de arroz cocido con 300 gramos de pollo a la plancha»",
                style = t.pequeno, color = c.texto3, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
            BasicTextField(
                frase, { onFrase(it.take(400)) },
                textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp).vidrio(RoundedCornerShape(16.dp)).padding(12.dp),
                decorationBox = { campo -> Box { if (frase.isEmpty()) Text("Lo que digas saldrá aquí; también puedes escribirlo o corregirlo.", style = t.cuerpo, color = c.texto3); campo() } },
            )
            Text(
                if (conIa) "Gemini separa los alimentos y los gramos; las calorías salen de la base de datos. Luego puedes cambiar lo que quieras."
                else "Sin clave de Gemini solo se entienden cantidades dichas («200 gramos de…», «dos huevos»). Luego puedes cambiar lo que quieras.",
                style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 8.dp),
            )
            if (error != null) Text(error, style = t.pequeno, color = c.rojo, modifier = Modifier.padding(top = 10.dp))
        }
        BotonPrincipal(
            if (ocupado) "Reconociendo…" else "Reconocer alimentos", onReconocer,
            activo = !ocupado && frase.isNotBlank(), modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}
