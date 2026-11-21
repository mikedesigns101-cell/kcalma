# App de Calorías por Foto (Android) — Plan del proyecto

> Proyecto: `D:\3. Claude\6. AppCalorias`
> Fecha: 16/10/2026
> Estado: diseño, todavía sin código. Fase 1 (base de datos) hecha.

---

## 0. Decisiones de Mikel (20/11/2026). Mandan sobre el resto del documento

| Tema | Decisión |
|---|---|
| Plataforma | **Solo Android**, Kotlin + Jetpack Compose. iPhone no, de momento |
| Uso | **Gratis**, para Mikel y sus amigos. Objetivo: que Mikel pierda peso fácilmente. Rentabilizar quizá más adelante |
| Distribución | **APK en un repositorio de GitHub** (como fud-ai u openGym). **Nada de Google Play** |
| IA | **Gemini** (gratis) por defecto. **Cada usuario puede poner la clave de API de la IA que quiera** (Gemini, Claude, OpenAI…). No se entrena un modelo propio |
| Carpeta | Se sigue en `6. AppCalorias` |
| Nombre | **Kcalma** |
| Estilo | **B, oscuro premium**, con la paleta de Raideo: azul petróleo `#070c10`, degradado cian `#3ad6f0` → magenta `#e05cc0`, títulos en Outfit. Debe poder cambiarse en el futuro, así que habrá un tema centralizado y quizá un selector de temas. Maqueta: `maquetas/kcalma-hoy.html` |
| Proceso | Mikel quiere ver una captura o maqueta antes de implementar cualquier pantalla |
| Identidad de la app | `applicationId = com.mikel.kcalma`, minSdk 26 (Android 8+). Aprobado ("Sí, todo así", 20/11/2026). No se cambia una vez instalada en móviles de amigos. Antes de compartir el APK hay que crear una clave de firma de release, guardada fuera del repositorio y con copia de seguridad |
| Nuevo alcance | Onboarding de actividad muy claro (trabajo, pasos, entrenos), calorías adaptativas, recomendador que cuadra las comidas con las kcal y macros restantes (alergias, gustos, supermercados españoles), uso diario 3-4 veces |
| Futuro | Módulo de gimnasio inspirado en openGym (sin reutilizar su código AGPL ni sus imágenes) |

### Onboarding (aprobado el 20/11/2026, maqueta en `maquetas/kcalma-onboarding.html`)
8 pantallas: bienvenida, sobre ti (el % de grasa es opcional), trabajo (4 niveles con profesiones de ejemplo), pasos (4 rangos más Health Connect), ejercicio (días, tipo y duración), objetivo con ritmo, tu plan con el desglose, y alergias y gustos (se puede saltar).
- **Ritmo de pérdida:** suave −0,25, normal −0,5 y rápido −0,75 kg/semana. Nunca más del 1 % del peso por semana.
- **Mínimo de calorías:** 1.500 kcal en hombres y 1.200 en mujeres.
- **Alergias y gustos** van en el onboarding, con la opción "Saltar por ahora".
- **Clave de Gemini:** se pide la primera vez que se pulsa la cámara, con una guía paso a paso. No va en el onboarding.

**Fórmulas** (las elige Claude; están en `dominio/Calculadora.kt`, con tests):
- **Reposo:** Mifflin-St Jeor, o Katch-McArdle si se conoce el % de grasa.
- **Factor de actividad = trabajo + pasos.** Trabajo: sentado 1,20 · de pie 1,30 · moviéndose 1,40 · físico 1,55. Pasos: menos de 5k +0 · 5–8k +0,15 · 8–12k +0,25 · más de 12k +0,35.
- **Ejercicio:** (MET − 1) × kg × horas × días / 7. MET: pesas 5, cardio 8, deporte 7.
- **Perder:** déficit = kg/semana × 7.700 / 7.
- **Recomposición:** −10 %.
- **Ganar:** suave +150, normal +250 y rápido +400 kcal.
- **Proteína:** 2,0 g/kg (1,8 al ganar). Si el IMC es 30 o más, se calcula sobre el peso objetivo.
- **Grasa:** 0,8 g/kg. **Hidratos:** el resto.

