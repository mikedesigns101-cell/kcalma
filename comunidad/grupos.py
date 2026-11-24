"""Robot de los grupos de amigos de Kcalma.

Cada móvil deja en una incidencia fija (el «buzón») un comentario con las cifras del mes de su dueño, cifradas con
el código de su grupo. Este robot, que lanza la GitHub Action con cada comentario nuevo, recoge TODOS los comentarios
pendientes, guarda cada uno en comunidad/grupos/<grupo>.json y borra el comentario.

El robot no puede leer las cifras (van cifradas) ni le hace falta: solo comprueba la forma y pone límites.
  - Solo valen los comentarios del dueño del repositorio, que es con cuya clave publican las apps.
  - Identificadores con la forma exacta, mes actual o vecino, y un cifrado de tamaño razonable.
  - Como mucho MAX_MIEMBROS personas por grupo y MAX_GRUPOS grupos.
  - Se guardan el mes en curso y el anterior; lo más viejo se borra.

El texto de los comentarios es entrada no fiable: solo se lee como JSON, nunca se ejecuta.
"""
import datetime
import json
import os
import re
import subprocess
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
CARPETA = os.path.join(AQUI, "grupos")
MAX_MIEMBROS = 30
MAX_GRUPOS = 500


def meses_validos(hoy):
    """El mes de hoy y sus dos vecinos (por la diferencia horaria y el cambio de mes)."""
    primero = hoy.replace(day=1)
    anterior = (primero - datetime.timedelta(days=1)).replace(day=1)
    siguiente = (primero + datetime.timedelta(days=32)).replace(day=1)
    return [m.strftime("%Y-%m") for m in (anterior, primero, siguiente)]


def leer_cuerpo(cuerpo):
    m = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", cuerpo or "", re.S)
    try:
        dato = json.loads(m.group(1) if m else cuerpo)
    except (ValueError, TypeError):
        return None
    return dato if isinstance(dato, dict) else None


def valido(d, hoy):
    return (
        isinstance(d.get("g"), str) and re.fullmatch(r"[0-9a-f]{16}", d["g"]) is not None
        and isinstance(d.get("m"), str) and re.fullmatch(r"[0-9a-f]{8}", d["m"]) is not None
        and d.get("mes") in meses_validos(hoy)
        and isinstance(d.get("d"), str) and 40 <= len(d["d"]) <= 800 and re.fullmatch(r"[A-Za-z0-9+/=]+", d["d"]) is not None
    )


def procesar(comentarios, grupos, hoy):
    """Aplica los comentarios (en orden) a [grupos] (id → {mes → {miembro → {d, t}}}). Devuelve los ids de grupo cambiados."""
    cambiados = set()
    guardar = meses_validos(hoy)[:2] + meses_validos(hoy)[2:]
    for c in comentarios:
        if c.get("assoc") != "OWNER":
            continue
        d = leer_cuerpo(c.get("body"))
        if d is None or not valido(d, hoy):
            continue
        if d["g"] not in grupos and len(grupos) >= MAX_GRUPOS:
            continue
        grupo = grupos.setdefault(d["g"], {})
        mes = grupo.setdefault(d["mes"], {})
        if d["m"] not in mes and len(mes) >= MAX_MIEMBROS:
            continue
        mes[d["m"]] = {"d": d["d"], "t": hoy.isoformat()}
        for viejo in [k for k in grupo if k not in guardar]:
            del grupo[viejo]
        cambiados.add(d["g"])
    return cambiados


def main():
    numero = sys.argv[1]
    repo = os.environ["GH_REPO"]
    salida = subprocess.run(
        ["gh", "api", f"repos/{repo}/issues/{numero}/comments", "--paginate",
         "--jq", ".[] | {id, assoc: .author_association, body}"],
        capture_output=True, text=True, check=True, encoding="utf-8",
    ).stdout
    comentarios = [json.loads(linea) for linea in salida.splitlines() if linea.strip()]
    if not comentarios:
        return
    os.makedirs(CARPETA, exist_ok=True)
    grupos = {}
    for nombre in os.listdir(CARPETA):
        if re.fullmatch(r"[0-9a-f]{16}\.json", nombre):
            with open(os.path.join(CARPETA, nombre), encoding="utf-8") as f:
                grupos[nombre[:-5]] = json.load(f)
    hoy = datetime.datetime.now(datetime.timezone.utc).date()
    cambiados = procesar(comentarios, grupos, hoy)
    for g in cambiados:
        with open(os.path.join(CARPETA, g + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(grupos[g], f, ensure_ascii=False, indent=1, sort_keys=True)
            f.write("\n")
    # El buzón se vacía: lo válido ya está guardado y lo demás no sirve
    for c in comentarios:
        subprocess.run(["gh", "api", "-X", "DELETE", f"repos/{repo}/issues/comments/{c['id']}"], check=False)
    print(f"{len(comentarios)} comentarios, {len(cambiados)} grupos actualizados")


if __name__ == "__main__":
    main()
