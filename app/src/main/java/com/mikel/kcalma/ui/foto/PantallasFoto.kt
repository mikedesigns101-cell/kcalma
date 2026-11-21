package com.mikel.kcalma.ui.foto

import com.mikel.kcalma.ui.componentes.vidrio
import com.mikel.kcalma.ui.componentes.fondoKcalma

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mikel.kcalma.datos.Registro
import com.mikel.kcalma.datos.Respuesta
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.dominio.decimal
import com.mikel.kcalma.ui.componentes.BotonPrincipal
import com.mikel.kcalma.ui.componentes.EnlaceInforme
import com.mikel.kcalma.ui.diario.BarraSuperior
import com.mikel.kcalma.ui.diario.BotonIcono
import com.mikel.kcalma.ui.diario.IconoAlimento
import com.mikel.kcalma.ui.diario.SelectorComida
import com.mikel.kcalma.ui.theme.KcalmaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

// ───────────────────────── 1 · Clave ─────────────────────────

@Suppress("DEPRECATION")
@Composable
fun PantallaClave(
    errorInicial: String?,
    onCerrar: () -> Unit,
    onAbrirWeb: () -> Unit,
    probar: suspend (String) -> Respuesta<Unit>,
    onGuardada: (String) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val portapapeles = LocalClipboardManager.current
    val alcance = rememberCoroutineScope()
    var clave by remember { mutableStateOf("") }
    var probando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(errorInicial) }

    Column(
        Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().imePadding()
            .padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
    ) {
        BarraSuperior("✕", onCerrar)
        Text("Activa la foto con IA", style = t.tituloGrande, color = c.texto)
        Text(
            "Kcalma usa Gemini de Google para reconocer lo que hay en el plato. Es gratis: solo necesitas tu propia clave.",
            style = t.cuerpo, color = c.texto2, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
        )
        PasoGuia(1, buildAnnotatedString {
            append("Abre ")
            withStyle(SpanStyle(color = c.acento)) { append("aistudio.google.com/apikey") }
            append(" e inicia sesión con tu cuenta de Google.")
        }, onAbrirWeb)
        PasoGuia(2, buildAnnotatedString {
            append("Pulsa ")
            withStyle(SpanStyle(color = c.texto)) { append("«Create API key»") }
            append(" y copia la clave.")
        })
        PasoGuia(3, buildAnnotatedString {
            append("Pégala aquí abajo. Se guarda ")
            withStyle(SpanStyle(color = c.texto)) { append("cifrada en tu móvil") }
            append("; no se envía a nadie más.")
        })

        val forma = RoundedCornerShape(14.dp)
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp).vidrio(forma, resaltado = error == null).then(if (error != null) Modifier.border(1.dp, c.rojo, forma) else Modifier).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (clave.isEmpty()) Text("AIza…", style = t.cuerpo, color = c.texto3)
                BasicTextField(
                    clave, { clave = it.trim(); error = null }, singleLine = true,
                    textStyle = t.cuerpo.copy(color = c.texto), cursorBrush = SolidColor(c.acento), modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                "Pegar", style = t.cuerpo, color = c.acento,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.acento.copy(alpha = .1f))
                    .clickable { portapapeles.getText()?.text?.trim()?.let { clave = it; error = null } }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        error?.let { Text(it, style = t.pequeno, color = c.rojo, modifier = Modifier.padding(top = 6.dp)) }

        Text(
            "ℹ️ En el plan gratuito, Google puede usar las fotos para mejorar sus modelos. No hagas fotos con caras ni datos personales.",
            style = t.pequeno, color = c.texto3,
            modifier = Modifier.padding(vertical = 16.dp).clip(RoundedCornerShape(12.dp)).background(c.fondo1).padding(12.dp),
        )
        BotonPrincipal(
            if (probando) "Comprobando…" else "Probar y guardar",
            activo = clave.length >= 20 && !probando,
            onClick = {
                probando = true
                alcance.launch {
                    error = when (val r = probar(clave)) {
                        is Respuesta.Ok -> null.also { onGuardada(clave) }
                        Respuesta.ClaveMala -> "Google no acepta esta clave. Cópiala otra vez entera."
                        Respuesta.SinConexion -> "Sin conexión. Hace falta internet para comprobarla."
                        Respuesta.Limite -> null.also { onGuardada(clave) } // la clave es buena, solo está al límite
                        is Respuesta.Fallo -> "No se ha podido comprobar (${r.detalle}). Inténtalo de nuevo."
                    }
                    probando = false
                }
            },
        )
        Text(
            buildAnnotatedString {
                append("¿Usas otra IA? ")
                withStyle(SpanStyle(color = c.acento)) { append("Elegir proveedor") }
            },
            style = t.pequeno, color = c.texto3, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                .clickable { Toast.makeText(contexto, "Por ahora solo Gemini. Otros proveedores llegarán más adelante.", Toast.LENGTH_SHORT).show() },
        )
    }
}

