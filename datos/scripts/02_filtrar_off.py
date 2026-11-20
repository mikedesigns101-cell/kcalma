"""Filtra el volcado de Open Food Facts: solo productos vendidos en España,
con macros válidos, y les asigna supermercado según su marca o tienda.

Lee el .csv.gz en streaming (no hace falta descomprimirlo, ~10 GB).
Salida: salida/off_espana.csv

Uso:  python 02_filtrar_off.py
"""
import csv
import gzip
import json
import sys
from collections import Counter
from pathlib import Path

from comun import CRUDO, SALIDA, normalizar, numero

ENTRADA = CRUDO / "openfoodfacts_products.csv.gz"
SALIDA_CSV = SALIDA / "off_espana.csv"
MAPA = Path(__file__).with_name("supermercados.json")

CAMPOS_SALIDA = [
    "codigo_barras", "nombre", "marca", "supermercado", "tiendas", "cantidad",
    "racion", "racion_g", "kcal_100g", "proteinas_100g", "hidratos_100g",
    "azucares_100g", "grasas_100g", "saturadas_100g", "fibra_100g", "sal_100g",
    "categoria", "nutriscore", "nova", "imagen", "popularidad",
]


def cargar_mapa():
    datos = json.loads(MAPA.read_text(encoding="utf-8"))
    marcas, tiendas = {}, {}
    for super_, info in datos.items():
        if super_.startswith("_"):
            continue
        for m in info["marcas"]:
            marcas[normalizar(m)] = super_
        for t in info["tiendas"]:
            tiendas[normalizar(t)] = super_
    return marcas, tiendas


def lista(campo):
    """'Hacendado, Mercadona' -> ['hacendado', 'mercadona'] normalizados."""
    return [n for n in (normalizar(x) for x in campo.split(",")) if n]


def asignar_supermercado(fila, marcas, tiendas):
    """Primero la marca (marca blanca = supermercado seguro); si no, la tienda."""
    for m in lista(fila["brands_tags"].replace("-", " ")) + lista(fila["brands"]):
        if m in marcas:
            return marcas[m], "marca"
    encontrados = {tiendas[t] for t in lista(fila["stores"]) if t in tiendas}
    if len(encontrados) == 1:
        return encontrados.pop(), "tienda"
    return "", ""


def macros_validos(kcal, prot, hidr, grasa):
    """Descarta fichas incompletas o imposibles."""
    if None in (kcal, prot, hidr, grasa):
        return False
    if not 0 <= kcal <= 900:
        return False
    if any(not 0 <= x <= 100 for x in (prot, hidr, grasa)) or prot + hidr + grasa > 101:
        return False
    # Las kcal deben cuadrar con 4·prot + 4·hidr + 9·grasa (margen por fibra,
    # alcohol y polialcoholes).
    calculadas = 4 * prot + 4 * hidr + 9 * grasa
    return abs(calculadas - kcal) <= max(30, 0.25 * kcal)


def kcal_de(fila):
    kcal = numero(fila["energy-kcal_100g"])
    if kcal is None:
        kj = numero(fila["energy-kj_100g"]) or numero(fila["energy_100g"])
        kcal = kj / 4.184 if kj is not None else None
    return kcal


def main():
    if not ENTRADA.exists():
        sys.exit(f"No existe {ENTRADA}. Ejecuta antes 01_descargar_off.py")
    marcas, tiendas = cargar_mapa()
    SALIDA.mkdir(parents=True, exist_ok=True)
    csv.field_size_limit(10**8)

    leidos = espana = guardados = 0
    descartes = Counter()
    por_super = Counter()
    por_metodo = Counter()
    vistos = set()

    with gzip.open(ENTRADA, "rt", encoding="utf-8", errors="replace", newline="") as f, \
            open(SALIDA_CSV, "w", encoding="utf-8", newline="") as out:
        lector = csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE)
        escritor = csv.DictWriter(out, fieldnames=CAMPOS_SALIDA)
        escritor.writeheader()

        for fila in lector:
            leidos += 1
            if leidos % 500_000 == 0:
                print(f"  {leidos:,} leídos · {espana:,} de España · {guardados:,} guardados", flush=True)
            if "en:spain" not in (fila.get("countries_tags") or ""):
                continue
            espana += 1

            codigo = (fila["code"] or "").strip()
            nombre = (fila["product_name"] or "").strip()
            if not nombre:
                descartes["sin nombre"] += 1
                continue
            if codigo in vistos:
                descartes["duplicado"] += 1
                continue

            kcal = kcal_de(fila)
            prot = numero(fila["proteins_100g"])
            hidr = numero(fila["carbohydrates_100g"])
            grasa = numero(fila["fat_100g"])
            if not macros_validos(kcal, prot, hidr, grasa):
                descartes["macros incompletos o incoherentes"] += 1
                continue

            sal = numero(fila["salt_100g"])
            if sal is None and (sodio := numero(fila["sodium_100g"])) is not None:
                sal = sodio * 2.5
            super_, metodo = asignar_supermercado(fila, marcas, tiendas)

            vistos.add(codigo)
            guardados += 1
            por_super[super_ or "(sin asignar)"] += 1
            if metodo:
                por_metodo[metodo] += 1
            escritor.writerow({
                "codigo_barras": codigo,
                "nombre": nombre,
                "marca": (fila["brands"] or "").split(",")[0].strip(),
                "supermercado": super_,
                "tiendas": fila["stores"],
                "cantidad": fila["quantity"],
                "racion": fila["serving_size"],
                "racion_g": numero(fila["serving_quantity"]),
                "kcal_100g": round(kcal, 1),
                "proteinas_100g": prot,
                "hidratos_100g": hidr,
                "azucares_100g": numero(fila["sugars_100g"]),
                "grasas_100g": grasa,
                "saturadas_100g": numero(fila["saturated-fat_100g"]),
                "fibra_100g": numero(fila["fiber_100g"]),
                "sal_100g": round(sal, 3) if sal is not None else None,
                "categoria": fila["main_category_en"],
                "nutriscore": fila["nutriscore_grade"] if fila["nutriscore_grade"] in "abcde" else "",
                "nova": fila["nova_group"],
                "imagen": fila["image_small_url"],
                "popularidad": fila["unique_scans_n"],
            })

    print(f"\nLeídos: {leidos:,}  ·  De España: {espana:,}  ·  Guardados: {guardados:,}")
    print("Descartados:", dict(descartes))
    print("Supermercado asignado por:", dict(por_metodo))
    print("Por supermercado:")
    for s, n in por_super.most_common():
        print(f"  {s:<20} {n:>8,}")
    print(f"\nArchivo: {SALIDA_CSV}")


if __name__ == "__main__":
    main()
