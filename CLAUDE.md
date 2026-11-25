# Kcalma (app de calorías para Android)

Lee primero las **decisiones de Mikel en `portal/data/decisiones/`**: mandan sobre el resto del plan (`PLAN.md`).

El proyecto tiene portal en `portal/data/` y se mantiene al día con la skill `portal` de Cerebro. Lo hecho y lo pendiente van ahí (ya no en `ESTADO.md`).

## Cómo trabajar con Mikel en este proyecto
- Tono profesional y serio.
- **Consultar cualquier decisión abierta** de producto, diseño, stack o datos, con opciones y una recomendación. No decidir por él.
- **Apuntar cada decisión nueva** en `portal/data/decisiones/` con la skill `portal`, para no volver a preguntarla.
- **Enseñar una maqueta o captura antes de implementar cualquier pantalla.** Las maquetas van en `maquetas/`.
- Gastar poco uso: respuestas concisas y trabajo eficiente.
- **"Arregla los errores/bugs":** `gh issue list --label informe --state open`. Enseñar a Mikel cada informe y el arreglo propuesto, arreglar solo lo que apruebe, enseñar capturas y esperar su OK antes de publicar. Cerrar cada incidencia arreglada citando la versión.

- **"Qué han propuesto" / "mira las propuestas":** `gh issue list --label propuesta --state open`. Enseñar a Mikel cada propuesta resumida; él decide cuáles se implementan. Cerrar las descartadas y las hechas (citando la versión).
- Las ideas propias de Mikel están en Obsidian: `C:\Users\Mikel\Documents\Obsidian Vault\Ideas Vibecoding\KCalma\Ideas para KCalma.md`.

## Datos técnicos clave
- Solo Android: Kotlin + Jetpack Compose. Sin servidor. Distribución como APK en GitHub, nunca en Google Play.
- La IA reconoce y la base de datos calcula. Gemini por defecto, con la clave de cada usuario cifrada en el móvil.
- `applicationId` = `com.mikel.kcalma`. No cambiarlo: los amigos perderían sus datos al actualizar.
- Todos los colores y tipografías van en `app/src/main/java/com/mikel/kcalma/ui/theme/`. Nada de colores hex en las pantallas.
- Compilar con el JDK 21 (`JAVA_HOME`) y el SDK de `D:\Android\Sdk`. Emulador: `openGym_API36`.

```bash
./gradlew :app:assembleDebug
```