### Diario y buscador (aprobado el 20/11/2026, maqueta en `maquetas/kcalma-diario.html`)
4 pantallas: añadir (buscador con filtros por supermercado, genéricos y "Mis comidas", y recientes), ficha (unidad / gramos / envase, macros y lo que queda para hoy), escáner y comida del día (borrar deslizando, guardar como comida habitual).
- **Base de datos en la APK:** la versión reducida (genéricos + productos de supermercados españoles + productos escaneados alguna vez, unos 20 MB). Funciona sin internet.
- **Código de barras no encontrado:** se consulta Open Food Facts por internet y el producto se guarda en el móvil.
- **Escáner:** ML Kit de Google (gratis y sin conexión).

### Foto con IA (aprobada el 20/11/2026, maqueta en `maquetas/kcalma-foto.html`)
4 pantallas: clave de Gemini la primera vez (guía de 3 pasos y aviso de que el plan gratuito de Google puede usar las fotos), foto con nota opcional y galería, análisis, y revisión con deslizadores de gramos, "Cambiar" y "Añadir algo que falte".
- **Proveedores:** solo Gemini en la primera versión. OpenAI y Claude, más adelante en "Elegir proveedor".
- **Alimento que no está en la base:** se usa la estimación de kcal y macros de la IA, marcada como "Estimado" y editable.
- **Modelo (medido el 20/11/2026 con 5 fotos reales):** Gemini Flash Lite primero (2-3 s, reconoce igual que 3.5 Flash); 3.5 Flash y el alias Flash solo de reserva si Lite falla (tardan 6-30 s o dan 503). Mikel pidió prioridad a rapidez y acierto.
- **Emparejado con la base:** los ingredientes de la búsqueda tienen que estar en el nombre del alimento (la tortilla de patatas nunca es «tortilla a la francesa»); si nada encaja, se usa la estimación de la IA.