@Composable
private fun PasoGuia(n: Int, texto: androidx.compose.ui.text.AnnotatedString, onClick: (() -> Unit)? = null) {
    val c = KcalmaTheme.colores
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(c.acento.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Text("$n", style = KcalmaTheme.tipo.pequeno, color = c.acento)
        }
        Text(texto, style = KcalmaTheme.tipo.cuerpo, color = c.texto2, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
    }
}

// ───────────────────────── 2 · Cámara ─────────────────────────

@Composable
fun PantallaCamara(
    comida: ComidaDia,
    nota: String,
    onNota: (String) -> Unit,
    onComida: (ComidaDia) -> Unit,
    onCerrar: () -> Unit,
    /** Texto de ayuda propio (foto de etiqueta): sin selector de comida ni nota. */
    guia: String? = null,
    onFoto: (Bitmap, ByteArray) -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var permiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permiso = it }
    val captura = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var hayCamara by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    var fallo by remember { mutableStateOf<String?>(null) } // error visible con «Mandar informe»

    fun entregar(bitmap: Bitmap, rotacion: Int) {
        alcance.launch {
            val (lista, jpeg) = withContext(Dispatchers.Default) { prepararFoto(bitmap, rotacion) }
            Registro.anotar("Foto preparada ${lista.width}x${lista.height}, ${jpeg.size / 1024} KB")
            ocupado = false
            onFoto(lista, jpeg)
        }
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        ocupado = true
        alcance.launch {
            val b = withContext(Dispatchers.IO) { leerDeGaleria(contexto, uri) }
            if (b != null) entregar(b, 0)
            else { Registro.anotar("ERROR galería: no se pudo leer $uri"); ocupado = false; fallo = "No se ha podido abrir la imagen de la galería." }
        }
    }

    fun disparar() {
        if (ocupado || !hayCamara) return
        ocupado = true
        fallo = null
        captura.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        captura.takePicture(ContextCompat.getMainExecutor(contexto), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imagen: ImageProxy) {
                val rot = imagen.imageInfo.rotationDegrees
                val b = runCatching { imagen.toBitmap() }.onFailure { Registro.error("convertir foto", it) }.getOrNull()
                Registro.anotar("Foto hecha ${imagen.width}x${imagen.height} rot $rot")
                imagen.close()
                if (b != null) entregar(b, rot) else { ocupado = false; fallo = "No se ha podido procesar la foto." }
            }
            override fun onError(e: ImageCaptureException) {
                Registro.error("hacer foto (${e.imageCaptureError})", e)
                ocupado = false
                fallo = "No se ha podido hacer la foto (error ${e.imageCaptureError})."
            }
        })
    }

    Box(Modifier.fillMaxSize().background(c.fondo0)) {
        if (permiso) VistaCaptura(captura) { ok -> hayCamara = ok; if (!ok) fallo = "No se ha podido abrir la cámara. Puedes elegir una foto de la galería." }
        else Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📸", style = t.numeroGrande)
            Text(
                "Para hacer la foto, Kcalma necesita la cámara. También puedes elegir una foto de la galería.",
                style = t.cuerpo, color = c.texto2, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp),
            )
            BotonPrincipal("Permitir cámara", onClick = { pedir.launch(Manifest.permission.CAMERA) })
        }

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotonIcono("✕", onCerrar)
                Spacer(Modifier.weight(1f))
                if (guia == null) SelectorComida(comida, onComida)
            }
            if (permiso) Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Text(
                    guia ?: "Encuadra todo el plato desde arriba", style = t.cuerpo, color = c.texto,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(c.fondo0.copy(alpha = .7f)).padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.weight(1f))

            fallo?.let { f ->
                val formaFallo = RoundedCornerShape(14.dp)
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(formaFallo).background(c.fondo1)
                        .border(1.dp, c.rojo.copy(alpha = .5f), formaFallo).padding(start = 12.dp, end = 12.dp, top = 12.dp),
                ) {
                    Text(f, style = t.cuerpo, color = c.texto)
                    EnlaceInforme("Cámara", f)
                }
            }

            val forma = RoundedCornerShape(14.dp)
            if (guia == null) Row(
                Modifier.fillMaxWidth().clip(forma).background(c.fondo0.copy(alpha = .78f)).border(1.dp, c.borde, forma).padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✏️", style = t.cuerpo)
                Box(Modifier.weight(1f).padding(start = 8.dp)) {
                    if (nota.isEmpty()) Text("Nota opcional: «lleva una cucharada de aceite»", style = t.pequeno, color = c.texto3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(
                        nota, { onNota(it.take(200)) }, textStyle = t.cuerpo.copy(color = c.texto),
                        cursorBrush = SolidColor(c.acento), maxLines = 3, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BotonRedondo("🖼️") { if (!ocupado) galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
                Box(
                    Modifier.size(76.dp).border(BorderStroke(4.dp, c.degradado), CircleShape).padding(8.dp).clip(CircleShape)
                        .background(c.texto).alpha(if (hayCamara && !ocupado) 1f else .4f).clickable { disparar() },
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BotonRedondo("⚡", activo = flash) { flash = !flash }
                }
            }
        }
    }
}

@Composable
private fun BotonRedondo(simbolo: String, activo: Boolean = false, onClick: () -> Unit) {
    val c = KcalmaTheme.colores
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(if (activo) c.acento.copy(alpha = .25f) else c.fondo0.copy(alpha = .7f))
            .border(1.dp, if (activo) c.acento else c.borde, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(simbolo, style = KcalmaTheme.tipo.titulo) }
}

@Composable
private fun VistaCaptura(captura: ImageCapture, onLista: (Boolean) -> Unit) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val proveedorFuturo = remember { ProcessCameraProvider.getInstance(contexto) }
    DisposableEffect(Unit) { onDispose { if (proveedorFuturo.isDone) proveedorFuturo.get().unbindAll() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val vista = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
            proveedorFuturo.addListener({
                val proveedor = proveedorFuturo.get()
                val vistaPrevia = Preview.Builder().build().also { it.surfaceProvider = vista.surfaceProvider }
                proveedor.unbindAll()
                onLista(runCatching { proveedor.bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, vistaPrevia, captura) }
                    .onFailure { Registro.error("abrir cámara", it) }.isSuccess)
            }, ContextCompat.getMainExecutor(ctx))
            vista
        },
    )
}

