package com.mikel.kcalma.ui.perfil

import com.mikel.kcalma.ui.componentes.BurbujaIcono
import com.mikel.kcalma.ui.componentes.Ic
import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.componentes.fondoKcalma

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.BuildConfig
import com.mikel.kcalma.dominio.Calculadora
import com.mikel.kcalma.dominio.Objetivo
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.dominio.Pesada
import com.mikel.kcalma.dominio.Ritmo
import com.mikel.kcalma.dominio.SUPERMERCADOS
import com.mikel.kcalma.dominio.Sexo
import com.mikel.kcalma.dominio.Trabajo
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.CampoNumero
import com.mikel.kcalma.ui.componentes.Etiqueta
import com.mikel.kcalma.ui.componentes.Nota
import com.mikel.kcalma.ui.hoy.BarraInferior
import com.mikel.kcalma.ui.hoy.Pestana
import com.mikel.kcalma.ui.onboarding.BloqueAlergias
import com.mikel.kcalma.ui.onboarding.BloqueDatos
import com.mikel.kcalma.ui.onboarding.BloqueEjercicio
import com.mikel.kcalma.ui.onboarding.BloqueGustos
import com.mikel.kcalma.ui.onboarding.BloqueObjetivo
import com.mikel.kcalma.ui.onboarding.BloquePasos
import com.mikel.kcalma.ui.onboarding.BloqueSupermercados
import com.mikel.kcalma.ui.onboarding.BloqueTrabajo
import com.mikel.kcalma.ui.onboarding.CamposDatos
import com.mikel.kcalma.ui.onboarding.miles
import com.mikel.kcalma.ui.onboarding.resaltado
import com.mikel.kcalma.ui.theme.KcalmaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Bloques del perfil que se editan en su propia pantalla. */
enum class Bloque(val titulo: String) {
    DATOS("Altura, edad y sexo"), ACTIVIDAD("Actividad y ejercicio"), OBJETIVO("Objetivo y ritmo"),
    ALERGIAS("Alergias"), GUSTOS("Gustos"), SUPERMERCADOS("Supermercados"),
}

