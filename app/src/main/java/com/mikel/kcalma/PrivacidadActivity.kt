package com.mikel.kcalma

import com.mikel.kcalma.ui.componentes.fondoKcalma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.ui.theme.KcalmaTheme

/** Explica a Health Connect para qué se usan los pasos (obligatoria para pedir el permiso). */
class PrivacidadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KcalmaTheme {
                val c = KcalmaTheme.colores
                Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().padding(24.dp)) {
                    Text("Kcalma y tus pasos", style = KcalmaTheme.tipo.tituloPaso, color = c.texto)
                    Text(
                        "Kcalma lee solo los pasos de hoy para sumar a tu objetivo la mitad de lo que camines por encima de lo habitual. " +
                            "No escribe nada en Health Connect, no guarda tu historial de pasos y no lo envía a ningún sitio: todo se queda en tu móvil.",
                        style = KcalmaTheme.tipo.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
