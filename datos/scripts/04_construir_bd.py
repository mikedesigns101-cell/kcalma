"""Une BEDCA (genéricos) y Open Food Facts España (productos de supermercado)
en una sola base de datos SQLite con búsqueda de texto (FTS5).

Salida: salida/alimentos.db
  - tabla `alimentos`        : un registro por alimento o producto
  - tabla `alimentos_buscar` : índice de búsqueda (sin tildes, por prefijos)

Es la misma estructura que usará la app (Room) y el servidor.

Uso:  python 04_construir_bd.py
      python 04_construir_bd.py "yogur griego hacendado"   (prueba una búsqueda)
"""
import csv
import sqlite3
import sys

from comun import SALIDA, normalizar, numero

BD = SALIDA / "alimentos.db"
BEDCA = SALIDA / "bedca.csv"
OFF = SALIDA / "off_espana.csv"

ESQUEMA = """
CREATE TABLE alimentos (
    id              INTEGER PRIMARY KEY,
    tipo            TEXT NOT NULL,      -- 'generico' (BEDCA) o 'producto' (supermercado)
    fuente          TEXT NOT NULL,      -- 'bedca' | 'openfoodfacts' | 'usuario'
    fuente_id       TEXT,               -- id en BEDCA o código de barras
    codigo_barras   TEXT,
    nombre          TEXT NOT NULL,
    marca           TEXT,
    supermercado    TEXT,
    cantidad        TEXT,               -- tamaño del envase, p. ej. '500 g'
    racion          TEXT,
    racion_g        REAL,
    kcal_100g       REAL,
    proteinas_100g  REAL,
    hidratos_100g   REAL,
    azucares_100g   REAL,
    grasas_100g     REAL,
    saturadas_100g  REAL,
    fibra_100g      REAL,
    sal_100g        REAL,
    categoria       TEXT,
    nutriscore      TEXT,
    nova            INTEGER,
    imagen          TEXT,
    popularidad     INTEGER DEFAULT 0
);
CREATE INDEX idx_codigo ON alimentos(codigo_barras);
CREATE INDEX idx_super  ON alimentos(supermercado);

CREATE VIRTUAL TABLE alimentos_buscar USING fts5(
    texto, content='', tokenize="unicode61 remove_diacritics 2", prefix='2 3'
);
"""

COLUMNAS = [
    "tipo", "fuente", "fuente_id", "codigo_barras", "nombre", "marca", "supermercado",
    "cantidad", "racion", "racion_g", "kcal_100g", "proteinas_100g", "hidratos_100g",
    "azucares_100g", "grasas_100g", "saturadas_100g", "fibra_100g", "sal_100g",
    "categoria", "nutriscore", "nova", "imagen", "popularidad",
]
NUMERICAS = {c for c in COLUMNAS if c.endswith("_100g") or c in ("racion_g",)}


def leer(ruta):
    with open(ruta, encoding="utf-8", newline="") as f:
        yield from csv.DictReader(f)


def filas_bedca():
    for r in leer(BEDCA):
        if numero(r["kcal_100g"]) is None:
            continue  # sin energía no sirve para contar calorías
        yield {
            "tipo": "generico", "fuente": "bedca", "fuente_id": r["bedca_id"],
            "nombre": r["nombre"], "popularidad": 1000,  # los genéricos van primero
            **{c: r[c] for c in COLUMNAS if c in r and c.endswith("_100g")},
        }


def filas_off():
    for r in leer(OFF):
        yield {
            "tipo": "producto", "fuente": "openfoodfacts", "fuente_id": r["codigo_barras"],
            **{c: r[c] for c in COLUMNAS if c in r},
        }


