# Estado de Kcalma (20/11/2026)

## Hecho
- **Onboarding + plan + pantalla Hoy** funcionando y guardados en el móvil.
- **Diario y buscador** (maqueta `maquetas/kcalma-diario.html` aprobada; decisiones en PLAN.md §0):
  - Base reducida `app/src/main/assets/alimentos.db`: 78.890 alimentos, 15,8 MB. Se genera con `datos/scripts/05_bd_movil.py`. Se busca con LIKE sobre una columna normalizada; no usa FTS5.
  - `datos/Catalogo.kt`: lee el catálogo, de solo lectura, que se copia al móvil.
  - `datos/Diario.kt`: guarda en `diario.db` el diario, las comidas habituales y los productos traídos de Open Food Facts. Cada fila guarda una copia del alimento.
  - `datos/OpenFoodFacts.kt` y `datos/Repositorio.kt`: un código se busca primero en el móvil y luego en Open Food Facts.
  - Pantallas en `ui/diario/`: Añadir, Ficha, Comida (deslizar para borrar, guardar como habitual) y Escáner (CameraX + ML Kit incluido; también se puede escribir el código).
  - Navegación con una pila en `ui/App.kt`. Hoy ya muestra lo comido de verdad.
- Los 12 tests pasan y la APK de depuración compila.

## Probado en el emulador
Buscar «yogur griego», abrir la ficha, poner 1,5 raciones y añadirlo a Comida: Hoy se actualiza (1.856 restantes) y se mantiene al reiniciar la app. Permiso de cámara: OK. Un código escrito a mano que no está en el móvil (8400000000024) se encontró en Open Food Facts.

## Probado después de reiniciar
Escáner (con la cámara emulada y escribiendo el código), Ver y añadir, borrar deslizando, guardar comida habitual, filtro «Mis comidas» y añadir una habitual: todo funciona. Corregido: redondeo de kcal en las habituales y legibilidad de los textos sobre la imagen de la cámara.

## Pendiente
1. Probar la lectura real de un código de barras con la cámara de un móvil (el emulador no tiene códigos que enfocar).
2. La APK de depuración pesa 43 MB (ML Kit con todas las arquitecturas). Decidir con Mikel si se divide por arquitectura (arm64) en la release.
3. Commit en git cuando Mikel lo pida.
4. Siguiente función, a elegir con Mikel: foto con IA (Gemini + clave propia), recomendador de comidas, Health Connect, clave de firma de release, copia de seguridad.
