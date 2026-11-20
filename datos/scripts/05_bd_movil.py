"""Crea la base reducida que va dentro de la APK (app/src/main/assets/alimentos.db).

Incluye los genéricos (BEDCA), los productos con supermercado español y los demás
productos escaneados por al menos MIN_POPULARIDAD personas en Open Food Facts.
En lugar de FTS5 (no siempre disponible en el SQLite de Android) lleva una columna
`busca` normalizada (minúsculas, sin tildes) para buscar con LIKE.
"""
import sqlite3
import sys

from comun import DATOS, SALIDA, normalizar

MIN_POPULARIDAD = 3
DESTINO = DATOS.parent / "app" / "src" / "main" / "assets" / "alimentos.db"

COLUMNAS = ("id, tipo, codigo_barras, nombre, marca, supermercado, cantidad, racion, racion_g, "
            "kcal_100g, proteinas_100g, hidratos_100g, azucares_100g, grasas_100g, "
            "saturadas_100g, fibra_100g, sal_100g, categoria, popularidad")


def main():
    origen = sqlite3.connect(SALIDA / "alimentos.db")
    DESTINO.parent.mkdir(parents=True, exist_ok=True)
    DESTINO.unlink(missing_ok=True)
    destino = sqlite3.connect(DESTINO)
    destino.executescript("""
        CREATE TABLE alimentos (
            id INTEGER PRIMARY KEY, tipo TEXT NOT NULL, codigo_barras TEXT, nombre TEXT NOT NULL,
            marca TEXT, supermercado TEXT, cantidad TEXT, racion TEXT, racion_g REAL,
            kcal_100g REAL, proteinas_100g REAL, hidratos_100g REAL, azucares_100g REAL,
            grasas_100g REAL, saturadas_100g REAL, fibra_100g REAL, sal_100g REAL,
            categoria TEXT, popularidad INTEGER, busca TEXT NOT NULL
        );
    """)
    filas = origen.execute(
        f"SELECT {COLUMNAS} FROM alimentos WHERE kcal_100g IS NOT NULL AND "
        "(tipo = 'generico' OR supermercado IS NOT NULL OR popularidad >= ?)",
        (MIN_POPULARIDAD,),
    )
    n = 0
    for f in filas:
        busca = " " + " ".join(normalizar(x) for x in (f[3], f[4], f[5]) if x) + " "
        destino.execute(f"INSERT INTO alimentos VALUES ({','.join('?' * 20)})", (*f, busca))
        n += 1
    destino.executescript("""
        CREATE INDEX idx_codigo ON alimentos(codigo_barras);
        CREATE TABLE android_metadata (locale TEXT);
        INSERT INTO android_metadata VALUES ('es_ES');
        PRAGMA user_version = 1;
    """)
    destino.commit()
    destino.execute("VACUUM")
    destino.close()
    print(f"{n} alimentos -> {DESTINO} ({DESTINO.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    sys.exit(main())
