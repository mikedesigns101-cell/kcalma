package com.mikel.kcalma.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.asCoroutineDispatcher
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
import com.mikel.kcalma.datos.Coach
import com.mikel.kcalma.datos.Copia
import com.mikel.kcalma.dominio.Cambio
import com.mikel.kcalma.dominio.Cocinado
import com.mikel.kcalma.dominio.Competicion
import com.mikel.kcalma.dominio.Cambios
import com.mikel.kcalma.dominio.Entrenador
import com.mikel.kcalma.dominio.MensajeChat
import com.mikel.kcalma.datos.Gemini
import com.mikel.kcalma.ui.componentes.BurbujasInforme
import com.mikel.kcalma.ui.componentes.TipoEnvio
import com.mikel.kcalma.ui.componentes.HojaInforme
import com.mikel.kcalma.ui.foto.PantallaClave
import com.mikel.kcalma.ui.hoy.Pestana
import com.mikel.kcalma.ui.hoy.AccionesMas
import com.mikel.kcalma.ui.hoy.LocalAccionesMas
import com.mikel.kcalma.ui.perfil.PantallaApariencia
import com.mikel.kcalma.ui.theme.Tema
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.mikel.kcalma.ui.componentes.fondoKcalma
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import androidx.health.connect.client.PermissionController
import com.mikel.kcalma.Lanzador
import com.mikel.kcalma.datos.ListaCompra
import com.mikel.kcalma.datos.Salud
import com.mikel.kcalma.dominio.Actividad
import com.mikel.kcalma.dominio.Ajuste
import com.mikel.kcalma.dominio.Ingrediente
import com.mikel.kcalma.ui.diario.AccionesPlatos
import com.mikel.kcalma.ui.diario.EdicionPlato
import com.mikel.kcalma.ui.diario.FlujoEtiqueta
import com.mikel.kcalma.ui.diario.PantallaPlato
import com.mikel.kcalma.ui.hoy.TarjetaAjuste
import com.mikel.kcalma.ui.ideas.PantallaLista
import com.mikel.kcalma.ui.perfil.PantallaRecordatorios
import com.mikel.kcalma.widget.WidgetHoy

/** Pantallas que se apilan encima de «Hoy». */
private sealed interface Destino {
    data class Comida(val comida: ComidaDia) : Destino
    /** [paraPlato]: se eligen ingredientes de un plato casero en vez de apuntar en el diario. */
    data class Anadir(val comida: ComidaDia, val paraPlato: Boolean = false, val voz: Boolean = false) : Destino
    data class Ficha(
        val alimento: Alimento, val comida: ComidaDia, val entrada: Entrada? = null, val paraPlato: Boolean = false,
        val sugerida: Pair<String, Entrada>? = null,
    ) : Destino
    data class Escaner(val comida: ComidaDia, val paraPlato: Boolean = false) : Destino
    data class Etiqueta(val comida: ComidaDia, val codigo: String?, val paraPlato: Boolean) : Destino
    data object Plato : Destino
    data object Lista : Destino
    data object Recordatorios : Destino
    data object Apariencia : Destino
    data class Foto(val comida: ComidaDia) : Destino
    data class Dictar(val comida: ComidaDia) : Destino
    data class Editar(val bloque: Bloque) : Destino
    data object Clave : Destino
    data class VerIdea(val idea: Idea, val comida: ComidaDia) : Destino
    data class Rutina(val id: String) : Destino
    data class Sesion(val id: Long) : Destino
    data class Ejercicio(val id: String) : Destino
    data class EditarRutina(val rutina: com.mikel.kcalma.dominio.Rutina) : Destino
    data object Biblioteca : Destino
    data object Historial : Destino
    data object Amigos : Destino
    data class Dia(val fecha: String) : Destino
    data object Propuesta : Destino
    data object Chat : Destino
}

