package com.mikel.kcalma.ui.foto

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.mikel.kcalma.datos.Claves
import com.mikel.kcalma.datos.Gemini
import com.mikel.kcalma.datos.Reconocido
import com.mikel.kcalma.datos.Repositorio
import com.mikel.kcalma.datos.Respuesta
import com.mikel.kcalma.dominio.Alimento
import com.mikel.kcalma.dominio.ComidaDia
import com.mikel.kcalma.ui.diario.PantallaAnadir
import kotlinx.coroutines.launch

/** Un alimento del plato, ya emparejado con la base de datos (o con la estimación de la IA). */
data class ItemFoto(
    val id: Int,
    val alimento: Alimento,
    val gramos: Double,
    val confianza: String,
    val deLaNota: Boolean = false,
    val estimado: Boolean = false,
)

private enum class Paso { CLAVE, CAMARA, ANALIZANDO, RESULTADO, BUSCAR }

/**
 * Foto con IA: clave (solo la primera vez), foto con nota, análisis y revisión.
 * [onAnadir] recibe los alimentos y gramos finales.
 */
@Composable
fun FlujoFoto(
    comidaInicial: ComidaDia,
    repo: Repositorio,
    supermercados: List<String>,
    onCerrar: () -> Unit,
    onAnadir: (ComidaDia, List<ItemFoto>) -> Unit,
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val claves = remember { Claves(contexto.applicationContext) }
    var paso by remember { mutableStateOf(if (claves.gemini() == null) Paso.CLAVE else Paso.CAMARA) }
    var comida by remember { mutableStateOf(comidaInicial) }
    var nota by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var avance by remember { mutableStateOf(0) } // 0 reconociendo · 1 cantidades · 2 base de datos
    val items = remember { mutableStateListOf<ItemFoto>() }
    var editando by remember { mutableStateOf<Int?>(null) } // id del item que se cambia; null = añadir uno nuevo
    var siguienteId by remember { mutableStateOf(0) }

    fun analizar(jpeg: ByteArray) {
        val clave = claves.gemini() ?: run { paso = Paso.CLAVE; return }
        error = null
        avance = 0
        paso = Paso.ANALIZANDO
        alcance.launch {
            when (val r = Gemini.analizar(clave, jpeg, nota)) {
                is Respuesta.Ok -> {
                    avance = 2
                    items.clear()
                    r.valor.forEach { rec -> items += emparejar(rec, repo, siguienteId++) }
                    paso = Paso.RESULTADO
                }
                Respuesta.ClaveMala -> { error = "Google no acepta la clave. Revísala o crea otra."; paso = Paso.CLAVE }
                Respuesta.Limite -> error = "Has llegado al límite gratuito de Gemini por ahora. Espera un minuto y vuelve a intentarlo."
                Respuesta.SinConexion -> error = "Sin conexión. La foto con IA necesita internet."
                is Respuesta.Fallo -> error = "Gemini no ha podido analizar la foto (${r.detalle}). Prueba otra vez."
            }
        }
    }

    BackHandler {
        when (paso) {
            Paso.BUSCAR -> paso = Paso.RESULTADO
            Paso.RESULTADO, Paso.ANALIZANDO -> paso = Paso.CAMARA
            else -> onCerrar()
        }
    }

    when (paso) {
        Paso.CLAVE -> PantallaClave(
            errorInicial = error,
            onCerrar = onCerrar,
            onAbrirWeb = {
                contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
            },
            probar = { clave -> Gemini.probarClave(clave) },
            onGuardada = { clave -> claves.guardarGemini(clave); error = null; paso = Paso.CAMARA },
        )
        Paso.CAMARA -> PantallaCamara(
            comida = comida,
            nota = nota,
            onNota = { nota = it },
            onComida = { comida = it },
            onCerrar = onCerrar,
            onFoto = { bitmap, jpeg -> foto = bitmap; analizar(jpeg) },
        )
        Paso.ANALIZANDO -> PantallaAnalizando(
            foto = foto,
            avance = avance,
            error = error,
            onCerrar = { paso = Paso.CAMARA },
            onReintentar = { foto?.let { analizar(prepararFoto(it).second) } },
        )
        Paso.RESULTADO -> PantallaResultado(
            comida = comida,
            items = items,
            onComida = { comida = it },
            onAtras = { paso = Paso.CAMARA },
            onGramos = { id, g -> items.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { items[it] = items[it].copy(gramos = g) } },
            onQuitar = { id -> items.removeAll { it.id == id } },
            onCambiar = { id -> editando = id; paso = Paso.BUSCAR },
            onAnadirOtro = { editando = null; paso = Paso.BUSCAR },
            onConfirmar = { onAnadir(comida, items.toList()) },
        )
        Paso.BUSCAR -> {
            fun elegir(a: Alimento) {
                val i = items.indexOfFirst { it.id == editando }
                if (i >= 0) items[i] = items[i].copy(alimento = a, confianza = "alta", estimado = false)
                else items += ItemFoto(siguienteId++, a, a.gramosRapidos, "alta")
                paso = Paso.RESULTADO
            }
            PantallaAnadir(
                comida = comida,
                supermercados = supermercados,
                repo = repo,
                onCambiarComida = {},
                onCerrar = { paso = Paso.RESULTADO },
                onAbrir = ::elegir,
                onAnadirRapido = ::elegir,
                onHabitual = { h ->
                    h.entradas.forEach { e -> items += ItemFoto(siguienteId++, e.alimento, e.gramos, "alta") }
                    paso = Paso.RESULTADO
                },
                onEscanear = null,
                titulo = if (editando != null) "Cambiar" else "Añadir",
                selectorComida = false,
            )
        }
    }
}

/**
 * Busca el alimento reconocido en la base (genéricos primero). Si no aparece, o lo que aparece
 * tiene unas calorías muy distintas de lo esperado (p. ej. «caldo de pollo» por «pollo»), se usa la
 * estimación de la IA marcada como estimada.
 */
private suspend fun emparejar(r: Reconocido, repo: Repositorio, id: Int): ItemFoto {
    val candidato = repo.buscar(r.busqueda, "").firstOrNull()
        ?: repo.buscar(r.nombre, "").firstOrNull()
        ?: repo.buscar(r.busqueda, null).firstOrNull()
    val razonable = candidato != null && (r.kcal100 < 30 || candidato.kcal < 30 ||
        candidato.kcal / r.kcal100 in 0.45..2.2)
    return if (candidato != null && razonable) {
        ItemFoto(id, candidato, r.gramos, r.confianza, r.deLaNota)
    } else {
        val estimado = Alimento(
            nombre = r.nombre, marca = "Estimado por IA",
            kcal = r.kcal100, proteinas = r.proteinas100, hidratos = r.hidratos100, grasas = r.grasas100,
        )
        ItemFoto(id, estimado, r.gramos, r.confianza, r.deLaNota, estimado = true)
    }
}
