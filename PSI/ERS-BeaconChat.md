# 02 · Requerimientos

> **Documento vigente (2026).** SOS local/GPS y adaptador mesh Android implementados en código; transporte SOS sintético/sin posición probado en JVM. **Captura satelital y BLE multisalto físicos pendientes.** GNSS real bloqueado en mesh; gateway, backend, mapas y centro no implementados. Los documentos de 2025 se conservan en el historial Git.


**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · Actualizado 18/10/2026**
[Índice](../README.md) · [Casos de uso](Casos-de-Uso-BeaconChat.md) · [Trazabilidad](Trazabilidad-BeaconChat.md)

## Convenciones y alcance de aceptación

Los IDs `R26-*` pertenecen a esta edición y no reemplazan los de los tres antecedentes. **E** = existente en código; **S** = lógica existente validada en simulaciones; **P** = propuesta sin implementación. La prioridad expresa importancia para el alcance futuro: **P0** esencial, **P1** ampliación de resiliencia, **P2** extensión. No indica que una función esté terminada.

Los criterios son verificables por las pruebas `R26-PR-*` de [06](Validacion-BeaconChat.md). Los objetivos cuantitativos futuros se identifican como hipótesis de laboratorio, sujetos a revisión; no son mediciones ni compromisos de rescate. Las etapas E0–E5 se definen en [07](Roadmap-BeaconChat.md).

### Perfil de laboratorio SOS · implementación #5

SOS V1 incluye UUID estable, tipo/gravedad declarada, creación civil local, nota opcional ≤256 bytes UTF-8 y posición opcional con precisión, fuente, edad y validez. Ausencia es `null`, nunca `(0,0)` automático. Codec estricto de hasta 433 bytes de contenido mesh. Un reintento conserva UUID/cuerpo del incidente y genera otro ID de intento mesh; no hay deduplicación durable de incidentes ni comprobante central.

GPS se solicita explícitamente en primer plano con `GPS_PROVIDER`, permiso preciso también en API 31+, timeout 1..120 s y cancelación. No usa red ni mapas. Un callback con edad ≤30 s puede ser reciente; un fix cacheado siempre es conocido y uno de más de 5 min se rotula antiguo. La edad del SOS se fija al crearlo, sin modificar el cuerpo durante reintentos. Sin fix/permiso se permite SOS sin posición.

**Solo se admite en mesh SOS de laboratorio con posición sintética fija o ausente.** Origen y recepción rechazan SOS estructurados con GNSS/mock reales, posición sintética arbitraria o formato inválido antes de registrar/reenviar contenido. El ensayo no copia coordenadas ni nota local. No hay autenticación/cifrado; la marca sintética no acredita seguridad. UI con estados local/cola/enlace/destino sin confirmar. [Contrato y evidencia](Validacion-BeaconChat.md).

## Requerimientos de usuario (RU)

| ID | Necesidad y resultado esperado | Prioridad | Estado |
| --- | --- | --- | --- |
| R26-RU-01 | Como persona afectada, quiero registrar SOS y comunicar ubicación con su calidad, o su ausencia, para que el centro interprete la solicitud. | P0 | E local / S ensayo sintético; centro y tránsito GNSS real P |
| R26-RU-02 | Como colaborador, quiero retransmitir solicitudes por vecinos sin necesitar Internet en cada teléfono. | P0 | E Android / S topologías; hardware pendiente |
| R26-RU-03 | Como emisor, quiero distinguir envío local, tránsito y recepción central para saber qué está confirmado. | P0 | E estados locales; confirmación central P |
| R26-RU-04 | Como usuario de gateway, quiero remitir solicitudes al centro cuando disponga de Internet y reconocer fallos de salida. | P0 | P |
| R26-RU-05 | Como operador autorizado, quiero visualizar y clasificar incidentes con ubicación/calidad y sin duplicados. | P0 | P |
| R26-RU-06 | Como emisor o colaborador, quiero conservar temporalmente solicitudes durante cortes y recuperarlas al volver el enlace. | P1 | P; no provisto por la cola actual |
| R26-RU-07 | Como participante, quiero decidir el uso de mis datos y que solo los destinatarios autorizados accedan a coordenadas y notas. | P0 | P |
| R26-RU-08 | Como usuario cercano, quiero utilizar texto BLE y señales locales disponibles aunque no haya servicio central. | P0 | E en código; hardware pendiente |
| R26-RU-09 | Como coordinador, quiero evaluar brigadistas, recursos, rutas reportadas y enlaces externos para futuras operaciones de crisis. | P2 | P |

