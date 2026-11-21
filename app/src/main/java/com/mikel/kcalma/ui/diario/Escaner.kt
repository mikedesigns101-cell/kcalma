package com.mikel.kcalma.ui.diario

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mikel.kcalma.datos.Busqueda
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Escáner de código de barras. [buscar] mira el móvil y, si no está, Open Food Facts. */
@Composable
fun Escaner(
    buscar: suspend (String) -> Busqueda,
    onCerrar: () -> Unit,
    onVer: (Alimento) -> Unit,
    onBuscarAMano: () -> Unit,
    onEtiqueta: ((String) -> Unit)? = null,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var permiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permiso = it }
    var codigo by remember { mutableStateOf<String?>(null) }
    var resultado by remember { mutableStateOf<Busqueda?>(null) }
    var camara by remember { mutableStateOf<Camera?>(null) }
    var linterna by remember { mutableStateOf(false) }
    var escribir by remember { mutableStateOf(false) }

    fun consultar(nuevo: String) {
        if (codigo != null) return // ya hay uno en pantalla
        codigo = nuevo
        resultado = null
        alcance.launch { resultado = buscar(nuevo) }
    }

    Box(Modifier.fillMaxSize().background(c.fondo0)) {
        if (permiso) VistaCamara(onCodigo = { consultar(it) }, onCamara = { camara = it })
        else Column(
            Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📷", style = t.numeroGrande)
            Text("Kcalma necesita la cámara para leer códigos de barras. Las imágenes no salen del móvil.",
                style = t.cuerpo, color = c.texto2, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp))
            BotonPrincipal("Permitir cámara", onClick = { pedir.launch(Manifest.permission.CAMERA) })
        }

        // Marco con borde degradado
        if (permiso) Box(
            Modifier.align(Alignment.Center).size(width = 260.dp, height = 130.dp)
                .border(BorderStroke(3.dp, c.degradado), RoundedCornerShape(18.dp)),
        ) { Box(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 12.dp).height(2.dp).background(c.acento)) }

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotonIcono("✕", onCerrar)
                Spacer(Modifier.weight(1f))
                if (camara?.cameraInfo?.hasFlashUnit() == true) BotonIcono("🔦", {
                    linterna = !linterna
                    camara?.cameraControl?.enableTorch(linterna)
                }, tinta = if (linterna) c.acento else c.texto2)
            }
            // Fondo oscuro para que se lea sobre cualquier imagen de la cámara
            if (permiso) Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Text("Apunta al código de barras", style = t.titulo, color = c.texto,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(c.fondo0.copy(alpha = .7f)).padding(horizontal = 14.dp, vertical = 7.dp))
            }
            Spacer(Modifier.weight(1f))

            if (codigo != null) HojaResultado(
                codigo!!, resultado,
                onVer = onVer,
                onReintentar = { val cod = codigo!!; codigo = null; consultar(cod) },
                onOtro = { codigo = null; resultado = null },
                onBuscarAMano = onBuscarAMano,
                onEtiqueta = onEtiqueta,
            ) else Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "¿No lo lee? Escribir el código", style = t.cuerpo, color = c.acento,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(c.fondo0.copy(alpha = .7f))
                        .clickable { escribir = true }.padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
    }

    if (escribir) {
        var texto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { escribir = false },
            containerColor = c.fondo2,
            title = { Text("Código de barras", style = t.titulo, color = c.texto) },
            text = {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.fondo3).padding(12.dp)) {
                    if (texto.isEmpty()) Text("8480000123456", style = t.cuerpo, color = c.texto3)
                    BasicTextField(texto, { texto = it.filter(Char::isDigit).take(14) }, singleLine = true,
                        textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { escribir = false; if (texto.length >= 8) consultar(texto) }) { Text("Buscar", color = c.acento) }
            },
            dismissButton = { TextButton(onClick = { escribir = false }) { Text("Cancelar", color = c.texto2) } },
        )
    }
}

@Composable
private fun HojaResultado(
    codigo: String, resultado: Busqueda?, onVer: (Alimento) -> Unit,
    onReintentar: () -> Unit, onOtro: () -> Unit, onBuscarAMano: () -> Unit,
    onEtiqueta: ((String) -> Unit)? = null,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val forma = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().clip(forma).background(c.fondo1).border(1.dp, c.borde, forma).padding(14.dp)) {
        when (resultado) {
            null -> Text("Buscando $codigo…", style = t.cuerpo, color = c.texto2)
            is Busqueda.Encontrado -> {
                val a = resultado.alimento
                Text("✓ Encontrado · $codigo", style = t.pequeno, color = c.verde)
                Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconoAlimento(a.nombre)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(a.nombre, style = t.tituloTarjeta, color = c.texto, maxLines = 2, modifier = Modifier.weight(1f, fill = false))
                            a.supermercado?.let { EtiquetaSuper(it) }
                        }
                        Text(a.marca ?: "", style = t.pequeno, color = c.texto3)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${a.kcal.roundToInt()}", style = t.tituloTarjeta, color = c.texto)
                        Text("kcal/100 g", style = t.diminuto, color = c.texto3)
                    }
                }
                BotonPrincipal("Ver y añadir", onClick = { onVer(a) })
            }
            Busqueda.NoExiste -> {
                Text("No conocemos este producto ($codigo)", style = t.tituloTarjeta, color = c.texto)
                if (onEtiqueta != null) {
                    Text("¿Le haces una foto a la etiqueta? La IA lee la tabla nutricional y queda guardado para la próxima vez.", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    BotonPrincipal("📷 Foto a la etiqueta", onClick = { onEtiqueta(codigo) })
                    Text(
                        "Buscar a mano", style = t.cuerpo, color = c.acento, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onBuscarAMano).padding(6.dp),
                    )
                } else {
                    Text("Búscalo por su nombre o usa un genérico parecido.", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    BotonPrincipal("Buscar a mano", onClick = onBuscarAMano)
                }
            }
            Busqueda.SinConexion -> {
                Text("No está en el móvil y no hay conexión", style = t.tituloTarjeta, color = c.texto)
                Text("Con internet lo buscamos en Open Food Facts.", style = t.pequeno, color = c.texto3, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                BotonPrincipal("Reintentar", onClick = onReintentar)
            }
        }
        if (resultado != null) Text(
            "Escanear otro", style = t.cuerpo, color = c.texto2, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onOtro).padding(6.dp),
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun VistaCamara(onCodigo: (String) -> Unit, onCamara: (Camera) -> Unit) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val alLeer by rememberUpdatedState(onCodigo)
    val proveedorFuturo = remember { ProcessCameraProvider.getInstance(contexto) }
    val lector = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build()
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            if (proveedorFuturo.isDone) proveedorFuturo.get().unbindAll()
            lector.close()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val vista = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
            proveedorFuturo.addListener({
                val proveedor = proveedorFuturo.get()
                val vistaPrevia = Preview.Builder().build().also { it.surfaceProvider = vista.surfaceProvider }
                val analisis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analisis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imagen ->
                    val media = imagen.image
                    if (media == null) { imagen.close(); return@setAnalyzer }
                    lector.process(InputImage.fromMediaImage(media, imagen.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codigos -> codigos.firstNotNullOfOrNull { it.rawValue }?.let(alLeer) }
                        .addOnCompleteListener { imagen.close() }
                }
                proveedor.unbindAll()
                // Sin cámara trasera (algunos emuladores) se queda la opción de escribir el código
                runCatching { proveedor.bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, vistaPrevia, analisis) }
                    .onSuccess(onCamara)
            }, ContextCompat.getMainExecutor(ctx))
            vista
        },
    )
}
