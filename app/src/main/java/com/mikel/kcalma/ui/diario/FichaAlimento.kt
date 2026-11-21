package com.mikel.kcalma.ui.diario

import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.componentes.fondoKcalma

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.dominio.Coccion
import com.mikel.kcalma.dominio.Unidad
import com.mikel.kcalma.dominio.decimal
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.Segmentado
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlin.math.roundToInt

/**
 * Ficha de un alimento: elegir cantidad (ración, gramos o envase) y añadirlo.
 * [restante] es lo que queda del día sin contar este alimento.
 */
@Composable
fun FichaAlimento(
    alimento: Alimento,
    comida: ComidaDia,
    restante: Nutrientes,
    inicial: Entrada?,
    onAtras: () -> Unit,
    onGuardar: (gramos: Double, unidad: Unidad, cantidad: Double) -> Unit,
    /** Cantidad con la que empezar al añadir (la última vez o lo dictado) y su explicación. */
    sugerida: Pair<String, Entrada>? = null,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val factorCocido = remember(alimento) { Coccion.factor(alimento) }
    val unidades = buildList {
        if (alimento.racionValida != null) add(Unidad.RACION)
        add(Unidad.GRAMOS)
        if (alimento.envaseG != null) add(Unidad.ENVASE)
        if (factorCocido != null) add(Unidad.COCIDO)
    }
    fun base(u: Unidad) = when (u) {
        Unidad.GRAMOS -> 1.0; Unidad.RACION -> alimento.racionValida ?: 100.0; Unidad.ENVASE -> alimento.envaseG ?: 100.0
        Unidad.COCIDO -> 1 / (factorCocido ?: 1.0)
    }
    fun enGramos(u: Unidad) = u == Unidad.GRAMOS || u == Unidad.COCIDO

    val inicio = inicial ?: sugerida?.second?.takeIf { it.unidad in unidades }
    var unidad by rememberSaveable { mutableStateOf(inicio?.unidad ?: unidades.first()) }
    var texto by rememberSaveable {
        mutableStateOf(inicio?.let { decimal(if (it.unidad == Unidad.GRAMOS) it.gramos else it.cantidad) } ?: if (unidad == Unidad.GRAMOS) "100" else "1")
    }
    val cantidad = texto.replace(',', '.').toDoubleOrNull() ?: 0.0
    val gramos = cantidad * base(unidad)
    val n = alimento.nutrientes(gramos)

    fun cambiarUnidad(u: Unidad) {
        if (u == unidad) return
        val nueva = gramos / base(u)
        texto = if (enGramos(u)) decimal(nueva.roundToInt().toDouble()) else decimal(((nueva * 2).roundToInt() / 2.0).coerceAtLeast(.5))
        unidad = u
    }
    fun sumar(signo: Int) {
        val paso = if (enGramos(unidad)) 10.0 else 0.5
        texto = decimal((cantidad + signo * paso).coerceAtLeast(paso))
    }

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
        BarraSuperior("‹", onAtras)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                IconoAlimento(alimento.nombre, 62)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(alimento.nombre, style = t.marca, color = c.texto)
                    val sub = listOfNotNull(if (alimento.generico) "Genérico" else alimento.marca, alimento.supermercado).joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, style = t.cuerpo, color = c.texto3)
                }
            }

            // Cantidad con − y +
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                val forma = RoundedCornerShape(14.dp)
                Row(
                    Modifier.weight(1f).vidrio(forma).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    BasicTextField(
                        texto, { nuevo -> nuevo.replace('.', ',').filter { it.isDigit() || it == ',' }.take(6).let { if (it.count { ch -> ch == ',' } <= 1) texto = it } },
                        singleLine = true, textStyle = t.campo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(IntrinsicSize.Min),
                    )
                    val detalle = when (unidad) {
                        Unidad.GRAMOS -> " g"
                        Unidad.RACION -> " ración · ${decimal(gramos)} g"
                        Unidad.ENVASE -> " envase · ${decimal(gramos)} g"
                        Unidad.COCIDO -> " g cocidos · ${decimal(gramos.roundToInt().toDouble())} g en seco"
                    }
                    Text(detalle, style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.width(8.dp))
                BotonPaso("−") { sumar(-1) }
                Spacer(Modifier.width(8.dp))
                BotonPaso("+") { sumar(1) }
            }
            if (inicial == null && inicio != null) sugerida?.let { (texto0, e) ->
                Text(
                    "$texto0: ${e.etiquetaCantidad}", style = t.pequeno, color = c.acento,
                    modifier = Modifier.padding(bottom = 10.dp).clip(RoundedCornerShape(8.dp)).background(c.acento.copy(alpha = .12f))
                        .clickable { unidad = e.unidad; texto = decimal(if (e.unidad == Unidad.GRAMOS) e.gramos else e.cantidad) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            if (unidades.size > 1) {
                val nombres = unidades.map { when (it) { Unidad.RACION -> "Ración"; Unidad.GRAMOS -> "Gramos"; Unidad.ENVASE -> "Envase"; Unidad.COCIDO -> "Cocido" } }
                Segmentado(nombres, unidades.indexOf(unidad)) { cambiarUnidad(unidades[it]) }
            }
            if (unidad == Unidad.COCIDO) Text(
                "Pesa la comida ya hecha: Kcalma calcula el peso en seco (÷${decimal(factorCocido ?: 1.0)}), que es el de la etiqueta.",
                style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 8.dp),
            ) else if (Unidad.COCIDO in unidades) Text(
                "¿Ya cocinado? Elige Cocido y pon lo que pesa hecho.",
                style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 8.dp),
            )

            // Resumen de kcal y macros
            val forma = RoundedCornerShape(16.dp)
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp).vidrio(forma).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 14.dp)) {
                    Text("${n.kcalInt}", style = t.tituloGrande, color = c.texto)
                    Text("kcal", style = t.diminuto, color = c.texto3)
                }
                Macro("Proteína", n.proteinas, c.proteina, Modifier.weight(1f))
                Macro("Hidratos", n.hidratos, c.hidratos, Modifier.weight(1f))
                Macro("Grasas", n.grasas, c.grasas, Modifier.weight(1f))
            }

            val kcalQuedan = (restante.kcal - n.kcal).roundToInt()
            val protQuedan = (restante.proteinas - n.proteinas).roundToInt()
            val aviso = buildAnnotatedString {
                if (kcalQuedan >= 0) {
                    append("Después te quedarán ")
                    withStyle(SpanStyle(color = c.verde, fontWeight = FontWeight.SemiBold)) { append("${miles(kcalQuedan)} kcal") }
                    if (protQuedan > 0) {
                        append(" y ")
                        withStyle(SpanStyle(color = c.verde, fontWeight = FontWeight.SemiBold)) { append("$protQuedan g de proteína") }
                    }
                    append(" para hoy.")
                } else {
                    append("Con esto te pasarás ")
                    withStyle(SpanStyle(color = c.rojo, fontWeight = FontWeight.SemiBold)) { append("${miles(-kcalQuedan)} kcal") }
                    append(" de tu objetivo de hoy.")
                }
            }
            Text(
                aviso, style = t.pequeno, color = c.texto2,
                modifier = Modifier.fillMaxWidth().vidrio(RoundedCornerShape(12.dp)).padding(horizontal = 11.dp, vertical = 9.dp),
            )

            Column(Modifier.padding(top = 10.dp)) {
                fun g(v: Double?) = v?.let { "${decimal(it * gramos / 100)} g" }
                listOf(
                    "Azúcares" to g(alimento.azucares), "Saturadas" to g(alimento.saturadas),
                    "Fibra" to g(alimento.fibra), "Sal" to g(alimento.sal),
                    "Por 100 g" to "${alimento.kcal.roundToInt()} kcal",
                ).filter { it.second != null }.forEach { (k, v) -> FilaDetalle(k, v!!) }
            }
            Text(
                "Datos: Open Food Facts (ODbL) · BEDCA. Revisa la etiqueta si algo no cuadra.",
                style = t.diminuto, color = c.texto3, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            )
        }
        BotonPrincipal(
            if (inicial != null) "Guardar cambios" else "Añadir a ${comida.nombre}",
            onClick = { onGuardar(gramos, unidad, cantidad) },
            activo = gramos > 0,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun BotonPaso(simbolo: String, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(c.fondo4).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(simbolo, style = KcalmaTheme.tipo.marca, color = c.acento) }
}

@Composable
private fun Macro(nombre: String, gramos: Double, color: Color, modifier: Modifier) {
    val c = KcalmaTheme.colores
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(decimal(gramos), style = KcalmaTheme.tipo.numero, color = c.texto)
        Text(nombre, style = KcalmaTheme.tipo.diminuto, color = c.texto3)
        Box(Modifier.padding(top = 3.dp).width(36.dp).height(3.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun FilaDetalle(nombre: String, valor: String) {
    val c = KcalmaTheme.colores
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(nombre, style = KcalmaTheme.tipo.pequeno, color = c.texto3, modifier = Modifier.weight(1f))
        Text(valor, style = KcalmaTheme.tipo.pequeno, color = c.texto2)
    }
}

private fun miles(n: Int): String = "%,d".format(java.util.Locale.US, n).replace(',', '.')
