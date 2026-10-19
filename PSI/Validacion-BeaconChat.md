# 06 · Validación y criterios de aceptación

**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · Actualizado 18/10/2026**
[Índice](../README.md) · [Requerimientos](ERS-BeaconChat.md) · [Trazabilidad](Trazabilidad-BeaconChat.md)

## Implementación #5 · SOS offline y laboratorio GNSS · 18/10/2026

**Resultado: 183 tests JVM aprobados (144 de la base + 39 nuevos), 0 fallos, 0 errores, 0 omitidos y APK debug compilada con Gradle 8.13/JDK 21.** Se construye, serializa, fragmenta, reensambla e interpreta un SOS válido sintético o sin posición, incluido A→B→C con el adaptador y GATT falso. **No se obtuvieron coordenadas satelitales ni se probó BLE físico.** ADB devolvió una lista vacía al iniciar y terminar.

### Base, alcance y archivos

Checkout `NicoButter/beaconchat`, `main`, HEAD **`4a1662d6d6f6adf322ef9681741e285b2f731df6`**, inicialmente limpio. `git rev-list --left-right --count HEAD...origin/main` devolvió `0 0`; `git ls-remote origin refs/heads/main` confirmó el mismo SHA remoto (consulta de lectura, sin fetch/reset). Se preservó todo el trabajo de la base. No se crean ramas, worktrees, commits ni push. Los informes inferiores son históricos y sus límites describen cada iteración de entonces.

| Archivos nuevos / modificados | Resultado |
| --- | --- |
| [SosIncident.kt](../app/src/main/java/com/nicobutter/beaconchat/sos/SosIncident.kt) | Modelo inmutable, enums con IDs de wire explícitos, ubicación opcional y validación estricta |
| [SosCodec.kt](../app/src/main/java/com/nicobutter/beaconchat/sos/SosCodec.kt) | Registro SOS binario V1 en contenido ASCII base64url; sin modificar protocolo mesh |
| [SosMeshPolicy.kt](../app/src/main/java/com/nicobutter/beaconchat/sos/SosMeshPolicy.kt) | Factory de ensayo separada, barrera de privacidad y gateway de aplicación; no es gateway de red/HTTPS |
| [GnssAcquisition.kt](../app/src/main/java/com/nicobutter/beaconchat/sos/GnssAcquisition.kt) | Política pura de solicitud única, tokens, deadline, precisión, edad, actual/conocida/antigua/ausente |
| [AndroidGnssLocationSource.kt](../app/src/main/java/com/nicobutter/beaconchat/sos/AndroidGnssLocationSource.kt) | LocationManager/GPS_PROVIDER asíncrono con cancelación/lifecycle y timeout configurable |
| [SosLaboratoryDialog.kt](../app/src/main/java/com/nicobutter/beaconchat/ui/screens/SosLaboratoryDialog.kt), [MeshScreen.kt](../app/src/main/java/com/nicobutter/beaconchat/ui/screens/MeshScreen.kt) | Laboratorio local independiente de disponibilidad BLE; recepción interpretada y estados sin confirmar |
| [MeshBleNode.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/MeshBleNode.kt), [AndroidMeshTransport.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/AndroidMeshTransport.kt) | Política SOS aplicada en origen y antes de entrega/relay; envío tipado del ensayo por el transporte actual |
| [AndroidManifest.xml](../app/src/main/AndroidManifest.xml) | FINE/COARSE sin maxSdk 30 para GPS también en Android reciente; sin INTERNET ni BACKGROUND_LOCATION |
| [SosModelCodecTest.kt](../app/src/test/java/com/nicobutter/beaconchat/sos/SosModelCodecTest.kt), [GnssAcquisitionTest.kt](../app/src/test/java/com/nicobutter/beaconchat/sos/GnssAcquisitionTest.kt), [SosMeshIntegrationTest.kt](../app/src/test/java/com/nicobutter/beaconchat/sos/SosMeshIntegrationTest.kt) | 39 tests nuevos; el helper GATT de MeshBleAdapterTest pasa de private a internal para reutilizar exactamente la misma simulación |
| [README español/inglés](../README.md), [Requerimientos](ERS-BeaconChat.md), [Arquitectura](Diseño-Arquitectura-BeaconChat.md), [Roadmap](Roadmap-BeaconChat.md), [Trazabilidad](Trazabilidad-BeaconChat.md), este informe | Estado actualizado sin duplicar documentos ni alterar logos/identidad de Vetrabyte |

MeshEngine, MeshMessage, MeshMessageCodec, MeshFragmentProtocol, MeshReassembler, MeshGattClient, MeshLinkProtocol, DirectBleProtocol y beacon SOS conservan su implementación. No se agregan dependencias, backend, gateway HTTPS, mapas, cuentas ni LoRa. El componente llamado SosMeshGateway es una frontera local de aplicación, no un servicio externo.

### Contrato SOS V1 y presupuesto

`SosIncident`: UUID no nulo estable, versión 1, tipo (`MEDICAL=1`, `FIRE=2`, `TRAPPED=3`, `OTHER=4`), gravedad **declarada** (`LOW=1`, `MODERATE=2`, `HIGH=3`, `CRITICAL=4`), creación epoch ms no negativa, nota opcional no blanca de hasta **256 bytes UTF-8** y posición opcional. No clasifica clínicamente ni asigna prioridad automática. Tiempo civil/identidad son declarados, sin autenticación ni sincronización entre equipos.

Posición: latitud/longitud double finitos en ±90/±180, precisión estimada opcional finita ≥0 metros, fuente GNSS/sintética/mock GPS, edad monotónica no negativa y validez coherente. `CURRENT` exige ≤30.000 ms; `LAST_KNOWN` ≤300.000 ms; `STALE` >300.000 ms. Un fix cacheado no se presenta como nuevo aunque tenga edad cero. Ausencia es **null**, nunca coordenadas de reemplazo; `(0,0)` sigue siendo una coordenada explícita válida.

Formato de contenido: prefijo ASCII **`BCSOS:`**, seguido de base64url canónico sin padding de un registro **big endian**:

| Campo, en orden | Bytes / regla |
| --- | --- |
| Versión | 1, solo 01 |
| Flags | 1; bit 0 posición, bit 1 precisión presente (requiere posición), bit 2 laboratorio; rechazar otros bits |
| UUID de incidente | 16, dos longs en orden MSB/LSB |
| Creación epoch ms | 8, no negativa |
| Tipo / gravedad | 1 + 1, IDs explícitos anteriores, nunca ordinal de enum |
| Si hay posición: fuente / validez | 1 + 1; GNSS=1, sintética=2, mock=3; CURRENT=1, LAST_KNOWN=2, STALE=3 |
| Latitud / longitud / edad | 8 + 8 + 8; doubles y long |
| Precisión, si su flag está presente | 8, double |
| Longitud de nota | 2, sin signo, 0 = ausente; exacta y ≤256 |
| Nota | UTF-8 estricto, sin sustituciones, truncado ni normalización |

Registro sin posición **30 + N bytes**; con posición **56 + N**, o **64 + N** si incluye precisión. Máximo **320 bytes binarios → 433 bytes ASCII** con prefijo, dentro de 1024 de MeshMessage. El máximo unicast queda en 486 bytes de envelope y **61 tramas ≤20 bytes**. Se rechazan versiones/enums/flags desconocidos, longitudes falsas, sobrantes, truncados, base64 no canónico, UTF-8 inválido, coordenadas/precisión no finitas y edades/validez inconsistentes. Base64 **no cifra**. La implementación no depende de java.util.Base64 (API 26): su codificación acotada es compatible con minSdk 24 y se contrasta en JVM con el encoder JDK.

El UUID y contenido SOS quedan fijos durante un reenvío o reintento del mismo objeto. **Cada reintento manual crea otro messageId mesh**, para no reutilizar la clave de un intento ya deduplicado, conservando el incidente dentro del contenido. No hay reintento automático, custodia persistente ni deduplicación durable de incidentes; un receptor puede observar dos intentos del mismo SOS. Para cambiar sus datos, la UI prepara otro incidente con nuevo UUID.

### GNSS offline, permisos y ciclo de vida

Solo `LocationManager.GPS_PROVIDER`: lectura de última posición de ese proveedor y `requestLocationUpdates`, sin proveedor NETWORK/fused, Wi-Fi/datos, geocoder o mapas. Callbacks y mantenimiento llegan al main looper; no se espera sincrónicamente un fix. Una solicitud activa y un candidato en memoria; token/deadline rechazan callbacks viejos o posteriores al vencimiento. Timeout **1..120 s**, predeterminado **30 s**. GPS apagado/ausente, timeout, denegación, acceso solo aproximado y revocación degradan explícitamente a conocido/ausente/error y permiten crear SOS sin ubicación.

