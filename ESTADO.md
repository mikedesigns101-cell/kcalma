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

## Versión 0.6.2
- La comprobación de actualizaciones pasa de `LaunchedEffect(Unit)` a `LifecycleResumeEffect` en `ui/App.kt`: también al volver a la app desde segundo plano. Antes solo se veía la tarjeta si Android había cerrado la app.
- versionCode 16 (el 15 se usó en una versión de prueba del emulador).
- Probado en el emulador con una versión de prueba temporal: la tarjeta sale al abrir y al volver a la app, sin cierres.

## Versión 0.7.0 (rediseño «cristal»)
- Temas en `ui/theme/Colores.kt`: Kcalma, Hielo, Aurora y Orquídea, translúcidos sobre un cielo oscuro con luces. `TemaRaideo` sigue opaco para el widget. Se eligen en Perfil → Apariencia (`PantallaApariencia.kt`), que también apaga las animaciones; si Android las tiene quitadas, Kcalma tampoco las pone (`KcalmaTheme.movimiento`).
- `ui/componentes/Cristal.kt` (`fondoKcalma`, `vidrio`, `esferaBrillo`, `BurbujaIcono`, `BotonCristal`, `entrada`) e `Iconos.kt` (46 iconos propios con degradado, generados desde las maquetas).
- Hoy: `Pompa.kt` (orbe con líquido = lo comido, olas, burbujas y chapoteo al tocar), saludo, buscador con voz y cámara, macros, comidas en dos columnas. Barra flotante con burbuja que se desliza y botón + con Buscar, Foto, Escanear y Dictar.
- Con maqueta y revisadas en el emulador: Hoy, barra, +, Perfil, Apariencia, Progreso, Ideas, Comida, Ficha, Añadir, cámara de Foto.
- Solo heredan el estilo cristal (sin maqueta propia): Etiqueta, Plato, Recordatorios, Lista, Privacidad, tarjetas de actualización y de ajuste, pasos del onboarding.
- Sin probar en el emulador: el onboarding (haría falta borrar los datos), Escáner con cámara real, Receta, Lista y la hoja de peso.

## Versión 0.8.0 (Entreno)
- Barra: Hoy, Ideas, +, Entreno, Progreso. Perfil, con el botón de arriba a la izquierda en Hoy.
- `dominio/Entreno.kt`, `Ejercicios.kt` (catálogo y rutinas hechas), `Progresion.kt` (doble progresión, récords; 11 tests).
- `datos/Entrenos.kt` (`entreno.db`, cada serie se guarda al marcarla; la sesión a medias se sigue desde "Continuar").
- `ui/entreno/`: pestaña Entreno, rutina, ficha con figura animada, sesión (series con descanso, EMOM, AMRAP) y editor.
- Probado en el emulador instalando encima de la 0.7.0 (los datos siguen): 80 × 8 en press de banca → la siguiente vez propone 80 × 8; EMOM apunta el minuto solo; sesión a medias sobrevive a reiniciar.

## Pendiente
1. Probar en el móvil: lectura real de un código de barras.
2. La APK release pesa 29 MB (ML Kit con todas las arquitecturas). Decidir con Mikel si se divide por arquitectura.
3. Hecho en v0.4.1: `datos/scripts/07_correcciones.py` corrige 15 genéricos de BEDCA (arroz, lenteja, garbanzo y judía cocidos, etc.) y quita 48 productos con kcal incoherentes; la base sube a la versión 2 y `Diario` (v2) corrige lo ya apuntado. Las bebidas alcohólicas no cuadran con sus macros por el alcohol, pero son correctas.
4. Mikel tiene que revisar los detalles provisionales de la 0.5.0 (PLAN.md §0) y probar el widget y la foto de la etiqueta en su móvil.

