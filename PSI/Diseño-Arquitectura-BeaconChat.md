# 04 · Arquitectura actual y propuesta

**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · Actualizado 18/10/2026**
[Índice](../README.md) · [Requerimientos](ERS-BeaconChat.md) · [Casos de uso](Casos-de-Uso-BeaconChat.md)

## Base verificable y fuentes locales

Base del checkout `ac1dd8983960f6500b95433d40d6d390b5a9ebb3` de `main`, con integración mesh Android experimental agregada el 18/10/2026. Las rutas siguientes sustentan el estado; el código de multisalto y sus pruebas JVM no demuestran funcionamiento físico, y `LightMapScreen` no demuestra mapas geográficos.

| Fuente | Evidencia |
| --- | --- |
| [MainActivity](../app/src/main/java/com/nicobutter/beaconchat/MainActivity.kt), [MeshScreen](../app/src/main/java/com/nicobutter/beaconchat/ui/screens/MeshScreen.kt) | Activity/Composables, coordinación de controladores, permisos y ciclo de vida. MainActivity deriva de ComponentActivity; no se identifica como una clase ViewModel. |
| [BLEMeshController](../app/src/main/java/com/nicobutter/beaconchat/mesh/BLEMeshController.kt), [DirectBleProtocol](../app/src/main/java/com/nicobutter/beaconchat/mesh/DirectBleProtocol.kt) | Advertising/scanning y chat GATT directo intacto, 20 bytes; características mesh separadas, activas solo por participación explícita. |
| [MeshMessage](../app/src/main/java/com/nicobutter/beaconchat/mesh/core/MeshMessage.kt), [MeshEngine](../app/src/main/java/com/nicobutter/beaconchat/mesh/core/MeshEngine.kt) | Modelo lógico, TTL, dedupe y cola; sin tareas de red ni persistencia. |
| [Codec](../app/src/main/java/com/nicobutter/beaconchat/mesh/transport/MeshMessageCodec.kt), [fragmentación](../app/src/main/java/com/nicobutter/beaconchat/mesh/transport/MeshFragmentProtocol.kt), [reensamblador](../app/src/main/java/com/nicobutter/beaconchat/mesh/transport/MeshReassembler.kt) | Transporte binario V1 independiente de Android, tramas ≤20 bytes y recursos acotados; formato sin cambios. |
| [MeshBleNode](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/MeshBleNode.kt), [MeshGattClient](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/MeshGattClient.kt), [MeshLinkProtocol](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/MeshLinkProtocol.kt), [AndroidMeshTransport](../app/src/main/java/com/nicobutter/beaconchat/mesh/ble/AndroidMeshTransport.kt) | Adaptador con cola, identidad declarada por sesión y escrituras/recibos secuenciales; puente BluetoothGatt real compilado, política compartida con pruebas JVM. |
| [Pruebas de topologías](../app/src/test/java/com/nicobutter/beaconchat/mesh/core/MeshTopologyTest.kt), [red simulada](../app/src/test/java/com/nicobutter/beaconchat/mesh/core/SimulatedNetwork.kt) | Transporte simulado JVM; no uso del stack Bluetooth físico. |
| [Integración de transporte](../app/src/test/java/com/nicobutter/beaconchat/mesh/transport/MeshTransportIntegrationTest.kt) | Topologías JVM que serializan, fragmentan, reensamblan y decodifican en cada enlace antes de entregar al motor. |
| [Pruebas del adaptador](../app/src/test/java/com/nicobutter/beaconchat/mesh/ble/MeshBleAdapterTest.kt) | Mismo adaptador de producción, I/O GATT falso: cadena sin A↔C, diamante/anillo, fallos, cuotas y reconexión. No ejecuta el stack Android. |
| [EmergencyManager](../app/src/main/java/com/nicobutter/beaconchat/controller/EmergencyManager.kt), [BleEmitter](../app/src/main/java/com/nicobutter/beaconchat/emitter/BleEmitter.kt), [BleScanner](../app/src/main/java/com/nicobutter/beaconchat/scanner/BleScanner.kt) | Orquestación de señales y beacon SOS con un byte de tipo de emergencia. No transmite incidente ni coordenadas. |
| [Transmisor](../app/src/main/java/com/nicobutter/beaconchat/ui/screens/TransmitterScreen.kt), [receptor QR](../app/src/main/java/com/nicobutter/beaconchat/transceiver/QRScanner.kt), [detectores](../app/src/main/java/com/nicobutter/beaconchat/transceiver/LightDetector.kt) | Componentes ópticos/Morse, QR y otros canales locales, con validación física insuficiente. |
| [UserPreferences](../app/src/main/java/com/nicobutter/beaconchat/data/UserPreferences.kt) | Callsign en DataStore; no repositorio durable de SOS. |
| [Gradle de app](../app/build.gradle.kts), [manifiesto](../app/src/main/AndroidManifest.xml) | SDK 24/34, BLE y permisos. Ubicación hasta API 30 para scan BLE; sin permiso INTERNET ni servicio de incidentes. |
| [Informe BLE](Validacion-BeaconChat.md), [informe del núcleo](Validacion-BeaconChat.md) | Resultados históricos reportados; no se reinterpretan como ensayos nuevos. |