/** Acciones de la pestaña Perfil que resuelve App. */
class AccionesPerfil(
    val onEditar: (Bloque) -> Unit,
    val onGuardarPeso: (Double) -> Unit,
    val onClave: () -> Unit,
    val onBorrarClave: () -> Unit,
    val onExportar: () -> Unit,
    val onImportar: () -> Unit,
    val onInforme: () -> Unit,
    val onActualizar: () -> Unit,
    val onRecordatorios: () -> Unit = {},
    val onActividad: () -> Unit = {},
    val onApariencia: () -> Unit = {},
    val onPodometro: () -> Unit = {},
    val onAmigos: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    perfil: Perfil,
    pesos: List<Pesada>,
    claveGuardada: Boolean,
    acciones: AccionesPerfil,
    /** Estado de Health Connect para la fila de Actividad del móvil. */
    actividad: String = "",
    /** Estado del podómetro del móvil («Encendido · 5.980 hoy», «Apagado»…). */
    podometro: String = "",
    /** Resumen del grupo de amigos («2.º este mes», «Sin grupo»). */
    amigos: String = "",
    /** Nombre del tema de color elegido. */
    tema: String = "",
    onPestana: (Pestana) -> Unit,
    onCamara: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val plan = remember(perfil) { Calculadora.calcular(perfil) }
    var hojaPeso by remember { mutableStateOf(false) }
    var dialogo by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().fondoKcalma()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp).padding(top = 14.dp, bottom = 130.dp),
        ) {
            // Cabecera
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(c.degradado), contentAlignment = Alignment.Center) {
                    Text("K", style = t.marca, color = c.sobreAcento)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Tu perfil", style = t.marca, color = c.texto)
                    Text(resumenObjetivo(perfil), style = t.pequeno, color = c.texto2)
                }
            }
            Spacer(Modifier.height(14.dp))
            // Plan
            val forma = RoundedCornerShape(16.dp)
            Column(
                Modifier.fillMaxWidth().vidrio(forma, resaltado = true)
                    .clickable { acciones.onEditar(Bloque.OBJETIVO) }.padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(miles(plan.kcal), style = t.numeroGrande.copy(fontSize = t.tituloGrande.fontSize), color = c.texto)
                    Text(" kcal/día", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(bottom = 5.dp).weight(1f))
                    Text("P ${plan.proteinaG} · H ${plan.hidratosG} · G ${plan.grasasG}", style = t.pequeno, color = c.texto2, modifier = Modifier.padding(bottom = 5.dp))
                }
                Text(
                    if (plan.manual) "Puesto a mano · toca para volver al calculado" else "Calculado con tus datos · se recalcula al cambiarlos",
                    style = t.diminuto, color = c.texto3, modifier = Modifier.padding(top = 4.dp),
                )
            }

            Grupo("App") {
                Fila(Ic.COPIA, "Actualizar Kcalma", "v${BuildConfig.VERSION_NAME}", onClick = acciones.onActualizar)
                Fila(Ic.PALETA, "Apariencia", tema, onClick = acciones.onApariencia)
            }
            Grupo("Mis datos") {
                val ultima = pesos.lastOrNull()
                Fila(Ic.PESO, "Peso", "${kgTexto(perfil.pesoKg)} kg" + (ultima?.let { " · ${cuando(it.fecha)}" } ?: "")) { hojaPeso = true }
                Fila(Ic.REGLA, Bloque.DATOS.titulo, "${perfil.alturaCm} cm · ${perfil.edad} · ${if (perfil.sexo == Sexo.HOMBRE) "H" else "M"}") { acciones.onEditar(Bloque.DATOS) }
                Fila(Ic.RAYO, Bloque.ACTIVIDAD.titulo, resumenActividad(perfil)) { acciones.onEditar(Bloque.ACTIVIDAD) }
                Fila(Ic.DIANA, Bloque.OBJETIVO.titulo, resumenObjetivoCorto(perfil)) { acciones.onEditar(Bloque.OBJETIVO) }
            }
            Grupo("Comida") {
                Fila(Ic.ALERTA, "Alergias", perfil.alergias.joinToString().ifEmpty { "Ninguna" }) { acciones.onEditar(Bloque.ALERGIAS) }
                Fila(Ic.CORAZON, "Gustos", "${perfil.meEncanta.size} me encantan · ${perfil.noMeGusta.size} no") { acciones.onEditar(Bloque.GUSTOS) }
                Fila(Ic.CARRITO, "Supermercados", perfil.supermercados.sortedBy { SUPERMERCADOS.indexOf(it) }.joinToString().ifEmpty { "Todos" }) {
                    acciones.onEditar(Bloque.SUPERMERCADOS)
                }
            }
            Grupo("Avisos y móvil") {
                Fila(Ic.AVISOS, "Recordatorios", "Comidas y peso", onClick = acciones.onRecordatorios)
                Fila(Ic.PASOS, "Pasos y ejercicio", actividad, onClick = acciones.onActividad)
                Fila(Ic.RAYO, "Podómetro del móvil", podometro, onClick = acciones.onPodometro)
                Fila(Ic.TROFEO, "Amigos", amigos, onClick = acciones.onAmigos)
            }
            Grupo("Foto con IA") {
                Fila(Ic.LLAVE, "Clave de Gemini", if (claveGuardada) "Guardada" else "Sin clave", valorColor = if (claveGuardada) c.verde else c.aviso) {
                    if (claveGuardada) dialogo = "clave" else acciones.onClave()
                }
            }
            Grupo("Datos y ayuda") {
                Fila(Ic.COPIA, "Copia de seguridad", "Exportar · Importar") { dialogo = "copia" }
                Fila(Ic.INFO, "Mandar informe", "", onClick = acciones.onInforme)
            }
        }
        BarraInferior(Modifier.align(Alignment.BottomCenter), Pestana.PERFIL, onPestana, onCamara)
    }

    if (hojaPeso) {
        ModalBottomSheet(
            onDismissRequest = { hojaPeso = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.hoja,
        ) { HojaPeso(perfil, pesos) { kg -> acciones.onGuardarPeso(kg); hojaPeso = false } }
    }

    when (dialogo) {
        "clave" -> Dialogo(
            "Clave de Gemini", "Está guardada cifrada en este móvil. ¿Qué quieres hacer?",
            "Cambiar clave" to { dialogo = null; acciones.onClave() },
            "Borrar clave" to { dialogo = null; acciones.onBorrarClave() },
        ) { dialogo = null }
        "copia" -> Dialogo(
            "Copia de seguridad",
            "Guarda en un archivo tu perfil, tu peso y tu diario (sin la clave de Gemini). Al importar se sustituye todo lo que hay en este móvil.",
            "Exportar" to { dialogo = null; acciones.onExportar() },
            "Importar" to { dialogo = "importar" },
        ) { dialogo = null }
        "importar" -> Dialogo(
            "¿Importar una copia?", "Se borrarán el perfil y el diario de este móvil y se pondrán los de la copia. No se puede deshacer.",
            "Elegir archivo" to { dialogo = null; acciones.onImportar() },
            null,
        ) { dialogo = null }
    }
}

