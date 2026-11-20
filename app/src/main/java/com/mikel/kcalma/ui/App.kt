package com.mikel.kcalma.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.mikel.kcalma.datos.Actualizador
import com.mikel.kcalma.datos.AlmacenPerfil
import com.mikel.kcalma.datos.Claves
import com.mikel.kcalma.datos.Copia
import com.mikel.kcalma.datos.Gemini
import com.mikel.kcalma.ui.componentes.mandarInforme
import com.mikel.kcalma.ui.foto.PantallaClave
import com.mikel.kcalma.ui.hoy.Pestana
import com.mikel.kcalma.ui.perfil.AccionesPerfil
import com.mikel.kcalma.ui.perfil.Bloque
import com.mikel.kcalma.ui.perfil.EditarPerfil
import com.mikel.kcalma.ui.perfil.PantallaPerfil
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.Intent
import android.net.Uri
import com.mikel.kcalma.datos.Repositorio
import com.mikel.kcalma.datos.VersionNueva
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.Calculadora
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.dominio.Entrada
import com.mikel.kcalma.dominio.FiltrosIdeas
import com.mikel.kcalma.dominio.Idea
import com.mikel.kcalma.dominio.Receta
import com.mikel.kcalma.dominio.Recomendador
import com.mikel.kcalma.datos.Recetas
import com.mikel.kcalma.datos.Respuesta
import com.mikel.kcalma.ui.ideas.DetalleIdea
import com.mikel.kcalma.ui.ideas.EstadoIa
import com.mikel.kcalma.ui.ideas.PantallaIdeas
import com.mikel.kcalma.dominio.Nutrientes
import com.mikel.kcalma.dominio.Perfil
import com.mikel.kcalma.dominio.SUPERMERCADOS
import com.mikel.kcalma.dominio.Unidad
import com.mikel.kcalma.ui.foto.FlujoFoto
import com.mikel.kcalma.ui.diario.Escaner
import com.mikel.kcalma.ui.diario.FichaAlimento
import com.mikel.kcalma.ui.diario.PantallaAnadir
import com.mikel.kcalma.ui.diario.PantallaComida
import com.mikel.kcalma.ui.hoy.PantallaHoy
import com.mikel.kcalma.ui.hoy.TarjetaActualizacion
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/** Pantallas que se apilan encima de «Hoy». */
private sealed interface Destino {
    data class Comida(val comida: ComidaDia) : Destino
    data class Anadir(val comida: ComidaDia) : Destino
    data class Ficha(val alimento: Alimento, val comida: ComidaDia, val entrada: Entrada? = null) : Destino
    data class Escaner(val comida: ComidaDia) : Destino
    data class Foto(val comida: ComidaDia) : Destino
    data class Editar(val bloque: Bloque) : Destino
    data object Clave : Destino
    data class VerIdea(val idea: Idea, val comida: ComidaDia) : Destino
}

