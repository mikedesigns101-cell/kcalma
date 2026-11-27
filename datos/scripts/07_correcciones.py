"""
Corrige datos erróneos de la base del móvil (app/src/main/assets/alimentos.db).

Lo llama 05_bd_movil.py al final; también se puede ejecutar solo. Es idempotente.
- Genéricos de BEDCA con valores imposibles (cocinados con valores de crudo, kcal que no cuadran
  con sus macros): se sustituyen por valores de referencia (USDA FoodData Central o los propios macros).
- Productos de Open Food Facts cuyas kcal no cuadran con sus macros (fuera de 0,6–1,6 veces):
  no se sabe cuál de los dos datos está mal, así que se quitan.
- Sube PRAGMA user_version para que la app vuelva a copiar la base (Catalogo.version).

Si cambias CORRECCIONES, copia lo mismo en Diario.kt (CORRECCIONES) para arreglar lo ya apuntado.
"""
import sqlite3
import sys
from pathlib import Path

BD = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets" / "alimentos.db"
VERSION = 3

# Genéricos que se apuntan por piezas: nombre exacto -> (texto de la ración, gramos sin piel)
RACIONES = {
    "Plátano": ("1 plátano mediano", 120),
}

# nombre exacto del genérico: kcal, proteínas, hidratos, grasas, fibra (por 100 g; None = no cambiar)
CORRECCIONES = {
    "Arroz, hervido": (130, 2.7, 28.2, 0.3, 0.4),
    "Garbanzo, hervido": (148, 8.9, 18.7, 2.5, 7.6),
    "Lenteja, hervida": (116, 9.0, 12.2, 0.4, 7.9),
    "Judías blancas, cocidas": (139, 9.7, 18.8, 0.4, 6.3),
    "Garbanzo, en conserva": (120, None, None, None, None),
    "Bonito, enlatado en aceite, escurrido": (None, None, None, 11.0, None),
    "Cangrejo": (87, 18.1, 0.0, 1.1, None),
    "Nécora": (87, 18.1, 0.0, 1.1, None),
    "Chirla": (47, None, None, None, None),
    "Dorada, cruda": (133, None, None, None, None),
    "Jurel, crudo": (None, None, None, 5.2, None),
    "Kebab": (173, None, None, None, None),
    "Pimienta, negra": (251, None, None, None, None),
    "Pimienta, blanca": (296, None, None, None, None),
    "Puré de patata y queso, en copos": (350, None, None, None, None),
}
CAMPOS = ("kcal_100g", "proteinas_100g", "hidratos_100g", "grasas_100g", "fibra_100g")


def corregir(bd: sqlite3.Connection) -> None:
    for nombre, valores in CORRECCIONES.items():
        cambios = [(c, v) for c, v in zip(CAMPOS, valores) if v is not None]
        sql = "UPDATE alimentos SET " + ", ".join(f"{c} = ?" for c, _ in cambios) + " WHERE tipo = 'generico' AND nombre = ?"
        n = bd.execute(sql, (*[v for _, v in cambios], nombre)).rowcount
        if n != 1:
            print(f"AVISO: «{nombre}» afecta a {n} filas")

    for nombre, (racion, gramos) in RACIONES.items():
        n = bd.execute("UPDATE alimentos SET racion = ?, racion_g = ? WHERE tipo = 'generico' AND nombre = ?", (racion, gramos, nombre)).rowcount
        if n != 1:
            print(f"AVISO: la ración de «{nombre}» afecta a {n} filas")

    filas = bd.execute(
        "SELECT id, kcal_100g, proteinas_100g, hidratos_100g, grasas_100g, IFNULL(fibra_100g, 0) FROM alimentos "
        "WHERE tipo = 'producto' AND kcal_100g IS NOT NULL AND proteinas_100g IS NOT NULL "
        "AND hidratos_100g IS NOT NULL AND grasas_100g IS NOT NULL"
    ).fetchall()
    malos = []
    for id_, k, p, h, g, f in filas:
        at = 4 * p + 4 * h + 9 * g + 2 * f
        if at >= 40 and not 0.6 <= k / at <= 1.6:
            malos.append((id_,))
    bd.executemany("DELETE FROM alimentos WHERE id = ?", malos)
    bd.execute(f"PRAGMA user_version = {VERSION}")
    bd.commit()
    print(f"{len(CORRECCIONES)} genéricos corregidos, {len(malos)} productos incoherentes quitados, versión {VERSION}")


def main() -> int:
    bd = sqlite3.connect(BD)
    corregir(bd)
    bd.execute("VACUUM")
    bd.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