## Requerimientos funcionales (RF)

| ID | Función / criterio de aceptación | Prioridad | Estado / etapa |
| --- | --- | --- | --- |
| R26-RF-01 | Descubrir pares y enviar texto directo con GATT. Rechazar UTF-8 inválido y paquetes mayores a **20 bytes totales**, callsign hasta 8 bytes; registrar éxito solo tras callback GATT y no como ACK de aplicación. | P0 | E / E0 |
| R26-RF-02 | Conservar señalización local y componentes de recepción: Morse/luz, vibración, audio, QR y beacon BLE SOS. Verificar cada canal físico por separado; el beacon codifica tipo de emergencia, no coordenadas ni entrega central. | P0 | E / E0 |
| R26-RF-03 | Procesar mensajes lógicos con origen/ID, destino opcional, TTL, dedupe y cola temporal. Un duplicado retenido no vuelve a entregar; TTL cero no reenvía; falta de transporte no produce ACK ni reintento automático. | P0 | S / E0 |
| R26-RF-04 | Registrar SOS con ID estable, tipo de emergencia, creación, gravedad declarada, nota breve opcional y ubicación opcional con metadatos. Validar campos antes de emitir; un reintento conserva identidad y contenido. | P0 | E modelo/codec/UI; S transporte de laboratorio / E3 parcial |
| R26-RF-05 | Intentar fix GNSS local con permiso informado y espera acotada, sin exigir red de datos. Registrar precisión estimada, instante monotónico de captura y edad; nunca convertir ausencia en `(0,0)`. | P0 | E GPS_PROVIDER; S política temporal; físico pendiente / E3 parcial |
| R26-RF-06 | Si falta fix/permiso, permitir posición conocida con edad/precisión explícita o SOS sin ubicación. No bloquear por falta de posición ni rotularla exacta. Referencia manual identificada queda propuesta. | P0 | E/S conocido/ausente; manual P / E3 parcial |
| R26-RF-07 | Integrar el núcleo al transporte BLE mediante formato versionado, identidad lógica, fragmentación/reensamblado acotado y reenvío A→B→C. Excluir salto anterior y descontar TTL una vez por transmisión lógica; conservar chat legado. | P0 | E Android / S GATT falso; físico pendiente / E2 parcial |
| R26-RF-08 | Exponer estados local, pendiente de tránsito, fallo/expiración y recepción central según evidencia. Solo una respuesta central válida produce «recibido por el centro»; un ACK GATT o handoff no lo hace. Sin retorno mesh de la confirmación, el origen debe seguir sin confirmar. | P0 | E estados locales/cola/enlace; central P / E3 parcial |
| R26-RF-09 | Habilitar rol gateway dinámico: verificar acceso al servicio por HTTPS, autenticarse y remitir el mismo SOS. Pérdida de red o respuesta ambigua no confirma entrega; reintentos y custodia durable dependen de R26-RF-12. | P0 | P / E3 |
| R26-RF-10 | Recibir en API, autenticar gateway y validar el SOS; deduplicar por identidad estable, guardar antes de confirmar y devolver comprobante correlacionado. Dos gateways con la misma solicitud generan un incidente, no dos. | P0 | P / E3 |
| R26-RF-11 | Permitir a operador autenticado ver mapa/lista, filtros por gravedad declarada/estado/tiempo y clasificar manualmente. Mostrar incertidumbre y ubicación no disponible sin inventar marcadores. Registrar cambios auditables. | P0 | P / E3 |
| R26-RF-12 | Implementar custodia durable con expiración, cuotas, reintentos espaciados y recuperación tras reinicio. Conservar ID y carga inmutable, limitar intentos y no restaurar TTL indefinidamente; eliminar conforme a retención acordada. | P1 | P / E4 |
| R26-RF-13 | Gestionar permisos/participación informada, identidades y autenticación de emisores/gateways, protección del contenido hasta el centro, roles, antiabuso, auditoría mínima y borrado/retención. Rechazar acceso indebido y evitar exposición pública de coordenadas. | P0 | E permisos/bloqueo GNSS real; criptografía y seguridad integral P / E3–E5 |
| R26-RF-14 | Incorporar brigadistas, disponibilidad/asignación de recursos, reportes de rutas bloqueadas y prioridad asistida bajo decisión humana. Identificar fuente/edad de cada reporte; no declarar una ruta segura automáticamente. | P2 | P / E4–E5 |
| R26-RF-15 | Evaluar transporte complementario LoRa mediante dispositivo/gateway externo compatible. Si se incorpora, preservar identidad/dedupe y no condicionar la cadena mínima BLE + Internet a disponer de LoRa. | P2 | P / E5 opcional |

