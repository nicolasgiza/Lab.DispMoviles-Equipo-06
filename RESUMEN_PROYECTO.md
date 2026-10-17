# Resumen del proyecto ChatFamiliar

## Estructura

Paquete Kotlin principal: `com.example.chatfamiliar`  
Ubicacion: `app/src/main/java/com/example/chatfamiliar/`

| Archivo | Responsabilidad |
|---|---|
| `MainActivity.kt` | Actividad principal; muestra la pantalla para iniciar videollamada. |
| `LoginActivity.kt` | Inicio de sesion con Firebase Auth y verificacion de correo. |
| `RegisterActivity.kt`, `RegisterScreen.kt` | Flujo y formulario de registro; crea la cuenta, guarda rol y solicita verificacion por correo. |
| `UserRoleManager.kt`, `Constants.kt` | Roles `ADMIN` y `USUARIO`, persistencia en Firestore, `UserRoleState` y nombres de colecciones. |
| `ChatScreen.kt` | Archivo presente pero vacio (0 bytes); no contiene actualmente la interfaz de chat. |
| `VideoCallManager.kt` | Pantalla e inicializacion de videollamada con Agora. |
| `LanguageManager.kt` | Cambio de idioma y selector de idioma. |
| `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` | Colores, tema y tipografia de Compose. |

Otros directorios relevantes:

- `app/src/main/AndroidManifest.xml`: permisos, caracteristicas y declaracion de actividades.
- `app/src/main/res/layout/`: layouts XML de login y registro.
- `app/src/main/res/values/`, `drawable/`, `mipmap-*`, `xml/`: recursos visuales, textos, iconos y reglas de respaldo.
- `app/src/test/` y `app/src/androidTest/`: pruebas unitarias e instrumentadas de ejemplo.
- Raiz: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` y wrapper Gradle.

## Funcionalidades y estado actual

- **Autenticacion y sesion:** Firebase Auth se usa para registro e inicio de sesion. El login exige correo verificado y cierra la sesion si no lo esta. El registro crea la cuenta y envia la verificacion.
- **Roles:** `UserRoleState` consulta el documento `users/{uid}` en Firestore y presenta `AdminDashboard` para `ADMIN`; en otro caso muestra `UserDashboard`. Los registros nuevos guardan `USUARIO` por defecto.
- **Normalizacion de correo:** login y registro aplican `.trim().lowercase()` al correo.
- **Chat:** `ChatScreen.kt` esta vacio; no estan implementados los globos de mensajes ni el formato horario `HH:mm` en ese archivo.
- **Videollamada:** `VideoCallManager.kt` contiene `VideoCallScreen`, solicitud de permisos en tiempo de ejecucion, video local/remoto y salida de llamada. El canal es parametrizable, pero `MainActivity.kt` pasa el valor fijo `"SalaFamiliar"`; no se encontro seleccion dinamica por ID de familia. El App ID de Agora se lee desde `AGORA_APP_ID` en `local.properties`.

## Configuracion Android

En `app/src/main/AndroidManifest.xml` estan declarados `INTERNET`, `CAMERA` y `RECORD_AUDIO`. La feature `android.hardware.camera` se declara con `required="false"`. Tambien figuran `MODIFY_AUDIO_SETTINGS`, `ACCESS_NETWORK_STATE` y `BLUETOOTH`.

En `app/build.gradle.kts`, dependencias clave:

- Firebase BoM `33.1.0`, `firebase-auth-ktx` y `firebase-firestore-ktx`.
- Agora RTC SDK `io.agora.rtc:full-sdk:4.3.0`.
- AndroidX Compose (BOM `2024.09.00`, Material 3, UI y Activity Compose), Core KTX, Lifecycle, AppCompat y Material.
- Configuracion: namespace y `applicationId` `com.example.chatfamiliar`, `minSdk 24`, `targetSdk 36`, Java/Kotlin JVM 11.