@Composable
private fun Dialogo(
    titulo: String,
    texto: String,
    principal: Pair<String, () -> Unit>,
    secundario: Pair<String, () -> Unit>?,
    onCerrar: () -> Unit,
) {
    val c = KcalmaTheme.colores
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = c.hoja,
        title = { Text(titulo, style = KcalmaTheme.tipo.titulo, color = c.texto) },
        text = { Text(texto, style = KcalmaTheme.tipo.cuerpo, color = c.texto2) },
        confirmButton = { TextButton(onClick = principal.second) { Text(principal.first, color = c.acento) } },
        dismissButton = {
            Row {
                if (secundario != null) TextButton(onClick = secundario.second) { Text(secundario.first, color = c.texto2) }
                TextButton(onClick = onCerrar) { Text("Cancelar", color = c.texto3) }
            }
        },
    )
}

@Composable
private fun Grupo(titulo: String, filas: @Composable () -> Unit) {
    val c = KcalmaTheme.colores
    Text(titulo.uppercase(), style = KcalmaTheme.tipo.etiqueta, color = c.texto3, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 6.dp))
    val forma = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().vidrio(forma)) { filas() }
}

@Composable
private fun Fila(icono: Ic, texto: String, valor: String, valorColor: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BurbujaIcono(icono, tamano = 32.dp, icon = 16.dp)
        Text(texto, style = t.cuerpo, color = c.texto, modifier = Modifier.padding(start = 10.dp).weight(1f))
        Text(
            valor, style = t.pequeno, color = valorColor ?: c.texto2, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp).width(140.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        Text("  ›", style = t.cuerpo, color = c.texto3)
    }
}

