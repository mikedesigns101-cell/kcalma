# Apelación a Google Play Protect

Formulario: https://support.google.com/googleplay/android-developer/contact/protectappeals
(si el enlace cambia, busca «Play Protect appeals» en la ayuda de Google Play Console). Lo rellena Mikel con su correo.

## Datos que pide

- **Nombre del paquete:** `com.mikel.kcalma`
- **Dónde se distribuye:** https://github.com/mikedesigns101-cell/kcalma/releases
- **APK:** la última `kcalma-X.Y.Z.apk` de esa página.
- **Código fuente:** https://github.com/mikedesigns101-cell/kcalma

## Texto (en inglés, que es como lo revisan)

Kcalma is a free, open-source calorie and macro tracker for Spanish users, distributed only as a signed APK through GitHub Releases to a small group of friends. It has no ads, no accounts and no server: all data stays on the device.

Every sensitive permission has one purpose:
- REQUEST_INSTALL_PACKAGES: in-app updates. The app downloads newer signed APKs only from its own GitHub Releases and hands them to the system installer, which asks the user.
- CAMERA: barcode scanner, nutrition-label photos and meal photos.
- POST_NOTIFICATIONS and RECEIVE_BOOT_COMPLETED: optional meal and weigh-in reminders the user turns on.
- health.READ_STEPS (Health Connect): optional; reads today's step count to adjust the daily calorie target. It never writes or uploads health data.
- INTERNET: Open Food Facts barcode lookups, the update check against GitHub, and photos sent to Google Gemini with the user's own API key, only when the user takes them.

The full source code is public in the repository above. Please review the app and stop flagging it as harmful.
