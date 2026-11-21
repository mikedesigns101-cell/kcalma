package com.mikel.kcalma.ui.perfil

import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.componentes.fondoKcalma

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.avisos.Recordatorio
import com.mikel.kcalma.avisos.Recordatorios
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.theme.KcalmaTheme

@Composable
fun PantallaRecordatorios(onAtras: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    var lista by remember { mutableStateOf(Recordatorios.lista(contexto)) }
    var permiso by remember { mutableStateOf(Recordatorios.permisoNotificaciones(contexto)) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permiso = it }
    LaunchedEffect(Unit) {
        Recordatorios.marcarConfigurados(contexto)
        if (!permiso && Build.VERSION.SDK_INT >= 33) pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun cambiar(r: Recordatorio) {
        Recordatorios.guardar(contexto, r)
        lista = Recordatorios.lista(contexto)
        if (r.activo && !permiso && Build.VERSION.SDK_INT >= 33) pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        BarraSuperior("‹", onAtras, "Recordatorios")
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Solo te avisamos si a esa hora aún no has apuntado nada en esa comida. Toca la hora para cambiarla.", style = t.cuerpo, color = c.texto2)
            if (!permiso) Column(Modifier.padding(top = 12.dp)) {
                Text("Kcalma no tiene permiso para mandarte notificaciones.", style = t.cuerpo, color = c.aviso)
                if (Build.VERSION.SDK_INT >= 33) BotonPrincipal("Permitir notificaciones", { pedir.launch(Manifest.permission.POST_NOTIFICATIONS) }, Modifier.padding(top = 8.dp))
            }
            val forma = RoundedCornerShape(14.dp)
            listOf(lista.filter { it.clave != Recordatorios.PESO }, lista.filter { it.clave == Recordatorios.PESO }).forEach { grupo ->
                Column(Modifier.padding(top = 14.dp).fillMaxWidth().vidrio(forma)) {
                    grupo.forEachIndexed { i, r ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.borde))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.fondo4), contentAlignment = Alignment.Center) {
                                Text(r.emoji, style = t.cuerpo)
                            }
                            Text(r.nombre, style = t.cuerpo, color = c.texto, modifier = Modifier.weight(1f).padding(start = 10.dp))
                            Text(
                                r.horaTexto, style = t.cuerpo, color = if (r.activo) c.texto2 else c.texto3,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                    TimePickerDialog(contexto, { _, h, m -> cambiar(r.copy(hora = h, minuto = m, activo = true)) }, r.hora, r.minuto, true).show()
                                }.padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                            Switch(
                                r.activo, { cambiar(r.copy(activo = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = c.texto, checkedTrackColor = c.acento,
                                    uncheckedThumbColor = c.texto3, uncheckedTrackColor = c.fondo4, uncheckedBorderColor = c.borde,
                                ),
                            )
                        }
                    }
                }
            }
            Text(
                "El de pesarte salta los lunes y solo si ese día aún no te has pesado.",
                style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
            )
        }
    }
}
