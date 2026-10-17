# 06 · Validación y criterios de aceptación

**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Requerimientos](ERS-BeaconChat.md) · [Trazabilidad](Trazabilidad-BeaconChat.md)

## Evidencia disponible y límites

| Evidencia | Resultado disponible | Qué no demuestra |
| --- | --- | --- |
| [Informe BLE directo](Validacion-BeaconChat.md) | **17 tests JVM reportados**: 13 protocolo, 3 estados y 1 original. Prueba A↔B pendiente; sin dispositivos en aquel informe | Stack Bluetooth, alcance, compatibilidad entre fabricantes ni mesh físico |
| [Informe del núcleo](Validacion-BeaconChat.md) | **48 tests totales reportados**, incluidos 31 del núcleo (22 motor + 9 topologías) y los 17 anteriores | GNSS, radio real, fragmentación, DTN, gateway, backend ni rescate |
| [Código de tests del motor](../app/src/test/java/com/nicobutter/beaconchat/mesh/core/MeshEngineTest.kt) y [topologías](../app/src/test/java/com/nicobutter/beaconchat/mesh/core/MeshTopologyTest.kt) | Cadena, ruptura de enlace, triángulo, anillo, caminos duplicados, límites, TTL y expiración mediante red simulada | Probabilidades de pérdida, obstáculos, consumo y restricciones de un teléfono |
| Señalización óptica/vibración/audio/QR/beacon BLE | Componentes presentes en código | Tasas de decodificación o funcionamiento garantizado bajo derrumbes |
| Revisión documental de esta intervención | Consultar [INFORME-REVISION](INFORME-REVISION.md) | No es una ejecución nueva de los ensayos anteriores |

**No se volvió a ejecutar Gradle, APK, tests JVM ni pruebas físicas durante esta tarea documental.** Los 48 incluyen los 31, no son 48 + 31. El informe BLE también registra errores de Lint preexistentes en módulos ópticos; no se presenta un build completo libre de incidencias ni se modifica código para resolverlos.

## Catálogo de pruebas

Los IDs completos son `R26-PR-01` a `R26-PR-19`. Solo PR-19 se ejecuta como revisión documental de esta entrega; PR-01/02 tienen evidencia histórica limitada y el resto queda pendiente. Cada resultado futuro debe señalar simulación, laboratorio físico o campo.

| ID | Preparación y procedimiento | Criterio de aceptación / evidencia | Estado |
| --- | --- | --- | --- |
| R26-PR-01 | Ejecutar tests de protocolo/estado y protocolo manual A↔B del informe BLE: permisos, Unicode, 20 bytes exactos/exceso, desconexión, pausa y timeout. | Tests del protocolo correctos; ambos teléfonos registran texto íntegro, sin truncar ni éxito falso; guardar modelos/SO/logs. | JVM histórico reportado; físico pendiente |
| R26-PR-02 | Revisar y reproducir suite del núcleo: cadena intacta/rota, TTL cero, triángulo/anillo/diamante, dedupe/colas/reloj y fallos de transporte. | Resultado lógico esperado y propagación termina; transporte no disponible consume intento sin reencolar ni ACK. | 31 JVM históricos reportados; sin nueva ejecución |
| R26-PR-03 | En dispositivos compatibles, probar cada emisor/receptor óptico, vibración, audio, QR y beacon; registrar luz/ruido/acoplamiento, permisos y detención. | Emisión/decodificación observada por canal y condición; errores y limitaciones explícitos, sin atribuir alcance genérico ni geolocalización al beacon. | Pendiente físico |
| R26-PR-04 | Con implementación futura, registrar SOS sintéticos válidos/invalidables y doble toque/reintento. | Esquema validado, ID/carga estable para reintentos, sin incidente accidental doble; nota/límites UTF-8 comprobados. | Pendiente |
| R26-PR-05 | Desactivar datos/Wi-Fi; ensayar GNSS al aire libre y en interior, con permisos, proveedor apagado y límite de espera configurado. Registrar tiempo hasta fix y precisión reportada. | No exige Internet, captura/edad/fuente visibles; timeout no fabrica posición ni bloquea SOS. No exigir fix en toda condición. | Pendiente físico |
| R26-PR-06 | Forzar sin fix, permiso denegado, posición antigua, dato manual inválido y ausencia completa de ubicación. | Admite SOS sin posición, rechaza rangos inválidos, rotula manual/reciente y no dibuja posición ficticia; antigüedad aumenta y reloj incierto se indica. | Pendiente |
| R26-PR-07 | Probar serializer/fragmentos futuros: pérdida, duplicación, desorden, corrupción, exceso, timeout, mezcla de versiones y peer legado. | Roundtrip íntegro solo con carga completa/válida; memoria acotada, liberación al vencer y chat legado intacto; TTL no se descuenta por fragmento. | Pendiente |
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

Comandos de reproducción JVM, **no ejecutados en esta intervención**, para un entorno ya preparado con JDK/SDK/dependencias del proyecto:

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

No hay valores observados nuevos para estas métricas de sistema. La aprobación documental de PR-19 no cambia el estado P del producto integral ni el carácter S de las topologías existentes.


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