FINE/COARSE se declaran sin límite API 30. La UI los solicita juntos al pulsar GPS; requiere FINE para iniciar GPS_PROVIDER. Android 12+ puede conceder solo aproximada: no se usa como fix preciso ni se solicita fondo. Se contrastó el manejo con [permisos en tiempo de ejecución](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime) y la edad/precisión con [Location](https://developer.android.com/reference/android/location/Location). La precisión es estimada; GPS_PROVIDER o una bandera no prueban por sí solos recepción satelital. Mocks Android se conservan como `MOCK_GPS`, nunca se convierten automáticamente en datos sintéticos autorizados.

La edad se calcula con elapsed realtime del mismo teléfono, se actualiza en la vista y se vuelve a evaluar al crear SOS. El cuerpo guarda la instantánea de edad/calidad **al crear**, rotulada así en UI; no se transforma una ubicación antigua en actual durante un retry. Al pausar/salir/cerrar se quitan listeners y watchdog, se cancelan solicitudes, se descarta el fix y el SOS local. Solo se usan estados Compose no guardados; no se persisten coordenadas/nota SOS ni se añaden al historial mesh. La nota editable queda solo en la memoria del diálogo y el ensayo usa una nota fija separada.

### Privacidad, integración y veracidad de estado

El laboratorio crea por separado datos de ensayo: otro UUID, nota fija y **posición constante ficticia `12.3456,-45.6789`, precisión 25 m, edad 0**, o null. La UI no copia GNSS ni nota del SOS local. El envío tipado exige laboratorio y posición ausente o exactamente esa fixture. `MeshBleNode` vuelve a aplicar la política tanto antes de originar como después de reensamblar/validar el envelope y **antes de MeshEngine.receive**: un SOS con GNSS/mock real, una posición arbitraria etiquetada sintética, o `BCSOS:` malformado no se entrega, registra con contenido ni retransmite.

Mensajes normales conservan su ruta y chat legado intactos; `BCSOS:` es el espacio reservado del nuevo contenido estructurado, no un cambio del protocolo binario mesh. Esta barrera es de aplicación: **no autentica declaraciones remotas ni detecta coordenadas escondidas en texto arbitrario**. No es anonimización/cifrado; la protección de coordenadas reales necesita un diseño criptográfico antes de levantar el bloqueo. Un peer antiguo desconoce esta política; no se le envían datos reales como prueba.

UI en Mesh Radar → “Laboratorio SOS offline”: tipo/gravedad, nota local, captura GPS, creación local con/sin posición y preparación separada de ensayo sintético/sin posición. Enviar/reintentar solo está habilitado con participación mesh y conserva el UUID del ensayo. `ORIGINATED_LOCALLY` significa generación del intento, **no admisión en cola**: la UI muestra admisión solo al observar QUEUED, enlace solo ante LINK_WRITTEN, recepción local tras interpretar el SOS, y siempre **destino final sin confirmar**. No hay servidor ni ACK final ni servicio operativo de rescate.

### Ejecución, resultados y límites de evidencia

Antes de cambios: `./gradlew testDebugUnitTest --rerun assembleDebug --console=plain`, JDK 21.0.7+6, **144 aprobados**, `BUILD SUCCESSFUL in 1s` (test reejecutado). El sandbox bloqueó inicialmente el lock de caché Gradle; se ejecutó con acceso autorizado, sin cambiar wrapper/dependencias. También se autorizó la consulta de lectura del remoto tras fallo DNS dentro del sandbox.

Validación final:

```bash
JAVA_HOME=/home/lordcommander/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 \
  ./gradlew testDebugUnitTest assembleDebug --console=plain
git diff --check
```

**BUILD SUCCESSFUL in 3s**, 44 tareas (7 ejecutadas, 37 actualizadas); suite ejecutada y APK recompilada. Primer intento de la suite nueva: APK compilada y error de compilación en un test por smart cast de propiedad nullable de otro módulo; se fijó una referencia local y las 183 pruebas pasaron. Tras aclarar la edad al crear en UI se repitió la validación final, nuevamente aprobada. Se conservan warnings de BuildConfig, source/target Java 8 y APIs obsoletas. No se ejecutó Lint ni instrumentación.

| Suite nueva | Tests | Cobertura |
| --- | ---: | --- |
| SosModelCodecTest | 16 | Vector binario fijo, ausencia/ceros explícitos, UUID/tiempo/enums, coordenadas/precisión, umbrales de edad, UTF-8 ≤256 bytes y exceso, máximo 433 bytes, base64 canónico, longitudes, todos los prefijos truncados y 3000 entradas mutadas/aleatorias |
| GnssAcquisitionTest | 14 | Solicitud única/token, precisión/edad/fuente, cache nunca actual, fix envejecido, GPS apagado, permiso insuficiente/revocado, timeout exacto, cancelación, callbacks tardíos/futuros, mock y límites de configuración; **sin Android/GPS real** |
| SosMeshIntegrationTest | 9 | SOS → codec → MeshMessage → fragmentos/reensamblado → interpretación; GATT falso A→B→C sin A↔C, UUID/cuerpo estable en retries, bloqueo de GNSS en gateway/origen/relay sin logs con contenido, ausencia de posición, chat normal/legacy y ningún éxito final inventado |
| Regresión anterior | 144 | Motor, transporte, adaptador BLE y protocolos directos |
| **Total** | **183** | **0 fallos, 0 errores, 0 omitidos** |

Evidencia local no versionada: `app/build/reports/tests/testDebugUnitTest/index.html`, XML `app/build/test-results/testDebugUnitTest/TEST-*.xml`, APK `app/build/outputs/apk/debug/app-debug.apk` (**12.222.833 bytes**, SHA-256 `08c4b3ea4a89b69c792cb4299d51793cc5ca5a42481132f3ebce9890c453ca92`). Suite final SOS de integración: **18/10/2026 23:43:51.904 UTC−03:00** (`2026-10-19T02:43:51.904Z`). `git diff --check` correcto. **AndroidGnssLocationSource/LocationManager/Compose se verificaron por compilación; no por ejecución en teléfono.**

### Pruebas físicas pendientes y próximos pasos

1. En un Android API 24–30 y otro API 31+, instalar la misma APK por serial explícito y documentar modelo/API/APK/HEAD, permisos y radios. Modo avión, Wi-Fi/datos apagados, GPS habilitado. Abrir el laboratorio y solicitar GPS al aire libre/interior: registrar tiempo, fuente, precisión, edad, timeout y disponibilidad real. No acreditar satélites solo por tener coordenadas; conservar evidencia del proveedor/estado GNSS y marcar mocks. No publicar capturas con ubicación/nota reales.
2. Ensayar denegación, “solo aproximada” Android 12+, revocación, GPS apagado, ausencia de fix/última posición, timeout 1/30/120 s, cancelación/Home/cierre y regreso. Confirmar listeners detenidos, sin ubicación de fondo, antiguo rotulado y SOS sin posición válido. Usar datos no sensibles o sintéticos para capturas compartidas.
3. Para tránsito, usar **exclusivamente** “Preparar SOS sintético” o “Preparar ensayo sin posición”. Activar mesh, aplicar el procedimiento A↔B/A→B→C inferior con extremos aislados y verificar el mismo UUID SOS/cuerpo en receptor e intentos. Confirmar que cola/enlace nunca anuncien destino final. Con un emisor de pruebas, inyectar SOS GNSS sintéticos marcados fuente GNSS para verificar rechazo; nunca inyectar coordenadas privadas reales.
4. Validar permisos/lifecycle/UI y radio en hardware; medir adquisición, energía y latencia. Mantener bloqueo hasta resolver autenticación/cifrado, consentimiento y retención. Backend, gateway, mapas, cuentas, LoRa, custodia y confirmación final siguen fuera de esta iteración.

## Histórico: integración Android mesh experimental · 18/10/2026

**Resultado actual: recorrido Android/GATT implementado y APK debug compilada; 144 tests JVM aprobados (110 anteriores + 34 nuevos), 0 fallos, 0 errores y 0 omitidos. BLE físico A↔B y A→B→C no ejecutado.** `adb devices -l` devolvió una lista vacía al iniciar y al finalizar la validación. Las topologías automatizadas sustituyen el I/O Bluetooth por un GATT falso; no constituyen resultados de radio.

Trabajo directamente en `main`, base `ac1dd8983960f6500b95433d40d6d390b5a9ebb3`, inicialmente limpia. Sin ramas, worktrees, commit ni push. No se agregan dependencias, permisos, GPS, gateway, servidor, mapas ni pantallas nuevas. Se preservan los protocolos directos y del beacon SOS; las secciones posteriores registran las iteraciones anteriores y sus límites de aquel momento. El formato binario V1 y sus cuotas siguen vigentes; el contrato GATT antes reservado ahora se registra y se habilita por participación explícita.

### Arquitectura implementada y archivos

| Archivo | Responsabilidad |
| --- | --- |
| [MeshBleNode.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/MeshBleNode.kt) | Un MeshEngine por NodeId local; vecinos, sesiones, reensamblador, admisión atómica del fanout, cola acotada e historial local |
| [MeshGattClient.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/MeshGattClient.kt) | Máquina de estados síncrona sobre un puerto asíncrono; conexión, capacidades/identidad, escritura y recibo correlacionado de cada trama, timeout/cancelación |
| [MeshLinkProtocol.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/MeshLinkProtocol.kt) | Hello versionado de 20 bytes, recibos de admisión por trama e IDs de transferencia sin reutilización dentro de la retención |
| [AndroidMeshTransport.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/AndroidMeshTransport.kt) | BluetoothGatt real, callbacks API 24–32/33+, reloj monotónico, mantenimiento con Handler, identidad persistida en IO y StateFlow de la UI |
| [BLEMeshController.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/BLEMeshController.kt) | Características mesh en el servicio existente, despacho separado de callbacks, descubrimiento desde scan y arbitraje con el envío directo |
| [MeshFragmentProtocol.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/transport/MeshFragmentProtocol.kt) | Extensión de MeshGattContract con UUID del servicio y característica de identidad; formato de fragmentación sin cambios |
| [MeshScreen.kt](../app/src/main/java/com/nicobutter/beaconchat/ui/screens/MeshScreen.kt) | Diálogo experimental: participación, mensaje/destino/TTL, NodeId copiable, vecinos, errores y actividad local |
| [MeshLinkProtocolTest.kt](../app/src/test/java/com/nicobutter/beaconchat/mesh/ble/MeshLinkProtocolTest.kt), [MeshGattClientTest.kt](../app/src/test/java/com/nicobutter/beaconchat/mesh/ble/MeshGattClientTest.kt), [MeshBleAdapterTest.kt](../app/src/test/java/com/nicobutter/beaconchat/mesh/ble/MeshBleAdapterTest.kt) | 34 tests nuevos con el mismo adaptador/máquina de estados usados por Android y un puerto GATT falso |
| Este documento y [Arquitectura](Diseño-Arquitectura-BeaconChat.md) | Contratos, decisiones, resultados, límites y procedimiento físico pendiente |

Recorrido de producción: `MeshScreen → MeshBleNode.originate → MeshEngine.dispatch → cola local → MeshGattClient → codec/fragmentador → BluetoothGatt WRITE → callback + READ de recibo → servidor GATT → MeshReassembler → codec → MeshEngine.receive(previousHop declarado) → cola del relay → otro vecino`. Solo MeshEngine descuenta TTL, una vez por salto lógico. La recepción deduplica por `(originId, messageId)`, conserva el origen original y excluye de la retransmisión el NodeId del salto anterior. Los tests verifican esta ruta en cadena, diamante y anillo.

### Canales, capacidades e identidad de sesión

Se conservan advertising BEEF, servicio C4A7 y característica directa 4D51, incluidos callsign y `CALLSIGN:MENSAJE`, UTF-8 estricto y máximo 20 bytes. No se modifica el beacon SOS. En el mismo servicio C4A7 se registran dos características independientes **READ + WRITE con respuesta**:

- Datos/recibo mesh: **`7d2ea28a-f7bd-4f56-9e75-6e57c1e9b501`** (`MeshGattContract.CHARACTERISTIC_UUID`). WRITE recibe exclusivamente tramas V1; READ devuelve el recibo de la última trama admitida en esa sesión.
- Identidad/capacidades: **`7d2ea28a-f7bd-4f56-9e75-6e57c1e9b502`** (`IDENTITY_UUID`). READ declara la identidad local; WRITE recibe la identidad del cliente de esa conexión.

Las características están presentes cuando existe el servidor de chat, pero **rechazan tráfico mesh con la participación desactivada**. Así no se reinicia advertising ni se cambia el esquema GATT al tocar el interruptor. No se agregan UUID al anuncio legacy ni se excede su presupuesto: se descubren candidatos con el scan existente, luego se verifican ambas características, sus propiedades y el hello por GATT. Un peer legacy sin ellas queda sin capacidad mesh y conserva su chat directo.

Hello de **20 bytes**, big endian: `FA | versiónHello=01 | versiónMeshMessage=01 | versiónFragmentos=01 | NodeId UUID128`. Se exige longitud exacta, versiones conocidas e identidad no nula. Se rechaza la identidad propia o un cambio de identidad dentro de una sesión ya asociada. Antes de enviar datos, el cliente lee el hello del servidor y escribe el suyo; una transferencia en cola exige que el NodeId remoto coincida con el verificado para ese vecino. El servidor no acepta tramas hasta recibir un hello válido y usa su NodeId declarado como `previousHop`, independientemente del origen del mensaje retransmitido.

El NodeId local se genera con UUID aleatorio y se conserva en preferencias privadas `mesh_identity_v1/node_id`. Lectura y persistencia se realizan en `Dispatchers.IO`; no se permite participar antes de confirmar la persistencia. La MAC es únicamente un handle transitorio para conectar y localizar la sesión actual, y el callsign solo sirve al chat/discovery legacy. No se vinculan identidades lógicas mediante coincidencias de callsign o direcciones centrales/anunciantes.

**Este intercambio valida formato y consistencia; no autentica criptográficamente la identidad.** Un vecino puede mentir sobre su UUID y sobre el origen de un envelope; suplantación/replay/DoS no están resueltos. CRC32 sigue siendo integridad accidental, no autenticación ni cifrado. Copiar/restaurar los datos privados puede duplicar una identidad; provisión, respaldo/recuperación y revocación requieren diseño posterior. No se atribuye confianza permanente a MAC, callsign ni UUID declarado.

### Secuencia GATT, concurrencia y resultados

Cada trabajo abre un cliente GATT, descubre capacidades, intercambia hello, envía como máximo un mensaje y cierra. Las tramas siguen limitadas a 20 bytes. Después de `onCharacteristicWrite` exitoso se solicita READ en la característica de datos. El recibo es la cabecera original de **12 bytes** (incluye transferencia, índice y CRC); solo una coincidencia exacta permite avanzar al siguiente fragmento. Lecturas de recibos antiguos se ignoran hasta el callback correcto o timeout. No se emite la siguiente escritura antes de esa comprobación.

Esto contempla que [onCharacteristicWrite](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt,android.bluetooth.BluetoothGattCharacteristic,int)) no incorpora un ID de solicitud propio de la aplicación. Un callback de escritura tardío no puede saltar silenciosamente un fragmento: necesita además el recibo correspondiente. Si el stack rechaza una lectura por estar ocupado, falla explícitamente el trabajo. Los callbacks de otra conexión/token se descartan y se cierra su handle antiguo. En API 33+ se usan valores explícitos al escribir/leer; en API 24–32 el valor leído se copia antes de encolarlo al hilo principal.

