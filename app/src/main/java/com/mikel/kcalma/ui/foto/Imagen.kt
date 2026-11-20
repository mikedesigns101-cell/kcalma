package com.mikel.kcalma.ui.foto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Lado mayor de la foto que se envía: basta para reconocer y ahorra datos y tiempo. */
private const val LADO_MAX = 1024

fun prepararFoto(original: Bitmap, rotacion: Int = 0): Pair<Bitmap, ByteArray> {
    val escala = (LADO_MAX.toFloat() / max(original.width, original.height)).coerceAtMost(1f)
    val m = Matrix().apply { postScale(escala, escala); postRotate(rotacion.toFloat()) }
    val lista = Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
    val jpeg = ByteArrayOutputStream().also { lista.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
    return lista to jpeg
}

fun leerDeGaleria(contexto: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contexto.contentResolver, uri)) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val lado = max(info.size.width, info.size.height)
            if (lado > LADO_MAX * 2) d.setTargetSampleSize(lado / (LADO_MAX * 2) + 1)
        }
    } else {
        contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
}.getOrNull()
