# 02 · Requerimientos

**Vetrabyte / Nicolás Butterfield · [MIT](../../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Casos de uso](03-casos-de-uso.md) · [Trazabilidad](10-trazabilidad.md)

## Convenciones y alcance de aceptación

Los IDs `R26-*` pertenecen a esta edición y no reemplazan los de los tres antecedentes. **E** = existente en código; **S** = lógica existente validada en simulaciones; **P** = propuesta sin implementación. La prioridad expresa importancia para el alcance futuro: **P0** esencial, **P1** ampliación de resiliencia, **P2** extensión. No indica que una función esté terminada.

Los criterios son verificables por las pruebas `R26-PR-*` de [06](06-validacion.md). Los objetivos cuantitativos futuros se identifican como hipótesis de laboratorio, sujetos a revisión; no son mediciones ni compromisos de rescate. Las etapas E0–E5 se definen en [07](07-roadmap.md).

## Requerimientos de usuario (RU)

| ID | Necesidad y resultado esperado | Prioridad | Estado |
| --- | --- | --- | --- |
| R26-RU-01 | Como persona afectada, quiero registrar SOS y comunicar ubicación con su calidad, o su ausencia, para que el centro interprete la solicitud. | P0 | P |
| R26-RU-02 | Como colaborador, quiero retransmitir solicitudes por vecinos sin necesitar Internet en cada teléfono. | P0 | P; antecedente lógico S |
| R26-RU-03 | Como emisor, quiero distinguir envío local, tránsito y recepción central para saber qué está confirmado. | P0 | P; confirmación GATT directa E |
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
| R26-RF-04 | Registrar SOS con ID estable, tipo de emergencia, creación, gravedad declarada, nota breve opcional y ubicación opcional con metadatos. Validar campos antes de emitir; un reintento conserva identidad y contenido. | P0 | P / E1 diseño, E3 código |
| R26-RF-05 | Intentar fix GNSS local con permiso informado y espera acotada, sin exigir red de datos. Registrar precisión estimada, captura y edad; nunca convertir ausencia de fix en coordenadas `(0,0)`. | P0 | P / E3 |
| R26-RF-06 | Si falta fix o permiso, permitir ubicación reciente con antigüedad/precisión explícita, referencia manual identificada o SOS sin ubicación. No bloquear la solicitud por falta de posición ni rotularla como exacta. | P0 | P / E3 |
| R26-RF-07 | Integrar el núcleo al transporte BLE mediante formato versionado, identidad lógica, fragmentación/reensamblado acotado y reenvío A→B→C. Excluir salto anterior y descontar TTL una vez por transmisión lógica; conservar chat legado. | P0 | P / E2 |
| R26-RF-08 | Exponer estados local, pendiente de tránsito, fallo/expiración y recepción central según evidencia. Solo una respuesta central válida produce «recibido por el centro»; un ACK GATT o handoff no lo hace. Sin retorno mesh de la confirmación, el origen debe seguir sin confirmar. | P0 | P / E3 |
| R26-RF-09 | Habilitar rol gateway dinámico: verificar acceso al servicio por HTTPS, autenticarse y remitir el mismo SOS. Pérdida de red o respuesta ambigua no confirma entrega; reintentos y custodia durable dependen de R26-RF-12. | P0 | P / E3 |
| R26-RF-10 | Recibir en API, autenticar gateway y validar el SOS; deduplicar por identidad estable, guardar antes de confirmar y devolver comprobante correlacionado. Dos gateways con la misma solicitud generan un incidente, no dos. | P0 | P / E3 |
| R26-RF-11 | Permitir a operador autenticado ver mapa/lista, filtros por gravedad declarada/estado/tiempo y clasificar manualmente. Mostrar incertidumbre y ubicación no disponible sin inventar marcadores. Registrar cambios auditables. | P0 | P / E3 |
| R26-RF-12 | Implementar custodia durable con expiración, cuotas, reintentos espaciados y recuperación tras reinicio. Conservar ID y carga inmutable, limitar intentos y no restaurar TTL indefinidamente; eliminar conforme a retención acordada. | P1 | P / E4 |
| R26-RF-13 | Gestionar permisos/participación informada, identidades y autenticación de emisores/gateways, protección del contenido hasta el centro, roles, antiabuso, auditoría mínima y borrado/retención. Rechazar acceso indebido y evitar exposición pública de coordenadas. | P0 | P / E3 laboratorio–E5 operación |
| R26-RF-14 | Incorporar brigadistas, disponibilidad/asignación de recursos, reportes de rutas bloqueadas y prioridad asistida bajo decisión humana. Identificar fuente/edad de cada reporte; no declarar una ruta segura automáticamente. | P2 | P / E4–E5 |
| R26-RF-15 | Evaluar transporte complementario LoRa mediante dispositivo/gateway externo compatible. Si se incorpora, preservar identidad/dedupe y no condicionar la cadena mínima BLE + Internet a disponer de LoRa. | P2 | P / E5 opcional |