/** Hoja rápida para apuntar el peso de hoy. */
@Composable
private fun HojaPeso(perfil: Perfil, pesos: List<Pesada>, onGuardar: (Double) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var kg by remember { mutableDoubleStateOf(pesos.lastOrNull()?.kg ?: perfil.pesoKg) }
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        Text("¿Cuánto pesas hoy?", style = t.tituloPaso, color = c.texto)
        Text("Mejor por la mañana, en ayunas. Se guarda con la fecha para ver tu progreso.", style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            BotonPaso("−") { kg = ((kg - 0.1) * 10).roundToInt() / 10.0 }
            Text(kgTexto(kg), style = t.numeroGrande, color = c.texto, modifier = Modifier.padding(horizontal = 18.dp))
            Text("kg", style = t.cuerpo, color = c.texto3)
            Spacer(Modifier.width(18.dp))
            BotonPaso("+") { kg = ((kg + 0.1) * 10).roundToInt() / 10.0 }
        }
        val anterior = pesos.lastOrNull { it.fecha != LocalDate.now().toString() }
        val falta = abs(perfil.pesoObjetivoKg - kg)
        val partes = listOfNotNull(
            anterior?.let { val d = kg - it.kg; "${if (d > 0) "+" else if (d < 0) "−" else "±"}${kgTexto(abs(d))} kg desde el ${fechaCorta(it.fecha)}" },
            if (falta >= 0.1) "te quedan ${kgTexto(falta)} kg" else "¡objetivo alcanzado!",
        )
        Text(partes.joinToString(" · "), style = t.pequeno, color = c.verde, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        // Últimas pesadas
        val ultimas = (pesos.takeLast(5).map { it.kg } + kg)
        if (ultimas.size > 1) {
            val min = ultimas.min() - 0.5
            val max = ultimas.max() + 0.5
            Row(Modifier.fillMaxWidth().height(54.dp).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                ultimas.forEachIndexed { i, v ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight(((v - min) / (max - min)).toFloat().coerceIn(.15f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .then(if (i == ultimas.lastIndex) Modifier.background(c.degradado) else Modifier.background(c.fondo4)),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        BotonPrincipal("Guardar peso", { onGuardar(kg) }, activo = kg in 35.0..300.0)
    }
}

@Composable
private fun BotonPaso(texto: String, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(c.fondo3).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(texto, style = KcalmaTheme.tipo.marca, color = c.acento) }
}

/** Edita un bloque del perfil reutilizando las preguntas del onboarding y enseña el plan nuevo antes de guardar. */
@Composable
fun EditarPerfil(bloque: Bloque, inicial: Perfil, onAtras: () -> Unit, onGuardar: (Perfil) -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    var perfil by remember { mutableStateOf(inicial) }
    val campos = remember { CamposDatos(inicial) }
    var manual by remember { mutableStateOf(inicial.kcalManual?.toString() ?: "") }
    var usarManual by remember { mutableStateOf(inicial.kcalManual != null) }
    BackHandler(onBack = onAtras)

    val valido = when (bloque) {
        Bloque.DATOS -> campos.validos
        Bloque.OBJETIVO -> !usarManual || manual.toIntOrNull()?.let { it in 1000..6000 } == true
        else -> true
    }
    fun resultado(): Perfil = when (bloque) {
        Bloque.DATOS -> if (campos.validos) campos.aplicar(perfil) else perfil
        Bloque.OBJETIVO -> perfil.copy(kcalManual = if (usarManual) manual.toIntOrNull()?.takeIf { it in 1000..6000 } else null)
        else -> perfil
    }
    val afectaPlan = bloque in setOf(Bloque.DATOS, Bloque.ACTIVIDAD, Bloque.OBJETIVO)

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).vidrio(RoundedCornerShape(10.dp)).clickable(onClick = onAtras),
                contentAlignment = Alignment.Center,
            ) { Text("‹", style = t.marca, color = c.texto2) }
            Text(bloque.titulo, style = t.marca, color = c.texto, modifier = Modifier.padding(start = 12.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val cambiar: (Perfil) -> Unit = { perfil = it }
            when (bloque) {
                Bloque.DATOS -> BloqueDatos(perfil, campos, cambiar)
                Bloque.ACTIVIDAD -> { BloqueTrabajo(perfil, cambiar); Spacer(Modifier.height(12.dp)); BloquePasos(perfil, cambiar); Spacer(Modifier.height(12.dp)); BloqueEjercicio(perfil, cambiar) }
                Bloque.OBJETIVO -> {
                    BloqueObjetivo(perfil.copy(kcalManual = null), cambiar)
                    Etiqueta("Calorías")
                    val calculado = Calculadora.calcular(perfil.copy(kcalManual = null)).kcal
                    com.mikel.kcalma.ui.componentes.Segmentado(listOf("Calculadas (${miles(calculado)})", "A mano"), if (usarManual) 1 else 0) {
                        usarManual = it == 1
                        if (usarManual && manual.isEmpty()) manual = calculado.toString()
                    }
                    if (usarManual) {
                        Spacer(Modifier.height(8.dp))
                        CampoNumero("Calorías al día", manual, "kcal", { manual = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), ejemplo = calculado.toString())
                        Nota(resaltado("Proteína y grasa se mantienen; los ", "hidratos", " se ajustan a las calorías que pongas."))
                    }
                }
                Bloque.ALERGIAS -> BloqueAlergias(perfil, cambiar)
                Bloque.GUSTOS -> BloqueGustos(perfil, cambiar)
                Bloque.SUPERMERCADOS -> BloqueSupermercados(perfil, cambiar)
            }
        }
        if (afectaPlan) {
            val antes = Calculadora.calcular(inicial).kcal
            val despues = Calculadora.calcular(resultado()).kcal
            val forma = RoundedCornerShape(13.dp)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).vidrio(forma)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tu plan nuevo", style = t.pequeno, color = c.texto2, modifier = Modifier.weight(1f))
                if (antes != despues) Text("${miles(antes)} → ", style = t.pequeno, color = c.texto3)
                Text("${miles(despues)} kcal", style = t.titulo, color = c.texto)
            }
        }
        Spacer(Modifier.height(12.dp))
        BotonPrincipal("Guardar", { onGuardar(resultado()) }, activo = valido)
        Spacer(Modifier.height(16.dp))
    }
}