**El recibo significa admisión de una trama en el vecino de ese enlace, nunca ACK del destino final.** La UI separa originado localmente, admitido en cola local, escrito en el enlace y recibido localmente. Escrito en enlace lleva siempre “no confirmado”; no existe un estado de entrega final. Un relay puede haber recibido y aun fallar al reenviar. `MeshTransport.ACCEPTED` significa solo que el fanout entró completo en la cola local; cuando no hay vecinos o capacidad se registra el fallo y el motor consume ese intento.

| Recurso / política | Límite actual |
| --- | --- |
| Vecinos descubiertos | 16 handles temporales; sin anuncios durante 30 s se eliminan; NodeId/capacidades se verifican por GATT |
| Sondeos | Hasta 2 pendientes, más el activo; enfriamiento por vecino de 30 s; al liberar un lugar se continúa con candidatos ya detectados |
| Cola saliente | **8 trabajos incluyendo el activo**, probes incluidos; fanout mayor que los lugares disponibles se rechaza sin admisión parcial |
| Conexiones salientes | **Una por nodo**; chat directo y mesh se arbitran. Mientras el chat posee el enlace, mesh espera con límite de cola; un envío directo iniciado con mesh ocupado informa ese estado |
| Espera en cola / conexión total | 30 s / 30 s |
| Operación GATT / transferencia activa | 5 s / 14 s; comprobadas por mantenimiento cada segundo, sujeto al scheduling Android |
| Sesiones mesh entrantes | 8; timeout de 20 s de inactividad o 30 s totales |
| Historial experimental | 32 eventos en memoria; UI muestra los últimos 8 |
| Reensamblador | Sin cambios: 8 activas globales, 2 por sesión, 32 entradas, 8616 bytes de payload reservado, 135 fragmentos y 15 s sin extensión |

Los IDs de transferencia avanzan globalmente en el adaptador y no se reutilizan entre sus clientes GATT consecutivos. Tras consumir los 65536 IDs se exige una pausa de al menos 15 s desde el último antes de reiniciar el contador; un agotamiento causa fallo explícito. Cada sesión entrante tiene un token nuevo; desconexión, cierre del servidor, cancelación o expiración ejecutan `discardSession`. No hay buffers incompletos entregados al motor. El host también mantiene limpieza mientras participa, no solo al recibir tramas.

Todas las mutaciones de Bluetooth/motor ocurren serializadas en el main looper; las operaciones GATT son asíncronas, sin esperas de radio ni disco en ese hilo. Al pausar/salir de MeshScreen o apagar Bluetooth se desactiva la participación, se cancelan trabajos y se cierran sesiones. Volver requiere activar otra vez. No hay servicio de fondo, persistencia de mensajes, reintento automático ni garantía de entrega. Los parámetros temporales y la sobrecarga de WRITE+READ por trama deberán medirse físicamente, especialmente con mensajes máximos de 135 tramas.

### Validación ejecutada

Antes de cambiar código se inspeccionaron controlador/UI, núcleo, codec, fragmentador, reensamblador, manifest y wrapper 8.13. La primera ejecución reutilizó resultados; se forzó después únicamente la tarea de test con `testDebugUnitTest --rerun assembleDebug`: **110 tests aprobados**, `BUILD SUCCESSFUL in 1s`. El sandbox bloqueó el lock de la caché Gradle; se ejecutó con acceso autorizado, sin cambiar el wrapper ni dependencias.

Validación final con Temurin JDK 21.0.7+6:

```bash
JAVA_HOME=/home/lordcommander/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 \
  ./gradlew testDebugUnitTest assembleDebug --console=plain
```

**BUILD SUCCESSFUL in 2s**, 44 tareas (7 ejecutadas, 37 actualizadas); test JVM ejecutado y APK debug recompilada. Las primeras 142 pruebas pasaron. Al ampliar a 144, el test de nueve vecinos detectó que el sondeo esperaba nuevos anuncios cuando su cola se llenaba; se corrigió la continuación de candidatos pendientes y las **144 pasaron**. Se mantienen advertencias previas de BuildConfig, Java source/target 8 y APIs obsoletas. No se ejecutó Lint ni instrumentación Android.

| Suite nueva | Tests | Evidencia |
| --- | ---: | --- |
| `MeshLinkProtocolTest` | 5 | Hello/versiones/UUID y 20 bytes; recibos por transferencia/posición/CRC; agotamiento y retención de IDs; separación legacy |
| `MeshGattClientTest` | 10 | Secuencia real de operaciones sobre puerto falso; callbacks repetidos/tardíos y tokens antiguos; recibos desfasados; identidades inválidas/cambiadas/propias; cancelación, desconexión y deadlines por fase/transferencia; ningún LINK_WRITTEN falso |
| `MeshBleAdapterTest` | 19 | A→B→C sin A↔C con 1024 bytes UTF-8; TTL; B desconectado y recuperado; cadena/diamante/anillo; duplicados; pérdida/corrupción sin handoff válido; saturación y fanout atómico; reconexiones; arbitraje con legacy; límites de vecinos/sesiones/historial; envelope inválido con CRC correcto |
| Regresión anterior | 110 | Protocolos directos, motor, codec, fragmentación y topologías previas |
| **Total** | **144** | **0 fallos, 0 errores, 0 omitidos** |

Evidencia local: `app/build/reports/tests/testDebugUnitTest/index.html`, XML `app/build/test-results/testDebugUnitTest/TEST-*.xml` y `app/build/outputs/apk/debug/app-debug.apk` (**12.194.123 bytes**, SHA-256 `7cb1c2c6e873c1a94502f4aae81e9e635d994a78201b5cbac94ff91209b86bf1`). Timestamp de la suite final del adaptador: **18/10/2026 23:04:46.125 UTC−03:00** (`2026-10-19T02:04:46.125Z`). Los tests usan las clases de política/estado de producción; **no ejecutan BluetoothGatt, Handler, preferencias Android ni radios reales**. El puente Android y Compose se validaron por compilación, y requieren comprobación de callbacks, permisos, ciclo de vida y coexistencia entre fabricantes en dispositivos.

### Procedimiento controlado en tres Android — pendiente, no ejecutado

Aplicar primero el procedimiento A↔B del informe directo inferior con participación mesh desactivada. Luego:

1. Preparar A, B y C físicos con API 24+, scan y advertising conectable. Registrar modelo/API, serial ADB, responsable, fecha/zona, HEAD, SHA-256 de APK, permisos y configuración de radios. Instalar la misma APK sin desinstalar datos ni elegir automáticamente un equipo. Comandos futuros, reemplazando los tres seriales explícitos: `adb -s SERIE_A install -r app/build/outputs/apk/debug/app-debug.apk`, y equivalentes B/C. Capturar `adb -s SERIE_A logcat -v threadtime` y equivalentes; grabar también la actividad mostrada por la UI. Usar HCI/observador BLE si se atribuye un recorrido físico concreto.
2. Modo avión en los tres; reactivar Bluetooth y verificar Wi-Fi/datos apagados. API 24–30 necesita Ubicación encendida para scan, sin obtener coordenadas. Mantener Mesh Radar en primer plano, callsigns ASCII distintos, y abrir “Mesh experimental” → “Participar” en los tres. Registrar/copiar los **NodeId completos**, distintos, y esperar a que los vecinos requeridos muestren “Mesh V1”. No basta observar un callsign o un anuncio.
3. Ubicar/aislar radios para conservar A↔B y B↔C sin A↔C. Esperar al menos 30 s para eliminar vecinos antiguos y registrar la tabla de identidades/capacidades de cada equipo. **No usar solo RSSI, distancia nominal o ausencia momentánea en pantalla como prueba de aislamiento.** Como control negativo, enviar desde A un mensaje único al NodeId de C con **TTL=1**: B puede aceptar el enlace, debe agotar TTL y C no debe registrar esa clave. Si C lo recibe, hay un camino directo permitido por el ensayo y no puede acreditarse la cadena A→B→C.
4. En A, destino = NodeId de C, **TTL=2**, texto sintético único `CADENA-01 sí 🆘`. Conservar originId/messageId visibles. Esperado: A “en cola” y luego “escrito en enlace · no confirmado”; B actividad de retransmisión con mismos UUID/texto/destino y TTL 0; C **“recibido localmente”**, mismos campos y TTL 0, una vez. La actividad de B no prueba por sí sola la recepción en C. Repetir C→B→A, y 10 intentos únicos por dirección esperando cada resultado; registrar también todos los fallidos. El objetivo provisional es 10/10 íntegros en la cadena estable, no una tasa ya medida.
5. Desactivar la participación de B **antes** de originar una nueva clave en A. C no debe recibirla por esa topología; A debe mostrar fallo/no vecinos/timeout y conservar “no confirmado”, incluso si un enlace previo había sido exitoso. Esperar hasta 40 s para observar los límites del trabajo, sin convertir la ausencia de recibo final en una garantía universal de no recepción. Reactivar B, esperar redescubrimiento (sondeo con cooldown hasta 30 s) y originar **otro mensaje**: la cola anterior no se reintenta automáticamente.
6. Repetir con broadcast, TTL 0, texto multibyte y 1024 bytes exactos; exceso rechazado sin truncar. Inducir desconexión durante fragmentación, Bluetooth off/on, salida/Home y revocación de permiso. Confirmar cancelación, sesiones liberadas y necesidad de reactivar participación. Con cliente BLE de prueba, inducir hello/tramas inválidos, offset/prepared write, recibo desfasado y callbacks/errores cuando la herramienta lo permita. Sin fallo realmente inducido, marcar el subcaso no ejecutado.
7. Comprobar convivencia: chat directo A↔B con mesh activo y tras desactivarlo; envío directo durante enlace mesh ocupado informa espera sin falso éxito. Probar además con un peer de la versión legacy si se dispone de esa APK. Verificar el beacon SOS con su procedimiento previo y no confundir sus anuncios con los del chat/mesh. Registrar por separado lo observado; no asumir compatibilidad de radio por haber preservado el formato.

Ficha por intento: `caso | hora local | modelos/API/seriales | APK/HEAD | radios/permisos | NodeId A/B/C | vecinos verificados/aislamiento | originId + messageId | destino/TTL/texto/bytes | acción/fallo | estados A/B/C | recepción local en destino | HCI/log/video | resultado`. Repetir retirando B para probar dependencia del relay; si no puede demostrarse la ausencia A↔C, informar “cadena física no aislada/no validada”.

### Límites y próximos pasos

La implementación Android está disponible para experimentar; la comunicación física sigue pendiente. No hay confirmación verificable del destino final, autenticación, cifrado, entrega durable ni recuperación automática. Sondeos/colas y deadlines favorecen límites explícitos y pueden descartar cargas con enlaces lentos o muchos vecinos. Validar primero A↔B y después el aislamiento A→B→C, medir tiempo por trama/consumo y comportamiento por fabricante, y decidir la política de fiabilidad e identidad segura con esa evidencia. El motor, codec, límites del reensamblador, formato legacy y beacon se mantienen; el alcance de esta iteración es su integración experimental.

## Histórico: transporte mesh binario V1 · implementación y validación JVM · 18/10/2026

**Resultado: 110 tests JVM aprobados (48 anteriores + 62 nuevos), 0 fallos, 0 errores y 0 omitidos; APK debug compilada.** Se cumple el recorrido `MeshMessage → codec → fragmentador → reensamblador → codec → MeshEngine` con enlaces simulados. **Esto no prueba BLE físico: la validación A↔B continúa pendiente.** No se ejecutaron pruebas instrumentadas ni se utilizaron teléfonos.