El `LocationManager` del controlador BLE solo comprueba proveedores habilitados para escaneo en Android anterior a 12. Esa comprobación no obtiene ni georreferencia un incidente. No hay componentes de GNSS para SOS, API/backend, mapas o LoRa. La integración MeshEngine↔BLE ahora existe como código experimental, con validación física pendiente.

## Arquitectura AS-IS

```mermaid
flowchart TB
    UI["E: MainActivity y pantallas Compose"] --> EM["E: EmergencyManager"]
    UI --> BC["E: BLEMeshController"]
    UI --> RX["E: recepción y análisis local"]
    UI --> PREF["E: UserPreferences / DataStore"]
    EM --> SIG["E: luz, vibración, audio y beacon SOS"]
    RX --> SENSOR["E: CameraX, acelerómetro, QR y BLE scanner"]
    BC --> RADIO["E: advertising, scan y GATT A-B"]
    BC --> ANDROID["E: AndroidMeshTransport<br/>GATT real, opt-in"]
    ANDROID <--> POLICY["E/S: MeshBleNode y MeshGattClient<br/>colas, identidad y sesiones"]
    ANDROID <--> RADIO
    POLICY <--> CORE["E/S: MeshEngine y MeshMessage<br/>lógica sin Android"]
    POLICY <--> WIRE["E/S: codec V1, fragmentador<br/>y reensamblador sin Android"]
    WIRE --> TEST["S: integración y topologías JVM"]
    POLICY --> TEST
    CORE --> OLDTEST["S: SimulatedNetwork / tests del núcleo"]
```

La separación es descriptiva de componentes reales, no una declaración de Clean Architecture/MVVM completo. E indica código existente y S evidencia por simulación; ninguno acredita por sí solo radio probada. El chat y la mesh experimental guardan mensajes/colas en memoria. Los controladores se detienen según navegación/pausa; no hay servicio que sostenga esta mesh con pantalla bloqueada.

### Transporte directo y núcleo lógico

El paquete directo es UTF-8 estricto `CALLSIGN:MENSAJE`: **20 bytes totales**, callsign de hasta 8 bytes y un separador. Con callsign de 8 bytes quedan 11 para texto; caracteres y bytes no son equivalentes. Cabe en el MTU ATT mínimo de 23, con 3 bytes de overhead ATT. No negocia un canal de 512 bytes ni fragmenta.

El callback de escritura GATT solo confirma esa operación; no lectura, ACK de aplicación, recepción central ni rescate. La aceptación de `MeshTransport` solo confirma handoff local. El producto no implementa Bluetooth Mesh Profile.

| Propiedad de MeshEngine | Comportamiento actual S |
| --- | --- |
| Identidad | NodeId y MessageId UUID; key `(originId, messageId)`, nunca MAC/callsign |
| Modelo | Versión 1, destino opcional (`null` broadcast), texto y saltos restantes |
| TTL | Predeterminado máximo 16; decrementa al encolar cada copia de transmisión; en cero puede entregar localmente, no reenviar |
| Dedupe | 256 keys por defecto, 120 s; una key expirada puede volver a admitirse |
| Cola | 64 mensajes FIFO por defecto, 30 s de retención; llena rechaza el reenvío nuevo |
| Contenido | Hasta 1024 bytes UTF-8; no define tamaño de frame BLE |
| Despacho | Un intento por item; lo retira incluso con transporte no disponible o excepción; sin reintento automático |
| Host | Debe serializar llamadas y suministrar reloj monotónico local; sin threads/timers/background propios |

