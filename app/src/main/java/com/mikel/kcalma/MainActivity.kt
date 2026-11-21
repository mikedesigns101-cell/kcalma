package com.mikel.kcalma

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mikel.kcalma.datos.AlmacenPerfil
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.datos.Repositorio
import com.mikel.kcalma.ui.App
import com.mikel.kcalma.ui.onboarding.Onboarding
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Estado de arranque: todavía leyendo, sin perfil (onboarding) o con perfil. */
private sealed interface Inicio {
    data object Cargando : Inicio
    data class Listo(val perfil: Perfil?) : Inicio
}

/** Pantalla que piden el widget o una notificación («foto», «anadir:CENA», «peso»). La app la consume. */
object Lanzador {
    val accion = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
}

class MainActivity : ComponentActivity() {
    companion object { const val ACCION = "accion" }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(ACCION)?.let { Lanzador.accion.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) intent?.getStringExtra(ACCION)?.let { Lanzador.accion.value = it }
        // App siempre oscura: iconos claros en las barras del sistema
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        com.mikel.kcalma.datos.Registro.iniciar(applicationContext)
        com.mikel.kcalma.avisos.Recordatorios.programarTodos(applicationContext)
        val almacen = AlmacenPerfil(applicationContext)
        val repo = Repositorio(applicationContext)
        setContent {
            KcalmaTheme {
                val flujo = remember { almacen.perfil.map<Perfil?, Inicio> { Inicio.Listo(it) } }
                val inicio by flujo.collectAsState(initial = Inicio.Cargando)
                val alcance = rememberCoroutineScope()
                when (val i = inicio) {
                    Inicio.Cargando -> Box(Modifier.fillMaxSize().background(KcalmaTheme.colores.fondo0))
                    is Inicio.Listo -> if (i.perfil == null) {
                        Onboarding(onTerminar = { p -> alcance.launch { almacen.guardar(p) } })
                    } else {
                        App(i.perfil, repo, almacen)
                    }
                }
            }
        }
    }
}