Trabajo sobre `NicoButter/beaconchat`, rama `main`, base `604d9898b6fa132a8b35ca81ef60752de4a0305b`, inicialmente sin cambios locales. Sin ramas, worktrees, commit ni push. `BLEMeshController`, `MeshScreen`, `DirectBleProtocol` y el núcleo existente permanecen intactos. No se agregan dependencias, permisos, pantallas, GPS, servidor ni gateway. Los informes inferiores se conservan como evidencia histórica de su fecha; sus menciones a un serializer futuro y al wrapper 9.7.1 describen el estado anterior.

### Archivos y separación del canal

- [MeshMessageCodec.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/transport/MeshMessageCodec.kt): serialización estricta del envelope lógico, independiente de Android.
- [MeshFragmentProtocol.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/transport/MeshFragmentProtocol.kt): formato de tramas, fragmentador y contrato GATT reservado.
- [MeshReassembler.kt](../app/src/main/java/com/nicobutter/beaconchat/mesh/transport/MeshReassembler.kt): cuotas, reensamblado, expiración y resultados explícitos, sin radio ni timers.
- Tests nuevos: [codec](../app/src/test/java/com/nicobutter/beaconchat/mesh/transport/MeshMessageCodecTest.kt), [tramas](../app/src/test/java/com/nicobutter/beaconchat/mesh/transport/MeshFragmentProtocolTest.kt), [reensamblador](../app/src/test/java/com/nicobutter/beaconchat/mesh/transport/MeshReassemblerTest.kt) e [integración](../app/src/test/java/com/nicobutter/beaconchat/mesh/transport/MeshTransportIntegrationTest.kt).
- [Wrapper](../gradle/wrapper/gradle-wrapper.properties): distribución corregida de 9.7.1 a **8.13**, compatible con la configuración existente de AGP 8.13.1. Documentación actualizada en este archivo y en [Arquitectura](Diseño-Arquitectura-BeaconChat.md).

`MeshGattContract.CHARACTERISTIC_UUID` reserva **`7d2ea28a-f7bd-4f56-9e75-6e57c1e9b501`** para una característica mesh futura. Es diferente de los UUID actuales de publicidad, servicio y escritura directa. **No está registrada, anunciada ni conectada al controlador.** El adaptador futuro deberá descubrir explícitamente esta característica y dirigir su callback al reensamblador; no se habilita detección automática sobre el chat directo ni fallback por texto/callsign. El marcador binario `F9` tampoco puede iniciar un paquete legacy UTF-8 válido. Los tests verifican el rechazo en ambos sentidos, conservando los tests originales del chat.

### Contrato binario del envelope

Todos los enteros multibyte usan **big endian**. Los UUID ocupan 16 bytes (64 bits más significativos seguidos de 64 menos significativos); no se envían sus representaciones textuales.

| Campo, en orden | Bytes | Regla V1 |
| --- | ---: | --- |
| Versión de MeshMessage | 1 | `01`; rechazar otras versiones |
| Flags | 1 | `00` broadcast, `01` destino presente; rechazar otros bits |
| `hopsRemaining` (TTL) | 1 | 0..16, perfil de transporte V1 consistente con el límite predeterminado del motor |
| `originId` | 16 | UUID lógico preservado íntegramente |
| `messageId` | 16 | UUID lógico preservado íntegramente |
| `destinationId`, solo unicast | 16 | Ausente = `null`; un UUID de valor cero sigue siendo un destino explícito |
| Longitud del contenido | 2 | 1..1024 **bytes** UTF-8, exactos y sin bytes sobrantes |
| Contenido | L | UTF-8 estricto, no vacío/blanco; preservar espacios, saltos, NUL y composición Unicode |

Tamaño total: **37 + L bytes broadcast / 53 + L unicast**; máximos **1061 / 1077 bytes**. No hay truncamiento ni sustitución de UTF-8/UTF-16 malformado. El codec rechaza longitudes, flags, versiones, TTL y contenido inválidos antes de producir un mensaje. Sus errores son `IllegalArgumentException`; el adaptador deberá manejarlos sin entregar al motor. Configuraciones del motor que amplíen TTL por encima de 16 o contenido por encima de 1024 no amplían este contrato V1.

### Contrato de fragmentación

Una trama ocupa **13..20 bytes**, compatible por tamaño con una carga ATT de 20 bytes sin negociar MTU. El formato define una posición canónica para cada byte; solo la última trama puede tener menos de 8 bytes útiles.

| Offset | Campo | Bytes | Regla |
| ---: | --- | ---: | --- |
| 0 | Marcador | 1 | `F9` |
| 1 | Versión de fragmentación | 1 | `01`, independiente de la versión del envelope |
| 2 | ID de transferencia | 2 | 0..65535; ámbito de una sesión local de enlace |
| 4 | Índice | 1 | Desde 0 hasta `cantidad - 1`, interpretado sin signo |
| 5 | Cantidad | 1 | `ceil(longitudTotal / 8)`, máximo **135** |
| 6 | Longitud total serializada | 2 | 1..1077, no la longitud del texto |
| 8 | CRC32 del payload serializado completo | 4 | El mismo en todas las tramas, recalculado para cada envelope enviado |
| 12 | Segmento del payload | 1..8 | 8 bytes salvo el último; longitud exacta obligatoria |

Cabecera de **12 bytes**, hasta **8 bytes útiles** por trama. Unicast con `HOLA` ocupa 57 bytes y **8 tramas**; los máximos unicast/broadcast requieren **135 / 133 tramas**. No se requiere una trama inicial especial: se admite la última primero y orden arbitrario. El fragmentador acepta bytes acotados; la validación del envelope sigue siendo responsabilidad del codec antes de enviar y después de reconstruir.

El ID de transferencia no reemplaza a `messageId`. El emisor futuro asignará IDs sin reutilizarlos dentro de la ventana de retención del receptor, incluso ante un reintento lógico. Un reenvío por otro salto puede usar otra transferencia y otro CRC, conservando los UUID, destino y texto originales. **Solo MeshEngine descuenta TTL al encolar cada salto lógico; codec, fragmentador y reensamblador no lo alteran.**

**CRC32 detecta corrupción accidental del conjunto, con posibilidad de colisiones; no equivale a autenticación ni cifrado.** No autentica nodos, no protege contra modificaciones deliberadas ni constituye una defensa criptográfica de replay. No se verifica integridad individual inmediata de una trama: el CRC se verifica al completar el conjunto. El formato sigue enviando contenido legible. La protección criptográfica y la asociación autenticada de UUID a vecinos siguen pendientes.

### Reensamblado, recursos y ciclo de vida

La clave de reensamblado es `(MeshLinkSession, transferId)`. `MeshLinkSession` es un token UUID opaco de una encarnación de conexión local, generado por el host y renovado al reconectar; **no es un NodeId, MAC ni callsign**. El host futuro deberá proporcionar `previousHop` como identidad lógica separada; esta iteración no implementa su descubrimiento/provisión.

| Límite predeterminado por instancia receptora | Valor / política |
| --- | --- |
| Transferencias activas globales | 8 |
| Transferencias activas por sesión | 2 |
| Entradas totales activas + registros de cierre (tombstones) | 32; no se expulsan entradas vigentes para admitir nuevas |
| Payload reservado global | **8616 bytes**, sumando longitudes completas antes de asignar buffers |
| Tamaño por transferencia | 1077 bytes máximo; configurable hacia abajo |
| Fragmentos por transferencia | 135 máximo; configurable hacia abajo |
| Expiración | **15.000 ms** desde el primer fragmento aceptado, reloj monotónico local |

Cada transferencia activa reserva un `ByteArray` del tamaño total y un `BooleanArray` de hasta 135 posiciones. Además del payload, hay hasta 1080 posiciones de control de recepción y objetos/mapa acotados por las cuotas; 8616 no es una medida del heap JVM total. Entradas de más de 20 bytes se rechazan antes de asignar buffers de transferencia. El payload devuelto en `Complete` pasa al llamador y ya no cuenta como memoria retenida por el reensamblador. El host deberá acotar sus propias colas de entrada/salida.

Resultados: `Pending`, `Duplicate` idéntico, `Complete(payload)` o `Rejected(reason)`. No se entrega carga parcial. Marcos malformados/versiones desconocidas se rechazan sin admisión ni destrucción de una transferencia válida. Metadatos incompatibles para una misma clave o un duplicado con otros bytes abortan la transferencia. CRC incorrecto al completar también aborta. El cierre libera el buffer y conserva solo un tombstone hasta la fecha límite original; los fragmentos posteriores reciben `CLOSED_TRANSFER`, sin reconstrucción ni nueva entrega. Un ID igual en otra sesión es independiente.

Las cuotas llenas rechazan la transferencia nueva, sin expulsar una activa. Duplicados, llegada de más fragmentos, finalización y errores **no prolongan** el vencimiento. `cleanup()` descarta incompletos y tombstones expirados sin entrega; se invoca al operar y el host deberá llamarlo mientras esté inactivo. `discardSession()` libera todos los recursos de una conexión cerrada; su token no podrá reutilizarse. Después de expirar la retención, una repetición completa puede reconstruirse otra vez: la deduplicación lógica temporal sigue perteneciendo a MeshEngine.

Clases síncronas, no thread safe: el host serializa llamadas y provee reloj monotónico, igual que para el motor. No hay workers, temporizadores, persistencia, ACK, retransmisión ni integración GATT activa. Solo un payload completo, con CRC correcto **y posteriormente decodificado/validado por el codec**, puede pasar a `MeshEngine.receive`.

### Ejecuciones y cobertura

Antes de cambiar código se revisaron las fuentes reales y se ejecutó la base con la instalación compatible Gradle 8.13 y Temurin **21.0.7+6**: **BUILD SUCCESSFUL in 42s**, 44 tareas ejecutadas, **48 tests aprobados** y APK debug. El sandbox impidió cargar la biblioteca nativa de Gradle; la ejecución necesitó acceso autorizado a la caché externa. El wrapper de este HEAD aún apuntaba a 9.7.1, por lo que se corrigió a 8.13 para las validaciones siguientes.

Comando de validación de la implementación, usando el wrapper del proyecto:

```bash
JAVA_HOME=/home/lordcommander/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 \
  ./gradlew testDebugUnitTest assembleDebug --console=plain
```

El intento dentro del sandbox se bloqueó al escribir `gradle-8.13-bin.zip.lck` en la caché de usuario; la validación se ejecutó con acceso autorizado. Primer intento con código nuevo: APK compilada y **109/110 tests aprobados**. La expectativa del test diamante confundía un despacho lógico con su fanout a dos vecinos; se corrigió para comprobar un solo handoff y una sola transferencia D→E. El motor no cambió. Reejecución final: **BUILD SUCCESSFUL in 2s**, 44 tareas (2 ejecutadas, 42 actualizadas); `testDebugUnitTest` volvió a ejecutar la suite y `assembleDebug` quedó actualizado respecto de la APK ya compilada con el código nuevo.

| Suite nueva | Tests | Cobertura |
| --- | ---: | --- |
| `MeshMessageCodecTest` | 14 | Vectores binarios fijos; broadcast/unicast; UUID, destino y TTL; 1024 bytes ASCII/multibyte exactos; exceso; Unicode sin normalizar; UTF malformado; flags/versiones/longitudes; todos los prefijos truncados y bytes sobrantes |
| `MeshFragmentProtocolTest` | 9 | Vectores fijos con CRC32 conocido; todas las longitudes 1..1077; 20 bytes máximos; índices sin signo y límites; tamaños/cantidades/versiones inválidas; separación inequívoca del legacy y UUID futuro |
| `MeshReassemblerTest` | 29 | Máximo UTF-8 partido; orden inverso y 20 permutaciones reproducibles; duplicados idénticos/conflictivos; mezcla de contenido/metadatos; sesiones/transferencias simultáneas; cuotas de memoria y entradas; tombstones; expiración exacta; reconexión; reloj; corrupción; 10.000 entradas aleatorias/mutadas reproducibles |
| `MeshTransportIntegrationTest` | 10 | Codec/tramas/reensamblado/motor en A↔B, cadena, anillo broadcast, diamante y cadena rota; TTL por salto; pérdida/corrupción sin handoff al motor; orígenes distintos con mismo messageId; sesión renovada; envelope inválido incluso con CRC válido |
| Suites anteriores | 48 | 13 protocolo directo, 3 estados directos, 22 motor, 9 topologías anteriores y 1 ejemplo |
| **Total final** | **110** | **0 fallos, 0 errores, 0 omitidos** |

