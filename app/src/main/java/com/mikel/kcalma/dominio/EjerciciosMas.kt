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
import com.mikel.kcalma.dominio.Patron.BISAGRA
import com.mikel.kcalma.dominio.Patron.CLEAN
import com.mikel.kcalma.dominio.Patron.CRUNCH
import com.mikel.kcalma.dominio.Patron.CURL
import com.mikel.kcalma.dominio.Patron.CURL_PIERNA
import com.mikel.kcalma.dominio.Patron.DOMINADA
import com.mikel.kcalma.dominio.Patron.ELEVACION
import com.mikel.kcalma.dominio.Patron.ESCALADOR
import com.mikel.kcalma.dominio.Patron.EXTENSION_PIERNA
import com.mikel.kcalma.dominio.Patron.FLEXION
import com.mikel.kcalma.dominio.Patron.FONDOS
import com.mikel.kcalma.dominio.Patron.GEMELO
import com.mikel.kcalma.dominio.Patron.GOBLET
import com.mikel.kcalma.dominio.Patron.JALON
import com.mikel.kcalma.dominio.Patron.PLANCHA
import com.mikel.kcalma.dominio.Patron.PRESS_BANCA
import com.mikel.kcalma.dominio.Patron.PRESS_VERTICAL
import com.mikel.kcalma.dominio.Patron.PUENTE
import com.mikel.kcalma.dominio.Patron.REMO
import com.mikel.kcalma.dominio.Patron.SALTO
import com.mikel.kcalma.dominio.Patron.SENTADILLA
import com.mikel.kcalma.dominio.Patron.SNATCH
import com.mikel.kcalma.dominio.Patron.SWING
import com.mikel.kcalma.dominio.Patron.THRUSTER
import com.mikel.kcalma.dominio.Patron.ZANCADA

/**
 * Ampliación del catálogo (0.9.0). Los ids son para siempre: el historial los guarda.
 * Pasos: «inicio en negrita|resto;…».
 */
internal object EjerciciosMas {
    private fun e(
        id: String, nombre: String, m: Material, p: Patron, prin: Set<Musculo>, sec: Set<Musculo>,
        pasos: String, error: String, porLado: Boolean = false,
    ) = Ejercicio(
        id, nombre, m, p, prin, sec,
        pasos.split(";").map { it.trim() }.filter { it.isNotEmpty() }.map { it.substringBefore("|") to it.substringAfter("|", "") },
        error,
        salto = when (m) { MAQUINA -> 5.0; MANCUERNAS -> 2.0; CUERPO -> 0.0; else -> 2.5 },
        porLado = porLado,
    )