La deduplicación es temporal, no exactamente-una-vez durable. No hay store-and-forward/DTN persistente en ese núcleo. Sus límites no son parámetros de capacidad ya validados para una catástrofe.

### Transporte binario y adaptador Android experimental

El envelope V1 serializa UUID lógicos, destino opcional, TTL y contenido UTF-8 estricto: **37 + L bytes broadcast / 53 + L unicast**, hasta 1024 bytes de contenido. No modifica ningún campo en un round-trip; el motor conserva la responsabilidad de descontar TTL una sola vez por salto lógico. El perfil de transporte admite TTL 0..16.

Las tramas tienen hasta **20 bytes**, con 12 de cabecera y hasta 8 de payload. Incluyen marcador binario, versión, ID de transferencia local al enlace, índice, cantidad, longitud total y CRC32 del envelope. El máximo unicast requiere 135 tramas. El reensamblador separa sesiones de conexión de identidades permanentes, admite desorden/duplicados idénticos y limita transferencias activas, entradas cerradas, memoria y duración; únicamente una carga completa, con CRC correcto y validada por el codec puede pasar al motor. CRC32 comprueba corrupción accidental del conjunto: **no equivale a autenticación ni cifrado**.

`MeshGattContract` registra dos características READ/WRITE propias en C4A7: datos/recibos `…b501` e identidad/capacidades `…b502`, rechazadas mientras la participación esté apagada. El advertising y la característica legacy permanecen con su formato anterior. Los candidatos detectados por scan se sondean mediante descubrimiento GATT y hello versionado de 20 bytes. Su NodeId declarado se asocia a la sesión, se verifica antes de cada envío y se usa como `previousHop`; MAC y callsign no identifican permanentemente al nodo. El UUID local se persiste en preferencias privadas mediante IO antes de permitir participar. Ese intercambio no autentica la identidad.

`MeshBleNode` es dueño del motor y del reensamblador. El motor autoriza copias, decrementa TTL una vez y pasa el fanout a una cola local atómica y acotada. `MeshGattClient` conecta con un solo vecino por vez, intercambia identidades y alterna WRITE de trama / READ del recibo correlacionado por transferencia, índice y CRC. Solo después avanza. `AndroidMeshTransport` implementa esas operaciones con BluetoothGatt asíncrono y callbacks serializados en main; no bloquea esperando radio o disco. El servidor reconstruye, verifica CRC y codec, entrega al motor y encola las copias autorizadas hacia otros NodeId. Chat directo y mesh arbitran el cliente saliente.

Hay hasta 16 vecinos, 8 trabajos incluyendo el activo, 8 sesiones entrantes y 32 eventos de historial; los límites del reensamblador se mantienen. Existen timeouts por operación, transferencia, cola y conexión; desconexión, cancelación y pausa liberan sesiones y descartan cargas parciales. Los callbacks de conexiones anteriores se ignoran, y un recibo antiguo no permite saltar de trama. No hay recuperación automática de mensajes perdidos ni custodia persistente.

El diálogo “Mesh experimental” de `MeshScreen` permite activar/desactivar participación, originar pruebas con destino opcional y TTL, copiar NodeId y consultar vecinos/actividad. La participación mantiene scan/publicidad en primer plano y se desactiva al pausar/salir. La UI distingue origen local, cola local, escrito en enlace y recibido localmente. **Handoff y recibo de trama no confirman el destino final.** No hay ACK extremo a extremo, autenticación ni cifrado.

El [contrato completo, cuotas, resultados y procedimiento de tres Android](Validacion-BeaconChat.md) registra **144 tests JVM aprobados**, incluidos 34 nuevos del adaptador. La ruta Android/GATT está implementada y compilada; su ejecución física A↔B y A→B→C continúa pendiente. No se agregan GPS, gateway, servidor ni mapas.

