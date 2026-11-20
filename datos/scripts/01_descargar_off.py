"""Descarga el volcado completo de Open Food Facts (CSV comprimido, ~1,3 GB).

Si la descarga se corta, al volver a ejecutarlo continúa donde se quedó.
Si el archivo ya está completo, no hace nada.

Uso:  python 01_descargar_off.py
"""
import os
import sys
import time
import urllib.request

from comun import CRUDO

URL = "https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz"
DESTINO = CRUDO / "openfoodfacts_products.csv.gz"
BLOQUE = 1024 * 1024


def tamano_remoto():
    req = urllib.request.Request(URL, method="HEAD")
    with urllib.request.urlopen(req, timeout=60) as r:
        return int(r.headers["Content-Length"])


def main():
    CRUDO.mkdir(parents=True, exist_ok=True)
    total = tamano_remoto()
    ya = DESTINO.stat().st_size if DESTINO.exists() else 0
    if ya == total:
        print(f"Ya descargado: {DESTINO} ({total / 1e9:.2f} GB)")
        return
    if ya > total:
        ya = 0

    req = urllib.request.Request(URL, headers={"Range": f"bytes={ya}-"} if ya else {})
    modo = "ab" if ya else "wb"
    print(f"Descargando {total / 1e9:.2f} GB (empezando en {ya / 1e9:.2f} GB)...")
    inicio, ultimo = time.time(), 0.0
    with urllib.request.urlopen(req, timeout=120) as r, open(DESTINO, modo) as f:
        if ya and r.status != 206:  # el servidor no admite reanudar
            f.seek(0)
            f.truncate()
            ya = 0
        hecho = ya
        while chunk := r.read(BLOQUE):
            f.write(chunk)
            hecho += len(chunk)
            if time.time() - ultimo > 10:
                vel = (hecho - ya) / max(time.time() - inicio, 1) / 1e6
                print(f"  {hecho / total:6.1%}  {hecho / 1e9:.2f} GB  {vel:.1f} MB/s", flush=True)
                ultimo = time.time()

    if os.path.getsize(DESTINO) != total:
        sys.exit("La descarga no está completa. Vuelve a ejecutar el script para continuar.")
    print(f"Listo: {DESTINO}")


if __name__ == "__main__":
    main()
