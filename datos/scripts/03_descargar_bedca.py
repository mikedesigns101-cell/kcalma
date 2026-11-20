"""Descarga BEDCA (Base de Datos Española de Composición de Alimentos, AESAN)
usando su servicio XML público, alimento por alimento.

Cada respuesta se guarda en crudo/bedca/<id>.xml; si el script se vuelve a
ejecutar, solo pide lo que falte. Va despacio a propósito (1 petición/seg)
para no cargar el servidor.

Salida: salida/bedca.csv  (valores por 100 g de porción comestible)

Uso:  python 03_descargar_bedca.py
"""
import csv
import time
import urllib.request
import xml.etree.ElementTree as ET

from comun import CRUDO, SALIDA, normalizar, numero

URL = "https://www.bedca.net/bdpub/procquery.php"
CACHE = CRUDO / "bedca"
SALIDA_CSV = SALIDA / "bedca.csv"
PAUSA = 1.0

CONSULTA_LISTA = """<?xml version="1.0" encoding="utf-8"?><foodquery><type level="1"/>
<selection><atribute name="f_id"/><atribute name="f_ori_name"/><atribute name="f_eng_name"/></selection>
<condition><cond1><atribute1 name="f_origen"/></cond1><relation type="EQUAL"/><cond3>BEDCA</cond3></condition>
<order ordtype="ASC"><atribute3 name="f_ori_name"/></order></foodquery>"""

CONSULTA_ALIMENTO = """<?xml version="1.0" encoding="utf-8"?><foodquery><type level="2"/>
<selection><atribute name="f_id"/><atribute name="f_ori_name"/><atribute name="f_eng_name"/>
<atribute name="f_origen"/><atribute name="edible_portion"/><atribute name="c_id"/>
<atribute name="c_ori_name"/><atribute name="best_location"/><atribute name="v_unit"/></selection>
<condition><cond1><atribute1 name="f_id"/></cond1><relation type="EQUAL"/><cond3>{id}</cond3></condition>
</foodquery>"""

# Identificador del componente en BEDCA -> nuestra columna
COMPONENTES = {
    "409": "energia_kj",
    "416": "proteinas_100g",
    "53": "hidratos_100g",
    "410": "grasas_100g",
    "299": "saturadas_100g",
    "307": "fibra_100g",
    "323": "sodio_mg",
    "417": "agua_100g",
    "404": "alcohol_100g",
}

CAMPOS_SALIDA = [
    "bedca_id", "nombre", "nombre_en", "porcion_comestible", "kcal_100g",
    "proteinas_100g", "hidratos_100g", "azucares_100g", "grasas_100g",
    "saturadas_100g", "fibra_100g", "sal_100g", "agua_100g", "alcohol_100g",
]


def pedir(xml):
    req = urllib.request.Request(URL, data=xml.encode("utf-8"),
                                 headers={"Content-Type": "text/xml"})
    for intento in range(4):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.read()
        except OSError as e:
            espera = 5 * (intento + 1)
            print(f"  error de red ({e}); reintento en {espera}s")
            time.sleep(espera)
    raise RuntimeError("BEDCA no responde")


def lista_alimentos():
    raiz = ET.fromstring(pedir(CONSULTA_LISTA))
    return [(f.findtext("f_id"), f.findtext("f_ori_name")) for f in raiz.findall("food")]


def detalle(id_):
    ruta = CACHE / f"{id_}.xml"
    if ruta.exists():
        return ruta.read_bytes(), False
    datos = pedir(CONSULTA_ALIMENTO.format(id=id_))
    ruta.write_bytes(datos)
    return datos, True


def procesar(xml_bytes):
    comida = ET.fromstring(xml_bytes).find("food")
    if comida is None or comida.findtext("f_id") is None:
        return None
    valores = {}
    azucares = None
    for v in comida.findall("foodvalue"):
        cid = v.findtext("c_id")
        valor = numero(v.findtext("best_location"))
        if cid in COMPONENTES:
            valores[COMPONENTES[cid]] = valor
        elif "azucar" in normalizar(v.findtext("c_ori_name")) and azucares is None:
            azucares = valor

    kj = valores.get("energia_kj")
    sodio = valores.get("sodio_mg")
    return {
        "bedca_id": comida.findtext("f_id"),
        "nombre": comida.findtext("f_ori_name"),
        "nombre_en": comida.findtext("f_eng_name"),
        "porcion_comestible": numero(comida.findtext("edible_portion")),
        "kcal_100g": round(kj / 4.184, 1) if kj is not None else None,
        "proteinas_100g": valores.get("proteinas_100g"),
        "hidratos_100g": valores.get("hidratos_100g"),
        "azucares_100g": azucares,
        "grasas_100g": valores.get("grasas_100g"),
        "saturadas_100g": valores.get("saturadas_100g"),
        "fibra_100g": valores.get("fibra_100g"),
        "sal_100g": round(sodio * 2.5 / 1000, 3) if sodio is not None else None,
        "agua_100g": valores.get("agua_100g"),
        "alcohol_100g": valores.get("alcohol_100g"),
    }


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    SALIDA.mkdir(parents=True, exist_ok=True)
    alimentos = lista_alimentos()
    print(f"BEDCA: {len(alimentos)} alimentos")

    filas, sin_energia = [], 0
    for i, (id_, nombre) in enumerate(alimentos, 1):
        datos, nuevo = detalle(id_)
        if nuevo:
            time.sleep(PAUSA)
        fila = procesar(datos)
        if fila is None:
            continue
        if fila["kcal_100g"] is None:
            sin_energia += 1
        filas.append(fila)
        if i % 50 == 0:
            print(f"  {i}/{len(alimentos)}  {nombre}", flush=True)

    with open(SALIDA_CSV, "w", encoding="utf-8", newline="") as f:
        escritor = csv.DictWriter(f, fieldnames=CAMPOS_SALIDA)
        escritor.writeheader()
        escritor.writerows(filas)
    print(f"\nGuardados {len(filas)} alimentos ({sin_energia} sin dato de energía) en {SALIDA_CSV}")


if __name__ == "__main__":
    main()
