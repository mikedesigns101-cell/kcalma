"""Robot de los alimentos de la comunidad de Kcalma.

Lo lanza la GitHub Action con cada incidencia nueva que lleve la etiqueta «alimento» (un alimento que un usuario
ha registrado en su móvil) o «alimento-mal» (un usuario avisa de que uno está mal).

Frenos, por orden:
  1. Solo se aceptan incidencias con esas etiquetas, que solo puede poner quien tiene permisos en el repositorio.
  2. El alimento tiene que pasar la validación (números posibles y coherentes, código de barras con su dígito de control).
  3. Un código que ya existe no se pisa: el primero que llegó se queda.
  4. Como mucho MAX_DIA altas y MAX_DIA retiradas al día; el resto espera a que Mikel las revise.
  5. Una denuncia retira el alimento para todos hasta que se revise.

El texto de la incidencia es entrada no fiable: solo se lee como JSON, nunca se ejecuta.
"""
import datetime
import json
import os
import re
import subprocess
import sys
import unicodedata

AQUI = os.path.dirname(os.path.abspath(__file__))
ALIMENTOS = os.path.join(AQUI, "alimentos.json")
RETIRADOS = os.path.join(AQUI, "retirados.json")
MAX_DIA = 20
MAX_TOTAL = 5000
PROHIBIDAS = ["http", "www.", "puta", "puto", "mierda", "gilipollas", "cabron", "polla", "coño", "joder", "maricon", "subnormal", "nazi", "hitler"]
NUMEROS = ["kcal", "proteinas", "hidratos", "grasas"]
OPCIONALES = ["azucares", "saturadas", "fibra", "sal"]


def leer(ruta):
    if not os.path.exists(ruta):
        return []
    with open(ruta, encoding="utf-8") as f:
        return json.load(f)


def escribir(ruta, datos):
    with open(ruta, "w", encoding="utf-8", newline="\n") as f:
        json.dump(datos, f, ensure_ascii=False, indent=1)
        f.write("\n")


def sin_tildes(s):
    return "".join(c for c in unicodedata.normalize("NFD", s.lower()) if unicodedata.category(c) != "Mn")


def codigo_valido(c):
    """GTIN de 8, 12, 13 o 14 cifras con su dígito de control."""
    if not isinstance(c, str) or not re.fullmatch(r"\d{8}|\d{12,14}", c):
        return False
    cifras = [int(x) for x in c]
    suma = sum(d * (3 if i % 2 == 0 else 1) for i, d in enumerate(reversed(cifras[:-1])))
    return (10 - suma % 10) % 10 == cifras[-1]


def texto_valido(s, minimo, maximo):
    if not isinstance(s, str):
        return False
    s = s.strip()
    if not (minimo <= len(s) <= maximo) or any(ord(c) < 32 for c in s) or "<" in s or ">" in s:
        return False
    plano = sin_tildes(s)
    return not any(p in plano for p in PROHIBIDAS)


def numero(v, minimo, maximo):
    return isinstance(v, (int, float)) and not isinstance(v, bool) and minimo <= v <= maximo


def validar(a):
    """Devuelve (alimento limpio, None) o (None, motivo del rechazo)."""
    if not isinstance(a, dict):
        return None, "no es un objeto"
    if not texto_valido(a.get("nombre"), 2, 80) or not re.search(r"[a-zA-ZáéíóúñÁÉÍÓÚÑ]{2}", a.get("nombre", "")):
        return None, "el nombre no vale"
    if a.get("marca") not in (None, "") and not texto_valido(a.get("marca"), 1, 40):
        return None, "la marca no vale"
    if not codigo_valido(a.get("codigo")):
        return None, "el código de barras no es válido"
    if not numero(a.get("kcal"), 0, 900):
        return None, "las kcal no son posibles"
    for k in NUMEROS[1:]:
        if not numero(a.get(k), 0, 100):
            return None, f"{k} no es posible"
    p, h, g = a["proteinas"], a["hidratos"], a["grasas"]
    if p + h + g > 100.5:
        return None, "proteína, hidratos y grasa suman más de 100 g"
    calculadas = 4 * p + 4 * h + 9 * g
    if abs(calculadas - a["kcal"]) > max(25, 0.20 * max(a["kcal"], calculadas)):
        return None, f"las kcal ({a['kcal']}) no cuadran con los macros ({round(calculadas)})"
    for k in OPCIONALES:
        if a.get(k) is not None and not numero(a.get(k), 0, 100):
            return None, f"{k} no es posible"
    if a.get("azucares") is not None and a["azucares"] > h + 0.5:
        return None, "hay más azúcares que hidratos"
    if a.get("saturadas") is not None and a["saturadas"] > g + 0.5:
        return None, "hay más saturadas que grasas"
    if a.get("racion_g") is not None and not numero(a.get("racion_g"), 1, 2000):
        return None, "la ración no es posible"
    limpio = {"nombre": a["nombre"].strip(), "marca": (a.get("marca") or "").strip() or None, "codigo": a["codigo"]}
    for k in NUMEROS + OPCIONALES + ["racion_g"]:
        if a.get(k) is not None:
            limpio[k] = round(float(a[k]), 2)
    return limpio, None