Los RF-01/02 no satisfacen el sistema integral. RF-04/05/06/07/08 tienen el alcance parcial indicado; gateway, centro, custodia y seguridad integral siguen pendientes. El adaptador fragmenta contenido de hasta 1024 bytes sin modificar el chat RF-01. [Presupuesto y límites](Diseño-Arquitectura-BeaconChat.md).

## Requerimientos no funcionales (RNF)

Todos los RNF siguientes son **P** como requisitos del sistema integral; algunos tienen antecedentes parciales en el prototipo. Se evalúan con datos sintéticos antes de cualquier uso con personas afectadas.

| ID | Atributo / criterio verificable | Prioridad |
| --- | --- | --- |
| R26-RNF-01 | Veracidad del estado: **cero confirmaciones centrales falsas** en los casos de prueba de fallo; recepción del servidor, lectura y despacho de ayuda son eventos distintos. No se establece garantía de entrega. | P0 |
| R26-RNF-02 | Usabilidad/accesibilidad: flujo principal propuesto de hasta 3 acciones desde inicio hasta registrar SOS, sin contar permisos del SO; feedback local objetivo ≤1 s en equipo de prueba. Revisar lector de pantalla, texto ampliado, contraste y alternativa sin posición. Umbrales de laboratorio por validar con usuarios. | P0 |
| R26-RNF-03 | Rendimiento: medir p50/p95 de adquisición GNSS y latencia central por escenario. Hipótesis inicial de PoC: 10/10 solicitudes sintéticas en cadena estable de 3 enlaces llegan en ≤120 s cada una; solo aplica a configuración registrada, no al campo. | P0 |
| R26-RNF-04 | Recursos limitados: fijar antes del ensayo cuotas por peer/globales, tiempos de reensamblado y tamaño de SOS; bajo saturación no excederlas ni ocultar descartes. El valor del núcleo actual es antecedente, no dimensionamiento de producción. | P0 |
| R26-RNF-05 | Privacidad: ningún log, captura o vista pública debe revelar coordenadas/notas reales. Retención local/central y borrado deben tener plazos acordados, finalidad y responsable; comprobar vencimiento y exclusión de backups sensibles. | P0 |
| R26-RNF-06 | Seguridad: verificar autenticidad e integridad de contenido, rechazo de replay/falsos SOS según política, acceso por roles y credenciales revocables. Probar gateway ilegítimo, manipulación, captura de tráfico y abuso de colas. Sin afirmar seguridad extremo a extremo actual. | P0 |
| R26-RNF-07 | Interoperabilidad: un peer legado conserva chat de 20 bytes; uno incompatible con mesh recibe rechazo claro y no interpreta fragmentos como texto. Probar versiones y Unicode de extremo a extremo. | P0 |
| R26-RNF-08 | Recuperación: en fase durable, reiniciar durante un envío no pierde un SOS con custodia aceptada y vigente ni duplica el incidente central. Un mensaje vencido no se reenvía; informar pérdida por cuota o fallo de escritura. | P1 |
| R26-RNF-09 | Mantenibilidad/verificación: cada RF debe tener RU, CU, prueba, etapa y fuente/estado; enlaces y Mermaid válidos. Cambios de esquema mantienen catálogo de versiones y compatibilidad documentados. | P0 |
| R26-RNF-10 | Eficiencia ambiental: medir consumo por hora de escaneo y por SOS, con baseline en el mismo dispositivo; limitar sesiones según política y registrar impacto de cámara/GNSS/BLE. No declarar ahorro energético sin comparación medida. | P1 |