## Arquitectura TO-BE, íntegramente propuesta

```mermaid
flowchart TB
    subgraph PHONE["P: teléfono origen"]
        SOS["UI SOS y permisos"] --> LOC["Proveedor GNSS / alternativa manual"]
        LOC --> DOM["Registro SOS mínimo"]
        DOM --> AD["Serialización, protección y adaptador mesh"]
        DOM -. "E4" .-> STORE["Custodia durable con expiración"]
    end
    AD -->|"BLE P"| B["P: nodo B retransmisor"]
    B -->|"BLE P"| C["P: nodo C retransmisor"]
    C -->|"BLE P"| D["P: nodo D gateway dinámico"]
    D -->|"HTTPS autenticado P"| API["P: API central"]
    API --> DB["P: incidentes y comprobantes durables"]
    DB --> OPS["P: centro de operaciones<br/>lista, mapa y clasificación humana"]
    EXT["P opcional: radio / gateway LoRa externo"] -. "E5, complemento" .-> D
```

Cada teléfono compatible podría ser origen/retransmisor/gateway según conectividad y política. El gateway exige acceso real a la API, no solo señal Wi-Fi. El centro inicial será de laboratorio, sin enlace a servicios oficiales. El mapa es una vista del centro; obtener coordenadas GNSS no requiere tener un mapa en la app.

### Integración, seguridad y presupuesto de bytes

Se conserva el chat directo; transporte V1 y adaptador Android experimental ya están implementados arriba con descubrimiento explícito de características mesh separadas. El mensaje SOS rico sigue siendo una propuesta: excederá el presupuesto directo con UUID, tiempos, posición y protección; no se enviará como texto truncado en `CALLSIGN:MENSAJE`.

El serializer V1 usa **53 + L bytes** unicast: con `HOLA` son 57, aun sin ubicación ni criptografía. Su fragmentador genera 8 tramas para ese mensaje. El presupuesto SOS futuro deberá sumar datos de dominio y protección, y revisar el límite actual de 1024 bytes de contenido. El reensamblador existente incorpora índices, cantidad, longitud total, CRC32, timeout y cuotas por sesión/globales; la pérdida de un fragmento impide entregar al núcleo, pero no desencadena recuperación automática.

El adaptador actual aplica backpressure mediante cola acotada y coordina llamadas serializadas al motor; conserva el TTL ya descontado por este. Ningún fragmento descuenta TTL. Aceptar el puerto síncrono solo admite la cola local. La UI experimental está conectada y rotula estados sin promesa de destino final; faltan ensayos de radio, peers legados y fallos parciales sobre teléfonos antes de atribuir funcionamiento físico.

Se propone un cuerpo SOS inmutable protegido hasta el centro, con metadatos mínimos de tránsito separados. El TTL mutable requiere reglas y controles antiabuso propios: proteger el cuerpo no impide que un nodo malicioso intente restaurar TTL. Identidad lógica no equivale a autenticación. Provisión/revocación de claves, cifrado offline hacia el centro y autenticación de emisores no están resueltos ni se elige aquí un protocolo criptográfico ad hoc.

### Secuencia normal propuesta

```mermaid
sequenceDiagram
    actor V as Persona
    participant A as A origen P
    participant GPS as GNSS local
    participant B as B relay P
    participant C as C relay P
    participant D as D gateway P
    participant API as API P
    participant DB as DB P
    actor O as Operador
    V->>A: Registrar SOS y permisos
    A->>GPS: Intentar fix<br/>con espera acotada
    GPS-->>A: Posicion, precision<br/>y captura
    A->>A: Crear ID y cuerpo SOS
    A->>B: Envelope BLE<br/>fragmentado, futuro
    B->>C: Reensamblar,<br/>dedupe y TTL
    C->>D: Reenviar mismo SOS
    D->>API: HTTPS autenticado<br/>y clave idempotente
    API->>DB: Guardar o recuperar<br/>incidente unico
    DB-->>API: Persistencia<br/>confirmada
    API-->>D: Comprobante central<br/>correlacionado
    Note over A,D: Origen sin confirmar hasta recibir<br/>comprobante verificable
    D-->>C: Retorno mesh propuesto<br/>del comprobante
    C-->>B: Retorno si hay enlaces
    B-->>A: Retorno si hay enlaces
    A-->>V: Recepcion central,<br/>si se verifico
    O->>API: Consultar y clasificar con rol
    Note over V,O: Recepcion no significa lectura,<br/>despacho ni rescate
```

