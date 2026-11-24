package com.mikel.kcalma.dominio

import com.mikel.kcalma.dominio.Material.BARRA
import com.mikel.kcalma.dominio.Material.CUERPO
import com.mikel.kcalma.dominio.Material.KETTLEBELL
import com.mikel.kcalma.dominio.Material.MANCUERNAS
import com.mikel.kcalma.dominio.Material.MAQUINA
import com.mikel.kcalma.dominio.Material.POLEA
import com.mikel.kcalma.dominio.Musculo.ABDOMEN
import com.mikel.kcalma.dominio.Musculo.ADUCTORES
import com.mikel.kcalma.dominio.Musculo.ANTEBRAZO
import com.mikel.kcalma.dominio.Musculo.BICEPS
import com.mikel.kcalma.dominio.Musculo.CUADRICEPS
import com.mikel.kcalma.dominio.Musculo.DORSAL
import com.mikel.kcalma.dominio.Musculo.GEMELOS
import com.mikel.kcalma.dominio.Musculo.GLUTEO
import com.mikel.kcalma.dominio.Musculo.HOMBRO
import com.mikel.kcalma.dominio.Musculo.ISQUIOS
import com.mikel.kcalma.dominio.Musculo.LUMBAR
import com.mikel.kcalma.dominio.Musculo.OBLICUOS
import com.mikel.kcalma.dominio.Musculo.PECHO
import com.mikel.kcalma.dominio.Musculo.TRAPECIO
import com.mikel.kcalma.dominio.Musculo.TRICEPS

