# 04 · Arquitectura actual y propuesta

**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Requerimientos](ERS-BeaconChat.md) · [Casos de uso](Casos-de-Uso-BeaconChat.md)

## Base verificable y fuentes locales

Lectura del commit `2e754e315a40533e9b3f61a0a2a292d2db9b96a2` de `main`. Las rutas siguientes sustentan el estado; el nombre `BLEMeshController` o `LightMapScreen` no demuestra mesh multisalto ni mapas geográficos.

| Fuente | Evidencia |
| --- | --- |
| [MainActivity](../app/src/main/java/com/nicobutter/beaconchat/MainActivity.kt), [MeshScreen](../app/src/main/java/com/nicobutter/beaconchat/ui/screens/MeshScreen.kt) | Activity/Composables, coordinación de controladores, permisos y ciclo de vida. MainActivity deriva de ComponentActivity; no se identifica como una clase ViewModel. |
| [BLEMeshController](../app/src/main/java/com/nicobutter/beaconchat/mesh/BLEMeshController.kt), [DirectBleProtocol](../app/src/main/java/com/nicobutter/beaconchat/mesh/DirectBleProtocol.kt) | Advertising/scanning y cliente/servidor GATT directo; máximo 20 bytes totales. |
| [MeshMessage](../app/src/main/java/com/nicobutter/beaconchat/mesh/core/MeshMessage.kt), [MeshEngine](../app/src/main/java/com/nicobutter/beaconchat/mesh/core/MeshEngine.kt) | Modelo lógico, TTL, dedupe y cola; no hay serializer BLE, tareas de red ni persistencia. |
| [Pruebas de topologías](../app/src/test/java/com/nicobutter/beaconchat/mesh/core/MeshTopologyTest.kt), [red simulada](../app/src/test/java/com/nicobutter/beaconchat/mesh/core/SimulatedNetwork.kt) | Transporte simulado JVM; no uso del stack Bluetooth físico. |
| [EmergencyManager](../app/src/main/java/com/nicobutter/beaconchat/controller/EmergencyManager.kt), [BleEmitter](../app/src/main/java/com/nicobutter/beaconchat/emitter/BleEmitter.kt), [BleScanner](../app/src/main/java/com/nicobutter/beaconchat/scanner/BleScanner.kt) | Orquestación de señales y beacon SOS con un byte de tipo de emergencia. No transmite incidente ni coordenadas. |
| [Transmisor](../app/src/main/java/com/nicobutter/beaconchat/ui/screens/TransmitterScreen.kt), [receptor QR](../app/src/main/java/com/nicobutter/beaconchat/transceiver/QRScanner.kt), [detectores](../app/src/main/java/com/nicobutter/beaconchat/transceiver/LightDetector.kt) | Componentes ópticos/Morse, QR y otros canales locales, con validación física insuficiente. |
| [UserPreferences](../app/src/main/java/com/nicobutter/beaconchat/data/UserPreferences.kt) | Callsign en DataStore; no repositorio durable de SOS. |
| [Gradle de app](../app/build.gradle.kts), [manifiesto](../app/src/main/AndroidManifest.xml) | SDK 24/34, BLE y permisos. Ubicación hasta API 30 para scan BLE; sin permiso INTERNET ni servicio de incidentes. |
| [Informe BLE](Validacion-BeaconChat.md), [informe del núcleo](Validacion-BeaconChat.md) | Resultados históricos reportados; no se reinterpretan como ensayos nuevos. |

El `LocationManager` del controlador BLE solo comprueba proveedores habilitados para escaneo en Android anterior a 12. Esa comprobación no obtiene ni georreferencia un incidente. El análisis de fuentes no encontró componentes de GNSS para SOS, API/backend, mapas, LoRa, fragmentación del envelope o integración entre MeshEngine y BLE.

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
    CORE["S: MeshEngine y MeshMessage<br/>lógica sin Android"] --> TEST["S: SimulatedNetwork / tests JVM"]
    CORE -. "sin integración" .-> BC
```

La separación es descriptiva de componentes reales, no una declaración de Clean Architecture/MVVM completo. El chat guarda peers/mensajes en memoria. Los controladores se detienen según navegación/pausa; no existe un servicio probado que sostenga la futura mesh con pantalla bloqueada.

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

Se propone conservar el chat directo y negociar una capacidad/versión mesh en un canal separado. Antes de implementar se deben definir formato, tamaños y pruebas. El mensaje rico excederá el presupuesto directo con UUID, tiempos, posición y protección; no se enviará como texto truncado en `CALLSIGN:MENSAJE`.

Como antecedente de cálculo, el [informe del núcleo](Validacion-BeaconChat.md) estima un envelope lógico binario unicast de **53 + L bytes**: con `HOLA` serían 57, aun sin ubicación ni criptografía. Es un ejemplo de diseño, no un serializer existente. El presupuesto SOS deberá sumar cabeceras, metadatos, autenticación y cifrado. Fragmentación necesita índices, cantidad, longitud total, integridad, timeout y cuotas por peer/globales; la pérdida de un fragmento no autoriza entregar una carga parcial al núcleo.

El adaptador asíncrono necesitará backpressure y coordinación serializada con MeshEngine; descontará TTL una vez por salto lógico, no por fragmento. No podrá asumir que su puerto síncrono equivale a recepción por Bluetooth. Se evaluarán pruebas con peers legados y fallos parciales antes de conectar la UI.

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
| AD-01 · BLE como cadena mínima | Reutilizar componentes, separar chat legado y mesh; no requerir LoRa | Negociación, serialización, fragmentos, límites y ensayos |
| AD-02 · Ubicación honesta | GNSS independiente de Internet; manual/sin fix permitidos | Espera máxima, umbral de dato reciente, política de precisión |
| AD-03 · Confirmar solo persistencia | Comprobante autenticable y dedupe central | Contrato API, errores, retorno mesh y conflicto de mismo ID |
| AD-04 · Custodia separada del núcleo | No convertir FIFO temporal en promesa DTN | Retención, cuotas, recuperación, TTL e idempotencia tras reinicio |
| AD-05 · Datos mínimos protegidos | Relays sin acceso innecesario a coordenadas/notas | Amenazas, claves offline, revocación, consentimiento y acceso |
| AD-06 · Centro de laboratorio primero | Clasificación humana, datos sintéticos; mapas solo si hay datos válidos | Responsable de operación, licencias cartográficas, integración oficial futura |

Son propuestas de esta edición, no ADR históricos aprobados. No se añaden dependencias, permisos, servicios ni módulos en esta intervención.


---

## Referencia de arquitectura previa

El inventario previo de componentes se mantiene [en el historial de Git](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/docs/ARCHITECTURE.md). La arquitectura vigente aquí reemplaza aquella interpretación histórica; no se considera validación del sistema multisalto.
