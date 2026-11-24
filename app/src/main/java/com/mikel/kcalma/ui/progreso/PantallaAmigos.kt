package com.mikel.kcalma.ui.progreso

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.CifradoGrupo
import com.mikel.kcalma.dominio.Competicion
import com.mikel.kcalma.dominio.Competicion.Marca
import com.mikel.kcalma.dominio.Competicion.Puesto
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.fondoKcalma
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.time.YearMonth
import java.util.Locale

private val ES = Locale.forLanguageTag("es-ES")
private fun nombreMes(mes: String) = runCatching { YearMonth.parse(mes).month.getDisplayName(java.time.format.TextStyle.FULL, ES) }.getOrDefault(mes)

/**
 * Competición mensual con amigos. Sin grupo: crear uno o unirse con un código. Con grupo: la general por puntos,
 * las cuatro listas, quién ganó el mes pasado y el código para invitar.
 */
@Composable
fun PantallaAmigos(
    /** Código normalizado del grupo, o null si no hay. */
    codigo: String?,
    apodoInicial: String,
    /** Mis cifras del mes, calculadas en este móvil (siempre al día). */
    mia: Marca?,
    /** Lo bajado del grupo: mes → marcas de todos. */
    marcas: Map<String, List<Marca>>,
    cargando: Boolean,
    puedePublicar: Boolean,
    onCrear: (apodo: String) -> Unit,
    onUnirse: (codigo: String, apodo: String) -> Boolean,
    onActualizar: () -> Unit,
    onCompartir: (String) -> Unit,
    onSalir: () -> Unit,
    onAtras: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().imePadding()) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            BarraSuperior("‹", onAtras, if (codigo != null && mia != null) "Amigos · ${nombreMes(mia.mes)}" else "Amigos") {
                if (codigo != null) Text(
                    if (cargando) "Actualizando…" else "Actualizar", style = t.tituloTarjeta, color = if (cargando) c.texto3 else c.acento,
                    modifier = Modifier.vidrio(RoundedCornerShape(14.dp)).clickable(enabled = !cargando, onClick = onActualizar).padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        if (codigo == null) SinGrupo(apodoInicial, onCrear, onUnirse)
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp)) {
            val mes = mia?.mes ?: Competicion.mes()
            // Mis cifras son siempre las de este móvil; las de los demás, las bajadas
            val todas = (marcas[mes].orEmpty().filter { it.miembro != mia?.miembro } + listOfNotNull(mia))
            if (!puedePublicar) item {
                Text(
                    "Esta copia de Kcalma no puede publicar tus cifras: los demás no las verán hasta que actualices.",
                    style = t.pequeno, color = c.aviso, modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (todas.size <= 1) item {
                Text(
                    "Todavía estás solo en el grupo. Pasa el código de abajo a tus colegas; sus cifras tardan unos minutos en aparecer.",
                    style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(14.dp),
                )
            }
            item { Clasificacion("General", Competicion.general(todas), mia?.miembro, destacada = true) }
            Competicion.Lista.entries.forEach { tipo -> item(key = tipo.name) { Clasificacion(tipo.titulo, Competicion.lista(tipo, todas), mia?.miembro) } }
            val anterior = runCatching { YearMonth.parse(mes).minusMonths(1).toString() }.getOrNull()
            marcas[anterior].orEmpty().takeIf { it.isNotEmpty() }?.let { pasado ->
                item {
                    val ganadores = Competicion.general(pasado).filter { it.posicion == 1 }
                    Tarjeta(nombreMes(anterior!!)) {
                        Text(
                            (if (ganadores.size == 1) "Ganó " else "Empataron ") + ganadores.joinToString(" y ") { it.marca.apodo } + " con ${ganadores.first().valor}",
                            style = t.tituloTarjeta, color = c.texto, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            item {
                Tarjeta("Código del grupo") {
                    Text(CifradoGrupo.bonito(codigo), style = t.tituloPaso, color = c.texto, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Compartir código", style = t.tituloTarjeta, color = c.acento, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).vidrio(RoundedCornerShape(16.dp), resaltado = true).clickable { onCompartir(CifradoGrupo.bonito(codigo)) }.padding(vertical = 10.dp),
                        )
                        var seguro by remember { mutableStateOf(false) }
                        Text(
                            if (seguro) "¿Seguro? Toca otra vez" else "Salir del grupo", style = t.tituloTarjeta, color = if (seguro) c.rojo else c.texto2, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).vidrio(RoundedCornerShape(16.dp)).clickable { if (seguro) onSalir() else seguro = true }.padding(vertical = 10.dp),
                        )
                    }
                }
            }
            item {
                Text(
                    "Solo se comparten tu apodo y estas cuatro cifras del mes; nunca tu peso ni lo que comes. Viajan cifradas con el código del grupo. " +
                        "«Cerca del límite» cuenta a partir de ${Competicion.DIAS_MINIMOS} días apuntados. La general da 3, 2 y 1 puntos a los tres primeros de cada lista.",
                    style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SinGrupo(apodoInicial: String, onCrear: (String) -> Unit, onUnirse: (String, String) -> Boolean) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var apodo by rememberSaveable { mutableStateOf(apodoInicial) }
    var codigo by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp)) {
        item {
            Column(Modifier.fillMaxWidth().vidrio(RoundedCornerShape(18.dp)).padding(14.dp)) {
                Text("Compite con tus colegas cada mes", style = t.titulo, color = c.texto)
                Text(
                    "Quién se queda más cerca de su objetivo, quién entrena más, quién lleva mejor racha y quién anda más.",
                    style = t.pequeno, color = c.texto2, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item { Rotulo("Tu apodo") }
        item { Campo(apodo, "Cómo te verán los demás") { apodo = it.take(20); error = null } }
        item {
            BotonPrincipal("Crear un grupo", { onCrear(apodo.trim()) }, Modifier.padding(top = 12.dp), activo = apodo.isNotBlank())
        }
        item { Rotulo("¿Te han pasado un código?") }
        item { Campo(codigo, "KCAL-XXXX-XXXX-XXXX") { codigo = it.take(24).uppercase(); error = null } }
        item {
            Text(
                "Unirme al grupo", style = t.tituloTarjeta, color = if (apodo.isNotBlank() && codigo.isNotBlank()) c.acento else c.texto3, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp))
                    .clickable(enabled = apodo.isNotBlank() && codigo.isNotBlank()) { if (!onUnirse(codigo, apodo.trim())) error = "Ese código no es de un grupo de Kcalma. Revísalo." }
                    .padding(vertical = 12.dp),
            )
        }
        if (error != null) item { Text(error!!, style = t.pequeno, color = c.rojo, modifier = Modifier.padding(top = 8.dp)) }
        item {
            Text(
                "Solo se comparten tu apodo y cuatro cifras del mes. Nunca tu peso ni lo que comes. Las cifras viajan cifradas con el código del grupo: solo las lee quien lo tiene.",
                style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun Rotulo(texto: String) {
    Text(texto.uppercase(), style = KcalmaTheme.tipo.etiqueta, color = KcalmaTheme.colores.texto3, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 2.dp))
}

@Composable
private fun Campo(valor: String, pista: String, onCambio: (String) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    BasicTextField(
        valor, onCambio, singleLine = true, textStyle = t.titulo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
        modifier = Modifier.fillMaxWidth().vidrio(RoundedCornerShape(16.dp)).padding(14.dp),
        decorationBox = { campo -> Box { if (valor.isEmpty()) Text(pista, style = t.titulo, color = c.texto3); campo() } },
    )
}

@Composable
private fun Tarjeta(titulo: String, destacada: Boolean = false, contenido: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 8.dp).fillMaxWidth().vidrio(RoundedCornerShape(18.dp), resaltado = destacada).padding(horizontal = 14.dp, vertical = 11.dp)) {
        Text(titulo.uppercase(), style = KcalmaTheme.tipo.etiqueta, color = KcalmaTheme.colores.acento)
        contenido()
    }
}

@Composable
private fun Clasificacion(titulo: String, puestos: List<Puesto>, yo: String?, destacada: Boolean = false) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Tarjeta(titulo, destacada) {
        puestos.forEach { p ->
            val mio = p.marca.miembro == yo
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(if (p.posicion == 1) c.acento.copy(alpha = .85f) else c.texto.copy(alpha = .08f)),
                    contentAlignment = Alignment.Center,
                ) { Text("${p.posicion}", style = t.diminuto, color = if (p.posicion == 1) c.sobreAcento else c.texto2) }
                Text(
                    p.marca.apodo + if (mio) " (tú)" else "", style = t.tituloTarjeta, color = if (mio) c.acento else c.texto,
                    maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                )
                Text(p.valor, style = t.tituloTarjeta, color = c.texto)
            }
        }
    }
}
