# Kcalma

App de Android para contar calorías y macros, pensada para España: alimentos de Mercadona, Lidl, Carrefour, Dia y otros supermercados, escáner de código de barras y foto del plato con IA.

## Instalar

1. Ve a [Releases](../../releases/latest) y descarga el archivo `kcalma-X.Y.Z.apk` desde el móvil.
2. Ábrelo. Android pedirá permitir «instalar apps desconocidas» para tu navegador o gestor de archivos: acéptalo.
3. Listo. Las siguientes versiones se instalan **desde la propia app**: cuando haya una nueva, aparece un aviso arriba en «Hoy».

Requiere Android 8 o superior.

### Los avisos al instalar son normales

Kcalma no está en Google Play, así que Android avisa como con cualquier app descargada de internet. El código es público en este repositorio y cada versión enlaza su análisis en [VirusTotal](https://www.virustotal.com).

| Aviso | Por qué sale | Qué hacer |
|---|---|---|
| Chrome: «Este tipo de archivo puede dañar tu dispositivo» | Sale con **cualquier** APK. | Pulsa **Descargar de todas formas**. |
| «Por tu seguridad, el teléfono no puede instalar apps desconocidas de esta fuente» | Es la primera vez que instalas algo desde Chrome o desde tus archivos. | **Ajustes** → activa **Permitir de esta fuente** → vuelve atrás. |
| Play Protect: «App no segura bloqueada» o «¿Analizar la app?» | Google aún no conoce la app y pide permisos que vigila: actualizarse sola y leer los pasos. | **Analizar app** (Google la revisa y suele dejarla instalar) o **Más detalles → Instalar de todas formas**. |

Qué hace con cada permiso: la **cámara**, para el escáner y las fotos; **instalar apps**, solo para actualizarse desde este repositorio; las **notificaciones**, para los recordatorios que actives; los **pasos** (Health Connect), solo si lo conectas, para sumar a tu objetivo lo que camines de más.

## Qué hace

- Calcula tus calorías y macros según tu cuerpo, tu trabajo, tus pasos y tu entrenamiento.
- Diario con 5 comidas, buscador de casi 80.000 alimentos (funciona sin conexión), escáner de código de barras y comidas habituales.
- Si un código no está en el móvil, lo busca en Open Food Facts. Si tampoco está, puedes hacer una foto a la etiqueta.
- Foto del plato con IA (Gemini, con tu propia clave gratuita).
- Platos caseros, ideas de recetas que encajan con lo que te queda y lista de la compra.
- Ajuste semanal del objetivo según tu peso real, recordatorios, widget y pasos de Health Connect.

## Privacidad

Todo se guarda en tu móvil. No hay servidor ni cuentas. La app solo se conecta a internet para buscar códigos de barras en Open Food Facts, para comprobar si hay versiones nuevas en este repositorio y, si pones tu clave, para mandar a Gemini las fotos que tú hagas. Tu clave se guarda cifrada en el móvil.

## Datos de alimentos

- [Open Food Facts](https://world.openfoodfacts.org), bajo licencia [ODbL](https://opendatacommons.org/licenses/odbl/1-0/). La base incluida en la app (`app/src/main/assets/alimentos.db`) es una obra derivada y se distribuye bajo la misma licencia.
- [BEDCA](https://www.bedca.net), Base de Datos Española de Composición de Alimentos.

Los scripts que generan la base están en `datos/scripts/` (ver `datos/README.md`).

## Compilar

Android Studio o la línea de comandos con JDK 17 o superior:

```bash
./gradlew :app:assembleDebug
```
