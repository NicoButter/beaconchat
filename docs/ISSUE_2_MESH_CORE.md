# Issue #2 — núcleo mesh lógico y simulaciones

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
