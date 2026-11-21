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
- La app mira la última release al abrirse (como mucho cada 10 min) y hay botón «Actualizar Kcalma» arriba en Perfil, que descarga directamente. Si hay versión nueva, sale la tarjeta «Nueva versión» en Hoy (`datos/Actualizador.kt`).

## Foto con IA (v0.2.0)
- `datos/Claves.kt` (clave cifrada con Android Keystore, fuera de copias), `datos/Gemini.kt` (REST + responseSchema), `ui/foto/` (FlujoFoto, PantallasFoto, Imagen).
- Emparejado: genéricos → todos; si no hay o las kcal no cuadran (0,45–2,2×), estimación de la IA marcada «Estimado».
- Modelos: Flash Lite primero (1-3 s); 3.5 Flash y el alias Flash de reserva (el alias da 503 por saturación en el plan gratuito; 2.5 Flash ya no existe para claves nuevas). Emparejado por raíces y sinónimos (`datos/Emparejado.kt`).
- Registro local (`datos/Registro.kt`, también a logcat con etiqueta Kcalma) y «Mandar informe» → incidencia en GitHub.
- Probado en el emulador (v0.2.1): clave, foto con nota «patatas cocidas» → «Patata, hervida» 200 g en 2,5 s.

## Perfil y Progreso (v0.3.0)
- `ui/perfil/`: datos, peso con historial, kcal a mano, clave de Gemini, copia de seguridad (`datos/Copia.kt`, JSON sin la clave), informe y buscar actualización.
- `ui/progreso/`: peso frente al ritmo del plan, kcal de la semana, proteína media y racha.

## Ideas (v0.4.0)
- `assets/recetas.json`: 39 recetas españolas generadas con `datos/scripts/06_recetas.py` a partir de la base (gramos en crudo).
- `dominio/Receta.kt` (Recomendador: objetivo por comida, escalado, «encaja», alergias, gustos, filtros) y `datos/Recetas.kt` (carga y conversión de las ideas de Gemini, emparejadas con la base).
- `ui/ideas/PantallaIdeas.kt`: lista, detalle y «Añadir a la comida». Hoy muestra las ideas de la próxima comida sin apuntar.
- Probado en el emulador: lista, detalle, «Más ideas con IA» (Flash Lite, ~5 s) y añadir a la cena.

## Gustos y atajos (v0.4.2)
- `dominio/Gustos.kt`: categorías con raíces de palabra; las usan el recomendador y la petición a la IA. Pantalla en `BloqueGustos`.
- Añadir: repetir la última comida, «Sueles tomar» (`Diario.frecuentes`) y «Añadir solo calorías».

## Versión 0.5.0
- Ajuste semanal (`dominio/Ajuste.kt`, `ui/hoy/TarjetaAjuste.kt`), platos caseros (`dominio/Plato.kt`, `ui/diario/PantallaPlato.kt`, tablas `platos` y `plato_items` en `diario.db` v3), lista de la compra (`datos/ListaCompra.kt`, `ui/ideas/PantallaLista.kt`), foto de la etiqueta (`ui/diario/PantallaEtiqueta.kt`, `Gemini.etiqueta`), recordatorios (`avisos/Recordatorios.kt`), widget (`widget/WidgetHoy.kt`) y pasos de Health Connect (`datos/Salud.kt`, `dominio/Actividad.kt`).
- Probado en el emulador: Perfil, Recordatorios (permiso y alarmas), permiso de pasos («Conectado»), lista desde una idea, crear un plato, añadir una ración y verlo en Hoy. Ningún cierre inesperado.
- Sin probar en el emulador: el widget colocado en la pantalla de inicio, la foto de la etiqueta (necesita un código desconocido y la cámara) y la tarjeta de ajuste (necesita semanas de datos; la cubren los tests).

## Versión 0.6.0
- Mover entradas o una comida entera a otra comida (⋯ en la pantalla de la comida; `Diario.mover`).
- Cantidad recordada: la ficha y el + empiezan con la última cantidad de ese alimento (`Diario.ultimaDe` y `ultimasCantidades`).
- Buscar por voz con el dictado del móvil; `dominio/Voz.kt` separa la cantidad («200 gramos de…», «dos huevos»).
- Actualizaciones con `PackageInstaller` (`Actualizador`, `ReceptorInstalacion`). Al terminar sale una notificación para volver a abrir la app. Probado en el emulador con dos versiones de prueba temporales: se instalaron solas y con los permisos intactos.
- Guía de avisos en el README y texto para la apelación a Play Protect en `docs/apelacion-play-protect.md`.
- Sin probar en el emulador: el dictado real (no hay micrófono); la lectura del texto la cubren los tests.

## Versión 0.6.1
- Unidad `COCIDO` (`dominio/Coccion.kt` detecta los secos que se cuecen; `Entrada.cantidad` guarda los gramos cocidos y `gramos` el equivalente en seco). Las versiones antiguas lo leerían como gramos en seco.
- Buscador: `Catalogo.singular` para plurales y pasta genérica delante al buscar tipos de pasta.
- `Voz` entiende «cocidos/cocinados/hervidos»; «Deshacer» al borrar en la pantalla de la comida; etiquetas «1 ración»; tomate frito y conservas a Despensa.
- Probado en el emulador: buscar «espaguetis» y «macarrones», Cocido en un macarrón de Mercadona (250 g cocidos = 104 g en seco), añadir, borrar y deshacer. Sin cierres.
- Sin probar en el emulador: el dictado con «cocidos» (lo cubren los tests).

## Pendiente
1. Probar en el móvil: lectura real de un código de barras.
2. La APK release pesa 29 MB (ML Kit con todas las arquitecturas). Decidir con Mikel si se divide por arquitectura.
3. Hecho en v0.4.1: `datos/scripts/07_correcciones.py` corrige 15 genéricos de BEDCA (arroz, lenteja, garbanzo y judía cocidos, etc.) y quita 48 productos con kcal incoherentes; la base sube a la versión 2 y `Diario` (v2) corrige lo ya apuntado. Las bebidas alcohólicas no cuadran con sus macros por el alcohol, pero son correctas.
4. Mikel tiene que revisar los detalles provisionales de la 0.5.0 (PLAN.md §0) y probar el widget y la foto de la etiqueta en su móvil.
