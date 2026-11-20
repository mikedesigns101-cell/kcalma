package com.mikel.kcalma.ui.diario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.decimal
import com.mikel.kcalma.dominio.emojiDe
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlin.math.roundToInt

/** Botón cuadrado pequeño de las barras superiores (✕, ‹, ☆…). */
@Composable
fun BotonIcono(simbolo: String, onClick: () -> Unit, tinta: Color = KcalmaTheme.colores.texto2) {
    val c = KcalmaTheme.colores
    val forma = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(34.dp).clip(forma).background(c.fondo2).border(1.dp, c.borde, forma).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(simbolo, style = KcalmaTheme.tipo.titulo, color = tinta) }
}

/** Barra superior: botón izquierdo, título y un elemento opcional a la derecha. */
@Composable
fun BarraSuperior(simbolo: String, onIzquierda: () -> Unit, titulo: String = "", derecha: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        BotonIcono(simbolo, onIzquierda)
        Spacer(Modifier.width(10.dp))
        Text(titulo, style = KcalmaTheme.tipo.marca, color = KcalmaTheme.colores.texto, modifier = Modifier.weight(1f))
        derecha()
    }
}

/** Etiqueta gris pequeña con el supermercado. */
@Composable
fun EtiquetaSuper(texto: String) {
    val c = KcalmaTheme.colores
    Text(
        texto, style = KcalmaTheme.tipo.diminuto, color = c.texto2, maxLines = 1,
        modifier = Modifier.padding(start = 5.dp).clip(RoundedCornerShape(5.dp)).background(c.fondo4).padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
fun IconoAlimento(nombre: String, tamano: Int = 38) {
    Box(
        Modifier.size(tamano.dp).clip(RoundedCornerShape((tamano * 0.28f).dp)).background(KcalmaTheme.colores.fondo3),
        contentAlignment = Alignment.Center,
    ) { Text(emojiDe(nombre), style = if (tamano > 50) KcalmaTheme.tipo.tituloPaso else KcalmaTheme.tipo.titulo) }
}

/** Fila de resultado: icono, nombre con supermercado, ración, kcal y botón + para añadir sin abrir la ficha. */
@Composable
fun FilaAlimento(a: Alimento, onAbrir: () -> Unit, onAnadir: () -> Unit, habitual: Entrada? = null) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    // Con [habitual], el + añade la cantidad de siempre
    val gramos = habitual?.gramos ?: a.gramosRapidos
    val racion = when {
        habitual != null -> "sueles ${habitual.etiquetaCantidad}"
        a.racionValida != null -> "1 ración (${decimal(gramos)} g)"
        else -> "100 g"
    }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onAbrir).padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoAlimento(a.nombre)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.nombre, style = t.tituloTarjeta, color = c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    a.supermercado?.let { EtiquetaSuper(it) }
                }
                Text(
                    listOfNotNull(if (a.generico) "Genérico" else a.marca, racion).joinToString(" · "),
                    style = t.pequeno, color = c.texto3, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${(a.kcal * gramos / 100).roundToInt()}", style = t.tituloTarjeta, color = c.texto)
                Text("kcal", style = t.diminuto, color = c.texto3)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.acento.copy(alpha = .12f)).clickable(onClick = onAnadir),
                contentAlignment = Alignment.Center,
            ) { Text("+", style = t.numero, color = c.acento) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.borde))
    }
}

/** Título de sección en mayúsculas pequeñas ("RESULTADOS", "RECIENTES"). */
@Composable
fun MiniTitulo(texto: String, modifier: Modifier = Modifier) {
    Text(texto.uppercase(), style = KcalmaTheme.tipo.etiqueta, color = KcalmaTheme.colores.texto3, modifier = modifier.padding(top = 6.dp, bottom = 6.dp))
}
