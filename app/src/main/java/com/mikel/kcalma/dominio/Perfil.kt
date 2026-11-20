package com.mikel.kcalma.dominio

enum class Sexo { HOMBRE, MUJER }

/** Actividad en el trabajo o el día a día, sin contar el ejercicio. */
enum class Trabajo(val factor: Double) { SENTADO(1.20), DE_PIE(1.30), MOVIENDOSE(1.40), FISICO(1.55) }

/** Pasos diarios: se suman al factor del trabajo. */
enum class Pasos(val extra: Double) { MENOS_5K(0.0), DE_5_A_8K(0.15), DE_8_A_12K(0.25), MAS_12K(0.35) }

/** MET medio de cada tipo de ejercicio. */
enum class TipoEjercicio(val met: Double) { PESAS(5.0), CARDIO(8.0), DEPORTE(7.0) }

enum class Duracion(val horas: Double) { MEDIA_HORA(0.5), UNA_HORA(1.0), HORA_Y_MEDIA(1.5) }

enum class Objetivo { PERDER, RECOMPOSICION, GANAR }

enum class Ritmo { SUAVE, NORMAL, RAPIDO }

/** Los 14 alérgenos de declaración obligatoria en la UE (Reglamento 1169/2011). */
val ALERGENOS = listOf(
    "Gluten", "Crustáceos", "Huevo", "Pescado", "Cacahuete", "Soja", "Lactosa",
    "Frutos secos", "Apio", "Mostaza", "Sésamo", "Sulfitos", "Altramuces", "Moluscos",
)

val SUPERMERCADOS = listOf("Mercadona", "Lidl", "Carrefour", "Dia", "Aldi", "Consum", "Eroski", "Alcampo", "Ahorramás")

data class Perfil(
    val sexo: Sexo = Sexo.HOMBRE,
    val edad: Int = 30,
    val alturaCm: Int = 175,
    val pesoKg: Double = 80.0,
    val pesoObjetivoKg: Double = 75.0,
    val grasaPct: Double? = null,
    val trabajo: Trabajo = Trabajo.SENTADO,
    val pasos: Pasos = Pasos.DE_5_A_8K,
    val diasEjercicio: Int = 3,
    val tipoEjercicio: TipoEjercicio = TipoEjercicio.PESAS,
    val duracion: Duracion = Duracion.UNA_HORA,
    val objetivo: Objetivo = Objetivo.PERDER,
    val ritmo: Ritmo = Ritmo.NORMAL,
    val alergias: Set<String> = emptySet(),
    val noMeGusta: Set<String> = emptySet(),
    val meEncanta: Set<String> = emptySet(),
    val supermercados: Set<String> = emptySet(),
    /** Calorías puestas a mano por el usuario; null = las calculadas. */
    val kcalManual: Int? = null,
)

/** Una pesada con su fecha (aaaa-mm-dd). */
data class Pesada(val fecha: String, val kg: Double)
