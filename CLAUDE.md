# Kcalma (app de calorías para Android)

Lee primero la **sección 0 de `PLAN.md`**: recoge las decisiones de Mikel y manda sobre el resto del plan.

## Cómo trabajar con Mikel en este proyecto
- Tono profesional y serio.
- **Consultar cualquier decisión abierta** de producto, diseño, stack o datos, con opciones y una recomendación. No decidir por él.
- **Apuntar cada decisión nueva** en la sección 0 de `PLAN.md` para no volver a preguntarla.
- **Enseñar una maqueta o captura antes de implementar cualquier pantalla.** Las maquetas van en `maquetas/`.
- Gastar poco uso: respuestas concisas y trabajo eficiente.

## Datos técnicos clave
- Solo Android: Kotlin + Jetpack Compose. Sin servidor. Distribución como APK en GitHub, nunca en Google Play.
- La IA reconoce y la base de datos calcula. Gemini por defecto, con la clave de cada usuario cifrada en el móvil.
- `applicationId` = `com.mikel.kcalma`. No cambiarlo: los amigos perderían sus datos al actualizar.
- Todos los colores y tipografías van en `app/src/main/java/com/mikel/kcalma/ui/theme/`. Nada de colores hex en las pantallas.
- Compilar con el JDK 21 (`JAVA_HOME`) y el SDK de `D:\Android\Sdk`. Emulador: `openGym_API36`.

```bash
./gradlew :app:assembleDebug
```