El retorno de comprobantes es una decisión de protocolo futura y puede fallar por movilidad. No está resuelto por el GATT directo ni por el núcleo actual. El centro puede recibir aunque A no llegue a enterarse.

### Secuencia de fallos y recuperación propuesta

```mermaid
sequenceDiagram
    actor V as Persona
    participant A as Origen futuro
    participant B as Vecino futuro
    participant D as Gateway futuro
    participant API as API futura
    V->>A: Solicitar SOS
    A->>A: GNSS no disponible o permiso denegado
    A-->>V: Reciente rotulada, manual o sin ubicacion
    alt Sin vecinos
        A-->>V: Sin enlace, recepcion central sin confirmar
        opt Custodia durable E4 implementada
            A->>A: Guardar con cuota y vencimiento
        end
    else Hay vecinos pero no salida
        A->>B: SOS valido
        B->>D: Reenvio logico futuro
        D->>API: Intentar HTTPS
        API--xD: Sin respuesta o conectividad perdida
        Note over A,D: No mostrar confirmacion central por ACK GATT
        opt Recuperacion durable E4 implementada
            D->>D: Guardar, esperar, no restaurar TTL sin limite
            D->>API: Reintentar mismo ID al volver Internet
            API-->>D: Guardado previo o nuevo, mismo incidente
        end
    end
```

## Modelo conceptual de datos propuesto

Este modelo no existe como clases de incidentes, esquema SQL ni API en el repositorio. Es una base de discusión; el formato lógico actual solo tiene los campos de MeshMessage descritos arriba.

| Entidad / campo | Semántica y validación propuesta |
| --- | --- |
| SOS: `originId`, `messageId` | Identidad estable compuesta, usada en dedupe/transporte y como clave idempotente central; un reintento no crea otro ID |
| SOS: `schemaVersion`, `emergencyType` | Versión y catálogo explícito; no asumir que ordinal interno de enum sea contrato público estable |
| SOS: `createdAt` | Momento declarado de creación; marcar incertidumbre del reloj civil, no prueba de autenticidad |
| SOS: `declaredSeverity`, `note` | Gravedad declarada y nota breve opcional, con longitud UTF-8 acotada antes de ensayar; sin nombres/documentos obligatorios |
| Location: `source`, coordenadas opcionales | `gnss`, `lastKnown`, `manual` o `unavailable`; latitud −90..90 y longitud −180..180 cuando existan; no usar valores por defecto como posición |
| Location: `accuracyM`, `capturedAt`, `ageAtCreationMs` | Precisión estimada si la fuente la provee, momento de captura y edad calculada localmente; dato desconocido explícito. Posición manual no recibe precisión GNSS ficticia |
| Location: `manualReference` | Descripción opcional de lugar, sin geocodificación inventada; puede existir sin coordenadas |
| Tránsito: `hopsRemaining`, versión y fragmentos | Metadatos por enlace, separados del cuerpo inmutable. Número de saltos no es tiempo de vida |
| Custodia E4: vencimiento, intentos, próxima ejecución | Persistencia local protegida, cuotas y política durable; no son los 30 s de cola actuales |
| Incident: `incidentId`, `receivedAt`, estado | ID interno central, referencia al SOS único, hora del servidor y ciclo de atención humana separado del transporte |
| Receipt: clave SOS, `receivedAt`, comprobante | Respuesta verificable de persistencia; no es ACK GATT ni orden de rescate |
| Review: operador, tiempo, clasificación | Cambio autorizado/auditable; gravedad revisada distinguible de la declarada |