def json_del_cuerpo(cuerpo):
    """El primer bloque ```json … ``` de la incidencia, o el cuerpo entero si es JSON."""
    m = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", cuerpo or "", re.S)
    try:
        return json.loads(m.group(1) if m else cuerpo)
    except (ValueError, TypeError):
        return None


def procesar(etiquetas, cuerpo, numero_incidencia, hoy, alimentos, retirados):
    """Decide qué hacer. Devuelve (acción, comentario): acción es 'publicado', 'retirado', 'duplicado' o 'esperar'."""
    datos = json_del_cuerpo(cuerpo)
    if "alimento-mal" in etiquetas:
        codigo = datos.get("codigo") if isinstance(datos, dict) else None
        i = next((k for k, x in enumerate(alimentos) if x.get("codigo") == codigo), None)
        if i is None:
            return "duplicado", "Ese alimento no está (o ya no está) en la lista de la comunidad."
        if sum(1 for x in retirados if x.get("retirado") == hoy) >= MAX_DIA:
            return "esperar", f"Hoy ya se han retirado {MAX_DIA} alimentos. Este aviso espera a que se revise a mano."
        quitado = alimentos.pop(i)
        quitado.update(retirado=hoy, aviso=numero_incidencia)
        retirados.append(quitado)
        return "retirado", f"Retirado «{quitado['nombre']}» ({codigo}) de la lista de la comunidad hasta que se revise."
    limpio, motivo = validar(datos)
    if limpio is None:
        return "esperar", f"No se publica solo: {motivo}. Queda a la espera de revisión."
    if any(x.get("codigo") == limpio["codigo"] for x in alimentos):
        return "duplicado", "Ese código de barras ya está en la lista de la comunidad; se queda el que llegó primero."
    if any(x.get("codigo") == limpio["codigo"] for x in retirados):
        return "esperar", "Ese código se retiró por un aviso. No vuelve a entrar sin revisión."
    if sum(1 for x in alimentos if x.get("fecha") == hoy) >= MAX_DIA or len(alimentos) >= MAX_TOTAL:
        return "esperar", f"Hoy ya han entrado {MAX_DIA} alimentos nuevos. Este espera a que se revise a mano."
    limpio.update(fecha=hoy, incidencia=numero_incidencia)
    alimentos.append(limpio)
    alimentos.sort(key=lambda x: x["codigo"])
    return "publicado", f"Publicado «{limpio['nombre']}» ({limpio['codigo']}). Llegará a las apps en un día como mucho."


def gh(*args):
    subprocess.run(["gh", *args], check=False)


def main():
    with open(sys.argv[1], encoding="utf-8") as f:
        incidencia = json.load(f)["issue"]
    etiquetas = [e["name"] for e in incidencia.get("labels", [])]
    if "alimento" not in etiquetas and "alimento-mal" not in etiquetas:
        return
    n = incidencia["number"]
    hoy = datetime.datetime.now(datetime.timezone.utc).date().isoformat()
    alimentos, retirados = leer(ALIMENTOS), leer(RETIRADOS)
    accion, comentario = procesar(etiquetas, incidencia.get("body"), n, hoy, alimentos, retirados)
    print(accion, "-", comentario)
    if accion in ("publicado", "retirado"):
        escribir(ALIMENTOS, alimentos)
        escribir(RETIRADOS, retirados)
    # Lo publicado y lo repetido se cierra; lo retirado y lo que espera queda abierto para revisarlo
    if accion in ("publicado", "duplicado"):
        gh("issue", "close", str(n), "--comment", comentario)
    else:
        gh("issue", "comment", str(n), "--body", comentario)
        gh("issue", "edit", str(n), "--add-label", "revisar")


if __name__ == "__main__":
    main()