// ───────────────────────── 3 · Analizando ─────────────────────────

@Composable
fun PantallaAnalizando(foto: Bitmap?, avance: Int, error: String?, onCerrar: () -> Unit, onReintentar: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    // Los pasos avanzan con el tiempo para que se vea progreso; la respuesta llega de golpe
    var visto by remember { mutableStateOf(0) }
    LaunchedEffect(error) { if (error == null) { visto = 0; delay(1600); visto = 1; delay(1800); visto = 2 } }
    val paso = max(avance, visto)

    Column(
        Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp),
    ) {
        BarraSuperior("✕", onCerrar)
        Spacer(Modifier.height(10.dp))
        foto?.let {
            Image(
                it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))
                    .border(BorderStroke(2.dp, c.degradado), RoundedCornerShape(20.dp)),
            )
        }
        Text(
            if (error == null) "Analizando…" else "No ha salido bien",
            style = t.tituloGrande, color = c.texto, modifier = Modifier.padding(top = 22.dp, bottom = 14.dp),
        )
        if (error == null) {
            listOf("Reconociendo alimentos con Gemini", "Estimando cantidades", "Buscando cada alimento en la base de datos")
                .forEachIndexed { i, texto ->
                    val hecho = i < paso
                    Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(if (hecho) c.verde.copy(alpha = .18f) else c.fondo3),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (hecho) "✓" else "${i + 1}", style = t.diminuto, color = if (hecho) c.verde else if (i == paso) c.acento else c.texto3) }
                        Text(texto, style = t.cuerpo, color = if (i <= paso) c.texto else c.texto3, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            Spacer(Modifier.weight(1f))
            Text(
                "La IA solo reconoce y estima gramos. Las calorías salen de nuestra base de datos española (BEDCA y Open Food Facts).",
                style = t.pequeno, color = c.texto3, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            )
        } else {
            Text(error, style = t.cuerpo, color = c.texto2)
            Spacer(Modifier.weight(1f))
            BotonPrincipal("Reintentar", onClick = onReintentar)
            Text(
                "Hacer otra foto", style = t.cuerpo, color = c.texto2, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onCerrar).padding(6.dp),
            )
            EnlaceInforme("Análisis de foto", error, Modifier.padding(bottom = 12.dp))
        }
    }
}

// ───────────────────────── 4 · Revisar y añadir ─────────────────────────

