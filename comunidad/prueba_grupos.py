"""Pruebas del robot de grupos: python comunidad/prueba_grupos.py"""
import datetime
import json

import grupos

HOY = datetime.date(2026, 11, 25)
G = "0123456789abcdef"
CIFRADO = "QUJD" * 20


def com(dato, assoc="OWNER"):
    cuerpo = dato if isinstance(dato, str) else "```json\n" + json.dumps(dato) + "\n```"
    return {"id": 1, "assoc": assoc, "body": cuerpo}


def bueno(**cambios):
    d = {"g": G, "m": "0a1b2c3d", "mes": "2026-11", "d": CIFRADO}
    d.update(cambios)
    return d


assert grupos.meses_validos(HOY) == ["2026-10", "2026-11", "2026-12"]
assert grupos.meses_validos(datetime.date(2027, 1, 31)) == ["2026-12", "2027-01", "2027-02"]

datos = {}
assert grupos.procesar([com(bueno())], datos, HOY) == {G}
assert datos[G]["2026-11"]["0a1b2c3d"] == {"d": CIFRADO, "t": "2026-11-25"}

# Una persona que vuelve a publicar sustituye su cifra; otra se añade
grupos.procesar([com(bueno(d="WFla" * 20)), com(bueno(m="ffffffff"))], datos, HOY)
assert datos[G]["2026-11"]["0a1b2c3d"]["d"] == "WFla" * 20 and len(datos[G]["2026-11"]) == 2

# Lo que no vale se ignora sin romper nada
malos = [com(bueno(), assoc="NONE"), com(bueno(g="XYZ")), com(bueno(m="1234")), com(bueno(mes="2025-01")), com(bueno(d="corto")),
         com(bueno(d="<script>" * 10)), com("hola"), com("```json\n[1]\n```"), com(bueno(g=None)), com(bueno(d="A" * 5000))]
antes = json.dumps(datos, sort_keys=True)
assert grupos.procesar(malos, datos, HOY) == set() and json.dumps(datos, sort_keys=True) == antes

# Tope de miembros por grupo y de grupos
lleno = {G: {"2026-11": {"%08x" % i: {"d": CIFRADO, "t": "x"} for i in range(grupos.MAX_MIEMBROS)}}}
assert grupos.procesar([com(bueno(m="eeeeeeee"))], lleno, HOY) == set()
muchos = {"%016x" % i: {} for i in range(grupos.MAX_GRUPOS)}
assert grupos.procesar([com(bueno(g="f" * 16))], muchos, HOY) == set()

# Los meses viejos se borran al escribir en el grupo
viejo = {G: {"2026-08": {"0a1b2c3d": {"d": CIFRADO, "t": "x"}}, "2026-10": {"0a1b2c3d": {"d": CIFRADO, "t": "x"}}}}
grupos.procesar([com(bueno())], viejo, HOY)
assert sorted(viejo[G]) == ["2026-10", "2026-11"]

print("ok: todas las pruebas del robot de grupos pasan")
