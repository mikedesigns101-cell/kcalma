# Datos de alimentos (Fase 1)

Scripts que montan la base de datos de alimentos de la app con datos abiertos.
Solo necesitan **Python 3.10+**, sin librerías extra.

## Cómo se ejecuta

Desde `datos\scripts\`, en orden:

```bash
python 01_descargar_off.py      # descarga Open Food Facts (~1,3 GB, se puede reanudar)
python 02_filtrar_off.py        # filtra España, limpia macros y asigna supermercado (~10-20 min)
python 03_descargar_bedca.py    # descarga BEDCA (~16 min la primera vez; luego usa la caché)
python 04_construir_bd.py       # une todo en salida\alimentos.db
```

Para probar una búsqueda:

```bash
python 04_construir_bd.py "yogur griego hacendado"
```

Para actualizar los datos (Open Food Facts cambia a diario): borra `crudo\openfoodfacts_products.csv.gz`
y vuelve a ejecutar los cuatro scripts.

## Carpetas

| Carpeta | Contenido |
|---|---|
| `scripts\` | Los scripts y `supermercados.json` (marcas propias de cada cadena) |
| `crudo\` | Descargas originales sin tocar (no se edita a mano) |
| `salida\` | `off_espana.csv`, `bedca.csv` y `alimentos.db` |

## Cómo se asigna el supermercado

`supermercados.json` tiene, para cada cadena, sus **marcas propias** y cómo aparece la **tienda** en Open Food Facts.

1. Si la marca es marca blanca (Hacendado, Milbona, Alipende…), el supermercado es seguro.
2. Si no, y el producto solo aparece en una tienda conocida, se usa esa tienda.
3. Las marcas de fabricante (Danone, Pascual, Gallo…) que se venden en todas partes se quedan sin supermercado. Es lo correcto, porque se encuentran por marca.

Para añadir o corregir marcas se edita el JSON y se vuelve a ejecutar desde el paso 2.

## Limpieza que se aplica

- Solo productos con `countries_tags` = España.
- Hacen falta nombre, kcal, proteínas, hidratos y grasas.
- Valores imposibles fuera (más de 900 kcal/100 g, macros por encima de 100 g…).
- Las kcal tienen que cuadrar con 4·P + 4·H + 9·G (con margen). Si no, la ficha está mal metida.
- Códigos de barras duplicados fuera.
- Sal calculada desde sodio cuando falta (sal = sodio × 2,5).

## Fuentes y licencias

- **Open Food Facts**: licencia ODbL. Uso libre, también comercial, pero hay que **citar la fuente**
  ("Datos de Open Food Facts, openfoodfacts.org") y compartir con la misma licencia las mejoras de la base de datos.
- **BEDCA** (AESAN, Ministerio): datos públicos de consulta. Antes de publicar la app en Google Play
  hay que revisar sus condiciones de uso y redistribución, y citarla.