/** Catálogo de ejercicios. Los ids son para siempre: el historial los guarda. */
object Ejercicios {
    val todos: List<Ejercicio> get() = TODOS
    private val TODOS: List<Ejercicio> by lazy { BASE + EjerciciosMas.lista }
    private val BASE: List<Ejercicio> = listOf(
        // ---------- Pierna ----------
        Ejercicio(
            "sentadilla", "Sentadilla", BARRA, Patron.SENTADILLA, setOf(CUADRICEPS, GLUTEO), setOf(ADUCTORES, ISQUIOS, LUMBAR),
            listOf(
                "Barra sobre los trapecios," to "pies a la anchura de los hombros y las puntas algo abiertas.",
                "Coge aire y aprieta el abdomen" to "antes de bajar.",
                "Baja sentándote entre los talones" to "hasta que el muslo quede paralelo al suelo o algo más.",
                "Sube empujando el suelo" to "con todo el pie, las rodillas en la línea de las puntas.",
            ),
            "Dejar que las rodillas se vayan hacia dentro al subir.", salto = 2.5,
        ),
        Ejercicio(
            "peso_muerto", "Peso muerto", BARRA, Patron.BISAGRA, setOf(GLUTEO, ISQUIOS, LUMBAR), setOf(DORSAL, TRAPECIO, ANTEBRAZO, CUADRICEPS),
            listOf(
                "La barra sobre la mitad del pie," to "pegada a las espinillas.",
                "Agarra justo por fuera de las piernas" to "con la espalda recta y el pecho arriba.",
                "Empuja el suelo y lleva la cadera adelante" to "con la barra rozando las piernas.",
                "Baja igual que subiste," to "primero la cadera atrás y luego las rodillas.",
            ),
            "Redondear la espalda baja para levantar la barra.", salto = 5.0,
        ),
        Ejercicio(
            "peso_muerto_rumano", "Peso muerto rumano", BARRA, Patron.BISAGRA, setOf(ISQUIOS, GLUTEO), setOf(LUMBAR, ANTEBRAZO),
            listOf(
                "De pie con la barra en las manos," to "rodillas apenas dobladas.",
                "Echa la cadera atrás" to "con la espalda recta; la barra baja pegada a los muslos.",
                "Para cuando notes tirar los isquios," to "normalmente justo por debajo de la rodilla.",
                "Vuelve llevando la cadera adelante" to "y aprieta los glúteos arriba.",
            ),
            "Convertirlo en sentadilla doblando mucho las rodillas.",
        ),
        Ejercicio(
            "zancadas", "Zancadas", MANCUERNAS, Patron.ZANCADA, setOf(CUADRICEPS, GLUTEO), setOf(ADUCTORES, ISQUIOS),
            listOf(
                "Una mancuerna en cada mano," to "de pie y con el tronco recto.",
                "Da un paso largo adelante" to "y baja hasta que la rodilla de atrás casi toque el suelo.",
                "La rodilla de delante," to "encima del tobillo, sin pasarse mucho de la punta.",
                "Empuja con el talón de delante" to "para volver y cambia de pierna.",
            ),
            "Dar pasos cortos: todo el trabajo se va a la rodilla.", salto = 2.0, porLado = true,
        ),
        Ejercicio(
            "curl_femoral", "Curl femoral", MAQUINA, Patron.CURL_PIERNA, setOf(ISQUIOS), setOf(GEMELOS),
            listOf(
                "Túmbate boca abajo" to "con el rodillo justo encima de los talones.",
                "Dobla las rodillas" to "llevando los talones hacia los glúteos.",
                "Aprieta un segundo arriba" to "sin levantar la cadera del banco.",
                "Baja despacio," to "en unos dos o tres segundos.",
            ),
            "Levantar la cadera para ayudarte.", salto = 5.0,
        ),
        Ejercicio(
            "hip_thrust", "Hip thrust", BARRA, Patron.PUENTE, setOf(GLUTEO), setOf(ISQUIOS, CUADRICEPS),
            listOf(
                "La espalda alta apoyada en un banco" to "y la barra sobre la cadera, con almohadilla.",
                "Pies planos," to "a la anchura de la cadera; las espinillas verticales arriba.",
                "Sube la cadera" to "hasta que el tronco quede recto, mirando al frente.",
                "Aprieta los glúteos arriba" to "y baja controlando.",
            ),
            "Arquear la espalda baja en vez de extender la cadera.", salto = 5.0,
        ),
        Ejercicio(
            "gemelos_pie", "Gemelos de pie", MAQUINA, Patron.GEMELO, setOf(GEMELOS),
            pasos = listOf(
                "La punta de los pies en el borde," to "los talones al aire.",
                "Sube lo más alto que puedas" to "y aguanta un segundo arriba.",
                "Baja hasta estirar del todo" to "el gemelo, sin rebotar.",
            ),
            error = "Rebotar abajo y hacer medio recorrido.", salto = 5.0,
        ),
        // ---------- Empuje ----------
        Ejercicio(
            "press_banca", "Press de banca", BARRA, Patron.PRESS_BANCA, setOf(PECHO), setOf(TRICEPS, HOMBRO),
            listOf(
                "Túmbate con los ojos bajo la barra" to "y los omóplatos juntos y hacia abajo.",
                "Agarre algo más ancho que los hombros," to "muñecas rectas.",
                "Baja la barra a la parte baja del pecho" to "con los codos a unos 45°.",
                "Empuja hacia arriba y algo atrás" to "hasta estirar los brazos.",
            ),
            "Abrir los codos a 90°: carga el hombro.",
        ),
        Ejercicio(
            "press_inclinado", "Press inclinado con mancuernas", MANCUERNAS, Patron.PRESS_BANCA, setOf(PECHO, HOMBRO), setOf(TRICEPS),
            listOf(
                "Banco a unos 30°," to "una mancuerna en cada mano a la altura del pecho.",
                "Empuja hacia arriba" to "juntando un poco las mancuernas arriba.",
                "Baja despacio" to "hasta notar estirar el pecho.",
            ),
            "Poner el banco muy vertical: pasa a ser press de hombro.", salto = 2.0,
        ),
        Ejercicio(
            "press_militar", "Press militar", BARRA, Patron.PRESS_VERTICAL, setOf(HOMBRO), setOf(TRICEPS, TRAPECIO, ABDOMEN),
            listOf(
                "De pie, la barra sobre la clavícula" to "y agarre a la anchura de los hombros.",
                "Aprieta glúteos y abdomen" to "para no arquear la espalda.",
                "Empuja la barra en línea recta" to "apartando la cabeza y vuelve a meterla debajo arriba.",
                "Baja controlando" to "hasta la clavícula.",
            ),
            "Echar el tronco atrás y convertirlo en press inclinado.",
        ),
        Ejercicio(
            "triceps_polea", "Extensión de tríceps en polea", POLEA, Patron.TRICEPS, setOf(TRICEPS),
            pasos = listOf(
                "De pie frente a la polea," to "codos pegados al cuerpo.",
                "Estira los brazos hacia abajo" to "moviendo solo el antebrazo.",
                "Vuelve despacio" to "hasta que el antebrazo pase de la horizontal.",
            ),
            error = "Separar los codos del cuerpo para mover más peso.", salto = 2.5,
        ),
        Ejercicio(
            "flexiones", "Flexiones", CUERPO, Patron.FLEXION, setOf(PECHO, TRICEPS), setOf(HOMBRO, ABDOMEN),
            listOf(
                "Manos algo más abiertas que los hombros," to "el cuerpo en línea de la cabeza a los talones.",
                "Baja el pecho hasta casi tocar el suelo" to "con los codos a unos 45°.",
                "Empuja el suelo" to "sin que se hunda la cadera.",
            ),
            "Dejar caer la cadera: la espalda baja sufre.", salto = 0.0,
        ),
        // ---------- Tirón ----------
        Ejercicio(
            "dominadas", "Dominadas", CUERPO, Patron.DOMINADA, setOf(DORSAL, BICEPS), setOf(ANTEBRAZO, TRAPECIO, HOMBRO),
            listOf(
                "Cuelga de la barra" to "con las palmas hacia delante, algo más abiertas que los hombros.",
                "Baja los hombros primero" to "y tira llevando los codos hacia las costillas.",
                "Sube hasta pasar la barbilla" to "por encima de la barra.",
                "Baja del todo" to "hasta estirar los brazos.",
            ),
            "Balancearse y hacer medio recorrido.", salto = 0.0,
        ),
        Ejercicio(
            "jalon", "Jalón al pecho", POLEA, Patron.JALON, setOf(DORSAL), setOf(BICEPS, TRAPECIO),
            listOf(
                "Sentado, los muslos bajo los rodillos" to "y la barra cogida algo más abierta que los hombros.",
                "Tira de la barra hacia la parte alta del pecho" to "con el pecho arriba.",
                "Vuelve despacio" to "hasta estirar los brazos.",
            ),
            "Echarse muy atrás y tirar con el cuerpo.", salto = 5.0,
        ),
        Ejercicio(
            "remo_barra", "Remo con barra", BARRA, Patron.REMO, setOf(DORSAL, TRAPECIO), setOf(BICEPS, LUMBAR, HOMBRO),
            listOf(
                "Inclínate con la espalda recta" to "y la barra colgando bajo los hombros.",
                "Tira de la barra hacia el ombligo" to "llevando los codos atrás.",
                "Junta los omóplatos arriba" to "y baja controlando.",
            ),
            "Incorporarse en cada repetición.",
        ),
        Ejercicio(
            "remo_mancuerna", "Remo con mancuerna", MANCUERNAS, Patron.REMO, setOf(DORSAL), setOf(BICEPS, TRAPECIO),
            listOf(
                "Una mano y una rodilla apoyadas en el banco," to "espalda recta.",
                "Tira de la mancuerna hacia la cadera" to "con el codo pegado.",
                "Baja hasta estirar el brazo" to "y repite; luego el otro lado.",
            ),
            "Girar el tronco para subir la mancuerna.", salto = 2.0, porLado = true,
        ),
        Ejercicio(
            "curl_biceps", "Curl de bíceps", MANCUERNAS, Patron.CURL, setOf(BICEPS), setOf(ANTEBRAZO),
            listOf(
                "De pie, una mancuerna en cada mano," to "codos pegados al cuerpo.",
                "Sube doblando el codo" to "girando la palma hacia arriba.",
                "Baja despacio" to "hasta estirar el brazo del todo.",
            ),
            "Balancear el cuerpo para subir el peso.", salto = 2.0,
        ),
        Ejercicio(
            "press_hombro_mancuernas", "Press de hombro con mancuernas", MANCUERNAS, Patron.PRESS_VERTICAL, setOf(HOMBRO), setOf(TRICEPS, TRAPECIO),
            listOf(
                "Mancuernas a la altura de los hombros," to "palmas hacia delante.",
                "Empuja hacia arriba" to "hasta casi juntarlas sobre la cabeza.",
                "Baja controlando" to "hasta la altura de las orejas.",
            ),
            "Arquear la espalda baja al empujar.", salto = 2.0,
        ),
        // ---------- Abdomen ----------
        Ejercicio(
            "plancha", "Plancha", CUERPO, Patron.PLANCHA, setOf(ABDOMEN), setOf(OBLICUOS, HOMBRO),
            listOf(
                "Antebrazos en el suelo," to "codos bajo los hombros.",
                "El cuerpo recto" to "de la cabeza a los talones; aprieta glúteos y abdomen.",
                "Aguanta respirando normal." to "Las «reps» son segundos.",
            ),
            "Subir la cadera o dejar que se hunda.", salto = 0.0,
        ),
        Ejercicio(
            "crunch", "Crunch abdominal", CUERPO, Patron.CRUNCH, setOf(ABDOMEN), setOf(OBLICUOS),
            listOf(
                "Tumbado, rodillas dobladas" to "y pies en el suelo.",
                "Enrolla el tronco" to "despegando los omóplatos, sin tirar del cuello.",
                "Baja despacio" to "sin apoyar del todo la cabeza.",
            ),
            "Tirar de la cabeza con las manos.", salto = 0.0,
        ),
        Ejercicio(
            "puente_gluteo", "Puente de glúteo", CUERPO, Patron.PUENTE, setOf(GLUTEO), setOf(ISQUIOS),
            listOf(
                "Tumbado boca arriba," to "rodillas dobladas y pies planos.",
                "Sube la cadera" to "hasta alinear rodillas, cadera y hombros.",
                "Aprieta arriba un segundo" to "y baja.",
            ),
            "Empujar con la espalda en vez de con los glúteos.", salto = 0.0,
        ),
        Ejercicio(
            "sentadilla_libre", "Sentadilla sin peso", CUERPO, Patron.SENTADILLA, setOf(CUADRICEPS, GLUTEO), setOf(ADUCTORES),
            listOf(
                "Pies a la anchura de los hombros," to "brazos al frente para equilibrar.",
                "Baja con el pecho arriba" to "hasta el paralelo o más.",
                "Sube empujando" to "con todo el pie.",
            ),
            "Levantar los talones del suelo.", salto = 0.0,
        ),
        // ---------- Kettlebell ----------
        Ejercicio(
            "kb_swing", "Swing con kettlebell", KETTLEBELL, Patron.SWING, setOf(GLUTEO, ISQUIOS), setOf(LUMBAR, HOMBRO, ABDOMEN),
            listOf(
                "Pies algo más abiertos que la cadera" to "y la kettlebell un palmo por delante.",
                "Bisagra de cadera:" to "echa el culo atrás con la espalda recta y lánzala entre las piernas.",
                "Extiende la cadera de golpe" to "apretando glúteos: la pesa sube sola hasta el pecho.",
                "Los brazos solo guían." to "No la subas con los hombros.",
            ),
            "Hacer una sentadilla en vez de bisagra. Las rodillas casi no se doblan.",
        ),
        Ejercicio(
            "kb_goblet", "Sentadilla goblet", KETTLEBELL, Patron.GOBLET, setOf(CUADRICEPS, GLUTEO), setOf(ABDOMEN, ADUCTORES),
            listOf(
                "La kettlebell pegada al pecho," to "cogida por los cuernos.",
                "Baja entre los talones" to "con los codos por dentro de las rodillas.",
                "Sube con el pecho arriba" to "empujando con todo el pie.",
            ),
            "Separar la pesa del pecho y encorvarse.",
        ),
        Ejercicio(
            "kb_press", "Press con kettlebell", KETTLEBELL, Patron.PRESS_VERTICAL, setOf(HOMBRO), setOf(TRICEPS, ABDOMEN),
            listOf(
                "Kettlebell en posición de rack:" to "sobre el antebrazo, codo pegado.",
                "Empuja hacia arriba" to "girando la palma hacia delante.",
                "Arriba el brazo junto a la oreja," to "y baja de nuevo al rack.",
            ),
            "Inclinarse hacia el lado contrario para subirla.", porLado = true,
        ),
        Ejercicio(
            "kb_remo", "Remo con kettlebell", KETTLEBELL, Patron.REMO, setOf(DORSAL), setOf(BICEPS, TRAPECIO),
            listOf(
                "Inclínate con la espalda recta," to "la kettlebell colgando bajo el hombro.",
                "Tira hacia la cadera" to "con el codo pegado al cuerpo.",
                "Baja controlando" to "hasta estirar el brazo.",
            ),
            "Tirar con el bíceps en vez de llevar el codo atrás.", porLado = true,
        ),
        Ejercicio(
            "kb_clean", "Cargada con kettlebell", KETTLEBELL, Patron.CLEAN, setOf(GLUTEO, HOMBRO), setOf(ISQUIOS, ANTEBRAZO, TRAPECIO),
            listOf(
                "Empieza como un swing" to "con la kettlebell entre las piernas.",
                "Extiende la cadera y lleva el codo pegado" to "para que la pesa suba cerca del cuerpo.",
                "Mete la mano por dentro del asa" to "y recíbela en rack sin que golpee el antebrazo.",
            ),
            "Dejar que la pesa dé la vuelta y golpee la muñeca.", porLado = true,
        ),
        Ejercicio(
            "kb_snatch", "Arrancada con kettlebell", KETTLEBELL, Patron.SNATCH, setOf(GLUTEO, HOMBRO), setOf(ISQUIOS, TRAPECIO, ABDOMEN),
            listOf(
                "Empieza como un swing" to "a una mano.",
                "Extiende la cadera con fuerza" to "y deja que la pesa suba cerca del cuerpo.",
                "Mete la mano y bloquea arriba" to "con el brazo estirado junto a la oreja.",
                "Baja dejándola caer" to "entre las piernas como en el swing.",
            ),
            "Tirar con el brazo estirado en arco: la pesa golpea el antebrazo.", porLado = true,
        ),
        Ejercicio(
            "kb_peso_muerto", "Peso muerto con kettlebell", KETTLEBELL, Patron.BISAGRA, setOf(GLUTEO, ISQUIOS), setOf(LUMBAR, ANTEBRAZO),
            listOf(
                "La kettlebell entre los pies," to "a la altura del arco.",
                "Cadera atrás con la espalda recta" to "y cógela con las dos manos.",
                "Sube empujando el suelo" to "y aprieta los glúteos arriba.",
            ),
            "Encorvarse para cogerla.",
        ),
        Ejercicio(
            "kb_zancada", "Zancada con kettlebell", KETTLEBELL, Patron.ZANCADA, setOf(CUADRICEPS, GLUTEO), setOf(ADUCTORES, ABDOMEN),
            listOf(
                "La kettlebell en la mano," to "colgando junto a la pierna.",
                "Paso atrás largo" to "y baja hasta casi tocar el suelo con la rodilla.",
                "Vuelve empujando con el pie de delante" to "y cambia de pierna.",
            ),
            "Inclinar el tronco hacia delante.", porLado = true,
        ),
        Ejercicio(
            "kb_thruster", "Thruster con kettlebell", KETTLEBELL, Patron.THRUSTER, setOf(CUADRICEPS, HOMBRO, GLUTEO), setOf(TRICEPS, ABDOMEN),
            listOf(
                "Kettlebell en rack" to "pegada al pecho.",
                "Sentadilla completa" to "con el pecho arriba.",
                "Al subir, aprovecha el impulso" to "para empujarla por encima de la cabeza.",
            ),
            "Hacer el press cuando ya estás de pie, sin aprovechar las piernas.",
        ),
    )