    val lista: List<Ejercicio> = listOf(
        // ---------- Pierna ----------
        e("sentadilla_frontal", "Sentadilla frontal", BARRA, SENTADILLA, setOf(CUADRICEPS), setOf(GLUTEO, ABDOMEN),
            "Barra sobre los hombros delanteros,|codos altos;Baja con el tronco muy erguido|hasta el paralelo;Sube empujando|sin dejar caer los codos", "Bajar los codos y redondear la espalda."),
        e("sentadilla_bulgara", "Sentadilla búlgara", MANCUERNAS, ZANCADA, setOf(CUADRICEPS, GLUTEO), setOf(ADUCTORES),
            "Pie de atrás sobre un banco,|mancuernas en las manos;Baja recto|hasta casi tocar el suelo con la rodilla;Sube con la pierna de delante|y cambia al terminar", "Adelantar la rodilla mucho más que la punta del pie.", porLado = true),
        e("prensa", "Prensa de piernas", MAQUINA, SENTADILLA, setOf(CUADRICEPS, GLUTEO), setOf(ADUCTORES, ISQUIOS),
            "Espalda pegada al respaldo,|pies a la anchura de la cadera;Baja la plataforma|hasta 90° de rodilla;Empuja con todo el pie|sin bloquear las rodillas", "Despegar la cadera del asiento al bajar."),
        e("hack", "Sentadilla hack", MAQUINA, SENTADILLA, setOf(CUADRICEPS), setOf(GLUTEO),
            "Espalda contra el respaldo|y hombros bajo los topes;Baja controlando|hasta el paralelo;Sube empujando|con los talones", "Rebotar abajo."),
        e("extension_cuadriceps", "Extensión de cuádriceps", MAQUINA, EXTENSION_PIERNA, setOf(CUADRICEPS), emptySet(),
            "Rodilla alineada con el eje|de la máquina;Estira las piernas|del todo;Baja despacio|sin dejar caer el peso", "Dar tirones con la cadera."),
        e("curl_femoral_tumbado", "Curl femoral tumbado", MAQUINA, CURL_PIERNA, setOf(ISQUIOS), setOf(GEMELOS),
            "Tumbado boca abajo,|rodillas justo fuera del banco;Lleva los talones hacia los glúteos;Baja despacio|sin levantar la cadera", "Levantar la cadera para ayudarte."),
        e("aductores_maquina", "Aductores en máquina", MAQUINA, EXTENSION_PIERNA, setOf(ADUCTORES), emptySet(),
            "Sentado con la espalda apoyada;Junta las piernas|apretando por dentro;Abre despacio|hasta notar estiramiento", "Abrir más de lo cómodo con mucho peso."),
        e("abductores_maquina", "Abductores en máquina", MAQUINA, EXTENSION_PIERNA, setOf(GLUTEO), emptySet(),
            "Sentado con la espalda apoyada;Abre las piernas|empujando con la parte de fuera;Vuelve despacio", "Balancear el tronco."),
        e("gemelos_sentado", "Gemelos sentado", MAQUINA, GEMELO, setOf(GEMELOS), emptySet(),
            "Rodillas bajo el tope,|puntas en la plataforma;Sube los talones|todo lo que puedas;Baja hasta estirar", "Rebotar abajo."),
        e("gemelos_libre", "Gemelos en escalón", CUERPO, GEMELO, setOf(GEMELOS), emptySet(),
            "Puntas en un escalón,|talones fuera;Sube despacio|a puntillas;Baja hasta estirar del todo", "Hacerlo rápido y a medias."),
        e("zancada_atras", "Zancada atrás con barra", BARRA, ZANCADA, setOf(CUADRICEPS, GLUTEO), setOf(ISQUIOS),
            "Barra sobre los trapecios;Paso largo atrás|y baja la rodilla;Vuelve con la pierna de delante", "Perder el equilibrio por pasos cortos.", porLado = true),
        e("zancada_caminando", "Zancada caminando", MANCUERNAS, ZANCADA, setOf(CUADRICEPS, GLUTEO), setOf(ADUCTORES),
            "Mancuernas a los lados;Paso largo adelante|y baja;Sigue andando|con la otra pierna", "Tronco inclinado hacia delante.", porLado = true),
        e("step_up", "Subida al banco", MANCUERNAS, ZANCADA, setOf(CUADRICEPS, GLUTEO), emptySet(),
            "Un pie entero sobre el banco;Sube empujando con ese pie|sin impulsarte con el de abajo;Baja controlando", "Empujar con la pierna de abajo.", porLado = true),
        e("goblet_mancuerna", "Sentadilla goblet con mancuerna", MANCUERNAS, GOBLET, setOf(CUADRICEPS, GLUTEO), setOf(ABDOMEN),
            "Mancuerna vertical pegada al pecho;Baja entre los talones|con el pecho arriba;Sube empujando el suelo", "Redondear la espalda abajo."),
        e("sumo", "Sentadilla sumo", MANCUERNAS, GOBLET, setOf(ADUCTORES, GLUTEO), setOf(CUADRICEPS),
            "Pies muy abiertos,|puntas hacia fuera;Mancuerna colgando entre las piernas;Baja y sube|con las rodillas hacia fuera", "Rodillas hacia dentro."),
        e("peso_muerto_sumo", "Peso muerto sumo", BARRA, BISAGRA, setOf(GLUTEO, ADUCTORES), setOf(ISQUIOS, LUMBAR, CUADRICEPS),
            "Pies muy abiertos,|manos por dentro de las piernas;Pecho arriba|y espalda recta;Empuja el suelo hacia fuera|y sube", "Tirar con la espalda redonda."),
        e("rumano_mancuernas", "Peso muerto rumano con mancuernas", MANCUERNAS, BISAGRA, setOf(ISQUIOS, GLUTEO), setOf(LUMBAR),
            "Mancuernas delante de los muslos;Cadera atrás|con las rodillas casi rectas;Baja hasta notar los isquios|y vuelve", "Bajar doblando la espalda."),
        e("rumano_una_pierna", "Peso muerto a una pierna", MANCUERNAS, BISAGRA, setOf(ISQUIOS, GLUTEO), setOf(LUMBAR, ABDOMEN),
            "Mancuerna en la mano contraria;Inclínate|levantando la pierna de atrás;Vuelve apretando el glúteo", "Abrir la cadera hacia un lado.", porLado = true),
        e("buenos_dias", "Buenos días", BARRA, BISAGRA, setOf(ISQUIOS, LUMBAR), setOf(GLUTEO),
            "Barra sobre los trapecios;Cadera atrás|con la espalda recta;Sube apretando glúteos", "Usar mucho peso al principio."),
        e("hiperextension", "Hiperextensión", CUERPO, BISAGRA, setOf(LUMBAR, GLUTEO), setOf(ISQUIOS),
            "Cadera apoyada en el banco romano;Baja el tronco|con la espalda recta;Sube hasta quedar en línea", "Subir de más arqueando la espalda."),
        e("hip_thrust_maquina", "Hip thrust en máquina", MAQUINA, PUENTE, setOf(GLUTEO), setOf(ISQUIOS),
            "Espalda en el respaldo,|cinturón sobre la cadera;Sube la cadera|apretando glúteos;Baja sin tocar abajo", "Arquear la zona lumbar arriba."),
        e("puente_una_pierna", "Puente a una pierna", CUERPO, PUENTE, setOf(GLUTEO), setOf(ISQUIOS),
            "Tumbado, un pie apoyado|y la otra pierna estirada;Sube la cadera|con el pie apoyado;Baja despacio", "Girar la cadera.", porLado = true),
        e("patada_gluteo", "Patada de glúteo en polea", POLEA, PUENTE, setOf(GLUTEO), emptySet(),
            "Tobillera en la polea baja;Lleva la pierna atrás|con la rodilla casi recta;Vuelve despacio", "Arquear la espalda.", porLado = true),
        e("sentadilla_salto", "Sentadilla con salto", CUERPO, SALTO, setOf(CUADRICEPS, GLUTEO), setOf(GEMELOS),
            "Baja a media sentadilla;Salta todo lo que puedas;Cae suave|y enlaza la siguiente", "Caer con las rodillas rectas."),
        e("zancada_salto", "Zancada con salto", CUERPO, SALTO, setOf(CUADRICEPS, GLUTEO), setOf(GEMELOS),
            "En posición de zancada;Salta y cambia de pierna|en el aire;Cae suave", "Rodilla de delante hacia dentro."),
        e("sentadilla_pared", "Sentadilla en pared", CUERPO, SENTADILLA, setOf(CUADRICEPS), setOf(GLUTEO),
            "Espalda contra la pared;Baja hasta 90°|de rodilla;Aguanta el tiempo", "Apoyar las manos en los muslos."),
        e("pistol", "Sentadilla a una pierna", CUERPO, SENTADILLA, setOf(CUADRICEPS, GLUTEO), setOf(ABDOMEN),
            "De pie sobre una pierna,|la otra estirada delante;Baja todo lo que controles;Sube sin apoyar la otra", "Dejar caer la rodilla hacia dentro.", porLado = true),
        e("curl_nordico", "Curl nórdico", CUERPO, CURL_PIERNA, setOf(ISQUIOS), setOf(GLUTEO),
            "De rodillas,|tobillos sujetos;Déjate caer hacia delante|lo más lento que puedas;Ayúdate con las manos|para volver", "Doblar la cadera."),

        // ---------- Pecho ----------
        e("press_banca_mancuernas", "Press de banca con mancuernas", MANCUERNAS, PRESS_BANCA, setOf(PECHO), setOf(TRICEPS, HOMBRO),
            "Tumbado, mancuernas a la altura del pecho;Empuja hacia arriba|juntándolas un poco;Baja despacio|hasta estirar el pecho", "Bajar los codos muy abiertos."),
        e("press_inclinado_barra", "Press inclinado con barra", BARRA, PRESS_BANCA, setOf(PECHO, HOMBRO), setOf(TRICEPS),
            "Banco a 30–45°;Baja la barra|a la parte alta del pecho;Empuja arriba", "Despegar la cadera del banco."),
        e("press_declinado", "Press declinado", BARRA, PRESS_BANCA, setOf(PECHO), setOf(TRICEPS),
            "Banco con la cabeza más baja;Baja la barra|a la parte baja del pecho;Empuja arriba", "Rebotar la barra en el pecho."),
        e("press_pecho_maquina", "Press de pecho en máquina", MAQUINA, PRESS_BANCA, setOf(PECHO), setOf(TRICEPS, HOMBRO),
            "Asiento con las asas a la altura del pecho;Empuja hasta estirar|sin bloquear;Vuelve despacio", "Encoger los hombros."),
        e("aperturas", "Aperturas con mancuernas", MANCUERNAS, PRESS_BANCA, setOf(PECHO), setOf(HOMBRO),
            "Tumbado, mancuernas arriba|con los codos algo doblados;Abre en arco|hasta notar el pecho;Cierra como abrazando un árbol", "Estirar los codos del todo con peso."),
        e("cruce_poleas", "Cruce de poleas", POLEA, PRESS_BANCA, setOf(PECHO), setOf(HOMBRO),
            "Entre las dos poleas altas,|un paso adelante;Junta las manos delante|en arco;Vuelve controlando", "Tirar con los brazos en vez de con el pecho."),
        e("pec_deck", "Contractor de pecho", MAQUINA, PRESS_BANCA, setOf(PECHO), emptySet(),
            "Espalda apoyada,|codos a la altura del hombro;Junta delante|apretando el pecho;Abre despacio", "Separar la espalda del respaldo."),
        e("fondos", "Fondos en paralelas", CUERPO, FONDOS, setOf(PECHO, TRICEPS), setOf(HOMBRO),
            "Brazos estirados en las paralelas;Baja inclinándote un poco|hasta 90° de codo;Sube empujando", "Bajar demasiado con los hombros adelantados."),
        e("fondos_banco", "Fondos en banco", CUERPO, FONDOS, setOf(TRICEPS), setOf(PECHO, HOMBRO),
            "Manos en el borde de un banco,|piernas delante;Baja doblando los codos|hacia atrás;Sube empujando", "Separar mucho la espalda del banco."),
        e("flexiones_inclinadas", "Flexiones inclinadas", CUERPO, FLEXION, setOf(PECHO), setOf(TRICEPS, HOMBRO),
            "Manos sobre un banco o mesa;Cuerpo recto;Baja el pecho al borde|y empuja", "Hundir la cadera."),
        e("flexiones_rodillas", "Flexiones de rodillas", CUERPO, FLEXION, setOf(PECHO), setOf(TRICEPS, HOMBRO),
            "Rodillas en el suelo,|cuerpo recto de rodillas a cabeza;Baja el pecho;Empuja hasta estirar", "Doblar la cadera."),
        e("flexiones_diamante", "Flexiones diamante", CUERPO, FLEXION, setOf(TRICEPS), setOf(PECHO),
            "Manos juntas bajo el pecho|formando un rombo;Baja con los codos pegados;Empuja", "Abrir los codos."),
        e("flexiones_declinadas", "Flexiones con pies elevados", CUERPO, FLEXION, setOf(PECHO, HOMBRO), setOf(TRICEPS),
            "Pies sobre un banco;Cuerpo recto;Baja y empuja", "Arquear la espalda."),
        e("pullover", "Pullover con mancuerna", MANCUERNAS, PRESS_BANCA, setOf(PECHO, DORSAL), setOf(TRICEPS),
            "Tumbado, mancuerna con las dos manos sobre el pecho;Bájala detrás de la cabeza|con los codos algo doblados;Vuelve arriba", "Doblar mucho los codos."),
        e("kb_press_suelo", "Press de suelo con kettlebell", KETTLEBELL, PRESS_BANCA, setOf(PECHO), setOf(TRICEPS),
            "Tumbado en el suelo,|kettlebell en una mano;Empuja arriba;Baja hasta apoyar el codo", "Girar el tronco.", porLado = true),

        // ---------- Espalda ----------
        e("dominadas_supinas", "Dominadas supinas", CUERPO, DOMINADA, setOf(DORSAL, BICEPS), setOf(ANTEBRAZO),
            "Palmas hacia ti,|manos a la anchura de los hombros;Sube hasta pasar la barbilla;Baja del todo", "Hacer medias repeticiones."),
        e("dominadas_asistidas", "Dominadas asistidas", MAQUINA, DOMINADA, setOf(DORSAL), setOf(BICEPS),
            "Rodillas en la plataforma;Sube llevando el pecho a la barra;Baja controlando", "Más asistencia de la que necesitas."),
        e("jalon_estrecho", "Jalón con agarre estrecho", POLEA, JALON, setOf(DORSAL), setOf(BICEPS),
            "Agarre en V;Tira hacia el pecho|sacando el pecho;Sube estirando la espalda", "Echarse muy atrás."),
        e("jalon_supino", "Jalón supino", POLEA, JALON, setOf(DORSAL, BICEPS), emptySet(),
            "Palmas hacia ti;Tira hacia la parte alta del pecho;Sube despacio", "Tirar con los brazos solos."),
        e("remo_polea", "Remo en polea baja", POLEA, REMO, setOf(DORSAL), setOf(BICEPS, TRAPECIO),
            "Sentado, rodillas algo dobladas;Tira hacia el ombligo|juntando escápulas;Estira los brazos", "Balancear el tronco."),
        e("remo_maquina", "Remo en máquina", MAQUINA, REMO, setOf(DORSAL), setOf(BICEPS, TRAPECIO),
            "Pecho apoyado;Tira de las asas|llevando los codos atrás;Vuelve despacio", "Encoger los hombros."),
        e("remo_t", "Remo en T", BARRA, REMO, setOf(DORSAL, TRAPECIO), setOf(BICEPS, LUMBAR),
            "Inclinado con la espalda recta;Tira de la barra hacia el pecho;Baja controlando", "Redondear la espalda."),
        e("remo_pendlay", "Remo Pendlay", BARRA, REMO, setOf(DORSAL, TRAPECIO), setOf(LUMBAR, BICEPS),
            "Tronco paralelo al suelo,|barra en el suelo;Tira explosivo al pecho;Deja la barra en el suelo|cada vez", "Levantar el tronco al tirar."),
        e("remo_invertido", "Remo invertido", CUERPO, REMO, setOf(DORSAL), setOf(BICEPS, TRAPECIO),
            "Debajo de una barra baja,|cuerpo recto;Tira del pecho a la barra;Baja despacio", "Dejar caer la cadera."),
        e("pullover_polea", "Pullover en polea", POLEA, JALON, setOf(DORSAL), setOf(TRICEPS),
            "De pie ante la polea alta,|brazos casi rectos;Baja la barra hasta los muslos;Sube despacio", "Doblar los codos."),
        e("face_pull", "Face pull", POLEA, REMO, setOf(HOMBRO, TRAPECIO), setOf(DORSAL),
            "Cuerda a la altura de la cara;Tira hacia la frente|abriendo las manos;Vuelve despacio", "Tirar con los codos bajos."),
        e("encogimientos", "Encogimientos", MANCUERNAS, CURL, setOf(TRAPECIO), setOf(ANTEBRAZO),
            "Mancuernas a los lados;Sube los hombros hacia las orejas;Baja despacio", "Girar los hombros."),
        e("encogimientos_barra", "Encogimientos con barra", BARRA, CURL, setOf(TRAPECIO), setOf(ANTEBRAZO),
            "Barra delante de los muslos;Sube los hombros|sin doblar los codos;Baja despacio", "Usar impulso de las piernas."),
        e("rack_pull", "Peso muerto desde rack", BARRA, BISAGRA, setOf(TRAPECIO, LUMBAR, GLUTEO), setOf(DORSAL, ANTEBRAZO),
            "Barra a la altura de las rodillas;Espalda recta;Sube apretando glúteos", "Hiperextender arriba."),
        e("kb_remo_renegado", "Remo renegado", KETTLEBELL, PLANCHA, setOf(DORSAL, ABDOMEN), setOf(BICEPS, OBLICUOS),
            "En plancha sobre dos kettlebells;Rema con una|sin girar la cadera;Alterna", "Abrir mucho los pies para no girar.", porLado = true),
        e("kb_remo_alto", "Remo alto con kettlebell", KETTLEBELL, CLEAN, setOf(TRAPECIO, HOMBRO), setOf(GLUTEO, ISQUIOS),
            "Como un swing|pero tirando al pecho;Codos altos arriba;Baja con la cadera", "Tirar solo con los brazos."),

        // ---------- Hombro ----------
        e("elevaciones_laterales", "Elevaciones laterales", MANCUERNAS, ELEVACION, setOf(HOMBRO), setOf(TRAPECIO),
            "Mancuernas a los lados;Sube hasta la altura del hombro|con los codos algo doblados;Baja despacio", "Subir con impulso del cuerpo."),
        e("elevaciones_frontales", "Elevaciones frontales", MANCUERNAS, ELEVACION, setOf(HOMBRO), setOf(PECHO),
            "Mancuernas delante de los muslos;Sube delante|hasta la altura de los ojos;Baja despacio", "Balancearse."),
        e("laterales_polea", "Elevación lateral en polea", POLEA, ELEVACION, setOf(HOMBRO), emptySet(),
            "Polea baja al lado contrario;Sube el brazo|hasta el hombro;Baja controlando", "Encoger el hombro.", porLado = true),
        e("pajaros", "Pájaros", MANCUERNAS, REMO, setOf(HOMBRO), setOf(TRAPECIO),
            "Inclinado, espalda recta;Abre los brazos|hacia los lados;Baja despacio", "Juntar escápulas en vez de abrir brazos."),
        e("posterior_maquina", "Deltoides posterior en máquina", MAQUINA, REMO, setOf(HOMBRO), setOf(TRAPECIO),
            "Pecho contra el respaldo;Abre los brazos hacia atrás;Vuelve despacio", "Doblar mucho los codos."),
        e("press_arnold", "Press Arnold", MANCUERNAS, PRESS_VERTICAL, setOf(HOMBRO), setOf(TRICEPS),
            "Mancuernas delante con palmas hacia ti;Sube girando|hasta mirar adelante;Baja deshaciendo el giro", "Arquear la espalda."),
        e("press_hombro_maquina", "Press de hombro en máquina", MAQUINA, PRESS_VERTICAL, setOf(HOMBRO), setOf(TRICEPS),
            "Asas a la altura de los hombros;Empuja arriba;Baja controlando", "Despegar la espalda."),
        e("push_press", "Push press", BARRA, THRUSTER, setOf(HOMBRO), setOf(TRICEPS, CUADRICEPS),
            "Barra en los hombros;Flexiona un poco las rodillas;Empuja con las piernas y los brazos", "Bajar demasiado las piernas."),
        e("remo_al_menton", "Remo al mentón", BARRA, CLEAN, setOf(HOMBRO, TRAPECIO), setOf(BICEPS),
            "Barra delante, agarre medio;Sube hasta el pecho|con los codos altos;Baja despacio", "Agarre muy estrecho."),
        e("pike_push_up", "Flexión pica", CUERPO, FLEXION, setOf(HOMBRO), setOf(TRICEPS),
            "Cadera arriba,|cuerpo en V invertida;Baja la cabeza hacia el suelo;Empuja", "Bajar con el pecho en vez de la cabeza."),
        e("kb_press_rodillas", "Press de kettlebell de rodillas", KETTLEBELL, PRESS_VERTICAL, setOf(HOMBRO), setOf(ABDOMEN, TRICEPS),
            "Medio arrodillado,|kettlebell en rack;Empuja arriba sin arquear;Baja al rack", "Inclinarte hacia un lado.", porLado = true),
        e("kb_halo", "Halo con kettlebell", KETTLEBELL, PRESS_VERTICAL, setOf(HOMBRO), setOf(TRAPECIO, ABDOMEN),
            "Kettlebell boca abajo delante de la cara;Rodea la cabeza|despacio;Cambia de sentido", "Mover el tronco."),
        e("kb_turkish", "Levantamiento turco", KETTLEBELL, PLANCHA, setOf(HOMBRO, ABDOMEN), setOf(GLUTEO, OBLICUOS),
            "Tumbado con la kettlebell arriba;Levántate por pasos|sin quitar la vista;Vuelve igual al suelo", "Doblar el codo de la kettlebell.", porLado = true),
        e("kb_windmill", "Molino con kettlebell", KETTLEBELL, BISAGRA, setOf(OBLICUOS, HOMBRO), setOf(ISQUIOS, GLUTEO),
            "Kettlebell arriba,|pies girados;Baja la otra mano al pie|mirando la kettlebell;Sube despacio", "Doblar el brazo de arriba.", porLado = true),

        // ---------- Brazos ----------
        e("curl_barra", "Curl con barra", BARRA, CURL, setOf(BICEPS), setOf(ANTEBRAZO),
            "Barra con las palmas hacia delante;Sube sin mover los codos;Baja despacio", "Balancear el tronco."),
        e("curl_martillo", "Curl martillo", MANCUERNAS, CURL, setOf(BICEPS, ANTEBRAZO), emptySet(),
            "Palmas mirándose;Sube las mancuernas;Baja despacio", "Adelantar los codos."),
        e("curl_concentrado", "Curl concentrado", MANCUERNAS, CURL, setOf(BICEPS), emptySet(),
            "Sentado, codo en el muslo;Sube la mancuerna;Baja hasta estirar", "Mover el hombro.", porLado = true),
        e("curl_predicador", "Curl en banco Scott", BARRA, CURL, setOf(BICEPS), setOf(ANTEBRAZO),
            "Brazos sobre el atril;Sube la barra;Baja sin estirar de golpe", "Levantar los codos."),
        e("curl_polea", "Curl en polea", POLEA, CURL, setOf(BICEPS), setOf(ANTEBRAZO),
            "Polea baja con barra;Sube con los codos quietos;Baja despacio", "Echarse atrás."),
        e("curl_inclinado", "Curl inclinado", MANCUERNAS, CURL, setOf(BICEPS), emptySet(),
            "Sentado en banco inclinado,|brazos colgando;Sube las mancuernas;Baja estirando del todo", "Adelantar los hombros."),
        e("kb_curl", "Curl con kettlebell", KETTLEBELL, CURL, setOf(BICEPS), setOf(ANTEBRAZO),
            "Kettlebell por el asa,|palmas delante;Sube sin balancear;Baja despacio", "Usar la cadera."),
        e("press_frances", "Press francés", BARRA, Patron.TRICEPS, setOf(TRICEPS), emptySet(),
            "Tumbado, barra arriba;Baja hacia la frente|doblando solo los codos;Sube", "Abrir los codos."),
        e("triceps_cuerda", "Tríceps con cuerda", POLEA, Patron.TRICEPS, setOf(TRICEPS), emptySet(),
            "Cuerda en la polea alta;Baja abriendo la cuerda abajo;Sube hasta 90°", "Mover los codos."),
        e("triceps_nuca", "Extensión de tríceps tras la nuca", MANCUERNAS, Patron.TRICEPS, setOf(TRICEPS), emptySet(),
            "Mancuerna con las dos manos sobre la cabeza;Bájala detrás;Sube estirando", "Abrir los codos."),
        e("patada_triceps", "Patada de tríceps", MANCUERNAS, Patron.TRICEPS, setOf(TRICEPS), emptySet(),
            "Inclinado, codo pegado al cuerpo;Estira el brazo atrás;Vuelve a 90°", "Bajar el codo.", porLado = true),
        e("press_cerrado", "Press de banca cerrado", BARRA, PRESS_BANCA, setOf(TRICEPS), setOf(PECHO),
            "Manos a la anchura de los hombros;Baja con codos pegados;Empuja", "Agarre demasiado estrecho."),
        e("curl_muneca", "Curl de muñeca", MANCUERNAS, CURL, setOf(ANTEBRAZO), emptySet(),
            "Antebrazos sobre los muslos;Sube solo la muñeca;Baja despacio", "Mover el antebrazo."),
        e("paseo_granjero", "Paseo del granjero", KETTLEBELL, GEMELO, setOf(ANTEBRAZO, TRAPECIO), setOf(ABDOMEN, GLUTEO),
            "Una kettlebell pesada en cada mano;Camina erguido|con pasos cortos;Aguanta el tiempo", "Encorvarse."),
        e("colgado", "Colgarse de la barra", CUERPO, DOMINADA, setOf(ANTEBRAZO), setOf(DORSAL),
            "Agarra la barra|con los brazos estirados;Aguanta colgado;Respira tranquilo", "Soltarse de golpe."),

        // ---------- Abdomen ----------
        e("plancha_lateral", "Plancha lateral", CUERPO, PLANCHA, setOf(OBLICUOS), setOf(ABDOMEN, GLUTEO),
            "De lado sobre el antebrazo;Cuerpo recto;Aguanta|y cambia de lado", "Dejar caer la cadera.", porLado = true),
        e("elevacion_piernas", "Elevación de piernas", CUERPO, CRUNCH, setOf(ABDOMEN), emptySet(),
            "Tumbado boca arriba;Sube las piernas rectas|hasta la vertical;Baja sin tocar el suelo", "Arquear la zona lumbar."),
        e("elevacion_piernas_colgado", "Elevación de piernas colgado", CUERPO, DOMINADA, setOf(ABDOMEN), setOf(ANTEBRAZO),
            "Colgado de la barra;Sube las rodillas al pecho;Baja sin balancear", "Balancearse."),
        e("rueda", "Rueda abdominal", CUERPO, PLANCHA, setOf(ABDOMEN), setOf(DORSAL, HOMBRO),
            "De rodillas con la rueda;Rueda adelante|con la espalda recta;Vuelve tirando del abdomen", "Hundir la zona lumbar."),
        e("crunch_polea", "Crunch en polea", POLEA, CRUNCH, setOf(ABDOMEN), emptySet(),
            "De rodillas, cuerda junto a la cabeza;Enrolla el tronco hacia abajo;Vuelve despacio", "Tirar con los brazos."),
        e("crunch_bicicleta", "Crunch bicicleta", CUERPO, CRUNCH, setOf(OBLICUOS, ABDOMEN), emptySet(),
            "Tumbado, manos tras la cabeza;Lleva el codo a la rodilla contraria;Alterna sin parar", "Tirar del cuello."),
        e("russian_twist", "Giro ruso", CUERPO, CRUNCH, setOf(OBLICUOS), setOf(ABDOMEN),
            "Sentado, tronco inclinado|y pies en el aire;Gira el tronco a un lado y al otro;Mantén la espalda recta", "Mover solo los brazos."),
        e("dead_bug", "Bicho muerto", CUERPO, CRUNCH, setOf(ABDOMEN), emptySet(),
            "Boca arriba, brazos y piernas arriba;Estira un brazo y la pierna contraria;Vuelve y alterna", "Despegar la zona lumbar."),
        e("hollow", "Hollow hold", CUERPO, CRUNCH, setOf(ABDOMEN), emptySet(),
            "Boca arriba;Levanta hombros y piernas|un palmo;Aguanta", "Arquear la espalda."),
        e("escaladores", "Escaladores", CUERPO, ESCALADOR, setOf(ABDOMEN), setOf(HOMBRO, CUADRICEPS),
            "En plancha alta;Lleva una rodilla al pecho|y cambia rápido;Mantén la cadera baja", "Subir la cadera."),
        e("pallof", "Press Pallof", POLEA, PRESS_VERTICAL, setOf(OBLICUOS, ABDOMEN), emptySet(),
            "De lado a la polea;Empuja las manos al frente|sin girar;Vuelve al pecho", "Dejarse girar.", porLado = true),
        e("lenador", "Leñador en polea", POLEA, ELEVACION, setOf(OBLICUOS), setOf(ABDOMEN, HOMBRO),
            "De lado a la polea alta;Tira en diagonal hacia abajo|girando el tronco;Vuelve despacio", "Tirar solo con los brazos.", porLado = true),
        e("kb_swing_ruso", "Swing ruso a una mano", KETTLEBELL, SWING, setOf(GLUTEO, ISQUIOS), setOf(ABDOMEN, ANTEBRAZO, OBLICUOS),
            "Kettlebell en una mano;Bisagra de cadera|como el swing;Sube a la altura del pecho", "Girar el tronco.", porLado = true),

        // ---------- Kettlebell ----------
        e("kb_swing_americano", "Swing americano", KETTLEBELL, SNATCH, setOf(GLUTEO, ISQUIOS, HOMBRO), setOf(ABDOMEN),
            "Como el swing|pero hasta encima de la cabeza;Brazos estirados arriba;Baja con la cadera", "Arquear la espalda arriba."),
        e("kb_doble_clean", "Cargada doble", KETTLEBELL, CLEAN, setOf(GLUTEO, ISQUIOS), setOf(ANTEBRAZO, TRAPECIO, ABDOMEN),
            "Una kettlebell en cada mano;Bisagra y cargada al rack;Vuelve entre las piernas", "Golpear los antebrazos."),
        e("kb_clean_press", "Cargada y press", KETTLEBELL, THRUSTER, setOf(HOMBRO, GLUTEO), setOf(TRICEPS, ISQUIOS),
            "Cargada al rack;Press arriba;Baja al rack y vuelve abajo", "Hacer el press con la espalda arqueada.", porLado = true),
        e("kb_sentadilla_rack", "Sentadilla en rack", KETTLEBELL, GOBLET, setOf(CUADRICEPS, GLUTEO), setOf(ABDOMEN),
            "Kettlebell en rack en una mano;Baja recto;Sube sin inclinarte", "Inclinarte hacia la kettlebell.", porLado = true),
        e("kb_peso_muerto_una", "Peso muerto a una pierna con kettlebell", KETTLEBELL, BISAGRA, setOf(ISQUIOS, GLUTEO), setOf(ABDOMEN),
            "Kettlebell en la mano contraria;Inclínate con la pierna atrás;Vuelve apretando glúteo", "Girar la cadera.", porLado = true),
        e("kb_gorila", "Remo gorila", KETTLEBELL, REMO, setOf(DORSAL), setOf(BICEPS, LUMBAR),
            "Dos kettlebells entre los pies,|bisagra;Rema una y luego la otra;Espalda quieta", "Levantar el tronco.", porLado = true),
        e("kb_high_pull", "Tirón alto con kettlebell", KETTLEBELL, CLEAN, setOf(TRAPECIO, HOMBRO, GLUTEO), setOf(ISQUIOS),
            "Swing a una mano;Tira con el codo alto|hasta el pecho;Vuelve con la cadera", "Dar tirón con la espalda.", porLado = true),
        e("kb_figura_8", "Ocho con kettlebell", KETTLEBELL, SWING, setOf(GLUTEO, OBLICUOS), setOf(ISQUIOS, ANTEBRAZO),
            "Media sentadilla;Pasa la kettlebell entre las piernas|haciendo un ocho;Cambia de mano en cada paso", "Redondear la espalda."),
        e("kb_sit_up", "Abdominal con kettlebell", KETTLEBELL, CRUNCH, setOf(ABDOMEN), setOf(HOMBRO),
            "Tumbado, kettlebell al pecho;Sube hasta sentarte;Baja despacio", "Tirar del cuello."),
        e("kb_suitcase", "Peso muerto maleta", KETTLEBELL, BISAGRA, setOf(GLUTEO, OBLICUOS), setOf(ISQUIOS, ANTEBRAZO),
            "Kettlebell a un lado;Baja con la espalda recta|sin inclinarte;Sube", "Torcerse hacia la kettlebell.", porLado = true),
        e("kb_sentadilla_salto", "Sentadilla con salto y kettlebell", KETTLEBELL, SALTO, setOf(CUADRICEPS, GLUTEO), setOf(GEMELOS),
            "Kettlebell al pecho;Baja y salta;Cae suave", "Caer rígido."),

        // ---------- Cardio / cuerpo ----------
        e("burpee", "Burpee", CUERPO, SALTO, setOf(CUADRICEPS, PECHO), setOf(HOMBRO, ABDOMEN),
            "Agáchate y apoya las manos;Salta atrás a plancha|y haz una flexión;Vuelve y salta arriba", "Hundir la cadera en la plancha."),
        e("jumping_jacks", "Saltos de tijera", CUERPO, SALTO, setOf(GEMELOS), setOf(HOMBRO),
            "De pie;Salta abriendo piernas y brazos;Vuelve a cerrar", "Caer sobre los talones."),
        e("comba", "Comba", CUERPO, GEMELO, setOf(GEMELOS), setOf(ANTEBRAZO, HOMBRO),
            "Codos pegados;Salta bajito|sobre las puntas;Gira con las muñecas", "Saltar demasiado alto."),
        e("rodillas_altas", "Rodillas altas", CUERPO, ESCALADOR, setOf(CUADRICEPS), setOf(ABDOMEN, GEMELOS),
            "Corre sin moverte;Sube las rodillas a la cadera;Brazos activos", "Ir encorvado."),
        e("salto_caja", "Salto al cajón", CUERPO, SALTO, setOf(CUADRICEPS, GLUTEO), setOf(GEMELOS),
            "Delante del cajón;Salta y cae con los dos pies;Baja andando", "Bajar saltando hacia atrás."),
        e("superman", "Superman", CUERPO, PLANCHA, setOf(LUMBAR, GLUTEO), setOf(HOMBRO),
            "Boca abajo, brazos estirados;Levanta brazos y piernas;Aguanta y baja", "Subir de golpe."),
        e("bird_dog", "Pájaro-perro", CUERPO, PLANCHA, setOf(LUMBAR, ABDOMEN), setOf(GLUTEO),
            "A cuatro patas;Estira un brazo y la pierna contraria;Aguanta y alterna", "Girar la cadera."),
        e("oso", "Paso de oso", CUERPO, ESCALADOR, setOf(ABDOMEN, HOMBRO), setOf(CUADRICEPS),
            "A cuatro patas con las rodillas en el aire;Avanza mano y pie contrarios;Espalda plana", "Subir la cadera."),
        e("plancha_toques", "Plancha con toques de hombro", CUERPO, PLANCHA, setOf(ABDOMEN), setOf(HOMBRO, OBLICUOS),
            "Plancha alta;Toca el hombro contrario;Alterna sin mover la cadera", "Balancear la cadera."),
    )
}