## Requerimientos del sistema (RS)

| ID | Restricción o dependencia / comprobación | Prioridad | Estado |
| --- | --- | --- | --- |
| R26-RS-01 | Base Android del repositorio: `minSdk 24`, `targetSdk 34`, `compileSdk 34`; BLE requerido por manifiesto. Matriz física por modelo/SO para el sistema futuro, sin asumir compatibilidad por compilar. | P0 | E configuración; validación P |
| R26-RS-02 | BLE usa ubicación hasta API 30 y Nearby Devices desde API 31. GPS requiere permiso preciso también en Android reciente; solicitar FINE/COARSE juntos, distinguir aproximado, denegado o revocado y cancelar en pausa, sin permiso de fondo. `neverForLocation` no sustituye ese permiso. | P0 | E permisos BLE/GPS; físico pendiente |
| R26-RS-03 | GNSS disponible y habilitado para fix automático; registrar fuente, tiempo y precisión. No depender de mapas descargados ni de geocodificación online para emitir SOS; admitir modo sin ubicación. | P0 | E componente local / S política; satélites pendientes |
| R26-RS-04 | Serializer, versiones y fragmentos acotados; validar integridad y mensaje completo antes del núcleo. SOS V1 se transporta como contenido sin cambiar el wire protocol mesh; criptografía consumirá presupuesto adicional. | P0 | E/S codec/mesh/SOS; protección P |
| R26-RS-05 | Diseñar ejecución y recuperación conforme al ciclo de vida Android, permisos y restricciones energéticas de las versiones ensayadas. Hoy la app detiene recursos al pausar: continuidad con pantalla bloqueada o background requiere trabajo y pruebas independientes. | P0 | P continuidad; E detención |
| R26-RS-06 | Centro futuro con API autenticada, almacenamiento durable/idempotente, control de acceso, auditoría y cartografía con licencia/fuente verificadas. Empezar con servicio de laboratorio, datos ficticios y operador de prueba. | P0 | P |
| R26-RS-07 | Solo el gateway requiere conectividad IP hacia la API y configuración HTTPS/credenciales; red Wi-Fi asociada no prueba Internet útil. El manifiesto actual no declara INTERNET; permisos y cliente de red se implementarán después. | P0 | P |
| R26-RS-08 | LoRa requiere equipo físico externo, alimentación, enlace con el teléfono, plan de radio y revisión de condiciones locales antes de un ensayo. No se presume radio LoRa en un Android común. | P2 | P |
| R26-RS-09 | El host/adaptador debe serializar llamadas a MeshEngine, proveer reloj monotónico e identidad lógica estable y definir handoff asíncrono con backpressure. Callsign o MAC no son credenciales ni identidad estable garantizada. | P0 | S contrato del núcleo; P host BLE |

## Condiciones de aprobación

El responsable revisará estos requisitos y sus prioridades con el equipo. «P0» define la necesidad del MVP conceptual, no obliga a programarlo durante las 28 horas. Las decisiones pendientes de formato, seguridad, cuotas, retención, tiempos GNSS y devolución de confirmaciones se documentan en [04](Diseño-Arquitectura-BeaconChat.md) y [05](Factibilidad-BeaconChat.md). Esta intervención entrega ingeniería documental, sin implementar los requisitos futuros.


---

## Procedencia de la versión anterior

El documento histórico [ERS-BeaconChat.md](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/PSI/ERS-BeaconChat.md) se archivó mediante el historial de Git. Podía contener hipótesis, cifras, certificaciones implícitas o estados de implementación no acreditados por el código actual; prevalece esta edición de 2026.

## Alcance de la base tecnológica actual

Además de los requisitos nuevos SOS+GNSS+mesh+gateway, BeaconChat contiene funciones experimentales de chat BLE directo, anuncios SOS BLE, Morse por luz, emisión de tonos, vibración, análisis de cámara y lectura/generación QR. Estos componentes se describen en el [README](../README.md), [arquitectura](Diseño-Arquitectura-BeaconChat.md), [protocolo óptico](Protocolo-Optico-BeaconChat.md) y [validación](Validacion-BeaconChat.md). Su funcionamiento en catástrofes reales **no está demostrado** y la integración con el SOS geolocalizado permanece pendiente.