@Composable
fun PantallaResultado(
    comida: ComidaDia,
    items: List<ItemFoto>,
    onComida: (ComidaDia) -> Unit,
    onAtras: () -> Unit,
    onGramos: (Int, Double) -> Unit,
    onQuitar: (Int) -> Unit,
    onCambiar: (Int) -> Unit,
    onAnadirOtro: () -> Unit,
    onConfirmar: () -> Unit,
) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val total = items.fold(Nutrientes()) { s, i -> s + i.alimento.nutrientes(i.gramos) }

    Column(Modifier.fillMaxSize().fondoKcalma().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        BarraSuperior("‹", onAtras, "Tu plato") { SelectorComida(comida, onComida) }

        val forma = RoundedCornerShape(16.dp)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp).vidrio(forma, resaltado = true).padding(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${total.kcalInt}", style = t.tituloGrande, color = c.texto)
                Text("kcal", style = t.pequeno, color = c.texto3)
            }
            Text(
                "P ${total.proteinas.roundToInt()} · H ${total.hidratos.roundToInt()} · G ${total.grasas.roundToInt()}",
                style = t.cuerpo, color = c.texto2,
            )
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (items.isEmpty()) item {
                Text(
                    "No hemos reconocido comida en la foto. Añade los alimentos a mano o haz otra foto.",
                    style = t.cuerpo, color = c.texto3, modifier = Modifier.padding(vertical = 10.dp),
                )
            }
            items(items, key = { it.id }) { item ->
                TarjetaItem(item, { onGramos(item.id, it) }, { onQuitar(item.id) }, { onCambiar(item.id) })
            }
            item {
                Text(
                    "+ Añadir algo que falte", style = t.cuerpo, color = c.acento, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                        .border(1.dp, c.borde, RoundedCornerShape(13.dp)).clickable(onClick = onAnadirOtro).padding(13.dp),
                )
            }
        }
        BotonPrincipal(
            when (items.size) { 0 -> "Nada que añadir"; 1 -> "Añadir 1 alimento a ${comida.nombre}"; else -> "Añadir ${items.size} alimentos a ${comida.nombre}" },
            activo = items.isNotEmpty(), onClick = onConfirmar, modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun TarjetaItem(item: ItemFoto, onGramos: (Double) -> Unit, onQuitar: () -> Unit, onCambiar: () -> Unit) {
    val c = KcalmaTheme.colores
    val t = KcalmaTheme.tipo
    val a = item.alimento
    val n = a.nutrientes(item.gramos)
    // Rango fijo por alimento para que el deslizador no salte mientras se mueve
    val maximo = remember(item.id, a) { (max(item.gramos * 2.5, 100.0) / 10).roundToInt() * 10f }
    val forma = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().vidrio(forma).padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoAlimento(a.nombre, 34)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        a.nombre + if (item.deLaNota) " (de tu nota)" else "", style = t.tituloTarjeta, color = c.texto,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    ChipConfianza(item)
                }
                Text(
                    "${a.subtitulo.ifEmpty { a.supermercado ?: "" }} · P ${n.proteinas.roundToInt()} · H ${n.hidratos.roundToInt()} · G ${n.grasas.roundToInt()}".trimStart(' ', '·'),
                    style = t.pequeno, color = c.texto3, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text("${n.kcalInt}", style = t.tituloTarjeta, color = c.texto)
            Spacer(Modifier.width(8.dp))
            Text("✕", style = t.cuerpo, color = c.texto3, modifier = Modifier.clip(CircleShape).clickable(onClick = onQuitar).padding(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Cambiar", style = t.pequeno, color = c.acento,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCambiar).padding(vertical = 6.dp, horizontal = 2.dp),
            )
            Slider(
                value = item.gramos.toFloat().coerceIn(0f, maximo),
                onValueChange = { onGramos((it / 5).roundToInt() * 5.0) },
                valueRange = 0f..maximo,
                colors = SliderDefaults.colors(thumbColor = c.acento, activeTrackColor = c.acento, inactiveTrackColor = c.fondo4),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Text("${decimal(item.gramos)} g", style = t.pequeno, color = c.texto2, textAlign = TextAlign.End, modifier = Modifier.width(52.dp))
        }
    }
}

@Composable
private fun ChipConfianza(item: ItemFoto) {
    val c = KcalmaTheme.colores
    val (texto, color) = when {
        item.estimado -> "Estimado" to c.acento2
        item.confianza == "alta" -> "Seguro" to c.verde
        else -> "Revisa" to c.aviso
    }
    Text(
        texto, style = KcalmaTheme.tipo.diminuto, color = color, maxLines = 1,
        modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = .14f)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