Evidencia local no versionada: [reporte JVM](../app/build/reports/tests/testDebugUnitTest/index.html), XML en `app/build/test-results/testDebugUnitTest/TEST-*.xml` y APK `app/build/outputs/apk/debug/app-debug.apk` (**11.957.910 bytes**, SHA-256 `d9015ed82d4bc578de30f4a29c8e5f8a51a4c8e949bbf869bf807c9bb5a44e0e`). Inicio del XML final de integración: **18/10/2026 21:48:25.598 UTC−03:00**, America/Argentina/Rio_Gallegos (`2026-10-19T00:48:25.598Z`). Se conservan advertencias previas de BuildConfig, source/target Java 8 y APIs obsoletas; no se ejecutó Lint. `git diff --check`: correcto. La comparación byte a byte con HEAD confirma intactos `BLEMeshController`, `MeshScreen`, `DirectBleProtocol`, `MeshEngine` y `MeshMessage`; los enlaces locales de ambos documentos actualizados existen.

### Pendientes y próximos pasos

1. Diseñar el adaptador BLE asíncrono con colas acotadas, backpressure, asignación/retención de IDs por conexión, limpieza al desconectar, descubrimiento del canal dedicado y provisión de identidades lógicas. Registrar/requerir la nueva característica solo en esa iteración, preservando peers legacy.
2. Definir política de pérdidas/reintentos/ACK si se necesita fiabilidad: hoy una transferencia incompleta vence y no hay recuperación automática. `Handoff.ACCEPTED` continúa siendo aceptación local, sin promesa de recepción remota.
3. Ejecutar el procedimiento físico A↔B inferior para el chat actual. Después de integrar el adaptador, probar tramas y fallos reales, compatibilidad legacy y cadena A→B→C. Compilación y JVM no sustituyen esas pruebas.
4. Resolver autenticación, cifrado y provisión/revocación de claves antes de atribuir garantías de seguridad. No se implementaron servicios de emergencia, ubicación ni backend.

## Validación técnica histórica — Issue #1 · 17/10/2026

**Resultado: tests JVM y APK debug aprobados con Gradle 8.13 / JDK 21; comunicación física A↔B pendiente. No corresponde declarar cerrado el criterio de aceptación de radio.** `adb devices -l` devolvió una lista vacía. No se instalaron APK en teléfonos ni se ejecutaron pruebas físicas o instrumentadas.

