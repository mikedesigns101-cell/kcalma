# App de Calorías por Foto (Android) — Plan del proyecto

> Proyecto: `D:\3. Claude\6. AppCalorias`
> Fecha: 16/10/2026
> Estado: diseño, todavía sin código. Fase 1 (base de datos) hecha.

---

## 0. Decisiones de Mikel (20/11/2026). Mandan sobre el resto del documento

Las decisiones de Mikel están en el portal del proyecto, en `portal/data/decisiones/`. Las nuevas se apuntan ahí con la skill `portal`, y mandan sobre el resto de este documento.

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