def insertar(con, filas):
    n = 0
    sql = f"INSERT INTO alimentos ({','.join(COLUMNAS)}) VALUES ({','.join('?' * len(COLUMNAS))})"
    for fila in filas:
        valores = []
        for c in COLUMNAS:
            v = fila.get(c)
            if c in NUMERICAS:
                v = numero(v)
            elif c in ("nova", "popularidad"):
                v = int(numero(v)) if numero(v) is not None else None
            elif v == "":
                v = None
            valores.append(v)
        cur = con.execute(sql, valores)
        texto = " ".join(normalizar(fila.get(c)) for c in ("nombre", "marca", "supermercado"))
        con.execute("INSERT INTO alimentos_buscar(rowid, texto) VALUES (?, ?)", (cur.lastrowid, texto))
        n += 1
    return n


def buscar(con, consulta, limite=10):
    """Búsqueda tal y como la hará la app: cada palabra como prefijo.

    Orden: relevancia del texto + popularidad; los genéricos BEDCA arriba y las
    fichas sin marca ni supermercado abajo. Agrupa los repetidos (mismo nombre,
    marca y kcal con distinto código de barras).
    """
    palabras = normalizar(consulta).split()
    if not palabras:
        return []
    fts = " ".join(f'"{p}"*' for p in palabras)
    filas = con.execute(
        """SELECT a.nombre, a.marca, a.supermercado, a.kcal_100g, a.proteinas_100g,
                  a.hidratos_100g, a.grasas_100g
           FROM alimentos_buscar b JOIN alimentos a ON a.id = b.rowid
           WHERE alimentos_buscar MATCH ?
           ORDER BY bm25(alimentos_buscar)
                    - ln(1 + coalesce(a.popularidad, 0)) * 0.3
                    + CASE WHEN a.tipo = 'producto' AND a.marca IS NULL
                                AND a.supermercado IS NULL THEN 3 ELSE 0 END
           LIMIT ?""",
        (fts, limite * 5),
    ).fetchall()
    vistos, resultado = set(), []
    for f in filas:
        clave = (normalizar(f[0]), normalizar(f[1]), round(f[3] or 0))
        if clave not in vistos:
            vistos.add(clave)
            resultado.append(f)
    return resultado[:limite]


def imprimir(resultados):
    for nombre, marca, sup, kcal, p, h, g in resultados:
        etiqueta = " · ".join(x for x in (marca, sup) if x)
        print(f"  {nombre[:45]:<45} {etiqueta[:28]:<28} {kcal or 0:>6.0f} kcal  "
              f"P {p or 0:>4.1f}  H {h or 0:>4.1f}  G {g or 0:>4.1f}")


def main():
    if len(sys.argv) > 1:
        con = sqlite3.connect(BD)
        con.create_function("ln", 1, lambda x: __import__("math").log(x))
        imprimir(buscar(con, " ".join(sys.argv[1:])))
        return

    for ruta in (BEDCA, OFF):
        if not ruta.exists():
            sys.exit(f"Falta {ruta}. Ejecuta antes los scripts 02 y 03.")

    BD.unlink(missing_ok=True)
    con = sqlite3.connect(BD)
    con.executescript(ESQUEMA)
    n_bedca = insertar(con, filas_bedca())
    n_off = insertar(con, filas_off())
    con.commit()
    con.execute("INSERT INTO alimentos_buscar(alimentos_buscar) VALUES ('optimize')")
    con.commit()
    con.execute("VACUUM")

    print(f"Genéricos BEDCA: {n_bedca:,}")
    print(f"Productos supermercado: {n_off:,}")
    print("\nPor supermercado:")
    for sup, n in con.execute(
            "SELECT coalesce(supermercado, '(genéricos / sin asignar)'), count(*) "
            "FROM alimentos GROUP BY 1 ORDER BY 2 DESC"):
        print(f"  {sup:<28} {n:>8,}")
    print(f"\nTamaño: {BD.stat().st_size / 1e6:.1f} MB  ->  {BD}")

    con.create_function("ln", 1, lambda x: __import__("math").log(x))
    for prueba in ("lentejas", "yogur griego hacendado", "pan molde", "aceite oliva virgen extra"):
        print(f"\nBúsqueda de prueba: '{prueba}'")
        imprimir(buscar(con, prueba, 5))


if __name__ == "__main__":
    main()