Repositorio `NicoButter/beaconchat`, rama `main`, HEAD `1addfd65f9b3f70382c6e1b955acc3f7837c2703`. Se consultó el [Issue #1](https://github.com/NicoButter/beaconchat/issues/1), abierto al momento de la revisión. Por instrucción de esta tarea se trabajó en el checkout existente: sin ramas, worktrees, commit, push, publicación ni modificación del issue.

### Entorno, ejecución y evidencia nueva

Al comenzar ya estaba modificado `gradle/wrapper/gradle-wrapper.properties`: distribución local **9.7.1**, mientras que HEAD fija **8.13**. Ese cambio previo se conserva íntegro; no se lo atribuye a esta intervención. SHA-256 del archivo local preservado: `536939b15bb97c97b0acb562a9ed916a6cc8c9fe34b67d387cbe2a66d1d9e295`.

| Ejecución del 17/10/2026 | Resultado observado |
| --- | --- |
| `./gradlew testDebugUnitTest assembleDebug --console=plain` dentro del sandbox | Bloqueado al abrir `gradle-9.7.1-bin.zip.lck`: caché de usuario de solo lectura. No llegó a ejecutar tareas. |
| Mismo comando con acceso autorizado a la caché; Java predeterminado 25.0.4.1 y wrapper local 9.7.1 | `BUILD FAILED` al aplicar AGP 8.13.1: `InternalProblems`, API interna eliminada en Gradle 9.6.0. No es un fallo de un test BLE. |
| Gradle 8.13 instalado con `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` | No inició: ese directorio del sistema no contiene un JDK válido. |
| Gradle 8.13 instalado y Temurin **21.0.7+6**, comando exacto debajo | **BUILD SUCCESSFUL in 36s**, 44 tareas ejecutadas; `testDebugUnitTest` y `assembleDebug` ejecutados, sin reutilizar resultados de tareas. |
| Lectura de los XML recién generados | **48 tests; 0 fallos, 0 errores, 0 omitidos**. Timestamp inicial `2026-10-17T19:13:03.726Z` (16:13:03, UTC−03:00, America/Argentina/Rio_Gallegos). |
| `apksigner verify --verbose app/build/outputs/apk/debug/app-debug.apk` (Build Tools 35.0.0) | Firma válida, esquema v2, un firmante debug. |
| `adb devices -l` | Sin dispositivos conectados; A↔B físico **no ejecutado**. |

Comando que efectivamente ejecutó ambas tareas en este entorno, sin editar el wrapper:

```bash
JAVA_HOME=/home/lordcommander/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 \
  /home/lordcommander/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle \
  testDebugUnitTest assembleDebug --console=plain
```

Para otro equipo: usar un JDK 21 válido, Android SDK 34 y Build Tools 35.0.0 con las dependencias disponibles, y ejecutar `gradle` de una instalación **8.13** contra este checkout. La [matriz oficial de AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes) indica Gradle 8.13 y JDK mínimo 17. El wrapper versionado en HEAD ya usa 8.13; **el wrapper local preservado sigue fallando con 9.7.1**. El éxito de la invocación alternativa no equivale a un éxito de `./gradlew` en su estado local actual.

| Suite JVM | Tests aprobados | Alcance |
| --- | --- | --- |
| `DirectBleProtocolTest` | 13 | Callsign, publicidad como bytes, paquete de 20 bytes, Unicode/emoji, UTF-8 inválido, espacios, mensajes vacíos y exceso sin truncamiento. |
| `DirectSendStateTest` | 3 | Orden y callbacks duplicados; finalización terminal en cada fase; nuevo intento después de finalizar otro. No ejecuta los timers ni callbacks Android del controlador. |
| `ExampleUnitTest` | 1 | Test aritmético de ejemplo; sin cobertura BLE. |
| `MeshEngineTest` / `MeshTopologyTest` | 22 / 9 | Regresión de lógica simulada preexistente. No valida GATT ni activa multisalto en la app. |

Evidencia local generada, no versionada:

- Reporte: `app/build/reports/tests/testDebugUnitTest/index.html`; resultados: `app/build/test-results/testDebugUnitTest/TEST-*.xml`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, **11.941.565 bytes**; SHA-256 `7820ea0a6a7333865f181c066c87439e2455ceb8be1ddb11e80f76bccceaacf4`.
- Logs de intentos: `/tmp/beaconchat-validation-gradle-initial.log`, `/tmp/beaconchat-validation-gradle-wrapper.log`, `/tmp/beaconchat-validation-gradle-8.13.log` y `/tmp/beaconchat-validation-gradle-8.13-jdk21.log` (último: ejecución exitosa).

Persisten advertencias de configuración BuildConfig, Java source/target 8, opt-in/variables en módulos existentes y stripping de `libimage_processing_util_jni.so`. **No se ejecutó Lint en esta intervención**; los errores de Lint de los anexos son antecedentes, no resultados nuevos.

### Revisión del flujo BLE y cobertura real

Se revisaron [BLEMeshController](../app/src/main/java/com/nicobutter/beaconchat/mesh/BLEMeshController.kt), [MeshScreen](../app/src/main/java/com/nicobutter/beaconchat/ui/screens/MeshScreen.kt), [DirectBleProtocol](../app/src/main/java/com/nicobutter/beaconchat/mesh/DirectBleProtocol.kt), [DirectSendState](../app/src/main/java/com/nicobutter/beaconchat/mesh/DirectSendState.kt), manifest y tests existentes. La tabla describe lo comprobado por lectura y JVM; **ninguna fila acredita ejecución del stack Bluetooth en Android**.

| Área | Comprobación en código / JVM | Verificación física pendiente |
| --- | --- | --- |
| Permisos | API 31+: SCAN, CONNECT y ADVERTISE; API 24–30: ubicación precisa, permisos Bluetooth legacy y chequeo de proveedor de ubicación para scan. La UI revalida permisos al reanudar. | Concesión, denegación, revocación desde Ajustes, ubicación apagada y recuperación por versión Android. |
| Publicidad | Servidor GATT y `onServiceAdded` antes de anunciar; conectable; UUID en primario, callsign en service data de scan response; sin nombre ni potencia. Presupuesto conservador con UUID de 128 bits: primario 21 y respuesta hasta 26 bytes, ambos ≤31. | Paquetes efectivamente emitidos, soporte del advertiser y recepción del scan response en ambos equipos. |
| Estados de publicidad | `isStartingAdvertising` durante inicio; `isAdvertising` solo tras `onStartSuccess`; generación de sesión descarta callbacks antiguos; timeout de inicio de 10 s y cierre por fallo. | Éxito/error reales y ausencia de “Visible” falso al detener/reanudar rápidamente. |
| Escaneo | Filtro por UUID; `scanRequested` al solicitar, `isScanning` al recibir resultado; parseo estricto del callsign y manejo de `onScanFailed`. La UI distingue solicitud de señales detectadas. | Descubrimiento A→B y B→A, RSSI, errores del stack y reintentos. |
| GATT saliente | Un envío por vez; `connectGatt(... TRANSPORT_LE)` → conexión → descubrimiento de servicio/characteristic → WRITE con respuesta → callback. IDs de servicio y característica coinciden entre cliente y servidor. | Conexión y escritura reales, ambos roles simultáneos y fallos de servicio/enlace. |
| Finalización | Timeout global de 20 s, error, cancelación, pausa o Bluetooth apagado pasan por cierre de GATT y liberación de `isSending`; callbacks tardíos de otra operación no completan un envío nuevo. | El timeout del Handler y la limpieza real de handles no están cubiertos por `DirectSendStateTest`. |
| Recepción | Servidor decodifica antes de agregar texto; rechaza UUID incorrecto, offset, prepared write, escritura sin respuesta y carga inválida. Envía respuesta GATT cuando corresponde. | Recepción íntegra en el chat correcto y respuesta efectiva del servidor. |
| Integridad y UI | `CALLSIGN:MENSAJE`, UTF-8 estricto, máximo 20 bytes; callsign ≤8 bytes; UI y controlador validan exceso. Historial saliente solo tras `GATT_SUCCESS`, con leyenda “sin ACK”. | Contador, botón bloqueado, borrador conservado ante fallo y aparición única de cada texto en ambos teléfonos. |
| Ciclo de vida | Observer de resume/pause y `onDispose` detienen scan, publicidad, servidor y envío. Broadcast Bluetooth actualiza estado y detiene al apagarse. | Navegación, Home, bloqueo, permisos y apagado/encendido sin recursos BLE residuales. |

La interpretación de callbacks se contrastó con [AdvertiseCallback](https://developer.android.com/reference/android/bluetooth/le/AdvertiseCallback) y [BluetoothGattCallback](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback). La [documentación de permisos Android](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) advierte que `neverForLocation` puede filtrar algunos beacons: su efecto sobre este anuncio debe comprobarse en equipos reales.

### Procedimiento reproducible con dos Android — no ejecutado

Preparación y registro:

1. Usar dos teléfonos físicos A y B con Android **7/API 24 o superior** (minSdk real de esta APK), ambos capaces de scan y publicidad BLE conectable. Preferir un par API 24–30 / API 31+ para cubrir ambos grupos de permisos. Registrar modelo, fabricante, versión/API, batería, fecha/zona horaria, responsable, HEAD y SHA-256 de la APK. Colocarlos a 1–2 m, sin obstáculos, y mantener ambos en primer plano durante los envíos normales.
2. Instalar la **misma APK** en ambos, usando USB y sus seriales explícitos; no usar scripts que elijan el primer dispositivo ni desinstalar automáticamente para resolver un conflicto de firma. Estos comandos son instrucciones futuras, no acciones realizadas:

   ```bash
   adb devices -l
   sha256sum app/build/outputs/apk/debug/app-debug.apk
   adb -s SERIE_A install -r app/build/outputs/apk/debug/app-debug.apk
   adb -s SERIE_B install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. En Ajustes de la app configurar callsigns ASCII distintos: A=`ALFA`, B=`BRAVO`. En cada teléfono activar **modo avión**, reactivar **Bluetooth manualmente** y comprobar que **Wi-Fi y datos móviles sigan apagados**, incluidas ambas SIM si aplica. Desactivar tethering si estuviera activo. En Android 7–11 activar Ubicación para escanear; no necesita Wi-Fi ni datos. Guardar capturas de los ajustes antes y después del ensayo; modo avión por sí solo no acredita Wi-Fi apagado.
4. Capturar logs en dos terminales durante el ensayo, uno por equipo. Por ejemplo, `adb -s SERIE_A logcat -v threadtime > /tmp/beaconchat-A-logcat.txt` y el equivalente B; detener con Ctrl+C. No usar un filtro de tags propios: el controlador actual no emite logs de sus callbacks. Grabar pantalla o tomar capturas de ambos chats. Para verificar transiciones GATT/paquetes o cese de radio que la UI no permite observar, usar registro Bluetooth HCI disponible en el dispositivo o inspección BLE externa y anotar el método; sin esa evidencia marcar esa parte como no comprobada.
5. Abrir **Mesh Radar** y ejecutar los casos en orden. Esperar hasta **30 s** para descubrimiento (ventana de observación del ensayo, no garantía de la app). Registrar todos los intentos, también los fallidos, sin cambiar umbrales después de observarlos.

| Caso | Acción y criterio esperado | Resultado de esta intervención |
| --- | --- | --- |
| BLE-01 · Permisos | Denegar el primer pedido: sin crash ni visibilidad/scan exitosos. Conceder desde Ajustes y volver: revalidación e inicio. Repetir revocando permisos con la app pausada. En API 24–30 apagar Ubicación: error explícito al intentar scan; activarla y pulsar Iniciar Escaneo. | No ejecutado |
| BLE-02 · Descubrimiento | Con permisos y Bluetooth activos en ambos, observar “Iniciando publicidad/GATT…” y después “Visible (Anunciando)” o error en ≤10 s más latencia de UI. Scan muestra solicitud hasta recibir señales. A debe listar BRAVO y B debe listar ALFA en la ventana de 30 s. La lista no prueba una conexión GATT. | No ejecutado |
| BLE-03 · A→B | Abrir BRAVO en A y ALFA en B. Enviar `HOLA B` desde A; observar conexión/descubrimiento/escritura si su duración permite capturarlos. Debe aparecer una sola burbuja saliente en A tras el callback exitoso y el texto exacto en B. No aceptar “entregado” como sustituto de observar B. | No ejecutado |
| BLE-04 · B→A | Enviar `HOLA A` desde B, con los mismos criterios en sentido inverso. Verificar que llega al chat de BRAVO en A. | No ejecutado |
| BLE-05 · Repetición | Enviar `A01`…`A10` desde A y `B01`…`B10` desde B, alternando y esperando cada resultado: **10 intentos por dirección**. Añadir un par simultáneo `ASIM` / `BSIM`. Registrar por separado intentados, escritura confirmada y recepción observada. En condición estable, cada texto debe aparecer íntegro y una vez en el receptor; anotar cualquier diferencia. | No ejecutado |
| BLE-06 · UTF-8 y borde | Enviar en ambas direcciones `sí:🆘` y ` a:b `, conservando sus espacios. Con ALFA el máximo de texto es 15 bytes: `123456789012345` debe pasar; agregar un dígito debe bloquear envío. Con BRAVO son 14: `12345678901234` debe pasar; agregar un dígito debe bloquear. Tres `🆘` ocupan 12 bytes y caben; cuatro ocupan 16 y deben rechazarse en ambos. Sin truncamiento ni burbuja saliente para el exceso. | No ejecutado |
| BLE-07 · Visibilidad y scan | Apagar/encender visibilidad en cada teléfono; verificar detención y nueva publicidad. Detener scan: estado detenido y lista vacía; reiniciar y redescubrir antes del siguiente envío. Repetir cambio de visibilidad durante inicio; no debe reaparecer “Visible” por una sesión detenida. Confirmar emisión/cese con HCI/observador BLE cuando se evalúe la radio. | No ejecutado |
| BLE-08 · Bluetooth off/on | Apagar Bluetooth en B justo después de iniciar un envío de A; repetir apagando A. Anotar si el mensaje ya había terminado antes del corte; en ese caso el fallo no fue inducido. En un envío interrumpido, `isSending` debe liberarse por error o timeout de 20 s más latencia de UI, borrador conservado y sin nueva burbuja exitosa. Encender, redescubrir y repetir A↔B. | No ejecutado |
| BLE-09 · Desconexión/timeout | Descubrir B, abrir su chat en A y luego cerrar Mesh en B o alejarlo antes de enviar. Debe fallar o vencer la espera, liberar el envío y permitir otro intento tras restaurar/redescubrir B. Si se observa un error inmediato, no declararlo prueba del timeout de 20 s. | No ejecutado |
| BLE-10 · Ciclo de vida | Salir de Mesh durante inicio; volver. Repetir durante envío, volver al radar, navegar a otra pantalla, pulsar Home y bloquear/desbloquear. Comprobar cancelación, borrador del chat ante fallo y reinicio conforme a controles al reanudar. Borrador no se promete tras abandonar la composición del chat. Con observación de radio verificar cese de scan/advertising y cierre GATT; la pantalla sola no lo demuestra. | No ejecutado |
| BLE-11 · Errores GATT | Si hay un cliente/periférico de prueba, inducir servicio/característica ausente, respuesta de escritura fallida, offset/prepared write y UTF-8 inválido. Esperar error o rechazo, sin truncamiento ni éxito falso, y siguiente envío posible. Registrar herramienta, UUID/carga y status. Sin herramienta o fallo inducido, dejar el subcaso no ejecutado. | No ejecutado |
| BLE-12 · Sin advertiser | Solo si hay hardware que no soporte advertising: debe mostrar error, nunca “Visible”. No inferir cobertura de este caso a partir de un teléfono compatible. | No ejecutado |

Para inspección GATT: UUID de descubrimiento/publicidad `0000BEEF-0000-1000-8000-00805f9b34fb`; servicio de chat `0000C4A7-0000-1000-8000-00805f9b34fb`; característica WRITE `00004D51-0000-1000-8000-00805f9b34fb`. Son UUID distintos por diseño; filtrar por BEEF no sustituye descubrir el servicio C4A7.

Ficha a completar por intento: `caso/subcaso | fecha y hora | A/B modelo/API | APK SHA-256 | permisos/radios | texto exacto y bytes | acción/fallo inducido | estados A/B | escritura confirmada | recepción observada | duración local/método | evidencia A/B | aprobado/fallido/no ejecutado`. Para los envíos normales, predefinir como criterio 10/10 recepciones íntegras por dirección y el par simultáneo sin duplicados. Ese es un criterio del ensayo, **no una tasa medida**. No calcular latencia unidireccional restando relojes de teléfonos sin sincronización documentada.

### Límites y dictamen para revisión

- Ningún test actual ejercita `BLEMeshController` o `MeshScreen` sobre Android; el test instrumentado existente solo comprueba el nombre del paquete. Faltan observaciones reales de permisos, callbacks, radios, compatibilidad entre fabricantes, consumo y alcance. No se declara resuelto el Issue #1 con evidencia exclusivamente JVM.
- GATT exitoso no equivale a ACK de aplicación, lectura ni persistencia. El receptor agrega el mensaje antes de responder GATT; si se pierde la respuesta puede haber recepción sin confirmación en el emisor. Reenviar puede duplicar contenido: no hay deduplicación en el chat directo.
- La asociación entre dirección central/anunciante usa callsign observado único como alternativa, sin autenticación. Con scan detenido, peers aún no descubiertos, callsigns duplicados o direcciones rotativas, debe comprobarse que la recepción aparezca en el chat correcto. Por lectura, la lista elimina peers de más de 30 s solo al procesar un nuevo anuncio válido; sin nuevos resultados puede conservar entradas antiguas. No asumir que un peer listado siga conectado/disponible.
- Callsign BLE se limita a **bytes**, mientras Ajustes valida caracteres alfanuméricos; un callsign Unicode aceptado por Ajustes puede exceder 8 bytes y ser rechazado por BLE. Usar ASCII en el ensayo base y registrar ese límite. La fórmula del texto es `20 - bytes(callsign) - 1`; sin fragmentación ni negociación de MTU.
- Historial y borrador son de memoria/composición; no hay garantía de servicio en segundo plano. El núcleo mesh simulado preexistente continúa sin integración BLE; no se añadió ni activó multisalto.
- **Cambios de esta intervención: solo este documento.** No se aplicaron correcciones de código BLE sin reproducción de un fallo; se conserva el cambio local previo del wrapper. El bloqueo de `./gradlew` con 9.7.1 queda identificado y la compilación válida se obtuvo mediante la instalación compatible, sin alterar ese archivo.
- `git diff --check`: correcto. El cierre físico queda pendiente de completar las fichas con dos Android; conservar la evidencia de fallos y revisar antes de publicar. Este informe no publica cambios ni cierra el issue.

## Evidencia disponible y límites

| Evidencia | Resultado disponible | Qué no demuestra |
| --- | --- | --- |
| [Informe BLE directo](Validacion-BeaconChat.md) | **17 tests JVM reportados**: 13 protocolo, 3 estados y 1 original. Prueba A↔B pendiente; sin dispositivos en aquel informe | Stack Bluetooth, alcance, compatibilidad entre fabricantes ni mesh físico |
| [Informe del núcleo](Validacion-BeaconChat.md) | **48 tests totales reportados**, incluidos 31 del núcleo (22 motor + 9 topologías) y los 17 anteriores | GNSS, radio real, fragmentación, DTN, gateway, backend ni rescate |
| Histórico: transporte binario V1 | **110 tests totales aprobados el 18/10/2026**: 48 anteriores + 62 nuevos; fragmentación/reensamblado e integración lógica con MeshEngine | Adaptador GATT activo, radio física, fiabilidad/ACK, autenticación ni cifrado |
| Histórico: integración Android mesh | **144 tests totales aprobados el 18/10/2026**: 110 anteriores + 34 nuevos; adaptador Android compilado y política probada con GATT falso | Ejecución real de BluetoothGatt/Handler, permisos, radio A↔B/A→B→C, entrega final, autenticación ni cifrado |
| Implementación #5, sección inicial | **183 aprobados**: base de 144 reejecutada + 39 SOS/GNSS/privacidad/integración; APK debug compilada | Obtención satelital, permisos/lifecycle Android físico, radio real, seguridad criptográfica ni rescate |
| [Código de tests del motor](../app/src/test/java/com/nicobutter/beaconchat/mesh/core/MeshEngineTest.kt) y [topologías](../app/src/test/java/com/nicobutter/beaconchat/mesh/core/MeshTopologyTest.kt) | Cadena, ruptura de enlace, triángulo, anillo, caminos duplicados, límites, TTL y expiración mediante red simulada | Probabilidades de pérdida, obstáculos, consumo y restricciones de un teléfono |
| Señalización óptica/vibración/audio/QR/beacon BLE | Componentes presentes en código | Tasas de decodificación o funcionamiento garantizado bajo derrumbes |
| Revisión documental previa | Referencia histórica a `INFORME-REVISION.md` (archivo no presente en este checkout) | No acredita una ejecución de los ensayos anteriores |

**La revisión documental previa no volvió a ejecutar Gradle, APK, tests JVM ni pruebas físicas.** La ejecución técnica nueva del 17/10/2026 se registra arriba. Los 48 incluyen los 31, no son 48 + 31. El informe BLE histórico también registra errores de Lint preexistentes en módulos ópticos; no se presenta una validación nueva de Lint ni se modifica código para resolverlos.

## Catálogo de pruebas

Los IDs completos son `R26-PR-01` a `R26-PR-19`. PR-19 corresponde a la revisión documental previa; PR-01/02 incorporan la ejecución JVM del 17/10/2026 y mantienen sus límites físicos. Cada resultado futuro debe señalar simulación, laboratorio físico o campo.

| ID | Preparación y procedimiento | Criterio de aceptación / evidencia | Estado |
| --- | --- | --- | --- |
| R26-PR-01 | Ejecutar tests de protocolo/estado y protocolo manual A↔B del informe BLE: permisos, Unicode, 20 bytes exactos/exceso, desconexión, pausa y timeout. | Tests del protocolo correctos; ambos teléfonos registran texto íntegro, sin truncar ni éxito falso; guardar modelos/SO/logs. | 13 protocolo + 3 estados JVM aprobados el 17/10/2026; físico pendiente |
| R26-PR-02 | Revisar y reproducir suite del núcleo: cadena intacta/rota, TTL cero, triángulo/anillo/diamante, dedupe/colas/reloj y fallos de transporte. | Resultado lógico esperado y propagación termina; transporte no disponible consume intento sin reencolar ni ACK. | 31 JVM aprobados el 17/10/2026; simulación sin radio |
| R26-PR-03 | En dispositivos compatibles, probar cada emisor/receptor óptico, vibración, audio, QR y beacon; registrar luz/ruido/acoplamiento, permisos y detención. | Emisión/decodificación observada por canal y condición; errores y limitaciones explícitos, sin atribuir alcance genérico ni geolocalización al beacon. | Pendiente físico |
| R26-PR-04 | Crear y serializar SOS sintéticos/sin posición válidos e inválidos; transportar/reintentar en el adaptador falso. | UUID/cuerpo del incidente estable, intentos mesh diferenciados, UTF-8/nota/posición validados; no afirmar dedupe central. | 16 modelo/codec + 9 integración JVM aprobados; UI física pendiente |
| R26-PR-05 | Desactivar datos/Wi-Fi; ensayar GNSS al aire libre/interior, permisos, proveedor apagado, timeout y cancelación. | No exige Internet; edad/fuente/precisión visibles; no inventa posición ni exige fix en toda condición. | 14 tests de política aprobados y componente Android compilado; GPS físico pendiente |
| R26-PR-06 | Forzar sin fix, permiso denegado/aproximado/revocado, cache antiguo y ausencia. Referencia manual futura separada. | SOS sin posición válido; rangos/edad/calidad estrictos, cache nunca actual, sin coordenadas de reemplazo. | Modelo/política JVM aprobados; Android físico y entrada manual pendientes |
| R26-PR-07 | Probar serializer/fragmentos V1 y adaptador: pérdida, duplicación, desorden, corrupción, exceso, timeout, mezcla de versiones, callbacks y separación legacy. | Roundtrip íntegro solo con carga completa/válida; memoria acotada, liberación al vencer y chat legado intacto; TTL por salto y ningún éxito final inventado. | 62 tests de transporte + 34 del adaptador aprobados el 18/10/2026; GATT experimental conectado a la UI, validación física pendiente |
| R26-PR-08 | A-B-C-D físicos, D único gateway, A sin enlace directo con C/D. Demostrar aislamiento retirando B/C y verificar que no haya salida IP en A/B/C. | Recorrido de IDs/saltos registrado; 10/10 SOS de prueba en cadena estable llegan en ≤120 s cada uno, objetivo provisional. Cadena rota no informa entrega falsa. | Pendiente; hipótesis de laboratorio |
| R26-PR-09 | Quitar vecinos/salida, agotar TTL, llenar dedupe/cola y luego restaurar enlaces. Separar motor actual de custodia futura. | Motor actual no promete recuperación; se observan descartes y límites. Futuro sistema muestra sin salida y aplica política explícita, sin bucle ni TTL restaurado indefinidamente. | Lógica parcial histórica; sistema pendiente |
| R26-PR-10 | Cortar Internet/DNS/API del gateway antes, durante y después del guardado; red Wi-Fi sin Internet; luego reconectar y probar otro gateway. | Sin confirmación central falsa, autenticación verificada y error/estado incierto claro. Reintento durable solo en E4; ID preservado. | Pendiente |
| R26-PR-11 | Enviar mismo SOS por dos gateways, perder respuesta tras guardado, repetir y provocar fallo de base. Ensayar retorno de comprobante al origen. | Un incidente por clave; confirmar solo después de persistir. Respuesta perdida no duplica. Sin retorno, A sigue sin confirmar; mismo ID con cuerpo distinto se rechaza/audita. | Pendiente |
| R26-PR-12 | Operador de prueba entra con distintos roles y observa incidentes actuales/viejos/manuales/sin ubicación; filtra y clasifica. | Lista completa, mapa sin coordenadas ficticias, incertidumbre visible, acceso mínimo y cambios auditables. | Pendiente |
| R26-PR-13 | En fase durable, reiniciar/matar proceso durante custodia, transferencia y recepción; simular disco lleno, vencimiento y reloj civil desajustado. | Custodia aceptada/vigente recuperable, reintento acotado, sin duplicado central; expirados no salen y falta de escritura nunca se declara guardada. | Pendiente E4 |
| R26-PR-14 | Revisar amenazas e intentar emisor/gateway falsos, replay, alteración, acceso sin rol, revocación, extracción de logs/backups y vencimiento/borrado. | Contenido protegido y controles observables; exposición pública cero en muestras sintéticas; retención y autenticación verificadas según política aprobada. No basta HTTPS para declarar protección de los saltos BLE. | Pendiente |
| R26-PR-15 | Tareas con usuarios de prueba, lector de pantalla/texto ampliado y fallos inducidos; comparar evidencia contra etiquetas de estado. | Meta ≤3 acciones y feedback ≤1 s en configuración registrada; cero confirmaciones centrales falsas; medir errores/comprensión sin inventar satisfacción. | Pendiente |
| R26-PR-16 | Ensayar carga/saturación y energía contra baseline en el mismo equipo: scan, GNSS, señales, cámara y reintentos; bloquear/pasar a background por SO. | Cumplir cuotas/timers definidos; informar pérdidas y continuidad real. Reportar consumo y diferencia observada, sin meta de ahorro ficticia. | Pendiente |
| R26-PR-17 | Simulación de mesa futura de recursos, brigadistas y rutas con fuentes contradictorias/viejas. | Trazabilidad de decisiones humanas y fuente/edad visibles; prioridad asistida no despacha automáticamente ni certifica rutas. | Pendiente E4–E5 |
| R26-PR-18 | Si se decide LoRa, definir equipos externos/plan de enlace y repetir intercambio/duplicados/fallo de radio. | Funciona solo con equipo externo documentado; sin radio se informa limitación y no se exige LoRa al flujo BLE básico. | Pendiente E5 opcional |
| R26-PR-19 | Revisar paquete 00–10, enlaces, IDs/trazabilidad, fences/diagramas Mermaid, autoría y límites de cambios en Git. | Sin enlace local roto ni requisito huérfano; Mermaid válido; solo altas documentales en PSI y antecedentes íntegros. | Ejecutada; detalle en informe |

El objetivo de PR-08 es un umbral de laboratorio propuesto para hacer falsable la PoC, no una tasa obtenida ni una promesa de campo. Si falla, registrar resultados y revisar parámetros/alcance; no borrar intentos fallidos ni cambiar el criterio después del ensayo sin dejar constancia.

## Protocolo reproducible de ensayo futuro

1. Registrar responsable, fecha/zona horaria, commit/APK y configuración, modelo/SO, permisos, batería, cuotas, TTL y topología. Documentar cómo se aisló el enlace A-C/D; no basta colocar etiquetas a los teléfonos.
2. Usar solicitudes sintéticas con IDs conocidos. Separar el modo demostración del SOS visible/audible público. No ensayar enviando alertas a un centro oficial no acordado.
3. Observar cada enlace/estado y correlacionar origen→relays→gateway→API→comprobante. Guardar evidencia sin notas/coordenadas personales.
4. Ejecutar casos normales y fallos del catálogo; conservar todos los intentos. Para el umbral PR-08 usar 10 intentos declarados; para estimaciones estadísticas posteriores justificar una muestra mayor y sus intervalos.
5. Comparar con criterios predefinidos, registrar desviaciones y repetir únicamente tras cambios identificados. Asignar a un revisor el dictamen de cada prueba.

Comandos de reproducción JVM propuestos en la revisión documental previa, **no ejecutados con estas invocaciones en la validación actual**, para un entorno ya preparado con wrapper compatible/JDK/SDK/dependencias del proyecto (ver arriba el comando alternativo efectivamente usado):

```bash
./gradlew testDebugUnitTest --tests 'com.nicobutter.beaconchat.mesh.core.*'
./gradlew testDebugUnitTest
```

No usar esos comandos como prueba de radio ni agregar aquí dependencias para ensayos futuros.

## Métricas y registro

| Métrica | Definición propuesta | Precaución |
| --- | --- | --- |
| Entrega central | SOS únicos persistidos / SOS únicos generados, por escenario y ventana | Incluir intentos sin ruta y expirados; no igualar con handoff |
| Latencia central | Generación hasta persistencia confirmada, con método de reloj declarado | No restar relojes de teléfonos distintos sin sincronización o incertidumbre |
| Confirmación al origen | Comprobantes válidos recibidos en origen / SOS generados | Puede ser menor que entrega central por falla del retorno |
| Duplicación | Incidentes centrales extra por una misma clave SOS | Objetivo de diseño: cero con idempotencia durable |
| GNSS | Tiempo hasta fix, tasa sin fix, precisión estimada y edad | Separar cielo abierto/interior/manual; no confundir estimación con error real medido |
| Recursos | Pico de cola/reensamblado, descartes, CPU/memoria | Registrar valores y límites aprobados antes de prueba |
| Energía | mWh por hora y por SOS, o proxy documentado si no hay instrumental | % de batería no equivale a energía comparable entre modelos |
| Usabilidad | Acciones, tiempos, errores y comprensión por tarea | Muestra y método explícitos; sin entrevistas inventadas |
| Seguridad/privacidad | Intentos rechazados, exposición y cumplimiento de retención | Un test exitoso no certifica ausencia de vulnerabilidades |

Ficha para cada ensayo: `ID de prueba | fecha | responsable/revisor | versión | entorno/topología | datos sintéticos | esperado | observado | métricas/método | evidencia | desvíos | resultado (aprobado/fallido/no ejecutado)`.

No hay valores observados nuevos para estas métricas de sistema. La aprobación documental de PR-19 y la ejecución JVM del 17/10/2026 no cambian el estado P del producto integral ni el carácter S de las topologías existentes.


---

## Anexo: prueba BLE directa A/B

> Antecedente conservado para auditoría histórica; resultados declarados y condiciones de 2025 requieren reconfirmación. [Ver archivo original](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/docs/PRUEBA_BLE_DIRECTO.md).

### Issue #1: comunicación BLE directa A ↔ B

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


---

## Anexo: ensayo lógico mesh y límites

> Antecedente conservado para auditoría histórica; resultados declarados y condiciones de 2025 requieren reconfirmación. [Ver archivo original](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/docs/ISSUE_2_MESH_CORE.md).

### Issue #2 — núcleo mesh lógico y simulaciones

## Rama, aislamiento y alcance

Implementación en `feat/issue-2-mesh-core`, worktree `/tmp/beaconchat-issue-2`, basada en `fix/issue-1-direct-ble` / `fa46afa9ce83a3c4c0c394ecec4d113e9babd864`.
Esta rama **depende de la #1 hasta su validación y merge**. No se modifica el checkout ni la APK del directorio de pruebas físicas. No hay commit, push ni PR.

Solo se agregan clases Kotlin en `mesh/core`, tests JVM y este informe. No se cambia `BLEMeshController`, `MeshScreen`, `DirectBleProtocol`, manifest, dependencias, navegación, GATT, advertising ni módulos SOS/óptico/vibración. Ningún componente de la app instancia o llama al núcleo; no hay mesh activado en BLE. El APK del worktree incluye las clases sin integración, y no se instala en teléfonos.

## Archivos

| Archivo bajo `app/src` | Función |
| --- | --- |
| `main/java/com/nicobutter/beaconchat/mesh/core/MeshMessage.kt` | Envelope inmutable, IDs lógicos, reloj y puerto de transporte |
| `main/java/com/nicobutter/beaconchat/mesh/core/MeshEngine.kt` | Recepción/originación, dedupe, entrega local, TTL, cola FIFO y despacho |
| `test/java/com/nicobutter/beaconchat/mesh/core/SimulatedNetwork.kt` | Red determinista con enlaces, reloj y transporte fake |
| `test/java/com/nicobutter/beaconchat/mesh/core/MeshTopologyTest.kt` | Topologías y separación de identidades |
| `test/java/com/nicobutter/beaconchat/mesh/core/MeshEngineTest.kt` | Límites, errores, expiración e idempotencia |

## Semántica del motor

- `NodeId(UUID)` y `MessageId(UUID)` son tipos distintos. El ID de nodo se inyecta, no se deriva de MAC ni callsign. `originate` usa UUID aleatorio por mensaje por defecto y permite inyectar generador o reutilizar ID para un reintento. Conservar el mismo `(originId, messageId)` y contenido en cada copia/reintento es responsabilidad del host.
- `MeshMessage` contiene versión 1, ID, origen, destino opcional (`null` = broadcast), texto y saltos restantes. El motor conserva todos los campos salvo el TTL al retransmitir. No se implementa serialización ni se añaden timestamps de red.
- `originate` fija el origen al nodo propio. `receive` descarta ecos cuyo origen es el propio nodo. Un destino local entrega solo localmente; un broadcast entrega localmente y puede retransmitir; un destino diferente solo puede retransmitir.
- TTL cuenta transmisiones por enlace. Origen con TTL=2 envía TTL=1 a B; B envía TTL=0 a C. C puede entregar, nunca retransmitir. TTL=0 en el origen permite entrega propia/broadcast local, sin emisiones. Se excluyen el nodo propio y el salto anterior del fanout. El transporte futuro deberá respetar esas exclusiones.
- El resultado `deliverLocally` es un evento que el host debe consumir una vez. `Acceptance.ACCEPTED` solo indica admisión local; ni él ni `Handoff.ACCEPTED` confirman entrega remota o lectura.
- Primera llegada válida gana: el key se registra **antes** de entregar/encolar. Un duplicado no refresca su expiración, no entrega ni encola, aunque tenga mayor TTL o contenido distinto. IDs de distintos orígenes nunca se fusionan.

## Límites y descartes reproducibles

Valores por defecto, configurables por nodo:

| Recurso | Límite / política |
| --- | --- |
| Dedupe | 256 keys, 120 s desde admisión; lleno rechaza el nuevo key sin expulsar entradas vivas |
| Cola | 64 mensajes FIFO, 30 s desde admisión; llena descarta el nuevo reenvío |
| TTL | 0–16; negativo o superior al límite se rechaza |
| Texto | 1–1024 bytes UTF-8, no blanco; Unicode inválido se rechaza, no se reemplaza/trunca |
| Despacho | Como máximo el tamaño inicial de la cola y el presupuesto solicitado, un intento por item |

La dedupe retiene solo keys y deadlines; la cola tiene tanto número de items como tamaño de contenido limitados. La entrega local de un broadcast sigue siendo posible si su reenvío se descarta por cola llena; ese key sigue deduplicado. La saturación de dedupe rechaza toda nueva admisión, incluso entrega local. No se promete fiabilidad bajo presión.

El reloj inyectado es **monotónico y local**, sin sincronización de hora entre nodos. Un reloj que retrocede produce error explícito. La limpieza corre en cada operación pública y también puede llamarse con `cleanup()` mientras el host está inactivo. Los deadlines vencen en el instante exacto `now >= deadline`; no hay trabajo en segundo plano.

`dispatch` retira cada item antes del handoff. Si el transporte no está disponible o lanza una excepción, devuelve `UNAVAILABLE` / `TRANSPORT_ERROR` y no lo reencola ni inventa confirmaciones. El resto de la cola puede continuar. No existe descubrimiento de rutas ni almacenamiento DTN: un destino desconectado puede perder el mensaje después de agotar los enlaces disponibles.

La deduplicación es **temporal**, no exactamente-una-vez durable. Un replay admitido después de expirar su key puede entregar otra vez. Incluso entonces el TTL disminuye en cada transmisión y acota la propagación de cada copia. No hay reintentos autónomos que reinicien TTL; un productor externo que reinjecte indefinidamente mensajes con TTL restaurado queda fuera de esa garantía. La política de primera llegada también puede descartar una ruta posterior mejor. Estas limitaciones se prueban y no se ocultan como éxito de entrega.

El host debe serializar las llamadas. El puerto es síncrono y no bloqueante: un futuro adapter asíncrono deberá tener su propia cola acotada y definir qué significa aceptar el handoff. No se crean threads, coroutines, tareas, permisos ni recursos Android.

## BLE actual: presupuesto y plan posterior (sin implementar)

`DirectBleProtocol` sigue aceptando **20 bytes totales** por escritura. Los 1024 bytes de texto del modelo lógico **no** son una ampliación del canal BLE actual, y los envelopes de estos tests nunca se pasan a ese protocolo.

Ejemplo de presupuesto para un envelope futuro, aún sin serializer:

| Campo | Bytes propuestos |
| --- | ---: |
| Versión | 1 |
| Flags/tipo de destino | 1 |
| TTL | 1 |
| UUID origen | 16 |
| UUID de mensaje | 16 |
| UUID destino (unicast; omitido en broadcast) | 16 |
| Longitud de texto UTF-8 | 2 |
| Texto | L |
| **Total unicast / broadcast** | **53 + L / 37 + L** |

Incluso un unicast con `HOLA` requiere 57 bytes: no cabe en 20. Con 1024 bytes de contenido serían 1077 bytes. No hay compresión ni cabeceras ocultas en la implementación actual.

Una fragmentación futura podría reservar 8 bytes por fragmento de 20 (magic 1, versión de fragmentación 1, token de transferencia 2, índice 1, cantidad 1, longitud total 2), dejando 12 útiles. El ejemplo de 57 bytes requeriría 5 fragmentos; el máximo unicast, 90. Es un presupuesto de diseño, no un wire protocol aprobado ni implementado. Token de 16 bits sería local a conexión/peer, distinto del UUID lógico; requeriría transferencias serializadas o manejo de colisiones.

Después de aprobar A↔B, una integración separada necesitará:

1. Identidad lógica estable administrada por el host, sin asumir identidad/autenticación por MAC o callsign. Persistencia/autenticación no se implementan aquí.
2. Negociar capacidad/versión explícita y preferiblemente una característica GATT distinta. Un peer legado debe conservar el flujo directo actual, sin recibir frames mesh ni mezclar formatos.
3. Definir serializer y pruebas de roundtrip/corrupción; límites de reensamblado por peer y globales, timeouts, fragmentos duplicados/faltantes/fuera de orden y validación completa antes de `receive`.
4. Adaptar el handoff asíncrono a la cola, manejar backpressure/fallos parciales, evitar duplicar fanout por fragmento y reducir TTL una sola vez por salto lógico.
5. Diseñar por separado ACK de aplicación si se quiere afirmar entrega; ni aceptación del adapter ni callback GATT bastan.
6. Probar compatibilidad con la APK de #1 y luego tres teléfonos reales. Nada de eso queda activado por esta rama.

## Simulaciones y comandos

Tests sin Android Context ni llamadas a Bluetooth, sensores, permisos, Wi-Fi o Internet. El build Gradle de la app sí requiere JDK/SDK y dependencias disponibles en el entorno.

```bash
cd /tmp/beaconchat-issue-2
export JAVA_HOME=/home/lordcommander/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
# Solo núcleo
./gradlew testDebugUnitTest --tests 'com.nicobutter.beaconchat.mesh.core.*'
# Suite completa existente + núcleo, y APK separada del worktree
./gradlew testDebugUnitTest assembleDebug
```

Topologías: dos nodos bidireccionales, cadena A→B→C sin alcance A-C, cadena rota, triángulo, anillo de seis nodos con broadcast y diamante de cinco nodos con caminos duplicados. El fake limita ciclos/frames y falla si la propagación no termina.

También se prueban TTL=0/agotado, origen propio, destino local/no alcanzable, reintentos, mismo messageId de orígenes distintos, callsigns iguales/MAC cambiante, caché y cola llenas, orden FIFO, expiración exacta, fallos de transporte, UTF-8, versiones, reloj y TTL después de expirar dedupe.

## Resultados de verificación

- Consulta del Issue #2 y sus comentarios mediante GitHub: título “Paso 2A — Núcleo mesh multisalto simulado, sin tocar BLE”; sin comentarios.
- `./gradlew testDebugUnitTest --tests 'com.nicobutter.beaconchat.mesh.core.*'`: correcto en la primera ejecución (30 tests, antes de agregar el caso adicional de TTL tras expiración).
- `./gradlew testDebugUnitTest assembleDebug`: **BUILD SUCCESSFUL**, 48 tests, cero fallos/errores/omitidos: 22 de `MeshEngineTest`, 9 de `MeshTopologyTest`, 17 existentes de #1.
- Ejecución independiente con `java -cp ... org.junit.runner.JUnitCore com.nicobutter.beaconchat.mesh.core.MeshEngineTest com.nicobutter.beaconchat.mesh.core.MeshTopologyTest`: **OK (31 tests)**. Classpath limitado a clases compiladas de `debug`/`debugUnitTest`, Kotlin stdlib 1.8.22, JUnit 4.13.2 y Hamcrest 1.3; sin `android.jar`, Gradle, permisos ni runtime Android durante esa ejecución.
- APK separada generada en `/tmp/beaconchat-issue-2/app/build/outputs/apk/debug/app-debug.apk`. No instalada, sin integración BLE mesh.
- Advertencias de build: configuración BuildConfig/Gradle deprecada, source/target Java 8, opt-in/variables de módulos existentes y stripping de una biblioteca nativa. La compilación y los tests terminaron correctamente. No se ejecutó Lint en esta iteración; el informe de #1 documenta sus errores ópticos previos.
- Checkout original verificado: rama `fix/issue-1-direct-ble`, HEAD `fa46afa9ce83a3c4c0c394ecec4d113e9babd864`, working tree limpio. Comparación SHA-256 de **125 archivos** (tracked + APK) contra snapshot previo: **sin diferencias**.
- SHA-256 de la APK original preservada: `b611329fdaa4a8e29e7041410c7dc4fd1e6afc55b064898d015ed05ea6e30f13`.
- Cambios de implementación limitados a seis archivos nuevos descritos arriba (dos de núcleo, tres de tests y este informe); ningún archivo existente modificado.
- No hay commit, push ni PR. No se afirma validación física de mesh ni entrega entre tres teléfonos.