## 0.9.0 (24/11/2026): hecha y probada en el emulador, SIN publicar
- Informes: burbuja 🐞 en todas las pantallas y hoja «Reportar un fallo» (`ui/componentes/Informe.kt`). Envío directo a GitHub si al compilar existe `C:\Users\Mikel\.kcalma\github-informes.txt`; si no, abre el navegador. El envío directo no se ha podido probar: falta el token.
- Intervalos (`Formato.INTERVALOS`, `Rutina.fase`), Tabata hecha, 152 ejercicios (`dominio/EjerciciosMas.kt`), Biblioteca (`ui/entreno/Biblioteca.kt`).
- Entrenador IA: `dominio/Entrenador.kt` (estadísticas y validación de cambios), `datos/Coach.kt`, `Gemini.propuesta` y `Gemini.chat`, pantallas en `ui/entreno/PantallasEntrenador.kt`. Probado con Gemini real: propuesta aceptada (días de Pierna) y chat con tarjeta de cambio.
- 55 tests pasan. Mikel quiere comentar cambios antes de publicar.

## 0.9.0, segunda tanda (24/11/2026): hecha, probada en el emulador y subida a GitHub; release SIN publicar
Ideas de la nota de Obsidian de Mikel (ver PLAN.md §0). Lo nuevo:
- Burbujas 💡 y 🐞 (`ui/componentes/Informe.kt`, `datos/GitHub.kt`); informes con los últimos 15 minutos (`Registro.recientes`).
- Dictar lo que has comido (`ui/foto/PantallaDictar.kt`, `Gemini.dictado`, `Voz.separar` sin clave). Probado con Gemini real escribiendo la frase; el micrófono no se puede probar en el emulador.
- Tarjeta de alimento con nombre entero, gramos a mano y cocinado (`dominio/Cocinado.kt`).
- Complex (`ui/entreno/SesionComplejo.kt`, `Formato.COMPLEJO`).
- Historial y días pasados editables (`ui/progreso/Historial.kt`, `diaPasado` en `ui/App.kt`).
- Entrenador que crea rutinas (`Cambio.Crear`, campo `rutina_nueva`). Probado con Gemini real: rutina guardada.
- Foto: instrucciones nuevas, modo preciso y tabla `preferidos` con las correcciones. Sin probar con fotos reales.
- Fluidez: fuera del hilo de pantalla el descifrado de la clave y Health Connect; ideas calculadas una vez. En el emulador no se nota diferencia (4-5 % de fotogramas lentos antes y después); hay que verlo en un móvil.
- Alimentos compartidos: `datos/Comunidad.kt`, `dominio/Compartidos.kt`, `comunidad/robot.py` y `.github/workflows/alimentos.yml`. Robot probado en GitHub real (publicar, dejar en revisión, retirar) y descarga probada en la app. El envío desde la app necesita el token, que aún no existe.
- `diario.db` pasa a la versión 5 (tablas `preferidos` y `comunidad`). 65 tests y las pruebas del robot pasan.

## 0.9.0, tercera tanda (25/11/2026): amigos y podómetro; código en GitHub, release SIN publicar
- Competición mensual con amigos: `dominio/Competicion.kt` (cifras y clasificaciones), `dominio/CifradoGrupo.kt` (AES-GCM con clave sacada del código del grupo), `datos/Amigos.kt`, `ui/progreso/PantallaAmigos.kt`. Se entra desde Progreso («Amigos ›») y desde Perfil.
- Robot: `comunidad/grupos.py` y `.github/workflows/grupos.yml`; el buzón es la incidencia 6 del repo. Probado en GitHub real con dos amigos simulados: la app descifra y enseña la general, las cuatro listas y el ganador del mes anterior.
- Podómetro del móvil: `datos/Podometro.kt` (sensor de pasos, lectura al abrir, con cada aviso y cada hora). El emulador no tiene ese sensor («Sin sensor»): la lectura real está sin probar; el reparto de pasos tiene tests.
- Publicar las cifras propias desde la app necesita el token de GitHub, que aún no existe.
- 25/11/2026: Mikel creó el token (`github-informes.txt`). Comprobado desde el emulador con la compilación que lo lleva: un informe (incidencia 7), una propuesta (incidencia 8) y las cifras de un grupo de amigos llegan directas a GitHub; el robot de grupos las guardó. Pruebas cerradas y borradas. Sin probar desde la app: el envío de un alimento nuevo (necesita escanear un código y fotografiar su etiqueta).
