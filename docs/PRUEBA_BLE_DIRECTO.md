# Issue #1: comunicación BLE directa A ↔ B

## Alcance y decisiones

Sin forwarding ni nuevas dependencias. Se conserva Mesh Radar y el chat por peer.

- Publicidad conectable: UUID BeaconChat en paquete primario; callsign en service data de scan response. Se omiten nombre del teléfono y potencia para respetar los 31 bytes de publicidad legacy. Incluso usando UUID de 128 bits: primario 21 bytes con flags y respuesta 26 con callsign de 8 bytes.
- Primero se registra el servidor/servicio GATT; solo después de `onServiceAdded` se solicita publicidad. “Visible” depende de `onStartSuccess`. Inicio tiene timeout de 10 s; los callbacks de sesiones detenidas se ignoran.
- Android no expone callback de escaneo iniciado. La UI muestra “Escaneo solicitado; esperando señales” hasta recibir un resultado, y distingue error/detenido. No se presenta la solicitud como escaneo confirmado.
- Una conexión LE saliente por vez, escritura con respuesta y cierre después del callback. Timeout global de 20 s. Errores, cancelación, pausa y Bluetooth apagado liberan GATT y `isSending`. Callbacks duplicados/tardíos no completan otro envío.
- Formato compatible `CALLSIGN:MENSAJE` UTF-8 estricto. Un paquete completo de hasta **20 bytes** cabe en MTU ATT mínimo 23, sin negociación ni fragmentación. Callsign hasta 8 bytes (vacío usa UNKNOWN); texto dispone de `20 - bytes(callsign) - 1`. Con 8 bytes de callsign: 11 bytes de texto. Acentos/emoji consumen varios bytes. La UI muestra el límite y bloquea excesos; el controlador valida nuevamente.
- El servidor rechaza paquetes inválidos, offset distinto de cero, prepared writes y escrituras sin respuesta. No se aceptan fragmentos ni se reemplaza UTF-8 corrupto.
- Solo se agrega un envío al historial tras escritura GATT exitosa. Eso **no es ACK de aplicación** ni garantía de lectura: la UI lo dice explícitamente. Ante desconexión/timeout la recepción puede haber ocurrido; comprobar en el otro teléfono antes de reenviar.
- Android puede usar direcciones distintas como central y anunciante. Al recibir se asocia la dirección a un callsign observado solo si es único; usar callsigns diferentes en la demo. Esta asociación no autentica identidad y callsigns duplicados/direcciones rotativas requieren validación adicional en hardware.
- El borrador se conserva ante errores y se limpia al aparecer el envío confirmado. Historial solo en memoria.
- Al salir de Mesh o pausar la actividad se detienen radios, servidor, envío y observador Bluetooth. Volver a la pantalla reinicia según los controles. Bluetooth off/on actualiza el estado y obtiene nuevos handles de scanner/advertiser.
- Android 6–11: permiso y servicio de ubicación activos para escanear. Android 12+: permisos Nearby Devices (SCAN/CONNECT/ADVERTISE); ubicación no requerida por este flujo.

## Compilación y pruebas automatizadas

El wrapper anterior (Gradle 9.7.1) fallaba aplicando AGP 8.13.1 por una API interna eliminada. Se fija Gradle 8.13, según la [matriz de AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes). Usar JDK 17 o 21; Java 25 no permite ejecutar este Gradle.

```bash
# JAVA_HOME debe apuntar a un JDK 17/21 instalado
./gradlew testDebugUnitTest assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
# Tests: app/build/reports/tests/testDebugUnitTest/index.html
```

Los tests cubren empaquetado/parseo, límites exactos de bytes, Unicode, UTF-8 inválido, frames incompletos, callsign, mensajes vacíos, callbacks duplicados y estados terminales. No simulan el stack Bluetooth ni reemplazan la prueba física siguiente.

## Prueba manual reproducible (pendiente con dos Android)

Registrar modelo, versión Android, versión APK, fecha, permisos y resultado de cada paso. Preferir un teléfono Android 6–11 y otro Android 12+; repetir con otros fabricantes si están disponibles.