    private val porId by lazy { TODOS.associateBy { it.id } }
    fun porId(id: String): Ejercicio? = porId[id]

    /** Los que se pueden hacer en cada sección. */
    fun de(s: Seccion): List<Ejercicio> = when (s) {
        Seccion.KETTLEBELL -> todos.filter { it.material == KETTLEBELL || it.material == CUERPO }
        Seccion.CASA -> todos.filter { it.material == CUERPO || it.material == MANCUERNAS || it.material == KETTLEBELL }
        Seccion.GIMNASIO -> todos
    }
}

/** Rutinas que vienen hechas. Al editar una, se guarda una copia del usuario y esta no cambia. */
object Rutinas {
    private fun b(id: String, s: Int, min: Int, max: Int, d: Int) = BloqueRutina(id, s, min, max, d)

    val predefinidas: List<Rutina> = listOf(
        Rutina(
            "p_torso", "Torso", Seccion.GIMNASIO,
            listOf(
                b("press_banca", 4, 6, 8, 150), b("remo_barra", 4, 6, 8, 150), b("press_militar", 3, 8, 10, 120),
                b("jalon", 3, 8, 12, 90), b("curl_biceps", 3, 10, 12, 60), b("triceps_polea", 3, 10, 12, 60),
            ),
            descripcion = "Pecho, espalda, hombros y brazos",
        ),
        Rutina(
            "p_pierna", "Pierna", Seccion.GIMNASIO,
            listOf(
                b("sentadilla", 4, 6, 8, 150), b("peso_muerto_rumano", 3, 8, 10, 120), b("zancadas", 3, 10, 12, 90),
                b("hip_thrust", 3, 8, 12, 90), b("curl_femoral", 3, 10, 12, 60), b("gemelos_pie", 4, 12, 15, 60),
            ),
            descripcion = "Cuádriceps, glúteos e isquios",
        ),
        Rutina(
            "p_cuerpo_entero", "Cuerpo entero", Seccion.GIMNASIO,
            listOf(
                b("sentadilla", 3, 6, 8, 150), b("press_banca", 3, 6, 8, 150), b("remo_barra", 3, 8, 10, 120),
                b("peso_muerto_rumano", 3, 8, 10, 120), b("plancha", 3, 30, 60, 60),
            ),
            descripcion = "3 días por semana, todo el cuerpo",
        ),
        Rutina(
            "p_kb_swing", "Swing diario", Seccion.KETTLEBELL,
            listOf(b("kb_swing", 10, 10, 15, 45), b("kb_goblet", 3, 8, 12, 60)),
            descripcion = "10 × 10 swings y sentadilla goblet",
        ),
        Rutina(
            "p_kb_emom", "EMOM 20", Seccion.KETTLEBELL,
            listOf(b("kb_swing", 1, 10, 10, 0), b("kb_goblet", 1, 8, 8, 0), b("kb_press", 1, 5, 5, 0), b("kb_remo", 1, 8, 8, 0)),
            formato = Formato.EMOM, minutos = 20, descripcion = "Cada minuto: swing, goblet, press y remo",
        ),
        Rutina(
            "p_kb_complejo", "Complejo de una kettlebell", Seccion.KETTLEBELL,
            listOf(b("kb_clean", 1, 5, 5, 0), b("kb_press", 1, 5, 5, 0), b("kb_goblet", 1, 5, 5, 0), b("kb_remo", 1, 5, 5, 0)),
            formato = Formato.COMPLEJO, rondas = 5, pausa = 90,
            descripcion = "Cargada, press, sentadilla y remo sin soltarla",
        ),
        Rutina(
            "p_kb_amrap", "AMRAP 12", Seccion.KETTLEBELL,
            listOf(b("kb_swing", 1, 15, 15, 0), b("flexiones", 1, 10, 10, 0), b("kb_zancada", 1, 10, 10, 0)),
            formato = Formato.AMRAP, minutos = 12, descripcion = "Máximas rondas: swing, flexión, zancada",
        ),
        Rutina(
            "p_kb_tabata", "Tabata de kettlebell", Seccion.KETTLEBELL,
            listOf(b("kb_swing", 1, 0, 0, 0), b("kb_goblet", 1, 0, 0, 0)),
            formato = Formato.INTERVALOS, trabajo = 20, pausa = 10, rondas = 8,
            descripcion = "20 s a tope, 10 s de descanso, 8 rondas",
        ),
        Rutina(
            "p_kb_fuerza", "Fuerza con kettlebell", Seccion.KETTLEBELL,
            listOf(
                b("kb_peso_muerto", 4, 8, 12, 90), b("kb_press", 4, 5, 8, 90), b("kb_remo", 4, 8, 12, 60),
                b("kb_thruster", 3, 6, 10, 90), b("kb_snatch", 3, 5, 8, 90),
            ),
            descripcion = "Series con descanso para ganar fuerza",
        ),
        Rutina(
            "p_casa", "En casa con mancuernas", Seccion.CASA,
            listOf(
                b("sentadilla_libre", 3, 12, 20, 60), b("flexiones", 3, 8, 15, 60), b("remo_mancuerna", 3, 10, 12, 60),
                b("press_hombro_mancuernas", 3, 10, 12, 60), b("zancadas", 3, 10, 12, 60), b("curl_biceps", 2, 10, 12, 45), b("plancha", 3, 30, 60, 45),
            ),
            descripcion = "Sin gimnasio: mancuernas y tu peso",
        ),
        Rutina(
            "p_casa_cuerpo", "Sin material", Seccion.CASA,
            listOf(
                b("sentadilla_libre", 4, 15, 25, 60), b("flexiones", 4, 8, 15, 60), b("puente_gluteo", 3, 15, 20, 45),
                b("crunch", 3, 15, 20, 45), b("plancha", 3, 30, 60, 45),
            ),
            descripcion = "Solo con tu peso, en 30 minutos",
        ),
    )
}