@Composable
fun App(perfil: Perfil, repo: Repositorio, almacen: AlmacenPerfil, tema: Tema, animaciones: Boolean) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val pila = remember { mutableStateListOf<Destino>() }
    val hoy = remember { LocalDate.now().toString() }
    // Día que se está viendo o editando: hoy, salvo que se haya abierto un día del historial
    var diaPasado by rememberSaveable { mutableStateOf<String?>(null) }
    val fecha = diaPasado ?: hoy
    val entradas by remember(fecha) { repo.diario.entradasDe(fecha) }.collectAsState(initial = emptyList())
    val plan = remember(perfil) { Calculadora.calcular(perfil) }
    val comido = entradas.fold(Nutrientes()) { s, e -> s + e.nutrientes }
    // Pasos de Health Connect: la mitad de lo caminado de más se suma al objetivo de hoy
    val salud = remember { Salud(contexto.applicationContext) }
    var estadoSalud by remember { mutableStateOf<Salud.Estado?>(null) }
    var pasosHoy by remember { mutableStateOf<Long?>(null) }
    val podometro = remember { com.mikel.kcalma.datos.Podometro(contexto.applicationContext) }
    var podometroOn by remember { mutableStateOf(podometro.activado) }
    val ejercicio = pasosHoy?.let { Actividad.suma(perfil, it) } ?: 0
    val restante = Nutrientes(plan.kcal + ejercicio - comido.kcal, plan.proteinaG - comido.proteinas, plan.hidratosG - comido.hidratos, plan.grasasG - comido.grasas)
    // Filtros del buscador: los supermercados del usuario o, si no eligió, los dos con más productos
    val supermercados = perfil.supermercados.sortedBy { SUPERMERCADOS.indexOf(it) }.ifEmpty { listOf("Mercadona", "Lidl") }

    // Actualizaciones desde GitHub Releases
    val actualizador = remember { Actualizador(contexto.applicationContext) }
    var nueva by remember { mutableStateOf<VersionNueva?>(null) }
    var descarga by remember { mutableStateOf<Float?>(null) }
    // Cada vez que se vuelve a la app (no solo al arrancarla), como mucho una consulta cada 10 minutos
    LifecycleResumeEffect(Unit) {
        val trabajo = alcance.launch {
            val v = actualizador.comprobar(); if (descarga == null) nueva = v
            com.mikel.kcalma.datos.Comunidad.sincronizar(contexto.applicationContext, repo.diario)
        }
        onPauseOrDispose { trabajo.cancel() }
    }

    var pestana by rememberSaveable { mutableStateOf(Pestana.HOY) }
    val pesos by almacen.pesos.collectAsState(initial = emptyList())
    val dias by remember(hoy) { repo.diario.totalesDesde(LocalDate.parse(hoy).minusDays(com.mikel.kcalma.ui.progreso.DIAS_HISTORIAL).toString()) }.collectAsState(initial = emptyMap())
    val claves = remember { Claves(contexto.applicationContext) }
    var claveGuardada by remember { mutableStateOf(claves.hayGemini()) }
    // La clave también se guarda desde la foto: se vuelve a mirar al cambiar de pantalla (sin descifrarla: sería lento)
    LaunchedEffect(pestana, pila.size) { claveGuardada = claves.hayGemini() }
    val aviso: (String) -> Unit = { Toast.makeText(contexto, it, Toast.LENGTH_SHORT).show() }
    // Health Connect y el widget: como mucho cada 30 s, no en cada cambio de pestaña
    var ultimaSalud by remember { mutableStateOf(0L) }
    LaunchedEffect(pestana, pila.size) {
        val ahoraMs = System.currentTimeMillis()
        if ((estadoSalud == Salud.Estado.CONECTADO || podometroOn) && ahoraMs - ultimaSalud < 30_000) return@LaunchedEffect
        ultimaSalud = ahoraMs
        estadoSalud = salud.estado()
        // Pasos de hoy: Health Connect, el podómetro del móvil o, si están los dos, la cifra más alta
        val deSalud = if (estadoSalud == Salud.Estado.CONECTADO) salud.pasosHoy() else null
        val delMovil = if (podometroOn) { podometro.leer(); podometro.pasosHoy() } else null
        pasosHoy = listOfNotNull(deSalud, delMovil).maxOrNull()
        pasosHoy?.let { WidgetHoy.guardarEjercicio(contexto, Actividad.suma(perfil, it)); WidgetHoy.refrescar(contexto) }
    }
    val pedirPasos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) { podometro.activar(); podometroOn = true; ultimaSalud = 0; aviso("Podómetro encendido") }
        else aviso("Sin ese permiso no se pueden contar los pasos")
    }

    // ---- Amigos: competición mensual. Mis cifras se calculan aquí; las de los demás se bajan cifradas ----
    val amigos = remember { com.mikel.kcalma.datos.Amigos(contexto.applicationContext) }
    var grupo by remember { mutableStateOf(amigos.codigo) }
    var miMarca by remember { mutableStateOf<Competicion.Marca?>(null) }
    var marcasGrupo by remember { mutableStateOf<Map<String, List<Competicion.Marca>>>(emptyMap()) }
    var cargandoAmigos by remember { mutableStateOf(false) }
    var compartirDetalle by remember { mutableStateOf(amigos.compartirDetalle) }
    val pedirSalud = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { concedidos ->
        if (concedidos.containsAll(salud.permisos)) { estadoSalud = Salud.Estado.CONECTADO; aviso("Pasos conectados") }
    }

    // Ajuste semanal del plan (propone; nunca cambia nada solo)
    val ultimaPropuesta by almacen.ultimaPropuesta.collectAsState(initial = "")
    val propuesta = remember(perfil, pesos, dias, ultimaPropuesta) {
        val u = ultimaPropuesta
        val reciente = u != null && runCatching { LocalDate.parse(u).plusDays(7) > LocalDate.now() }.getOrDefault(false)
        if (u == "" || reciente) null else Ajuste.proponer(perfil, pesos, dias)
    }

    // Platos caseros y lista de la compra
    var edicion by remember { mutableStateOf<EdicionPlato?>(null) }
    val lista = remember { ListaCompra(contexto.applicationContext) }
    val estadoLista by lista.estado.collectAsState()
    fun volverAPlato() { while (pila.isNotEmpty() && pila.last() != Destino.Plato) pila.removeAt(pila.lastIndex) }
    fun anadirIngrediente(a: Alimento, gramos: Double) {
        edicion?.ingredientes?.add(Ingrediente(a, gramos))
        volverAPlato()
    }

    // Widget y notificaciones abren directamente una pantalla
    val accion by Lanzador.accion.collectAsState()
    LaunchedEffect(accion) {
        val a = accion ?: return@LaunchedEffect
        Lanzador.accion.value = null
        pila.clear()
        when {
            a == "foto" -> pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour)))
            a == "peso" -> pestana = Pestana.PERFIL
            a.startsWith("anadir:") -> ComidaDia.entries.firstOrNull { it.name == a.removePrefix("anadir:") }?.let { pila.add(Destino.Anadir(it)) }
        }
    }
    val cambiarPestana: (Pestana) -> Unit = { pestana = it }
    var informePerfil by remember { mutableStateOf(false) }

    // ---- Entreno: las escrituras van en orden por un solo hilo (una serie corregida dos veces no se pisa) ----
    val entrenos = repo.entrenos
    val rutinas by remember { entrenos.rutinas() }.collectAsState(initial = emptyList())
    val sesionesE by remember { entrenos.sesiones() }.collectAsState(initial = emptyList())
    val ajustesE by remember { entrenos.ajustes() }.collectAsState(initial = com.mikel.kcalma.datos.AjustesEntreno())
    val hiloEntreno = remember { java.util.concurrent.Executors.newSingleThreadExecutor().asCoroutineDispatcher() }
    var seccion by rememberSaveable { mutableStateOf(com.mikel.kcalma.dominio.Seccion.GIMNASIO) }
    fun enBd(f: () -> Unit) { alcance.launch(hiloEntreno) { f() } }
    fun empezar(r: com.mikel.kcalma.dominio.Rutina) {
        val enCurso = sesionesE.firstOrNull { !it.terminada }
        if (enCurso != null) { aviso("Termina antes el entreno a medias"); pila.add(Destino.Sesion(enCurso.id)); return }
        alcance.launch {
            val id = withContext(hiloEntreno) { entrenos.empezar(r) }
            pila.add(Destino.Sesion(id))
        }
    }
    val abrirRutina: (com.mikel.kcalma.dominio.Rutina) -> Unit = { pila.add(Destino.Rutina(it.id)) }
    val continuar: (com.mikel.kcalma.dominio.Sesion) -> Unit = { pila.add(Destino.Sesion(it.id)) }

    // ---- Entrenador IA: los números los calcula Entrenador; Gemini elige y redacta; Cambios valida ----
    val propuestaIa = remember(ajustesE.propuesta) { Coach.propuestaDe(ajustesE.propuesta)?.takeIf { it.semana == Entrenador.semana() } }
    var cargandoPropuesta by remember { mutableStateOf(false) }
    var errorPropuesta by remember { mutableStateOf<String?>(null) }
    val chat = remember { mutableStateListOf<MensajeChat>() }
    var pensando by remember { mutableStateOf(false) }
    var errorChat by remember { mutableStateOf<String?>(null) }
    fun datosEntrenador(): String {
        // Todo calculado aquí: la IA solo lo lee
        val semana = (1..7).mapNotNull { dias[LocalDate.parse(hoy).minusDays(it.toLong()).toString()] }
        val hace4 = pesos.lastOrNull { it.fecha <= LocalDate.parse(hoy).minusDays(28).toString() } ?: pesos.firstOrNull()
        val nutricion = buildString {
            append("Nutrición de hoy: plan de ${plan.kcal + ejercicio} kcal y ${plan.proteinaG} g de proteína; ")
            append("lleva ${comido.kcalInt} kcal y ${comido.proteinas.toInt()} g de proteína.")
            if (semana.isNotEmpty()) append("\nMedia de los últimos ${semana.size} días apuntados: ${semana.sumOf { it.kcal }.toInt() / semana.size} kcal y ${semana.sumOf { it.proteinas }.toInt() / semana.size} g de proteína al día.")
            append("\nPeso: ${perfil.pesoKg} kg ahora; objetivo ${perfil.pesoObjetivoKg} kg.")
            if (hace4 != null && pesos.size > 1) append(" El ${hace4.fecha} pesaba ${hace4.kg} kg.")
            append("\n${perfil.sexo.name.lowercase().replaceFirstChar { it.uppercase() }}, ${perfil.edad} años, ${perfil.alturaCm} cm. Dice que entrena ${perfil.diasEjercicio} días por semana.")
            pasosHoy?.let { append("\nPasos de hoy (Health Connect): $it.") }
        }
        return Entrenador.contexto(Entrenador.estadisticas(sesionesE, rutinas, perfil), rutinas, nutricion)
    }
    fun errorIa(r: Respuesta<*>) = when (r) {
        Respuesta.ClaveMala -> "La clave de Gemini no es válida. Cámbiala en Perfil."
        Respuesta.Limite -> "Has llegado al límite gratuito de Gemini por ahora. Prueba en un rato."
        Respuesta.SinConexion -> "Sin conexión a internet."
        else -> "La IA no respondió. Prueba otra vez."
    }
    fun pedirPropuesta() {
        val clave = claves.gemini() ?: run { pila.add(Destino.Clave); return }
        cargandoPropuesta = true; errorPropuesta = null
        alcance.launch {
            when (val r = Gemini.propuesta(clave, datosEntrenador())) {
                is Respuesta.Ok -> {
                    val cambios = r.valor.second.mapNotNull { bruto ->
                        Coach.cambioDe(bruto)?.let { Cambios.validar(it, rutinas) }
                            ?: null.also { com.mikel.kcalma.datos.Registro.anotar("Entrenador: cambio descartado: ${bruto.toString().take(700)}") }
                    }.distinct().take(3)
                    val p = Coach.Propuesta(Entrenador.semana(), r.valor.first, cambios)
                    withContext(hiloEntreno) { entrenos.guardarAjuste("propuesta", Coach.json(p)) }
                    if (cambios.isNotEmpty()) pila.add(Destino.Propuesta)
                }
                else -> errorPropuesta = errorIa(r)
            }
            cargandoPropuesta = false
        }
    }
    /** Lee, cambia y guarda en el hilo de entreno: dos cambios seguidos a la misma rutina no se pisan. */
    fun aplicarCambios(cs: List<Cambio>) = enBd {
        var lista = entrenos.leerRutinas()
        cs.forEach { c ->
            if (Cambios.validar(c, lista) == null) return@forEach
            if (c is Cambio.Crear) { entrenos.guardarRutina(Cambios.rutinaNueva(c)); lista = entrenos.leerRutinas(); return@forEach }
            val nueva = Cambios.aplicar(lista.first { it.id == c.rutina }, c)
            entrenos.guardarRutina(nueva)
            lista = lista.map { if (it.id == nueva.id) nueva else it }
        }
    }
    fun resolver(indices: List<Int>) = enBd {
        Coach.propuestaDe(entrenos.leerAjustes().propuesta)?.let { p -> entrenos.guardarAjuste("propuesta", Coach.json(p.copy(resueltos = p.resueltos + indices))) }
    }
    fun preguntar(texto: String) {
        val clave = claves.gemini() ?: run { pila.add(Destino.Clave); return }
        chat.add(MensajeChat(true, texto)); pensando = true; errorChat = null
        // Últimos mensajes, empezando por uno del usuario y sin dos seguidos del mismo lado
        val turnos = mutableListOf<Pair<Boolean, String>>()
        chat.takeLast(12).dropWhile { !it.mio }.forEach { m ->
            val t = m.texto + (m.cambio?.let { c ->
                " [Cambio propuesto: ${Cambios.titulo(c, rutinas.firstOrNull { it.id == c.rutina })} · " +
                    "${when (m.estado) { 1 -> "aceptado"; 2 -> "rechazado"; else -> "sin responder" }}]"
            } ?: "")
            val u = turnos.lastOrNull()
            if (u != null && u.first == m.mio) turnos[turnos.lastIndex] = u.first to u.second + "\n" + t else turnos.add(m.mio to t)
        }
        alcance.launch {
            when (val r = Gemini.chat(clave, datosEntrenador(), turnos)) {
                is Respuesta.Ok -> {
                    val bruto = r.valor.second
                    val cambio = bruto?.let { Coach.cambioDe(it) }?.let { Cambios.validar(it, rutinas) }
                    // Si la IA propone algo que no pasa la validación, no se enseña, pero se dice y queda en el registro
                    val fallido = bruto != null && cambio == null
                    if (fallido) com.mikel.kcalma.datos.Registro.anotar("Entrenador: cambio descartado: ${bruto.toString().take(700)}")
                    chat.add(MensajeChat(false, r.valor.first + if (fallido) " (No he podido prepararlo con los ejercicios de la app: pídemelo otra vez o de otra forma.)" else "", cambio))
                }
                else -> errorChat = errorIa(r)
            }
            pensando = false
        }
    }

    /** Calcula mis cifras del mes, las publica si han cambiado y baja las del grupo. */
    fun refrescarAmigos(forzar: Boolean = false) {
        if (amigos.codigo == null || cargandoAmigos) return
        cargandoAmigos = true
        alcance.launch {
            val dia = LocalDate.now()
            // Pasos de cada día del mes: la cifra más alta entre Health Connect y el podómetro del móvil
            val deSalud = if (estadoSalud == Salud.Estado.CONECTADO) salud.pasosPorDia(dia.withDayOfMonth(1)) else emptyMap()
            val delMovil = if (podometroOn) podometro.pasosPorDia() else emptyMap()
            val pasos = (deSalud.keys + delMovil.keys).associateWith { maxOf(deSalud[it] ?: 0L, delMovil[it] ?: 0L) }
            val base = Competicion.miMarca(
                amigos.miembro, amigos.apodo, dia, dias, plan.kcal,
                sesionesE.filter { it.terminada }.map { Entrenador.fecha(it.inicio) }, pasos,
            )
            // El detalle (kilos perdidos y calorías de hoy) solo sale si no se ha apagado en Amigos
            val mia = if (!amigos.compartirDetalle) base else base.copy(
                kgPerdidos = Competicion.kgPerdidos(pesos), kcalHoy = (dias[dia.toString()]?.kcal ?: 0.0).toInt(),
                objetivoHoy = plan.kcal + ejercicio, momento = System.currentTimeMillis(),
            )
            miMarca = mia
            val publicado = amigos.publicar(mia, forzar)
            marcasGrupo = amigos.descargar(forzar)
            cargandoAmigos = false
            if (!publicado && forzar) aviso("No se han podido publicar tus cifras. Revisa la conexión.")
        }
    }

    // Al volver a la app se publican mis cifras si han cambiado (como mucho una vez por hora); se espera un poco a que cargue el diario
    LifecycleResumeEffect(grupo) {
        val trabajo = alcance.launch { kotlinx.coroutines.delay(4000); refrescarAmigos() }
        onPauseOrDispose { trabajo.cancel() }
    }

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
    // Recomendar recorre todas las recetas: se calcula solo cuando cambia algo, no en cada redibujado
    val ideasHoy = remember(recetas, deIa.size, perfil, proxima, pendiente, plan.kcal, restante) {
        if (pendiente == null) emptyList() else ideasDe(proxima, FiltrosIdeas())
    }
    val ideasPestana = remember(recetas, deIa.size, perfil, comidaIdea, filtros, plan.kcal, restante) { ideasDe(comidaIdea, filtros) }
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
            runCatching { Copia.exportar(contexto, uri, almacen, repo.diario, repo.entrenos) }
                .onSuccess { aviso("Copia guardada") }
                .onFailure { com.mikel.kcalma.datos.Registro.error("Exportar copia", it); aviso("No se pudo guardar la copia") }
        }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) alcance.launch {
            runCatching { Copia.importar(contexto, uri, almacen, repo.diario, repo.entrenos) }
                .onSuccess { ok -> aviso(if (ok) "Copia importada" else "Ese archivo no es una copia de Kcalma") }
                .onFailure { com.mikel.kcalma.datos.Registro.error("Importar copia", it); aviso("No se pudo leer la copia") }
        }
    }

    BackHandler(enabled = pila.isNotEmpty()) { pila.removeAt(pila.lastIndex) }
    // Al salir del día del historial se vuelve a hoy
    LaunchedEffect(pila.size) { if (pila.none { it is Destino.Dia }) diaPasado = null }
    BackHandler(enabled = pila.isEmpty() && pestana != Pestana.HOY) { pestana = Pestana.HOY }

    fun guardar(d: Destino.Ficha, gramos: Double, unidad: Unidad, cantidad: Double, cocinado: Cocinado) {
        alcance.launch(Dispatchers.IO) {
            if (d.entrada != null) repo.diario.actualizar(d.entrada.id, gramos, unidad, cantidad)
            else {
                repo.diario.anadir(fecha, d.comida, d.alimento, gramos, unidad, cantidad)
                // El aceite de cocinarlo va aparte, para poder quitarlo o corregirlo
                cocinado.aceite(gramos).takeIf { it > 0 }?.let { g -> repo.diario.anadir(fecha, d.comida, cocinado.alimentoAceite(), g, Unidad.GRAMOS, g) }
            }
        }
        // Vuelve a la comida para ver el resultado
        while (pila.lastOrNull().let { it is Destino.Ficha || it is Destino.Anadir || it is Destino.Escaner }) pila.removeAt(pila.lastIndex)
        if (pila.lastOrNull() != Destino.Comida(d.comida)) pila.add(Destino.Comida(d.comida))
    }

    /** El + de las listas: la cantidad de la última vez o, si nunca se apuntó, 1 ración o 100 g. */
    fun anadirRapido(a: Alimento, comida: ComidaDia) {
        alcance.launch(Dispatchers.IO) {
            val ultima = repo.diario.ultimaDe(a)
            val gramos = ultima?.gramos ?: a.gramosRapidos
            val unidad = ultima?.unidad ?: if (a.racionValida != null) Unidad.RACION else Unidad.GRAMOS
            val cantidad = ultima?.cantidad ?: if (unidad == Unidad.RACION) 1.0 else gramos
            repo.diario.anadir(fecha, comida, a, gramos, unidad, cantidad)
            withContext(Dispatchers.Main) {
                Toast.makeText(contexto, "Añadido a ${comida.nombre} · ${a.nutrientes(gramos).kcalInt} kcal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val ahora = { ComidaDia.segunHora(LocalTime.now().hour) }
    val accionesMas = AccionesMas(
        buscar = { pila.add(Destino.Anadir(ahora())) },
        voz = { pila.add(Destino.Dictar(ahora())) },
        foto = { pila.add(Destino.Foto(ahora())) },
        escanear = { pila.add(Destino.Escaner(ahora())) },
    )
    // Nombre de la pantalla actual para los informes de fallos
    // (nombres escritos a mano: en la release R8 cambia los nombres de las clases)
    val pantalla = when (val d = pila.lastOrNull()) {
        null -> when (pestana) { Pestana.HOY -> "Hoy"; Pestana.IDEAS -> "Ideas"; Pestana.ENTRENO -> "Entreno"; Pestana.PROGRESO -> "Progreso"; Pestana.PERFIL -> "Perfil" }
        is Destino.Comida -> "Comida (${d.comida.nombre})"
        is Destino.Anadir -> if (d.paraPlato) "Añadir ingrediente" else "Añadir alimento"
        is Destino.Ficha -> "Ficha de alimento (${d.alimento.nombre})"
        is Destino.Escaner -> "Escáner"
        is Destino.Etiqueta -> "Leer etiqueta"
        Destino.Plato -> "Plato casero"
        Destino.Lista -> "Lista de la compra"
        Destino.Recordatorios -> "Recordatorios"
        Destino.Apariencia -> "Apariencia"
        is Destino.Foto -> "Foto de comida"
        is Destino.Dictar -> "Dictar comida"
        is Destino.Editar -> "Editar perfil"
        Destino.Clave -> "Clave de Gemini"
        is Destino.VerIdea -> "Idea (${d.idea.receta.nombre})"
        is Destino.Rutina -> "Rutina ${d.id}"
        is Destino.Sesion -> "Sesión de entreno"
        is Destino.Ejercicio -> "Ejercicio ${d.id}"
        is Destino.EditarRutina -> "Editor de rutina"
        Destino.Biblioteca -> "Biblioteca de ejercicios"
        Destino.Historial -> "Historial"
        Destino.Amigos -> "Amigos"
        is Destino.Dia -> "Día del historial"
        Destino.Propuesta -> "Propuesta del entrenador"
        Destino.Chat -> "Chat con el entrenador"
    }
    if (informePerfil) HojaInforme(TipoEnvio.FALLO, "Desde Perfil", "Perfil", "") { informePerfil = false }
    CompositionLocalProvider(LocalAccionesMas provides accionesMas) {
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
    when (val d = pila.lastOrNull()) {
        null -> if (pestana == Pestana.IDEAS) PantallaIdeas(
            comida = comidaIdea,
            restante = restante,
            ideas = ideasPestana,
            filtros = filtros,
            estadoIa = estadoIa,
            onComida = { comidaIdeas = it },
            onFiltros = { filtros = it },
            onAbrir = { pila.add(Destino.VerIdea(it, comidaIdea)) },
            onMasIa = { pedirIa() },
            onPestana = cambiarPestana,
            onCamara = { pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour))) },
            enLista = estadoLista.items.count { !it.hecho },
            onLista = { pila.add(Destino.Lista) },
        ) else if (pestana == Pestana.PROGRESO) com.mikel.kcalma.ui.progreso.PantallaProgreso(
            perfil = perfil, pesos = pesos, dias = dias, onPestana = cambiarPestana,
            onHistorial = { pila.add(Destino.Historial) },
            onAmigos = { pila.add(Destino.Amigos) },
            onCamara = { pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour))) },
        ) else if (pestana == Pestana.ENTRENO) com.mikel.kcalma.ui.entreno.PantallaEntreno(
            seccion = seccion, rutinas = rutinas, sesiones = sesionesE, ajustes = ajustesE,
            onSeccion = { seccion = it },
            onAbrir = abrirRutina,
            onEmpezar = { empezar(it) },
            onContinuar = continuar,
            onCrear = { pila.add(Destino.EditarRutina(com.mikel.kcalma.dominio.Rutina("", "", seccion, emptyList()))) },
            onKettlebells = { kg -> enBd { entrenos.guardarKettlebells(kg) } },
            onBiblioteca = { pila.add(Destino.Biblioteca) },
            onChat = { pila.add(Destino.Chat) },
            entrenador = {
                com.mikel.kcalma.ui.entreno.TarjetaEntrenador(
                    propuestaIa, propuestaIa?.let { p -> p.pendientes.count { Cambios.validar(p.cambios[it], rutinas) != null } } ?: 0,
                    cargandoPropuesta, errorPropuesta, sesionesE.count { it.terminada },
                    onPedir = { pedirPropuesta() }, onVer = { pila.add(Destino.Propuesta) },
                )
            },
            onPestana = cambiarPestana,
            onCamara = { pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour))) },
        ) else if (pestana == Pestana.PERFIL) PantallaPerfil(
            perfil = perfil,
            pesos = pesos,
            claveGuardada = claveGuardada,
            tema = tema.nombre,
            podometro = when {
                !podometro.disponible -> "Sin sensor"
                podometroOn -> "Encendido · " + "%,d".format(java.util.Locale.US, podometro.pasosHoy()).replace(',', '.') + " hoy"
                else -> "Apagado"
            },
            amigos = if (grupo == null) "Sin grupo" else "Competición del mes",
            actividad = when (estadoSalud) {
                Salud.Estado.CONECTADO -> pasosHoy?.let { "%,d pasos hoy".format(java.util.Locale.US, it).replace(',', '.') } ?: "Conectado"
                Salud.Estado.SIN_PERMISO -> "Sin conectar"
                Salud.Estado.HAY_QUE_INSTALAR -> "Instala Health Connect"
                else -> "No disponible"
            },
            acciones = AccionesPerfil(
                onRecordatorios = { pila.add(Destino.Recordatorios) },
                onApariencia = { pila.add(Destino.Apariencia) },
                onPodometro = {
                    when {
                        !podometro.disponible -> aviso("Este móvil no tiene sensor de pasos")
                        podometroOn -> { podometro.desactivar(); podometroOn = false; aviso("Podómetro apagado") }
                        podometro.conPermiso() -> { podometro.activar(); podometroOn = true; ultimaSalud = 0; aviso("Podómetro encendido") }
                        else -> pedirPasos.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                },
                onAmigos = { pila.add(Destino.Amigos) },
                onActividad = {
                    when (estadoSalud) {
                        Salud.Estado.CONECTADO -> aviso("Conectado con Health Connect. Se suma la mitad de lo que camines de más.")
                        Salud.Estado.SIN_PERMISO -> runCatching { pedirSalud.launch(salud.permisos) }.onFailure { aviso("No se pudo abrir Health Connect") }
                        Salud.Estado.HAY_QUE_INSTALAR -> runCatching {
                            contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")))
                        }.onFailure { aviso("Instala Health Connect desde Google Play") }
                        else -> aviso("Este móvil no tiene Health Connect")
                    }
                },
                onEditar = { pila.add(Destino.Editar(it)) },
                onGuardarPeso = { kg -> alcance.launch { almacen.guardar(perfil.copy(pesoKg = kg), forzarPesada = true) }; aviso("Peso guardado") },
                onClave = { pila.add(Destino.Clave) },
                onBorrarClave = { claves.borrarGemini(); claveGuardada = false; aviso("Clave borrada") },
                onExportar = { exportar.launch("kcalma-copia-${LocalDate.now()}.json") },
                onImportar = { importar.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                onInforme = { informePerfil = true },
                onActualizar = {
                    aviso("Buscando actualizaciones…")
                    // De paso se baja ya la lista de alimentos de la comunidad, sin esperar al día siguiente
                    alcance.launch { com.mikel.kcalma.datos.Comunidad.sincronizar(contexto.applicationContext, repo.diario, forzar = true) }
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
            onHistorial = { pila.add(Destino.Historial) },
            perfil = perfil,
            entradas = entradas,
            onAbrirComida = { c -> pila.add(if (entradas.any { it.comida == c }) Destino.Comida(c) else Destino.Anadir(c)) },
            onAnadir = { c -> pila.add(Destino.Anadir(c)) },
            onCamara = { pila.add(Destino.Foto(ComidaDia.segunHora(LocalTime.now().hour))) },
            onPestana = cambiarPestana,
            onPerfil = { pestana = Pestana.PERFIL },
            onAvisos = { pila.add(Destino.Recordatorios) },
            entreno = if (com.mikel.kcalma.ui.entreno.hayEntrenoHoy(rutinas, sesionesE)) {
                { com.mikel.kcalma.ui.entreno.TarjetaEntrenoHoy(rutinas, sesionesE, abrirRutina, { empezar(it) }, continuar, mapa = false) }
            } else null,
            ideas = ideasHoy,
            tituloIdeas = "Ideas para ${if (proxima == ComidaDia.MEDIA_MANANA) "" else "la "}${proxima.nombre.lowercase()}",
            onIdea = { pila.add(Destino.VerIdea(it, proxima)) },
            onVerIdeas = { comidaIdeas = null; pestana = Pestana.IDEAS },
            ejercicio = ejercicio,
            detalleEjercicio = pasosHoy?.takeIf { ejercicio > 0 }?.let { p ->
                "👟 ${"%,d".format(java.util.Locale.US, p).replace(',', '.')} pasos · sumamos la mitad de lo que caminas de más"
            },
            tarjeta = propuesta?.let { p ->
                {
                    TarjetaAjuste(
                        p,
                        onAceptar = { alcance.launch { almacen.responderPropuesta(p.nuevaCorreccion) }; aviso("Plan cambiado a ${p.kcalNueva} kcal/día") },
                        onAhoraNo = { alcance.launch { almacen.responderPropuesta(null) } },
                    )
                }
            },
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
        is Destino.Rutina -> {
            val r = rutinas.firstOrNull { it.id == d.id }
            if (r == null) LaunchedEffect(d) { if (rutinas.isNotEmpty()) pila.remove(d) }
            else com.mikel.kcalma.ui.entreno.PantallaRutina(
                r = r, sesiones = sesionesE,
                onAtras = { pila.removeAt(pila.lastIndex) },
                onEmpezar = { empezar(r) },
                onEditar = { pila.add(Destino.EditarRutina(r)) },
                onBorrar = {
                    enBd { entrenos.borrarRutina(r.id) }
                    if (!r.predefinida) sesionesE.filter { !it.terminada && it.rutinaId == r.id }.forEach { x -> enBd { entrenos.terminar(x.id) } }
                    if (!r.predefinida) { pila.removeAt(pila.lastIndex); aviso("Rutina borrada") } else aviso("Rutina como la original")
                },
                onDias = { dias -> enBd { entrenos.guardarRutina(r.copy(dias = dias)) } },
                onEjercicio = { pila.add(Destino.Ejercicio(it.id)) },
            )
        }
        is Destino.EditarRutina -> com.mikel.kcalma.ui.entreno.PantallaEditorRutina(
            inicial = d.rutina,
            onAtras = { pila.removeAt(pila.lastIndex) },
            onGuardar = { nueva ->
                alcance.launch {
                    val id = withContext(hiloEntreno) { entrenos.guardarRutina(nueva) }
                    pila.remove(d)
                    if (d.rutina.id.isEmpty()) pila.add(Destino.Rutina(id))
                    aviso("Rutina guardada")
                }
            },
        )
        Destino.Propuesta -> propuestaIa?.let { p ->
            com.mikel.kcalma.ui.entreno.PantallaPropuesta(
                propuesta = p, rutinas = rutinas,
                datos = remember(sesionesE, rutinas, perfil) { Entrenador.estadisticas(sesionesE, rutinas, perfil) },
                onAtras = { pila.remove(d) },
                onAceptar = { ids ->
                    aplicarCambios(ids.map { p.cambios[it] }); resolver(ids)
                    aviso(if (ids.size == 1) "Rutina cambiada" else "${ids.size} cambios hechos")
                },
                onDescartar = { resolver(it) },
            )
        } ?: LaunchedEffect(d) { pila.remove(d) }
        Destino.Chat -> com.mikel.kcalma.ui.entreno.PantallaChat(
            mensajes = chat, pensando = pensando, error = errorChat, rutinas = rutinas,
            onAtras = { pila.remove(d) },
            onEnviar = { preguntar(it) },
            onCambio = { i, aceptar ->
                val m = chat[i]
                val cambio = m.cambio
                if (cambio != null) {
                    if (!aceptar) chat[i] = m.copy(estado = 2)
                    else if (Cambios.validar(cambio, rutinas) == null) { aviso("Ese cambio ya no se puede aplicar"); chat[i] = m.copy(estado = 2) }
                    else { aplicarCambios(listOf(cambio)); chat[i] = m.copy(estado = 1); aviso("Rutina cambiada") }
                }
            },
        )
        Destino.Amigos -> {
            LaunchedEffect(grupo) { refrescarAmigos() }
            com.mikel.kcalma.ui.progreso.PantallaAmigos(
                codigo = grupo, apodoInicial = amigos.apodo, mia = miMarca, marcas = marcasGrupo, cargando = cargandoAmigos,
                puedePublicar = com.mikel.kcalma.datos.GitHub.hayToken,
                compartirDetalle = compartirDetalle,
                onCompartirDetalle = { amigos.compartirDetalle = it; compartirDetalle = it; refrescarAmigos(forzar = true) },
                onCrear = { apodo -> grupo = amigos.crear(apodo); miMarca = null; marcasGrupo = emptyMap() },
                onUnirse = { codigo, apodo -> amigos.unirse(codigo, apodo).also { if (it) { grupo = amigos.codigo; miMarca = null; marcasGrupo = emptyMap() } } },
                onActualizar = { refrescarAmigos(forzar = true) },
                onCompartir = { codigo ->
                    val i = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "Únete a mi grupo de Kcalma. En Progreso › Amigos pon este código: $codigo")
                    contexto.startActivity(Intent.createChooser(i, "Compartir código"))
                },
                onSalir = { amigos.salir(); grupo = null; miMarca = null; marcasGrupo = emptyMap() },
                onAtras = { pila.remove(d) },
            )
        }
        Destino.Historial -> com.mikel.kcalma.ui.progreso.PantallaHistorial(
            dias = dias, pesos = pesos, planKcal = plan.kcal,
            onAtras = { pila.remove(d) },
            onDia = { f -> diaPasado = f; pila.add(Destino.Dia(f)) },
        )
        is Destino.Dia -> com.mikel.kcalma.ui.progreso.PantallaDia(
            fecha = d.fecha, entradas = if (fecha == d.fecha) entradas else emptyList(), planKcal = plan.kcal,
            peso = pesos.firstOrNull { it.fecha == d.fecha }?.kg,
            onAtras = { pila.remove(d) },
            onComida = { c -> pila.add(Destino.Comida(c)) },
            onAnadir = { c -> pila.add(Destino.Anadir(c)) },
            onPeso = { kg -> alcance.launch { almacen.guardarPesada(d.fecha, kg) }; aviso("Peso guardado") },
        )
        Destino.Biblioteca -> com.mikel.kcalma.ui.entreno.PantallaBiblioteca(
            onAtras = { pila.removeAt(pila.lastIndex) },
            onEjercicio = { pila.add(Destino.Ejercicio(it.id)) },
        )
        is Destino.Ejercicio -> com.mikel.kcalma.dominio.Ejercicios.porId(d.id)?.let { ej ->
            com.mikel.kcalma.ui.entreno.PantallaFichaEjercicio(ej, sesionesE) { pila.removeAt(pila.lastIndex) }
        } ?: LaunchedEffect(d) { pila.remove(d) }
        is Destino.Sesion -> {
            val s = sesionesE.firstOrNull { it.id == d.id }
            val r = s?.let { x -> rutinas.firstOrNull { it.id == x.rutinaId } ?: com.mikel.kcalma.dominio.Rutinas.predefinidas.firstOrNull { it.id == x.rutinaId } }
            if (s == null || r == null || s.terminada) {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().then(androidx.compose.ui.Modifier.fondoKcalma()))
                // La sesión tarda un instante en llegar de la base de datos; si no existe o ya terminó, se cierra
                LaunchedEffect(d, sesionesE, rutinas) {
                    kotlinx.coroutines.delay(400)
                    val x = sesionesE.firstOrNull { it.id == d.id }
                    // Sin su rutina (borrada o no está en una copia) se cierra guardando lo marcado; si no, quedaría a medias para siempre
                    if (x != null && !x.terminada && r == null) enBd { entrenos.terminar(x.id) }
                    if (x == null || x.terminada || r == null) pila.remove(d)
                }
            } else com.mikel.kcalma.ui.entreno.PantallaSesion(
                sesion = s, rutina = r, sesiones = sesionesE, ajustes = ajustesE,
                onSerie = { serie -> enBd { entrenos.guardarSerie(s.id, serie) } },
                onTerminar = { enBd { entrenos.terminar(s.id) }; pila.remove(d); aviso("Entreno guardado") },
                onDescartar = { enBd { entrenos.descartar(s.id) }; pila.remove(d) },
                onSalir = { pila.remove(d) },
                onFicha = { pila.add(Destino.Ejercicio(it.id)) },
            )
        }
        is Destino.Comida -> PantallaComida(
            comida = d.comida,
            entradas = entradas.filter { it.comida == d.comida },
            onAtras = { pila.removeAt(pila.lastIndex) },
            onBuscar = { pila.add(Destino.Anadir(d.comida)) },
            onEscanear = { pila.add(Destino.Escaner(d.comida)) },
            onEditar = { e -> pila.add(Destino.Ficha(e.alimento, d.comida, e)) },
            onBorrar = { e -> alcance.launch(Dispatchers.IO) { repo.diario.borrar(e.id) } },
            onDeshacer = { e -> alcance.launch(Dispatchers.IO) { repo.diario.anadir(e.fecha, e.comida, e.alimento, e.gramos, e.unidad, e.cantidad) } },
            onGuardarHabitual = { nombre ->
                val lista = entradas.filter { it.comida == d.comida }
                alcance.launch(Dispatchers.IO) { repo.diario.guardarHabitual(nombre, lista) }
            },
            onFoto = { pila.add(Destino.Foto(d.comida)) },
            onMover = { lista, destino ->
                alcance.launch(Dispatchers.IO) { repo.diario.mover(lista.map { it.id }, destino) }
                val todas = lista.size == entradas.count { it.comida == d.comida }
                aviso(if (todas) "Movido todo a ${destino.nombre}" else "Movido a ${destino.nombre}")
                // Si se ha movido todo, se va a la comida de destino
                if (todas) { pila.removeAt(pila.lastIndex); pila.add(Destino.Comida(destino)) }
            },
        )
        is Destino.Anadir -> if (d.paraPlato) PantallaAnadir(
            comida = d.comida,
            supermercados = supermercados,
            repo = repo,
            onCambiarComida = {},
            onCerrar = { pila.removeAt(pila.lastIndex) },
            onAbrir = { a -> pila.add(Destino.Ficha(a, d.comida, paraPlato = true)) },
            onAnadirRapido = { a -> anadirIngrediente(a, a.gramosRapidos) },
            onHabitual = { h -> h.entradas.forEach { e -> edicion?.ingredientes?.add(Ingrediente(e.alimento, e.gramos)) }; volverAPlato() },
            onEscanear = { pila.add(Destino.Escaner(d.comida, paraPlato = true)) },
            titulo = "Ingrediente",
            selectorComida = false,
        ) else PantallaAnadir(
            comida = d.comida,
            supermercados = supermercados,
            repo = repo,
            empezarPorVoz = d.voz,
            onVozLanzada = { if (pila.lastOrNull() == d) pila[pila.lastIndex] = d.copy(voz = false) },
            onCambiarComida = { c -> pila[pila.lastIndex] = Destino.Anadir(c) },
            onCerrar = { pila.removeAt(pila.lastIndex) },
            onAbrir = { a -> pila.add(Destino.Ficha(a, d.comida)) },
            onAnadirRapido = { a -> anadirRapido(a, d.comida) },
            onAbrirCon = { a, s -> pila.add(Destino.Ficha(a, d.comida, sugerida = s)) },
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
            onAnadirEntrada = { e ->
                alcance.launch(Dispatchers.IO) { repo.diario.anadir(fecha, d.comida, e.alimento, e.gramos, e.unidad, e.cantidad) }
                aviso("Añadido a ${d.comida.nombre} · ${e.nutrientes.kcalInt} kcal")
            },
            accionesPlatos = AccionesPlatos(
                onAbrir = { p -> pila.add(Destino.Ficha(p.alimento, d.comida)) },
                onAnadir = { p -> anadirRapido(p.alimento, d.comida) },
                onEditar = { p -> edicion = p?.let { EdicionPlato.de(it) } ?: EdicionPlato(); pila.add(Destino.Plato) },
            ),
        )
        is Destino.Ficha -> FichaAlimento(
            alimento = d.alimento,
            comida = d.comida,
            // Al editar, lo que ya estaba apuntado no cuenta como comido
            restante = if (d.entrada != null) restante + d.entrada.nutrientes else restante,
            inicial = d.entrada,
            onAtras = { pila.removeAt(pila.lastIndex) },
            onGuardar = { g, u, c, coc -> if (d.paraPlato) anadirIngrediente(d.alimento, g) else guardar(d, g, u, c, coc) },
            conCocinado = !d.paraPlato,
            onDenunciar = if (d.alimento.supermercado == com.mikel.kcalma.dominio.Compartidos.ORIGEN) ({ motivo ->
                alcance.launch {
                    val ok = com.mikel.kcalma.datos.Comunidad.denunciar(repo.diario, d.alimento, motivo)
                    aviso(if (ok) "Gracias: se retira para todos hasta revisarlo" else "Quitado de tu móvil. No se pudo avisar a los demás.")
                }
                pila.removeAt(pila.lastIndex)
            }) else null,
            sugerida = d.sugerida ?: remember(d) {
                if (d.entrada != null || d.paraPlato) null
                else repo.diario.ultimaDe(d.alimento)?.let { "↺ La última vez" to it }
            },
        )
        is Destino.Escaner -> Escaner(
            buscar = { repo.porCodigo(it) },
            onCerrar = { pila.removeAt(pila.lastIndex) },
            onVer = { a -> pila.add(Destino.Ficha(a, d.comida, paraPlato = d.paraPlato)) },
            onBuscarAMano = {
                pila.removeAt(pila.lastIndex)
                if (pila.lastOrNull() != Destino.Anadir(d.comida, d.paraPlato)) pila.add(Destino.Anadir(d.comida, d.paraPlato))
            },
            onEtiqueta = { codigo -> pila[pila.lastIndex] = Destino.Etiqueta(d.comida, codigo, d.paraPlato) },
        )
        is Destino.Etiqueta -> FlujoEtiqueta(
            codigo = d.codigo,
            onCerrar = { pila.removeAt(pila.lastIndex) },
            onSinClave = { pila.add(Destino.Clave) },
            onGuardado = { a ->
                alcance.launch(Dispatchers.IO) {
                    if (a.codigoBarras != null) {
                        repo.diario.guardarProducto(a)
                        // Y se comparte: el robot de GitHub lo valida y lo publica para los demás usuarios
                        com.mikel.kcalma.datos.Comunidad.enviar(a)
                    }
                }
                aviso(if (a.codigoBarras != null && com.mikel.kcalma.datos.GitHub.hayToken) "Guardado y compartido con los demás usuarios" else "Guardado: lo reconocerá la próxima vez que lo escanees")
                pila[pila.lastIndex] = Destino.Ficha(a, d.comida, paraPlato = d.paraPlato)
            },
        )
        Destino.Plato -> edicion?.let { e ->
            PantallaPlato(
                e,
                onAtras = { pila.removeAt(pila.lastIndex) },
                onAnadirIngrediente = { pila.add(Destino.Anadir(ComidaDia.COMIDA, paraPlato = true)) },
                onGuardar = {
                    val p = e.plato()
                    alcance.launch(Dispatchers.IO) { repo.diario.guardarPlato(p) }
                    aviso("«${p.nombre}» guardado en Mis platos")
                    pila.removeAt(pila.lastIndex)
                },
                onBorrar = if (e.id > 0) ({
                    alcance.launch(Dispatchers.IO) { repo.diario.borrarPlato(e.id) }
                    aviso("Plato borrado")
                    pila.removeAt(pila.lastIndex)
                }) else null,
            )
        }
        Destino.Lista -> PantallaLista(
            estadoLista,
            onAtras = { pila.removeAt(pila.lastIndex) },
            onMarcar = { lista.marcar(it) },
            onVaciar = { lista.vaciar() },
            onCompartir = {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, lista.texto())
                contexto.startActivity(Intent.createChooser(i, "Compartir lista"))
            },
        )
        Destino.Recordatorios -> PantallaRecordatorios(onAtras = { pila.removeAt(pila.lastIndex) })
        Destino.Apariencia -> PantallaApariencia(
            tema = tema, animaciones = animaciones,
            onTema = { alcance.launch { almacen.guardarTema(it.name) } },
            onAnimaciones = { alcance.launch { almacen.guardarAnimaciones(it) } },
            onAtras = { pila.removeAt(pila.lastIndex) },
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
        is Destino.Dictar -> FlujoFoto(
            comidaInicial = d.comida,
            repo = repo,
            supermercados = supermercados,
            dictar = true,
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
            onLista = { lista.anadir(d.idea.receta); aviso("Ingredientes añadidos a la lista de la compra") },
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
    // Burbujas 💡 y 🐞 encima de todo, abajo a la derecha por encima de la barra
    BurbujasInforme(
        pantalla,
        androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomEnd).navigationBarsPadding()
            .padding(end = 14.dp, bottom = if (pila.isEmpty()) 96.dp else if (pila.last() == Destino.Chat) 150.dp else 90.dp),
    )
    }
    }
}
