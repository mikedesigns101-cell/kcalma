"""Rutas y utilidades compartidas por los scripts de datos."""
import re
import unicodedata
from pathlib import Path

DATOS = Path(__file__).resolve().parent.parent
CRUDO = DATOS / "crudo"      # descargas originales (no se tocan)
SALIDA = DATOS / "salida"    # resultados: CSV y base de datos SQLite


def normalizar(texto):
    """Minúsculas, sin tildes y sin signos: 'Café Bonka®' -> 'cafe bonka'."""
    if not texto:
        return ""
    texto = unicodedata.normalize("NFKD", texto.lower())
    texto = "".join(c for c in texto if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9ñ]+", " ", texto).strip()


def numero(valor):
    """Convierte a float; devuelve None si está vacío o no es un número."""
    if valor is None:
        return None
    valor = str(valor).strip().replace(",", ".")
    if not valor:
        return None
    try:
        return float(valor)
    except ValueError:
        return None