@Composable
fun App(perfil: Perfil, repo: Repositorio, almacen: AlmacenPerfil) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val pila = remember { mutableStateListOf<Destino>() }
    val fecha = remember { LocalDate.now().toString() }
    val entradas by remember(fecha) { repo.diario.entradasDe(fecha) }.collectAsState(initial = emptyList())
    val plan = remember(perfil) { Calculadora.calcular(perfil) }
    val comido = entradas.fold(Nutrientes()) { s, e -> s + e.nutrientes }
    val restante = Nutrientes(plan.kcal - comido.kcal, plan.proteinaG - comido.proteinas, plan.hidratosG - comido.hidratos, plan.grasasG - comido.grasas)
    // Filtros del buscador: los supermercados del usuario o, si no eligió, los dos con más productos
    val supermercados = perfil.supermercados.sortedBy { SUPERMERCADOS.indexOf(it) }.ifEmpty { listOf("Mercadona", "Lidl") }

    // Actualizaciones desde GitHub Releases
    val actualizador = remember { Actualizador(contexto.applicationContext) }
    var nueva by remember { mutableStateOf<VersionNueva?>(null) }
    var descarga by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(Unit) { nueva = actualizador.comprobar() }

    var pestana by rememberSaveable { mutableStateOf(Pestana.HOY) }
    val pesos by almacen.pesos.collectAsState(initial = emptyList())
    val dias by remember(fecha) { repo.diario.totalesDesde(LocalDate.parse(fecha).minusDays(100).toString()) }.collectAsState(initial = emptyMap())
    val claves = remember { Claves(contexto.applicationContext) }
    var claveGuardada by remember { mutableStateOf(claves.gemini() != null) }
    // La clave también se guarda desde la foto: se vuelve a mirar al cambiar de pantalla
    LaunchedEffect(pestana, pila.size) { claveGuardada = claves.gemini() != null }
    val aviso: (String) -> Unit = { Toast.makeText(contexto, it, Toast.LENGTH_SHORT).show() }
    val cambiarPestana: (Pestana) -> Unit = { pestana = it }

    // ---- Ideas: recetas propias + las que pida a la IA ----
    val recetas by produceState(emptyList<Receta>()) { value = runCatching { Recetas.todas(contexto.applicationContext) }.getOrDefault(emptyList()) }
    // Próxima comida sin apuntar a partir de la hora actual (la cena si ya va todo apuntado)
    val pendiente = ComidaDia.entries.drop(ComidaDia.segunHora(LocalTime.now().hour).ordinal)
        .firstOrNull { cd -> entradas.none { it.comida == cd } }
    val proxima = pendiente ?: ComidaDia.CENA
    var comidaIdeas by rememberSaveable { mutableStateOf<ComidaDia?>(null) }
    val comidaIdea = comidaIdeas ?: proxima
    var filtros by remember { mutableStateOf(FiltrosIdeas()) }
    val deIa = remember { mutableStateListOf<Receta>() }
    var estadoIa by remember { mutableStateOf<EstadoIa>(EstadoIa.Nada) }
    val ideasDe: (ComidaDia, FiltrosIdeas) -> List<Idea> = { cd, f ->
        Recomendador.recomendar(recetas + deIa.filter { cd in it.comidas }, perfil, cd, plan.kcal, restante, f)
    }
    fun pedirIa() {
        val clave = claves.gemini() ?: run { pila.add(Destino.Clave); return }
        estadoIa = EstadoIa.Cargando
        val cd = comidaIdea
        alcance.launch {
            val obj = Recomendador.objetivo(cd, plan.kcal, restante.kcal)
            val prot = if (restante.kcal > 0) (restante.proteinas * obj / restante.kcal).coerceAtLeast(10.0) else 25.0
            estadoIa = when (val r = Gemini.ideas(clave, Recetas.peticion(perfil, cd, obj, prot, filtros.rapidas))) {
                is Respuesta.Ok -> {
                    val nuevas = Recetas.deIa(repo, cd, r.valor)
                    deIa.removeAll { it.nombre in nuevas.map { n -> n.nombre } }
                    deIa.addAll(0, nuevas)
                    if (nuevas.isEmpty()) EstadoIa.Error("La IA no devolvió recetas válidas. Prueba otra vez.") else EstadoIa.Nada
                }
                Respuesta.ClaveMala -> EstadoIa.Error("La clave de Gemini no es válida. Cámbiala en Perfil.")
                Respuesta.Limite -> EstadoIa.Error("Has llegado al límite gratuito de Gemini por ahora. Prueba en un rato.")
                Respuesta.SinConexion -> EstadoIa.Error("Sin conexión a internet.")
                is Respuesta.Fallo -> EstadoIa.Error("La IA no respondió. Prueba otra vez.")
            }
        }
    }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) alcance.launch {
            runCatching { Copia.exportar(contexto, uri, almacen, repo.diario) }
                .onSuccess { aviso("Copia guardada") }
                .onFailure { com.mikel.kcalma.datos.Registro.error("Exportar copia", it); aviso("No se pudo guardar la copia") }
        }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) alcance.launch {
            runCatching { Copia.importar(contexto, uri, almacen, repo.diario) }
                .onSuccess { ok -> aviso(if (ok) "Copia importada" else "Ese archivo no es una copia de Kcalma") }
                .onFailure { com.mikel.kcalma.datos.Registro.error("Importar copia", it); aviso("No se pudo leer la copia") }
        }
    }

    BackHandler(enabled = pila.isNotEmpty()) { pila.removeAt(pila.lastIndex) }
    BackHandler(enabled = pila.isEmpty() && pestana != Pestana.HOY) { pestana = Pestana.HOY }

    fun guardar(d: Destino.Ficha, gramos: Double, unidad: Unidad, cantidad: Double) {
        alcance.launch(Dispatchers.IO) {
            if (d.entrada != null) repo.diario.actualizar(d.entrada.id, gramos, unidad, cantidad)
            else repo.diario.anadir(fecha, d.comida, d.alimento, gramos, unidad, cantidad)
        }
        // Vuelve a la comida para ver el resultado
        while (pila.lastOrNull().let { it is Destino.Ficha || it is Destino.Anadir || it is Destino.Escaner }) pila.removeAt(pila.lastIndex)
        if (pila.lastOrNull() != Destino.Comida(d.comida)) pila.add(Destino.Comida(d.comida))
    }

    fun anadirRapido(a: Alimento, comida: ComidaDia) {
        val gramos = a.gramosRapidos
        val unidad = if (a.racionValida != null) Unidad.RACION else Unidad.GRAMOS
        alcance.launch(Dispatchers.IO) { repo.diario.anadir(fecha, comida, a, gramos, unidad, if (unidad == Unidad.RACION) 1.0 else gramos) }
        Toast.makeText(contexto, "Añadido a ${comida.nombre} · ${a.nutrientes(gramos).kcalInt} kcal", Toast.LENGTH_SHORT).show()
    }

    when (val d = pila.lastOrNull()) {
        null -> if (pestana == Pestana.IDEAS) PantallaIdeas(
            comida = comidaIdea,
            restante = restante,
            ideas = ideasDe(comidaIdea, filtros),
            filtros = filtros,
            estadoIa = estadoIa,
            onComida = { comidaIdeas = it },
            onFiltros = { filtros = it },
            onAbrir = { pila.add(Destino.VerIdea(it, comidaIdea)) },
            onMasIa = { pedirIa() },
            onPestana = cambiarPestana,
            onCamara = { pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour))) },
        ) else if (pestana == Pestana.PROGRESO) com.mikel.kcalma.ui.progreso.PantallaProgreso(
            perfil = perfil, pesos = pesos, dias = dias, onPestana = cambiarPestana,
            onCamara = { pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour))) },
        ) else if (pestana == Pestana.PERFIL) PantallaPerfil(
            perfil = perfil,
            pesos = pesos,
            claveGuardada = claveGuardada,
            acciones = AccionesPerfil(
                onEditar = { pila.add(Destino.Editar(it)) },
                onGuardarPeso = { kg -> alcance.launch { almacen.guardar(perfil.copy(pesoKg = kg), forzarPesada = true) }; aviso("Peso guardado") },
                onClave = { pila.add(Destino.Clave) },
                onBorrarClave = { claves.borrarGemini(); claveGuardada = false; aviso("Clave borrada") },
                onExportar = { exportar.launch("kcalma-copia-${LocalDate.now()}.json") },
                onImportar = { importar.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                onInforme = { mandarInforme(contexto, "Desde Perfil", "") },
                onActualizar = {
                    aviso("Buscando actualizaciones…")
                    alcance.launch {
                        val v = actualizador.comprobar(forzar = true)
                        if (v == null) aviso("Tienes la última versión (${com.mikel.kcalma.BuildConfig.VERSION_NAME})")
                        else {
                            // Descarga directa: en Hoy se ve el progreso y al terminar se abre el instalador
                            nueva = v; pestana = Pestana.HOY; descarga = 0f
                            runCatching { actualizador.instalar(v) { descarga = it } }.onFailure {
                                aviso("No se pudo descargar. Revisa la conexión.")
                            }
                            descarga = null
                        }
                    }
                },
            ),
            onPestana = cambiarPestana,
            onCamara = { pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour))) },
        ) else PantallaHoy(
            perfil = perfil,
            entradas = entradas,
            onAbrirComida = { c -> pila.add(if (entradas.any { it.comida == c }) Destino.Comida(c) else Destino.Anadir(c)) },
            onAnadir = { c -> pila.add(Destino.Anadir(c)) },
            onCamara = { pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour))) },
            onPestana = cambiarPestana,
            ideas = if (pendiente == null) emptyList() else ideasDe(proxima, FiltrosIdeas()),
            tituloIdeas = "Ideas para ${if (proxima == ComidaDia.MEDIA_MANANA) "" else "la "}${proxima.nombre.lowercase()}",
            onIdea = { pila.add(Destino.VerIdea(it, proxima)) },
            onVerIdeas = { comidaIdeas = null; pestana = Pestana.IDEAS },
            aviso = nueva?.let { v ->
                {
                    TarjetaActualizacion(
                        v, descarga,
                        onActualizar = {
                            descarga = 0f
                            alcance.launch {
                                runCatching { actualizador.instalar(v) { descarga = it } }.onFailure {
                                    Toast.makeText(contexto, "No se pudo descargar. Revisa la conexión.", Toast.LENGTH_SHORT).show()
                                }
                                descarga = null
                            }
                        },
                        onOcultar = { actualizador.ocultar(v); nueva = null },
                    )
                }
            },
        )
        is Destino.Comida -> PantallaComida(
            comida = d.comida,
            entradas = entradas.filter { it.comida == d.comida },
            onAtras = { pila.removeAt(pila.lastIndex) },
            onBuscar = { pila.add(Destino.Anadir(d.comida)) },
            onEscanear = { pila.add(Destino.Escaner(d.comida)) },
            onEditar = { e -> pila.add(Destino.Ficha(e.alimento, d.comida, e)) },
            onBorrar = { e -> alcance.launch(Dispatchers.IO) { repo.diario.borrar(e.id) } },
            onGuardarHabitual = { nombre ->
                val lista = entradas.filter { it.comida == d.comida }
                alcance.launch(Dispatchers.IO) { repo.diario.guardarHabitual(nombre, lista) }
            },
            onFoto = { pila.add(Destino.Foto(d.comida)) },
        )
        is Destino.Anadir -> PantallaAnadir(
            comida = d.comida,
            supermercados = supermercados,
            repo = repo,
            onCambiarComida = { c -> pila[pila.lastIndex] = Destino.Anadir(c) },
            onCerrar = { pila.removeAt(pila.lastIndex) },
            onAbrir = { a -> pila.add(Destino.Ficha(a, d.comida)) },
            onAnadirRapido = { a -> anadirRapido(a, d.comida) },
            onHabitual = { h ->
                alcance.launch {
                    withContext(Dispatchers.IO) {
                        h.entradas.forEach { e -> repo.diario.anadir(fecha, d.comida, e.alimento, e.gramos, e.unidad, e.cantidad) }
                    }
                    Toast.makeText(contexto, "«${h.nombre}» añadida a ${d.comida.nombre}", Toast.LENGTH_SHORT).show()
                }
                pila.removeAt(pila.lastIndex)
                if (pila.lastOrNull() != Destino.Comida(d.comida)) pila.add(Destino.Comida(d.comida))
            },
            onEscanear = { pila.add(Destino.Escaner(d.comida)) },
        )
        is Destino.Ficha -> FichaAlimento(
            alimento = d.alimento,
            comida = d.comida,
            // Al editar, lo que ya estaba apuntado no cuenta como comido
            restante = if (d.entrada != null) restante + d.entrada.nutrientes else restante,
            inicial = d.entrada,
            onAtras = { pila.removeAt(pila.lastIndex) },
            onGuardar = { g, u, c -> guardar(d, g, u, c) },
        )
        is Destino.Escaner -> Escaner(
            buscar = { repo.porCodigo(it) },
            onCerrar = { pila.removeAt(pila.lastIndex) },
            onVer = { a -> pila.add(Destino.Ficha(a, d.comida)) },
            onBuscarAMano = {
                pila.removeAt(pila.lastIndex)
                if (pila.lastOrNull() != Destino.Anadir(d.comida)) pila.add(Destino.Anadir(d.comida))
            },
        )
        is Destino.Foto -> FlujoFoto(
            comidaInicial = d.comida,
            repo = repo,
            supermercados = supermercados,
            onCerrar = { pila.removeAt(pila.lastIndex) },
            onAnadir = { comida, items ->
                alcance.launch(Dispatchers.IO) {
                    items.filter { it.gramos > 0 }.forEach { i -> repo.diario.anadir(fecha, comida, i.alimento, i.gramos, Unidad.GRAMOS, i.gramos) }
                }
                pila.removeAt(pila.lastIndex)
                if (pila.lastOrNull() != Destino.Comida(comida)) pila.add(Destino.Comida(comida))
            },
        )
        is Destino.Editar -> EditarPerfil(
            bloque = d.bloque,
            inicial = perfil,
            onAtras = { pila.removeAt(pila.lastIndex) },
            onGuardar = { nuevo ->
                alcance.launch { almacen.guardar(nuevo) }
                pila.removeAt(pila.lastIndex)
                aviso("Guardado")
            },
        )
        is Destino.VerIdea -> DetalleIdea(
            idea = d.idea,
            comida = d.comida,
            restante = restante,
            planKcal = plan.kcal,
            onAtras = { pila.removeAt(pila.lastIndex) },
            onAnadir = {
                alcance.launch(Dispatchers.IO) {
                    d.idea.receta.ingredientes.forEach { i -> repo.diario.anadir(fecha, d.comida, i.alimento, i.gramos, Unidad.GRAMOS, i.gramos) }
                }
                aviso("«${d.idea.receta.nombre}» añadida a ${d.comida.nombre}")
                pila.removeAt(pila.lastIndex)
                pila.add(Destino.Comida(d.comida))
            },
        )
        Destino.Clave -> PantallaClave(
            errorInicial = null,
            onCerrar = { pila.removeAt(pila.lastIndex) },
            onAbrirWeb = { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) },
            probar = { clave -> Gemini.probarClave(clave) },
            onGuardada = { clave ->
                claves.guardarGemini(clave); claveGuardada = true
                pila.removeAt(pila.lastIndex); aviso("Clave guardada")
            },
        )
    }
}