// ---------- Textos ----------

private fun resumenObjetivo(p: Perfil): String {
    val que = when (p.objetivo) { Objetivo.PERDER -> "Perder peso"; Objetivo.RECOMPOSICION -> "Recomposición"; Objetivo.GANAR -> "Ganar músculo" }
    val ritmo = if (p.objetivo == Objetivo.RECOMPOSICION) "" else " · ritmo ${nombreRitmo(p.ritmo)}"
    return "$que · ${kgTexto(p.pesoKg)} → ${kgTexto(p.pesoObjetivoKg)} kg$ritmo"
}

private fun resumenObjetivoCorto(p: Perfil): String = when (p.objetivo) {
    Objetivo.PERDER -> "Perder · ${nombreRitmo(p.ritmo)}"
    Objetivo.RECOMPOSICION -> "Recomposición"
    Objetivo.GANAR -> "Ganar · ${nombreRitmo(p.ritmo)}"
}

private fun nombreRitmo(r: Ritmo) = when (r) { Ritmo.SUAVE -> "suave"; Ritmo.NORMAL -> "normal"; Ritmo.RAPIDO -> "rápido" }

private fun resumenActividad(p: Perfil): String {
    val trabajo = when (p.trabajo) { Trabajo.SENTADO -> "Sentado"; Trabajo.DE_PIE -> "De pie"; Trabajo.MOVIENDOSE -> "Moviéndote"; Trabajo.FISICO -> "Físico" }
    val ejercicio = if (p.diasEjercicio == 0) "sin ejercicio" else "${p.diasEjercicio} días " + p.tipoEjercicio.name.lowercase().replace("pesas", "pesas")
    return "$trabajo · $ejercicio"
}

internal fun kgTexto(x: Double): String = "%.1f".format(Locale.US, x).removeSuffix(".0").replace('.', ',')

private fun cuando(fecha: String): String {
    val f = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return ""
    return when (val dias = java.time.temporal.ChronoUnit.DAYS.between(f, LocalDate.now())) {
        0L -> "hoy"; 1L -> "ayer"; else -> "hace $dias días"
    }
}

internal fun fechaCorta(fecha: String): String = runCatching {
    LocalDate.parse(fecha).format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))).removeSuffix(".")
}.getOrDefault(fecha)
