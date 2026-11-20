package com.mikel.kcalma.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mikel.kcalma.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, vararg pesos: Int) = FontFamily(
    pesos.map { p ->
        Font(res, weight = FontWeight(p), variationSettings = FontVariation.Settings(FontVariation.weight(p)))
    }
)

/** Outfit para títulos y números; Inter para el texto. */
val Outfit = variable(R.font.outfit, 400, 500, 600, 700)
val Inter = variable(R.font.inter, 400, 500, 600)

@Immutable
data class Tipografia(
    val numeroGrande: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 44.sp),
    val numeroPlan: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 54.sp, lineHeight = 58.sp),
    val tituloGrande: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    val tituloPaso: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 29.sp),
    val campo: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
    val etiqueta: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.6.sp),
    val marca: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    val titulo: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    val tituloTarjeta: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    val numero: TextStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    val cuerpo: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    val pequeno: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 11.5.sp),
    val diminuto: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 10.5.sp),
)
