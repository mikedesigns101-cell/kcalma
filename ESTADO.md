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

## GitHub y actualizaciones
- Repo público `mikedesigns101-cell/kcalma`. Cada versión es una release con `kcalma-X.Y.Z.apk` firmada (clave en `C:\Users\Mikel\.kcalma\`, fuera del repo).
- La app mira una vez al día la última release y muestra la tarjeta «Nueva versión» en Hoy (`datos/Actualizador.kt`).

## Foto con IA (v0.2.0)
- `datos/Claves.kt` (clave cifrada con Android Keystore, fuera de copias), `datos/Gemini.kt` (REST + responseSchema), `ui/foto/` (FlujoFoto, PantallasFoto, Imagen).
- Emparejado: genéricos → todos; si no hay o las kcal no cuadran (0,45–2,2×), estimación de la IA marcada «Estimado».
- Modelos: Flash Lite primero (1-3 s); 3.5 Flash y el alias Flash de reserva (el alias da 503 por saturación en el plan gratuito; 2.5 Flash ya no existe para claves nuevas). Emparejado por raíces y sinónimos (`datos/Emparejado.kt`).
- Registro local (`datos/Registro.kt`, también a logcat con etiqueta Kcalma) y «Mandar informe» → incidencia en GitHub.
- Probado en el emulador (v0.2.1): clave, foto con nota «patatas cocidas» → «Patata, hervida» 200 g en 2,5 s.

## Pendiente
1. Probar en el móvil: análisis de foto con Gemini, lectura real de un código de barras y actualización 0.1.0 → 0.2.0 desde la app.
2. La APK release pesa 29 MB (ML Kit con todas las arquitecturas). Decidir con Mikel si se divide por arquitectura.
3. Pantalla Perfil para cambiar o borrar la clave de Gemini.
4. Siguiente función, a elegir con Mikel: recomendador de comidas, Health Connect, copia de seguridad, otros proveedores de IA.