Los RF-01/02 actuales no satisfacen por sí mismos RF-04 a RF-15. El contenido lógico de hasta 1024 bytes de RF-03 no cabe automáticamente en RF-01. [Presupuesto y límites](04-arquitectura.md).

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
| R26-RS-02 | Permisos por versión: el flujo BLE actual usa ubicación hasta API 30 y Nearby Devices desde API 31. La futura captura GNSS necesita permisos/política propios también en Android reciente; `neverForLocation` del scan BLE no los sustituye. Denegación/revocación debe degradar sin éxito falso. | P0 | E BLE; P GNSS |
| R26-RS-03 | GNSS disponible y habilitado para fix automático; registrar fuente, tiempo y precisión. No depender de mapas descargados ni de geocodificación online para emitir SOS; admitir modo sin ubicación. | P0 | P |
| R26-RS-04 | Definir serializer, esquema/versiones, negociación BLE y fragmentos acotados; validar integridad y mensaje completo antes de pasarlo al núcleo. Cabeceras y protección criptográfica consumen bytes del presupuesto real. | P0 | P |
| R26-RS-05 | Diseñar ejecución y recuperación conforme al ciclo de vida Android, permisos y restricciones energéticas de las versiones ensayadas. Hoy la app detiene recursos al pausar: continuidad con pantalla bloqueada o background requiere trabajo y pruebas independientes. | P0 | P continuidad; E detención |
| R26-RS-06 | Centro futuro con API autenticada, almacenamiento durable/idempotente, control de acceso, auditoría y cartografía con licencia/fuente verificadas. Empezar con servicio de laboratorio, datos ficticios y operador de prueba. | P0 | P |
| R26-RS-07 | Solo el gateway requiere conectividad IP hacia la API y configuración HTTPS/credenciales; red Wi-Fi asociada no prueba Internet útil. El manifiesto actual no declara INTERNET; permisos y cliente de red se implementarán después. | P0 | P |
| R26-RS-08 | LoRa requiere equipo físico externo, alimentación, enlace con el teléfono, plan de radio y revisión de condiciones locales antes de un ensayo. No se presume radio LoRa en un Android común. | P2 | P |
| R26-RS-09 | El host/adaptador debe serializar llamadas a MeshEngine, proveer reloj monotónico e identidad lógica estable y definir handoff asíncrono con backpressure. Callsign o MAC no son credenciales ni identidad estable garantizada. | P0 | S contrato del núcleo; P host BLE |

## Condiciones de aprobación

El responsable revisará estos requisitos y sus prioridades con el equipo. «P0» define la necesidad del MVP conceptual, no obliga a programarlo durante las 28 horas. Las decisiones pendientes de formato, seguridad, cuotas, retención, tiempos GNSS y devolución de confirmaciones se documentan en [04](04-arquitectura.md) y [05](05-factibilidad.md). Esta intervención entrega ingeniería documental, sin implementar los requisitos futuros.
