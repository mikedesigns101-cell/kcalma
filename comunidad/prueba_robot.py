"""Pruebas del robot: python comunidad/prueba_robot.py"""
import json

import robot

HOY = "2026-11-25"
BUENO = {"nombre": "Hamburguesa de vaca madurada", "marca": "Hacendado", "codigo": "8480000123459",
         "kcal": 250, "proteinas": 17.5, "hidratos": 0.8, "grasas": 19.6, "saturadas": 8.9, "azucares": 0.5, "sal": 1.2}


def cuerpo(a):
    return "```json\n" + json.dumps(a, ensure_ascii=False) + "\n```"


def caso(etiqueta, a, alimentos=None, retirados=None):
    alimentos = [] if alimentos is None else alimentos
    retirados = [] if retirados is None else retirados
    return robot.procesar([etiqueta], cuerpo(a) if isinstance(a, dict) else a, 7, HOY, alimentos, retirados), alimentos, retirados


assert robot.codigo_valido("8480000123459") and robot.codigo_valido("96385074")
assert not robot.codigo_valido("8480000123450") and not robot.codigo_valido("abc") and not robot.codigo_valido(8480000123459)

(accion, _), lista, _ = caso("alimento", BUENO)
assert accion == "publicado" and lista[0]["fecha"] == HOY and lista[0]["incidencia"] == 7, accion

# No cuadra: kcal imposibles para esos macros, macros que suman más de 100, nombre con enlace o insulto, código falso
for malo in [dict(BUENO, kcal=20), dict(BUENO, proteinas=60, grasas=50), dict(BUENO, nombre="Mira http://x.es"),
             dict(BUENO, nombre="Comida de mierda"), dict(BUENO, codigo="1234567890123"), dict(BUENO, kcal="250"),
             dict(BUENO, azucares=30), dict(BUENO, nombre="<script>"), dict(BUENO, kcal=True)]:
    (accion, motivo), lista, _ = caso("alimento", malo)
    assert accion == "esperar" and lista == [], (malo, accion, motivo)

# Texto que no es JSON, o JSON que no es un objeto: no se publica y no revienta
for raro in ["hola", "```json\n[1,2]\n```", "", None, "```json\n{\"nombre\": __import__('os')}\n```"]:
    (accion, _), lista, _ = caso("alimento", raro)
    assert accion == "esperar" and lista == [], raro

# Un código que ya está no se pisa
(accion, _), lista, _ = caso("alimento", dict(BUENO, nombre="Otra cosa", kcal=255), [dict(BUENO, fecha="2026-11-01")])
assert accion == "duplicado" and lista[0]["nombre"] == BUENO["nombre"]

# Tope diario
llenos = [dict(BUENO, codigo=str(i), fecha=HOY) for i in range(robot.MAX_DIA)]
(accion, _), lista, _ = caso("alimento", BUENO, llenos)
assert accion == "esperar" and len(lista) == robot.MAX_DIA

# Una denuncia lo retira y no deja que vuelva a entrar solo
(accion, _), lista, fuera = caso("alimento-mal", {"codigo": BUENO["codigo"], "motivo": "no existe"}, [dict(BUENO, fecha="2026-11-01")])
assert accion == "retirado" and lista == [] and fuera[0]["retirado"] == HOY
(accion, _), lista, _ = caso("alimento", BUENO, [], fuera)
assert accion == "esperar" and lista == []
(accion, _), _, _ = caso("alimento-mal", {"codigo": "000"}, [])
assert accion == "duplicado"

print("ok: todas las pruebas del robot pasan")