1. Instalar la misma APK debug en A y B. Asignar callsigns diferentes de 3–8 caracteres ASCII, por ejemplo `ALFA` y `BRAVO`.
2. En ambos: activar modo avión, reactivar Bluetooth manualmente y mantener Wi-Fi/datos apagados. En Android 6–11 activar también Ubicación.
3. Abrir Mesh Radar, conceder permisos. Verificar publicidad en estado de inicio y luego “Visible”; escaneo solicitado pasa a señales detectadas al encontrar al otro. Confirmar callsign y RSSI, sin nombre largo del teléfono en publicidad.
4. Seleccionar BRAVO desde A. Enviar `HOLA B`; verificar texto íntegro en B y una sola burbuja saliente en A con “Escritura GATT confirmada · sin ACK”. En B seleccionar ALFA y responder `HOLA A`. Verificar ambas direcciones sin red.
5. Repetir al menos 10 envíos alternados y uno simultáneo en cada dirección. Probar `sí:🆘`, espacios internos y texto exactamente en el límite mostrado. No deben aparecer duplicados por callbacks ni truncamiento.
6. Superar el límite con ASCII y emoji: contador/error, botón bloqueado y borrador intacto. Callsign superior a 8 bytes UTF-8 debe mostrar error explícito al iniciar publicidad/enviar.
7. Apagar Bluetooth en B durante un envío de A. A debe dejar de enviar por error o antes de 20 s; nunca agregar un envío fallido. Puede haber recepción sin confirmación si se perdió la respuesta. Reactivar B, redescubrir y volver a probar A↔B. Repetir apagando Bluetooth en A.
8. Salir de Mesh durante inicio de publicidad y durante un envío. No deben reaparecer estados “Visible” por callbacks tardíos ni continuar radios en segundo plano. Volver, apagar/encender el switch de visibilidad y detener/reiniciar escaneo.
9. Pulsar Home, bloquear/desbloquear y volver. Verificar detención en pausa y reinicio al volver. Repetir denegando permisos, revocándolos desde Ajustes y restaurándolos. Sin crash ni estado de éxito falso.
10. Probar peer fuera de alcance o sin servicio de chat (APK antigua): error o timeout, borrador conservado y siguiente envío posible. Si el hardware no soporta advertising debe mostrar error y nunca “Visible”.

## Resultado automatizado en este entorno

- JDK: `/home/lordcommander/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2`.
- `JAVA_HOME=/home/lordcommander/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 ./gradlew testDebugUnitTest assembleDebug`: compilación y tests correctos (ver reportes locales).
- 17 tests correctos: 13 de protocolo, 3 de estados de envío y 1 test original; cero fallos/errores.
- La ejecución adicional `testDebugUnitTest assembleDebug lintDebug` completó tests y APK, pero falló en Lint: **48 errores**, todos en archivos sin modificar (`OscilloscopeScreen.kt`: 24; `ReceiverScreen.kt`: 24), relacionados con opt-in de Camera2. Ningún error de Lint en los archivos BLE de este cambio. No se alteran los módulos ópticos para resolverlos en esta iteración.
- `apksigner verify --verbose app/build/outputs/apk/debug/app-debug.apk`: firma válida, esquema v2, un firmante debug.
- SHA-256 APK: `b611329fdaa4a8e29e7041410c7dc4fd1e6afc55b064898d015ed05ea6e30f13`.
- `git diff --check`: correcto.
- `adb devices -l`: sin dispositivos conectados. No se ejecutaron tests instrumentados ni prueba física.
- El comando pedido `gh issue view 1 --repo NicoButter/beaconchat` se intentó con gh 2.83.2 temporal en `/tmp`; respondió que no hay autenticación. Se leyeron el issue y sus comentarios (ninguno) mediante la conexión GitHub disponible.

## Registro de aceptación

- Prueba física A↔B: **pendiente**, no se afirma validación en hardware.
- Completar para cada escenario: resultado, latencia aproximada, captura de ambos teléfonos y log de errores si falla.
- Sin ACK de aplicación, cifrado nuevo, persistencia, retransmisión ni compatibilidad garantizada con APK anteriores que enviaban paquetes mayores al límite.

## Archivos de la implementación

| Archivo | Cambio |
| --- | --- |
| `mesh/BLEMeshController.kt` | Publicidad, callbacks reales, servidor/cliente GATT, timeout, errores y ciclo de vida |
| `mesh/DirectBleProtocol.kt` | Empaquetado, parseo y validación UTF-8 de mensajes/callsign |
| `mesh/DirectSendState.kt` | Secuencia de envío y estados terminales |
| `ui/screens/MeshScreen.kt` | Estados observables, permisos, pausa/reanudación, límite y borrador |
| `AndroidManifest.xml` | Permisos de ubicación limitados a Android ≤11 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.13 |
| `src/test/.../mesh/DirectBleProtocolTest.kt` | 13 pruebas de protocolo |
| `src/test/.../mesh/DirectSendStateTest.kt` | 3 pruebas de estados/errores |
| `docs/PRUEBA_BLE_DIRECTO.md` | Decisiones, evidencia y protocolo físico |