### GitHub y actualizaciones (20/11/2026)
- **Repositorio público** en la cuenta de GitHub de Mikel. Las versiones se publican como *Releases* con la APK firmada.
- **Actualización desde la app:** una vez al día consulta la última Release. Si hay una nueva, muestra una tarjeta arriba en «Hoy» ("✨ Nueva versión X · Actualizar", con ✕ para ocultarla hasta la siguiente). Al pulsar, descarga la APK y Android pide confirmar la instalación.
- **Firma de release:** la clave está fuera del repositorio (`C:\Users\Mikel\.kcalma\`). Si se pierde, los amigos no podrán actualizar sin desinstalar.

### Informes de errores (20/11/2026)
- La app guarda en el móvil un registro local (versión, modelo de móvil, fallos de cámara y de Gemini, cierres; sin fotos, claves ni diario).
- **«Mandar informe» abre una incidencia en GitHub** con el informe ya escrito (es público y pide cuenta de GitHub; el usuario lo revisa antes de enviarlo).
- **El botón está en cada pantalla de error** (cámara y análisis de foto), con el error incluido.
- Las pestañas y botones que aún no existen (Progreso, Ideas, Perfil, «Ver más») muestran «llega en una próxima versión».

### Perfil (20/11/2026, maqueta en `maquetas/kcalma-perfil.html`; Mikel pidió hacer todas las pantallas y sacarlas juntas, se aplican las opciones recomendadas)
- Pestaña con cabecera, tarjeta del plan y grupos: Mis datos, Comida, Foto con IA, Datos y ayuda.
- **Editar un bloque reutiliza las pantallas del onboarding** y enseña el plan nuevo antes de guardar.
- **Peso con historial y fecha** (hoja rápida con − / +); alimenta Progreso.
- **Plan a mano:** se pueden fijar kcal a mano, con «Volver al calculado».
- **Copia de seguridad** (exportar e importar un archivo) en esta versión.
- También: cambiar o borrar la clave de Gemini, Mandar informe y Buscar actualizaciones.

### Progreso e Ideas (aprobadas el 20/11/2026, maqueta en `maquetas/kcalma-progreso-ideas.html`)
- **Progreso:** gráfica de peso con la línea del ritmo del plan y fecha estimada, calorías de la semana frente al objetivo, proteína media y racha de días apuntados; periodos 1 mes, 3 meses y todo.
- **Ideas:** lo que queda del día y filtros (comida, rápidas, supermercado, lo que me encanta). **Fuente: las dos.** Una lista propia de recetas españolas calculadas con la base y filtradas por lo que queda, alergias y gustos (sin internet), y el botón «Más ideas con IA» que pide otras a Gemini. Al tocar una: ingredientes, pasos y «Añadir a la comida».
- **Publicación:** la 0.3.0 sale ya con la foto arreglada y Perfil; Progreso e Ideas van en la 0.4.0.
- **Ajustes al implementar (0.4.0, Mikel pidió que decidiera yo):** sin chip de supermercado (las recetas usan alimentos genéricos, no productos de una cadena). Reparto orientativo por comida: desayuno 22 %, media mañana 10 %, comida 35 %, merienda 10 %, cena 25 %; la cena se lleva lo que quede del día, con un tope del 40 % del plan. Las cantidades se escalan entre ×0,6 y ×1,6 y la etiqueta «encaja» sale a ±12 %. Las ideas de la IA se emparejan con la base y, si no hay un alimento fiable, quedan como «Estimado por IA». En Hoy salen las ideas de la próxima comida sin apuntar; si ya están todas, no salen.

### Gustos y atajos (21/11/2026, v0.4.2, Mikel pidió categorías y que yo eligiera atajos útiles)
- **Gustos por categorías:** la app ofrece 26 categorías (Setas, Pescado, Marisco, Cerdo…); un toque = no me gusta, dos = me encanta. Cada categoría lleva sus palabras (Setas → champiñón, boletus, níscalo…), que filtran las recetas y se mandan a la IA. Lo que se escribe a mano solo bloquea esa palabra («nata» no quita la leche).
- **Atajos en Añadir:** «Repetir tu comida de ayer» (lo último que apuntaste en esa comida, de un toque), «Sueles tomar» (lo que más repites en esa comida en 45 días, con la cantidad de siempre) y «Añadir solo calorías» (kcal y proteína opcional, para comer fuera).

### Funciones de la 0.5.0 (21/11/2026)
Elegidas por Mikel: ajuste automático del plan, recordatorios, mis platos caseros, widget, Health Connect, foto de la etiqueta y lista de la compra. Descartado: calorías por semana.
- **Ajuste automático:** la app propone y Mikel acepta. Una vez por semana, si el gasto real calculado con el peso y lo comido difiere, sale una tarjeta con el cambio; nunca se aplica solo.
- **Health Connect:** se suma la mitad de las calorías activas de más que diga el móvil o el reloj (suelen exagerar).
- **Foto de la etiqueta:** la lee Gemini con la clave del usuario.
- **Publicación:** todo junto en la 0.5.0.
- **Detalles provisionales (los decidió Claude el 21/11/2026 sin ti, porque estabas fuera; revísalos):**
  - Ajuste: mira los últimos 28 días. Necesita 14 días apuntados (al menos la mitad del plan) y 3 pesadas separadas 10 días o más. Solo propone si la diferencia pasa de 100 kcal, con un tope de ±300 por propuesta y una propuesta por semana. Se guarda como corrección del gasto y no se aplica con kcal a mano.
  - Health Connect lee **pasos**, no calorías activas, porque los entrenamientos ya están en el plan. Suma la mitad de lo que camines por encima de lo que dijiste en el onboarding (4.000 / 6.500 / 10.000 / 13.500), a unas 0,5 kcal por kg cada 1.000 pasos.
  - Recordatorios por defecto: desayuno 10:30, comida 16:00, cena 22:30 y peso los lunes a las 9:00. Media mañana y merienda van apagadas. Solo avisa si esa comida está sin apuntar, y no se programa nada hasta que abres la pantalla.
  - Widget de 4×2: anillo, kcal restantes y botones de foto y añadir.
  - Los platos caseros salen en «Mis comidas» y en la búsqueda. Entran en la copia de seguridad; la lista de la compra no.

### Rediseño «cristal» (21/11/2026, Mikel, a partir de una imagen de referencia)
- Estilo cristal: paneles esmerilados con borde brillante, luces de color difuminadas detrás, orbe de vidrio con el anillo de kcal y botón + flotante en la barra. Maquetas en `maquetas/kcalma-cristal.html` y `kcalma-cristal-2.html`.
- **Paleta: los colores de Kcalma (cian → magenta), opción 1A**, y además variaciones, aprobadas: Kcalma (por defecto), Hielo (cian → azul), Aurora (cian → verde menta) y Orquídea (magenta → violeta), elegibles en Perfil → Apariencia. Maqueta en `maquetas/kcalma-colores.html`.
- Iconos propios en vez de emojis: trazo redondeado de 1,8, degradado del tema y burbuja de cristal (aprobado).
- Animaciones fluidas y limpias, de 0,25 a 0,6 s con frenada suave: orbe que se dibuja y respira, tarjetas en cascada, + con hoja de cristal, burbuja en la barra, cifra que cuenta al añadir, iconos que se trazan. Demo en `maquetas/kcalma-animaciones.html` (aprobado).
- **Orbe: pompa «B · Líquida»** (elegida entre tres en `maquetas/kcalma-pompas.html`): dentro hay líquido que se mece con dos olas; su nivel es lo comido hoy; al tocarla chapotea; flota y suben burbujitas. Quieta si el móvil tiene «Quitar animaciones».
- **Alcance: todo de golpe** en una 0.7.0. Antes, maquetas cristal de todas las pantallas para aprobarlas.

### Aviso de malware y comodidad (21/11/2026, Mikel)
- **Aviso al instalar:** se hacen las tres cosas. Mikel manda a Google la apelación de Play Protect con el texto de `docs/apelacion-play-protect.md`. Cada release lleva el enlace a su análisis en VirusTotal. El README tiene una guía de instalación que explica los avisos.
- **0.6.0, mejoras de comodidad elegidas:** mover entradas (o una comida entera) a otra comida, recordar la última cantidad de cada alimento y buscar por voz. Descartadas por ahora: deshacer al borrar, ver días anteriores, copiar de otro día, cantidades rápidas y contador de agua. Maqueta en `maquetas/kcalma-0.6.html` (aprobada).
- **Actualizaciones sin preguntar (0.6.0):** Kcalma se instala sus propias actualizaciones con una sesión de `PackageInstaller` (Android 12 o superior), así que Android ya no pregunta. Si falla, vuelve al instalador clásico. Los permisos se conservan al actualizar (comprobado en el emulador). Si piden otra vez la cámara, lo más probable es que se eligiera «Solo esta vez».
- **Cocido (0.6.1, 21/11/2026, Mikel: «las dos cosas»).** La ficha de pasta, arroz, legumbres, cuscús y quinoa secos tiene la unidad «Cocido»: se pone lo que pesa hecho y se divide por un factor medio (pasta y legumbres ×2,4, arroz ×2,7, cuscús y quinoa ×2,5) para calcular con los datos en seco. Además, buscar «espaguetis», «macarrones», etc. enseña arriba la pasta genérica, seca y cocida, y el buscador tolera plurales. Maqueta en `maquetas/kcalma-0.6.1.html`.
- **Extras de la 0.6.1 (elegidos por Claude con permiso de Mikel):** el dictado entiende «cocidos» («250 gramos de espaguetis cocidos» abre la ficha en Cocido); «Deshacer» durante 5 segundos al borrar un alimento; «1 ud» pasa a «1 ración»; el tomate frito, las conservas y las salsas van a Despensa en la lista de la compra.
- **Aviso de actualización al abrir (0.6.2, 21/11/2026, a petición de Mikel):** la tarjeta «Nueva versión» de Hoy se comprueba cada vez que se vuelve a la app, no solo al arrancarla (como mucho una consulta a GitHub cada 10 minutos). Se llama 0.6.2 en vez de 0.6.1.1 para seguir el formato de tres números.
- **Verificación de desarrolladores de Google (21/11/2026, Mikel: esperar).** Se decide cuando Google dé fechas para Europa. Google obligará a que las APK sueltas sean de un desarrollador verificado. En España, previsiblemente desde 2027. Hay una cuenta gratuita de «distribución limitada» (hasta 20 dispositivos, solo con correo). Opciones: registrarse con esa cuenta o con la normal, que pide documento de identidad, o esperar a que haya fechas para Europa.

**Arquitectura sin servidor (confirmada el 20/11/2026):** no hay Supabase ni ningún backend. La app funciona entera en el móvil: base de datos local, la clave de cada usuario guardada cifrada (Android Keystore) y la llamada a la IA directamente desde la app. Cada amigo saca su propia clave gratis de Gemini en Google AI Studio, con una guía dentro de la app. La clave de Mikel nunca va en el APK. Sin sincronización: botón de exportar e importar copia de seguridad. Esto sustituye al "servidor intermedio" y a Supabase de las secciones 3 y 5.

---

### Entrenamientos (21/11/2026, Mikel; maqueta `maquetas/kcalma-entreno.html` aprobada)
- Apartado de gimnasio para usar Kcalma también como app de entreno: hacer y registrar sesiones, rutinas hechas y editables, rutinas propias.
- **Progresión:** cada serie sale rellenada con lo de la última vez (80 kg × 8 → la próxima vez 80 × 8). Doble progresión calculada por código, no por la IA: con todas las series en lo alto del rango propone +2,5 kg; en kettlebell, la siguiente pesa que tenga el usuario o más series y reps.
- **Mapa del cuerpo** (frente y espalda) al ver o crear una rutina y en cada ejercicio, con los músculos que trabajan iluminados. Dibujado por nosotros con el estilo Kcalma.
- **Kettlebell como apartado propio:** rutinas hechas y propias; series, EMOM, AMRAP e intervalos; solo las pesas que tenga el usuario.
- **Ficha de ejercicio:** figura animada propia (un maniquí estilizado dibujado en código, en bucle y con los músculos iluminados), pasos, errores típicos, historial y récord. Unos 150 ejercicios bien hechos, no miles.
- **Navegación:** la barra pasa a ser Hoy, Ideas, +, Entreno, Progreso. A Perfil se llega con un botón arriba en Hoy, que ocupa el sitio del botón de peso.
- **Entrenador IA** (Gemini, con la clave del usuario): cada semana **propone** cambios en una tarjeta y el usuario los acepta o descarta. Tiene además un chat para dudas de técnica, ejercicios y dieta.
- No se suman kcal del gimnasio: los días de pesas ya cuentan en el factor de actividad.
- **Fases:** la 0.8.0 trae biblioteca, rutinas, registro, progresión, mapa y kettlebell; la 0.9.0, el entrenador IA y el chat.
- **Hecho en la 0.8.0 (decisiones de implementación, pendientes de que Mikel las revise):**
  - "Anterior" es por ejercicio: lo último que hiciste en ese ejercicio, en cualquier rutina.
  - Las rutinas hechas vienen sin días; la tarjeta "Hoy toca" solo sale si pones días o tienes un entreno a medias.
  - Los entrenos van en `entreno.db`, aparte del diario, y entran en la copia de seguridad.
  - Editar una rutina hecha guarda tu copia con el mismo id ("Volver a la original" la deshace).
  - Orden de ejercicios en el editor con flechas ↑ ↓, no arrastrando.
  - Catálogo inicial: 31 ejercicios (9 de kettlebell) y 10 rutinas hechas (3 gimnasio, 5 kettlebell con EMOM y AMRAP, 2 casa). Faltan ejercicios hasta los ~150 y el modo intervalos.
  - EMOM apunta solo cada minuto; AMRAP cuenta rondas con + y −.

### Informes de fallos y 0.9.0 (22/11/2026, Mikel; maqueta `maquetas/kcalma-0.9.html`, pendiente de aprobar)
- **Envío directo a GitHub, sin cuenta:** la APK lleva una clave de GitHub que solo puede crear incidencias en el repo de Kcalma (fine-grained, *Issues: write*). La crea Mikel; se guarda fuera del repo, en `C:\Users\Mikel\.kcalma\`. Si alguien la saca y manda spam, se revoca.
- **Botón pequeño de reportar en todas las pantallas** (un bicho de cristal en la esquina). Abre una hoja: "¿Qué ha pasado?" + pantalla, versión, móvil y registro de errores (sin datos personales).
- **"Arregla los errores":** Claude lee las incidencias con la etiqueta `informe`, enseña a Mikel la lista y lo que cambiaría en cada una, arregla solo lo aprobado y enseña capturas antes de publicar.
- **0.9.0 incluye:** entrenador IA (propuesta semanal en tarjeta) y chat, unos 150 ejercicios con biblioteca, e intervalos de kettlebell (trabajo/descanso por rondas).

## 1. Qué hace la app

1. **Haces una foto** del plato (o de varios platos).
2. **Añades una nota opcional** (texto o voz) con lo que no se ve en la foto:
   *"lleva una cucharada de aceite", "el arroz es integral", "la salsa es de nata", "son 2 huevos"*.
3. La IA **identifica cada alimento** y **estima los gramos**.
4. La app **busca cada alimento en nuestra base de datos española** (genéricos + productos de Mercadona, Dia, Lidl, Aldi, Ahorramás, etc.) y **calcula calorías, proteínas, hidratos, grasas** (y fibra, azúcar, sal).
5. Te enseña el resultado **editable**: puedes cambiar gramos con un deslizador, cambiar un alimento por otro ("no es pan blanco, es pan de centeno Hacendado") o añadir lo que falte.
6. Se guarda en tu **diario del día** con objetivos de calorías y macros.

Extra: **escáner de código de barras** para productos envasados (lo más preciso que hay: datos exactos de la etiqueta).

---

## 2. La idea clave: la IA *reconoce*, la base de datos *calcula*

Un error típico es pedirle a la IA "dime cuántas calorías tiene esto". Se inventa los números y cambian cada vez.

Nosotros lo haremos en dos pasos:

| Paso | Quién lo hace | Qué devuelve |
|---|---|---|
| Reconocer y estimar cantidad | IA de visión (Claude) | Lista de alimentos + gramos estimados + confianza |
| Calcular nutrientes | Nuestra base de datos | kcal y macros reales por 100 g × gramos |

Así los valores nutricionales salen de fuentes fiables (tablas oficiales y etiquetas de supermercado), y lo único "estimado" son los gramos, que el usuario puede corregir.

---

## 3. Cómo funciona por dentro (flujo)

```
[Móvil Android]
   │ 1. Foto (comprimida a ~1024 px) + nota del usuario
   ▼
[Nuestro servidor (backend)]  ← guarda la clave de la API, nunca va en el móvil
   │ 2. Envía foto + nota + instrucciones a Claude (visión)
   ▼
[Claude]
   │ 3. Devuelve JSON:
   │    [{ "alimento": "arroz blanco cocido", "gramos": 180, "confianza": 0.8 },
   │     { "alimento": "pechuga de pollo a la plancha", "gramos": 150, ... },
   │     { "alimento": "aceite de oliva", "gramos": 10, "origen": "nota usuario" }]
   ▼
[Servidor] 4. Busca cada alimento en la base de datos (búsqueda por texto + sinónimos)
   │          y elige la mejor coincidencia (o varias opciones)
   ▼
[Móvil] 5. Muestra el resultado editable → el usuario confirma → se guarda en el diario
```

### Lo que la IA tiene en cuenta
- **Ocultos**: aceite, mantequilla, salsas, azúcar en el café… La IA preguntará si no está claro
  (*"¿Lleva aceite? ¿Cuánto?"*) con botones rápidos: *nada / un chorrito / una cucharada*.
- **Referencias de tamaño**: plato, cubiertos, mano, lata… para estimar gramos.
- **Cocido vs crudo**: muy importante (100 g de arroz crudo ≈ 350 kcal; cocido ≈ 130 kcal).
- **Platos españoles enteros**: tortilla de patatas, paella, lentejas, cocido, croquetas, salmorejo… tendrán ficha propia como "receta" con sus ingredientes.

### Cómo mejorar la estimación de gramos (el punto más difícil)
- MVP: estimación de la IA + deslizador para corregir.
- Después: foto desde dos ángulos, objeto de referencia (tarjeta / mano), y la **API de profundidad de ARCore** en móviles compatibles.
- La app **aprende de tus correcciones**: si siempre subes el arroz un 20 %, lo tendrá en cuenta.

---

## 4. Base de datos de alimentos (España)

Combinamos varias fuentes, de más a menos fiable:

### 4.1 Alimentos genéricos — **BEDCA**
Base de Datos Española de Composición de Alimentos (AESAN / Ministerio). Valores oficiales de alimentos básicos españoles: fruta, verdura, carne, pescado, legumbres, platos típicos. Es la base para "pollo", "lentejas cocidas", "manzana"...

### 4.2 Productos de supermercado — **Open Food Facts**
Base de datos abierta (licencia ODbL, uso libre citando la fuente) con **cientos de miles de productos vendidos en España**, con código de barras y tabla nutricional de la etiqueta. Se puede filtrar por país = España y por marca. Incluye las marcas blancas:

| Supermercado | Marcas propias (ejemplos) |
|---|---|
| Mercadona | Hacendado, Deliplus, Bosque Verde, Compy |
| Dia | Dia, Dia Delicious, Al Punto |
| Lidl | Milbona, Pikok, Cien, Solevita, Combino, Belbake, Crownfield, Freeway, Chef Select |
| Aldi | Milsani, Golden Bridge, Choceur, Lyttos, Ombra |
| Ahorramás | Alipende |
| Carrefour / Eroski / Alcampo | Carrefour, Eroski, Auchan |

> Las marcas de cada cadena se revisarán al montar la base de datos (algunas cambian con el tiempo).

Nos descargamos el volcado completo de Open Food Facts, nos quedamos con los productos de España, los limpiamos (quitar duplicados, fichas sin macros, valores imposibles) y los metemos en nuestra base de datos con un campo `supermercado`.

### 4.3 Webs de los supermercados — con cuidado
Mercadona, Dia, etc. muestran información en sus tiendas online, pero **copiar sus datos de forma automática (scraping) puede ir contra sus condiciones de uso**. Plan:
- No dependemos de ello para el MVP.
- Si queremos rellenar huecos, lo revisamos caso a caso (o pedimos permiso / usamos datos que publiquen abiertamente).

### 4.4 Aportaciones de los usuarios
Si escaneas un código que no está, la app te pide **foto de la tabla nutricional**, la IA la lee (OCR) y crea la ficha. Opcionalmente se puede enviar también a Open Food Facts para que mejore para todos.

### 4.5 Respaldo
USDA FoodData Central (EE. UU., libre) para alimentos raros que no estén en lo anterior.

### Estructura de la tabla de alimentos (simplificada)

| Campo | Ejemplo |
|---|---|
| id | 84123 |
| nombre | Yogur griego natural |
| marca | Hacendado |
| supermercado | Mercadona |
| codigo_barras | 8480000xxxxxx |
| kcal_100g | 120 |
| proteinas_100g | 6.0 |
| hidratos_100g | 4.0 |
| azucares_100g | 4.0 |
| grasas_100g | 9.0 |
| saturadas_100g | 6.0 |
| fibra_100g | 0 |
| sal_100g | 0.1 |
| raciones | "1 unidad = 125 g" |
| fuente | openfoodfacts / bedca / usuario |
| sinonimos | "yogur griego, yogurt griego" |

---

## 5. Tecnología

### App Android
| Parte | Tecnología |
|---|---|
| Lenguaje / UI | **Kotlin + Jetpack Compose** |
| Cámara | CameraX |
| Código de barras | ML Kit Barcode Scanning (funciona sin internet) |
| Base de datos local | Room (SQLite) con búsqueda de texto rápida (FTS) — los genéricos y lo más usado funcionan sin conexión |
| Red | Retrofit / Ktor |
| Gráficas del diario | Vico o Compose Charts |
| Voz → texto para la nota | SpeechRecognizer de Android (gratis, en el móvil) |

### Servidor (backend)
| Parte | Tecnología |
|---|---|
| Base de datos + usuarios | **Supabase** (PostgreSQL + login + almacenamiento) |
| Función que llama a la IA | Supabase Edge Function o Cloudflare Worker |
| IA de visión | **Claude Sonnet 5.5** (`claude-sonnet-5-5`) con salida en JSON estructurado |
| Búsqueda de alimentos | PostgreSQL con búsqueda por texto en español + similitud (pg_trgm) |

**Por qué un servidor intermedio:** la clave de la API de la IA no se puede meter en el APK (cualquiera podría sacarla y gastarte dinero). Además, así controlamos límites de uso por usuario y podemos cambiar de modelo sin actualizar la app.

---

## 6. Pantallas

1. **Hoy** — anillo de calorías + barras de proteínas / hidratos / grasas; lista de comidas (desayuno, comida, merienda, cena).
2. **Cámara** — botón grande de foto, botón de código de barras, botón de micrófono para la nota.
3. **Resultado** — foto con los alimentos marcados, cada uno con gramos (deslizador) y macros; botones "cambiar alimento", "añadir", "guardar".
4. **Buscar** — buscador manual con filtro por supermercado ("solo Mercadona").
5. **Historial / estadísticas** — semanas, medias, peso.
6. **Perfil** — objetivos (perder / mantener / ganar), cálculo automático de calorías objetivo, preferencias.

---

## 7. Fases de desarrollo

### Fase 0 — Preparación
- Instalar Android Studio, crear el proyecto Kotlin/Compose.
- Cuenta de Supabase y clave de la API de Anthropic.

### Fase 1 — Base de datos
- Script (Python) que descarga Open Food Facts, filtra España, limpia y asigna supermercado por marca.
- Importar BEDCA (genéricos).
- Subir todo a Supabase + generar un paquete reducido para la base local del móvil.

### Fase 2 — MVP de la app
- Diario manual (buscar y añadir alimentos).
- Escáner de código de barras.
- Objetivos diarios.

### Fase 3 — Foto + IA
- Backend que recibe foto + nota y devuelve alimentos y gramos.
- Emparejar con la base de datos.
- Pantalla de resultado editable y preguntas rápidas sobre ocultos (aceite, salsas).

### Fase 4 — Mejoras
- Recetas propias y platos favoritos ("mi desayuno de siempre").
- Leer etiquetas nutricionales con la cámara para productos que falten.
- Aprendizaje de las correcciones del usuario.
- Estimación de volumen con ARCore.
- Widget y recordatorios.

### Fase 5 — Publicación
- Política de privacidad (RGPD: las fotos son datos personales; borrarlas tras analizarlas salvo que el usuario quiera guardarlas).
- Aviso: *la app da estimaciones, no sustituye a un nutricionista*.
- Google Play (cuenta de desarrollador).

---

## 8. Costes aproximados
- **Open Food Facts, BEDCA, USDA**: gratis (citando la fuente).
- **Supabase**: plan gratuito suficiente para desarrollo y primeros usuarios.
- **IA**: se paga por foto analizada; compensa reducir la foto a ~1024 px y limitar análisis diarios en la versión gratuita. Los precios exactos los miramos al llegar a la Fase 3.
- **Google Play**: pago único de alta como desarrollador.

---

## 9. Riesgos y cómo los cubrimos

| Riesgo | Solución |
|---|---|
| La IA se equivoca de gramos | Siempre editable + preguntas sobre ocultos + aprende de correcciones |
| Producto que no está en la base | Escanear etiqueta con la cámara y crear ficha |
| Datos de Open Food Facts erróneos | Filtros de limpieza (p. ej. kcal ≠ 4·prot + 4·hidr + 9·grasa ± margen) |
| Condiciones de uso de los supermercados | No hacer scraping sin revisarlo; usar fuentes abiertas |
| Clave de la API robada | Solo en el servidor, nunca en la app |
| Privacidad de las fotos | Se borran tras el análisis por defecto |

---

## 10. Siguiente paso
Empezar por la **Fase 1 (base de datos)**: es la base de todo y se puede probar sin la app. Después montamos el esqueleto de la app en Android Studio.