```mermaid
erDiagram
    NODE ||--o{ SOS : origina
    SOS ||--o| LOCATION : describe
    SOS ||--o{ CUSTODY : conserva
    SOS ||--o| INCIDENT : registra
    INCIDENT ||--o{ RECEIPT : confirma
    INCIDENT ||--o{ REVIEW : recibe
    OPERATOR ||--o{ REVIEW : realiza
    NODE {
        uuid originId PK
    }
    SOS {
        uuid originId PK
        uuid messageId PK
        int schemaVersion
        string emergencyType
        datetime createdAt
        string declaredSeverity
        string note
    }
    LOCATION {
        string source
        float latitude
        float longitude
        float accuracyM
        datetime capturedAt
        long ageAtCreationMs
        string manualReference
    }
    CUSTODY {
        uuid holderNodeId
        datetime expiresAt
        int attempts
    }
    INCIDENT {
        uuid incidentId PK
        datetime receivedAt
        string status
    }
    RECEIPT {
        uuid receiptId PK
        datetime receivedAt
    }
    OPERATOR {
        uuid operatorId PK
        string role
    }
    REVIEW {
        datetime reviewedAt
        string classification
    }
```

SOS→INCIDENT es 0..1 mientras todavía no hubo llegada central; la unicidad es por clave compuesta SOS, con transacción persistente. El gráfico no expresa nulabilidad de atributos: coordenadas/calidad pueden faltar. En la carga se incluirá el estado explícito `unavailable` cuando corresponda. Nuevos datos de posición o cancelación requerirán eventos/revisiones correlacionadas diseñadas por separado; no se muta silenciosamente un SOS deduplicado.

La edad de ubicación aumenta durante el tránsito. La API debe conservar edad al crear, captura declarada y recepción de servidor, e indicar incertidumbre temporal; relojes de teléfonos distintos pueden discrepar. El reloj monotónico del núcleo es local, no sincroniza los tiempos civiles del SOS.

## Estados y decisiones abiertas

```mermaid
stateDiagram-v2
    [*] --> Registrado
    Registrado --> EnTransito: enlace disponible
    Registrado --> SinSalida: no hay vecino
    EnTransito --> SinSalida: fallo o falta de salida
    SinSalida --> Guardado: solo custodia durable E4
    Guardado --> EnTransito: reintento acotado
    Guardado --> Expirado: vencimiento
    SinSalida --> Fallido: sin custodia o politica agotada
    EnTransito --> ConfirmadoCentral: comprobante valido recibido
    ConfirmadoCentral --> [*]
    Fallido --> [*]
    Expirado --> [*]
```

Es la vista de conocimiento de un nodo, no el estado global del rescate. El centro puede tener el incidente aunque el emisor permanezca sin confirmar. Los estados centrales de clasificación/asignación/atención son otro ciclo futuro, con responsables y reglas propias.

| Decisión de diseño | Propuesta y consecuencia | Pendiente antes de código/operación |
| --- | --- | --- |
| AD-01 · BLE como cadena mínima | Reutilizar componentes, separar chat legado y mesh; no requerir LoRa | Codec/fragmentos y adaptador GATT experimental implementados; faltan ensayos físicos y diseño de fiabilidad/identidad segura |
| AD-02 · Ubicación honesta | GNSS independiente de Internet; manual/sin fix permitidos | Espera máxima, umbral de dato reciente, política de precisión |
| AD-03 · Confirmar solo persistencia | Comprobante autenticable y dedupe central | Contrato API, errores, retorno mesh y conflicto de mismo ID |
| AD-04 · Custodia separada del núcleo | No convertir FIFO temporal en promesa DTN | Retención, cuotas, recuperación, TTL e idempotencia tras reinicio |
| AD-05 · Datos mínimos protegidos | Relays sin acceso innecesario a coordenadas/notas | Amenazas, claves offline, revocación, consentimiento y acceso |
| AD-06 · Centro de laboratorio primero | Clasificación humana, datos sintéticos; mapas solo si hay datos válidos | Responsable de operación, licencias cartográficas, integración oficial futura |

Las decisiones de SOS/centro son propuestas, no ADR históricos aprobados. El transporte V1 y la política del adaptador están probados en JVM; el puente Android/GATT está implementado y compilado, sin resultado físico todavía. No se añaden dependencias, permisos ni servicios Android en esta intervención.


---

## Referencia de arquitectura previa

El inventario previo de componentes se mantiene [en el historial de Git](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/docs/ARCHITECTURE.md). La arquitectura vigente aquí reemplaza aquella interpretación histórica; no se considera validación del sistema multisalto.
