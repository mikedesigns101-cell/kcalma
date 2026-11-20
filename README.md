# Kcalma

App de Android para contar calorías y macros, pensada para España: alimentos de Mercadona, Lidl, Carrefour, Dia y otros supermercados, escáner de código de barras y, muy pronto, foto del plato con IA.

## Instalar

1. Ve a [Releases](../../releases/latest) y descarga el archivo `kcalma-X.Y.Z.apk` desde el móvil.
2. Ábrelo. Android pedirá permitir «instalar apps desconocidas» para tu navegador o gestor de archivos: acéptalo.
3. Listo. Las siguientes versiones se instalan **desde la propia app**: cuando haya una nueva, aparece un aviso arriba en «Hoy».

Requiere Android 8 o superior.

## Qué hace

- Calcula tus calorías y macros según tu cuerpo, tu trabajo, tus pasos y tu entrenamiento.
- Diario con 5 comidas, buscador de casi 80.000 alimentos (funciona sin conexión), escáner de código de barras y comidas habituales.
- Si un código no está en el móvil, lo busca en Open Food Facts.

## Privacidad

Todo se guarda en tu móvil. No hay servidor ni cuentas. La app solo se conecta a internet para buscar códigos de barras en Open Food Facts y para comprobar si hay versiones nuevas en este repositorio.

## Datos de alimentos

- [Open Food Facts](https://world.openfoodfacts.org), bajo licencia [ODbL](https://opendatacommons.org/licenses/odbl/1-0/). La base incluida en la app (`app/src/main/assets/alimentos.db`) es una obra derivada y se distribuye bajo la misma licencia.
- [BEDCA](https://www.bedca.net), Base de Datos Española de Composición de Alimentos.

Los scripts que generan la base están en `datos/scripts/` (ver `datos/README.md`).

## Compilar

Android Studio o la línea de comandos con JDK 17 o superior:

```bash
./gradlew :app:assembleDebug
```
