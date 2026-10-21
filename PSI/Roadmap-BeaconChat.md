# 07 · Roadmap y puertas de validación

**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · Actualizado 21/10/2026**
[Índice](../README.md) · [Validación](Validacion-BeaconChat.md) · [Trazabilidad](Trazabilidad-BeaconChat.md)

## Etapas propuestas

Las etapas expresan dependencias y evidencia, no fechas prometidas. E0 es la base; E2 tiene adaptador Android/codec/fragmentos implementados y E3 tiene SOS/GPS local, ensayos sintéticos y gateway HTTPS, sin cerrar las puertas físicas ni de seguridad. Backend/centro E3 y E4/E5 siguen propuestos. No se asignan compromisos a organizaciones no consultadas.

## Estado tras implementación #7A

Resultado actual: **235 tests JVM aprobados (225 de base + 10 nuevos BLE)**, con APK debug en Gradle 8.13/JDK 21. #7A corrige sesiones entrantes y deadlines, agrega mediciones sin payload y prepara herramientas/procedimiento físico. Las duraciones de 1,6–40,5 s reportadas son simuladas; ADB sin dispositivos, A↔B/A→B→C **NO EJECUTADOS**. Gateway HTTPS Android integrado y apagado por defecto, recibe SOS sintéticos mesh aceptados, deduplica UUID/contenido, administra cola volátil y valida comprobante correlacionado de API falsa. GNSS real sigue bloqueado. No hay validación física BLE/GNSS/HTTPS ni backend real. [Contrato y evidencia](Validacion-BeaconChat.md).

| Etapa | Resultado y alcance | Dependencias | Puerta de salida / pruebas |
| --- | --- | --- | --- |
| **E0 · Base actual** | BLE directo E, señales locales E y núcleo mesh S; conservar fuentes/evidencia | Commit base y antecedentes revisados | Lectura de PR-01/02 históricos; aceptar que A↔B y canales físicos siguen pendientes PR-01/03 |
| **E1 · Rally y PoC conceptual** | Documentos 00–10, diagrama completo y fallos; storyboard, insumos del reporte y simulación existente rotulada | Desafío/plantilla/aporte del equipo F-01–04 para adaptación final | PR-19 documental; equipo revisa y produce sus piezas propias. No afirmar demo mesh física |
| **E2 · Mesh BLE real** | Android/identidad declarada/fragmentos/colas implementados y simulados; falta validar A↔B y cadena con extremos aislados | PR-01 física, permisos/ciclo de vida y límites por fabricante | PR-07 simulación aprobada; PR-08/09/16 físicos pendientes; no atribuir radio a JVM |
| **E3 · SOS + gateway + centro de laboratorio** | SOS/codec/GPS y fallback local implementados; mesh solo sintético/sin posición. Gateway HTTPS/cola/dedupe y validador de comprobantes implementados y simulados; API real, mapas, cuentas y retorno al origen pendientes | Ensayos GNSS/BLE y protección de datos antes de tránsito real; sistema central requiere diseño posterior | PR-04 y subconjuntos de PR-10/11 simulados; PR-05/06 y BLE/HTTPS físicos pendientes. API/idempotencia real y PR-12/14/15 integrales pendientes |
| **E4 · Resiliencia y funciones de crisis** | Custodia durable/DTN, reconexión/reinicio, cuotas y expiración; primeros reportes de rutas/brigadistas/recursos | E3; política de reintento/TTL, almacenamiento y datos acordada | PR-09/13/16 y PR-17 de mesa; mensajes vigentes recuperables sin duplicar centro; funciones de crisis revisadas por humanos |
| **E5 · Piloto de centro de operaciones y extensiones** | Operación acordada, gestión de recursos/estados, validación social y de seguridad; LoRa externo opcional | E3/E4 verificadas; F-05/06/07, presupuesto, responsables y condiciones de datos/operación resueltos | Revisión integral PR-10–17; PR-18 solo si se añade LoRa. Piloto controlado antes de cualquier uso operativo |

El centro de E3 usa datos sintéticos y no declara conexión con servicios oficiales. E5 requiere responsable institucional y protocolos de atención; no se alcanza con desplegar un dashboard. El MVP conceptual incluye clasificación humana; prioridad asistida, rutas y brigadistas son diferenciadores posteriores. No se presupone IA de triage.

## Organización del esfuerzo durante el Rally

Propuesta de distribución relativa, **no agenda oficial** de la sede: dentro del tiempo disponible, primero identificar desafío, equipo y material heredado; después definir problema, usuarios y aporte propio; luego contrastar factibilidad y evidencia; finalmente adaptar el formato, grabar/revisar piezas y preparar las cargas. Reservar desde el inicio tiempo para la interacción internacional y para verificar enlaces/duración.

El paquete sirve para explicar una cadena futura con simulaciones identificadas. Si solo hay lógica JVM, presentar esa evidencia con rótulo S y usar diagramas P para la cadena. No es un objetivo obligatorio implementar E2–E5 durante las 28 horas ni se trata esta edición como si lo hubiera hecho.

## Hitos verificables y criterios de recorte

1. **Antes de declarar mesh físico:** validar A↔B y luego A→B→C con aislamiento; el código ya está integrado, pero sus pruebas JVM no cierran esa puerta.
2. **Antes de prometer multisalto:** demostrar que extremos aislados dependen de los intermedios y que la ruptura impide entrega. Tests JVM no habilitan esa afirmación física.
3. **Antes de mostrar recepción central:** persistencia idempotente y comprobante correlacionado; si falta retorno a A, su UI conserva «sin confirmar».
4. **Antes de declarar recuperación tras cortes:** ensayo durable tras reinicio, cuotas y vencimiento. La cola FIFO actual no satisface este hito.
5. **Antes de transmitir coordenadas reales:** resolver autenticación/cifrado, minimización, retención y acceso; mantener el bloqueo actual y datos sintéticos. Probar GPS offline al aire libre/interior, permiso preciso/aproximado, denegación/revocación, timeout y cancelación sin fondo. Capturar localmente no autoriza tránsito ni operación de rescate.
6. **Antes de ampliar con LoRa o funciones de crisis:** la cadena BLE/HTTPS básica debe estar validada; cada extensión conserva identidad, límites y trazabilidad.

## Responsables propuestos y decisiones pendientes

| Frente | Rol propuesto | Decisión necesaria |
| --- | --- | --- |
| Ingeniería base / autoría | Vetrabyte / Nicolás Butterfield | Revisar esta propuesta, estado del repositorio y crédito del código existente |
| Competencia y originalidad | Equipo participante | Desafío, aporte propio, plantilla, rúbrica y condiciones de sede |
| App / mesh / GNSS | Desarrollo Android e integración | Protocolo, fragmentos, capacidades, permisos, continuidad y consumo |
| Gateway / centro | Gateway sintético ya integrado; falta backend y UX de operaciones | API DRF con transacción/UUID único, autenticación, prueba TLS/teléfonos, comprobante estable/retorno, roles, mapas y clasificación humana |
| Seguridad / datos | Responsable a designar con institución | Amenazas, claves, consentimiento/modo emergencia, retención y auditoría |
| Validación / sostenibilidad | Pruebas y equipo | Ensayos reproducibles, costos sustentados, usuarios, energía y mantenimiento |

La revisión de la documentación precede a cualquier publicación. En esta intervención se trabaja sobre `main` por pedido del responsable, con cambios locales sin commit/push/PR. Publicar piezas del Rally o el repositorio queda fuera de esta entrega.
