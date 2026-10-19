# Lab.DispMoviles-Equipo-06

## Configuración local

1. Crea `local.properties` copiando `local.properties.example`. Android Studio genera y administra `sdk.dir`; no hace falta agregarlo manualmente.
2. Obtén el App ID desde el proyecto de Agora Console. Para pruebas, genera allí un token temporal y colócalos en `AGORA_APP_ID` y `AGORA_TEMP_TOKEN`, respectivamente.
3. El token temporal dura 24 horas y solo funciona para el canal de esa familia. Si vence o cambia el canal, genera otro token que coincida con el canal y vuelve a compilar.
4. Usa el `google-services.json` correspondiente al mismo proyecto Firebase configurado para la aplicación.
5. Configura Gradle JDK 17 en Android Studio.