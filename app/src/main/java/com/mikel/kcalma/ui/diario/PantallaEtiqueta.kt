package com.mikel.kcalma.ui.diario

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.datos.Claves
import com.mikel.kcalma.datos.Gemini
import com.mikel.kcalma.datos.Respuesta
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.foto.PantallaCamara
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Producto que no está en ninguna base: foto de la tabla nutricional, Gemini la lee y el usuario la revisa.
 * Se guarda con su código de barras para la próxima vez.
 */
@Composable
fun FlujoEtiqueta(codigo: String?, onCerrar: () -> Unit, onSinClave: () -> Unit, onGuardado: (Alimento) -> Unit) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val claves = remember { Claves(contexto.applicationContext) }
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    var leyendo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var datos by remember { mutableStateOf<Map<String, String>?>(null) }

    BackHandler { if (foto != null && !leyendo) { foto = null; datos = null; error = null } else onCerrar() }

    if (foto == null) {
        PantallaCamara(
            comida = ComidaDia.COMIDA, nota = "", onNota = {}, onComida = {}, onCerrar = onCerrar,
            guia = "Encuadra la tabla de información nutricional",
            onFoto = { b, jpeg ->
                val clave = claves.gemini() ?: run { onSinClave(); return@PantallaCamara }
                foto = b; leyendo = true; error = null
                alcance.launch {
                    when (val r = Gemini.etiqueta(clave, jpeg)) {
                        is Respuesta.Ok -> datos = r.valor
                        Respuesta.ClaveMala -> error = "Google no acepta la clave de Gemini. Cámbiala en Perfil."
                        Respuesta.Limite -> error = "Has llegado al límite gratuito de Gemini por ahora. Prueba en un rato."
                        Respuesta.SinConexion -> error = "Sin conexión. Leer la etiqueta necesita internet."
                        is Respuesta.Fallo -> error = "Gemini no ha podido leer la etiqueta. Prueba con otra foto más de cerca."
                    }
                    leyendo = false
                }
            },
        )
        return
    }
    FormularioEtiqueta(foto!!, leyendo, error, datos, onAtras = { foto = null; datos = null; error = null }) { a ->
        onGuardado(a.copy(codigoBarras = codigo))
    }
}

private val CAMPOS = listOf(
    "nombre" to "Nombre", "marca" to "Marca", "kcal" to "Energía (kcal/100 g)", "proteinas" to "Proteínas (g)",
    "hidratos" to "Hidratos (g)", "azucares" to "  de ellos azúcares (g)", "grasas" to "Grasas (g)",
    "saturadas" to "  de ellas saturadas (g)", "fibra" to "Fibra (g)", "sal" to "Sal (g)", "racion" to "Ración (g)",
)

@Composable
private fun FormularioEtiqueta(
    foto: Bitmap, leyendo: Boolean, error: String?, datos: Map<String, String>?,
    onAtras: () -> Unit, onGuardar: (Alimento) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val valores = remember(datos) { mutableStateOf(datos ?: emptyMap()) }
    fun num(k: String) = valores.value[k]?.replace(',', '.')?.toDoubleOrNull()
    val nombre = valores.value["nombre"].orEmpty().trim()
    val kcal = num("kcal")
    val listo = !leyendo && datos != null && nombre.isNotEmpty() && kcal != null

    Column(Modifier.fillMaxSize().background(c.fondo0).statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
        BarraSuperior("‹", onAtras, "Producto nuevo")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Image(
                foto.asImageBitmap(), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp)),
            )
            when {
                leyendo -> Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = c.acento, strokeWidth = 2.dp)
                    Text("  Leyendo la etiqueta…", style = t.cuerpo, color = c.texto2)
                }
                error != null -> Text(error, style = t.cuerpo, color = c.aviso, modifier = Modifier.padding(vertical = 12.dp))
                else -> {
                    Text("Leído con Gemini. Revisa que cuadre con la etiqueta.", style = t.pequeno, color = c.texto2, modifier = Modifier.padding(vertical = 10.dp))
                    val forma = RoundedCornerShape(14.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(forma).background(c.fondo2).border(1.dp, c.borde, forma).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CAMPOS.forEach { (k, etiqueta) ->
                            val texto = k == "nombre" || k == "marca"
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(etiqueta, style = t.cuerpo, color = c.texto2, modifier = Modifier.weight(1f))
                                BasicTextField(
                                    valores.value[k].orEmpty(), { v -> valores.value = valores.value + (k to v.take(if (texto) 50 else 6)) },
                                    singleLine = true, textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                                    keyboardOptions = KeyboardOptions(keyboardType = if (texto) KeyboardType.Text else KeyboardType.Decimal),
                                    modifier = Modifier.width(if (texto) 170.dp else 80.dp).clip(RoundedCornerShape(8.dp)).background(c.fondo3)
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.padding(vertical = 12.dp)) {
            BotonPrincipal("Guardar y añadir", activo = listo, onClick = {
                if (!listo) return@BotonPrincipal
                onGuardar(
                    Alimento(
                        nombre = nombre, marca = valores.value["marca"]?.trim()?.ifEmpty { null },
                        racion = num("racion")?.let { "1 ración (${it.toInt()} g)" }, racionG = num("racion"),
                        kcal = kcal!!, proteinas = num("proteinas") ?: 0.0, hidratos = num("hidratos") ?: 0.0, grasas = num("grasas") ?: 0.0,
                        azucares = num("azucares"), saturadas = num("saturadas"), fibra = num("fibra"), sal = num("sal"),
                    ),
                )
            })
        }
    }
}

/** Número para el formulario: «6,8», sin ceros de sobra. */
internal fun numeroEtiqueta(x: Double): String =
    if (x == Math.floor(x)) x.toLong().toString() else "%.1f".format(Locale.US, x).replace('.', ',')
