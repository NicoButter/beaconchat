# 07 · Roadmap y puertas de validación

**Vetrabyte / Nicolás Butterfield · [MIT](../../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Validación](06-validacion.md) · [Trazabilidad](10-trazabilidad.md)

## Etapas propuestas

Las etapas son dependencias técnicas y de evidencia, no fechas prometidas ni trabajo de código autorizado por este paquete. Solo E0 representa una base existente; E1 se entrega como documentación para revisión. E2–E5 están pendientes. Roles y esfuerzo deben acordarse con el equipo, sin asignar compromisos a organizaciones no consultadas.

| Etapa | Resultado y alcance | Dependencias | Puerta de salida / pruebas |
| --- | --- | --- | --- |
| **E0 · Base actual** | BLE directo E, señales locales E y núcleo mesh S; conservar fuentes/evidencia | Commit base y antecedentes revisados | Lectura de PR-01/02 históricos; aceptar que A↔B y canales físicos siguen pendientes PR-01/03 |
| **E1 · Rally y PoC conceptual** | Documentos 00–10, diagrama completo y fallos; storyboard, insumos del reporte y simulación existente rotulada | Desafío/plantilla/aporte del equipo F-01–04 para adaptación final | PR-19 documental; equipo revisa y produce sus piezas propias. No afirmar demo mesh física |
| **E2 · Mesh BLE real** | Validar A↔B; definir formato/identidad, negociar versión, fragmentar/reensamblar y adaptar núcleo; cadena de teléfonos | PR-01 física aprobada; límites y protocolo acordados; política Android por versión | PR-07/08 en componente de tránsito, PR-09/16; sin mezcla con chat legado y sin bucles. La parte gateway de PR-08 se completa en E3 |
| **E3 · SOS + gateway + centro de laboratorio** | Registro SOS, GNSS/fallback, comprobantes, gateway dinámico HTTPS, API idempotente y mapa/lista con clasificación humana | E2; esquema, seguridad de laboratorio y roles definidos; datos sintéticos | PR-04/05/06/08/10/11/12/14/15; cadena completa y fallos sin confirmaciones falsas. Retención/borrado de datos de ensayo establecidos |
| **E4 · Resiliencia y funciones de crisis** | Custodia durable/DTN, reconexión/reinicio, cuotas y expiración; primeros reportes de rutas/brigadistas/recursos | E3; política de reintento/TTL, almacenamiento y datos acordada | PR-09/13/16 y PR-17 de mesa; mensajes vigentes recuperables sin duplicar centro; funciones de crisis revisadas por humanos |
| **E5 · Piloto de centro de operaciones y extensiones** | Operación acordada, gestión de recursos/estados, validación social y de seguridad; LoRa externo opcional | E3/E4 verificadas; F-05/06/07, presupuesto, responsables y condiciones de datos/operación resueltos | Revisión integral PR-10–17; PR-18 solo si se añade LoRa. Piloto controlado antes de cualquier uso operativo |

El centro de E3 usa datos sintéticos y no declara conexión con servicios oficiales. E5 requiere responsable institucional y protocolos de atención; no se alcanza con desplegar un dashboard. El MVP conceptual incluye clasificación humana; prioridad asistida, rutas y brigadistas son diferenciadores posteriores. No se presupone IA de triage.

## Organización del esfuerzo durante el Rally

Propuesta de distribución relativa, **no agenda oficial** de la sede: dentro del tiempo disponible, primero identificar desafío, equipo y material heredado; después definir problema, usuarios y aporte propio; luego contrastar factibilidad y evidencia; finalmente adaptar el formato, grabar/revisar piezas y preparar las cargas. Reservar desde el inicio tiempo para la interacción internacional y para verificar enlaces/duración.

El paquete sirve para explicar una cadena futura con simulaciones identificadas. Si solo hay lógica JVM, presentar esa evidencia con rótulo S y usar diagramas P para la cadena. No es un objetivo obligatorio implementar E2–E5 durante las 28 horas ni se trata esta edición como si lo hubiera hecho.

## Hitos verificables y criterios de recorte

1. **Antes de integrar mesh:** aprobar A↔B físico; si falla, investigar compatibilidad/permisos/ciclo de vida y mantener la propuesta como conceptual.
2. **Antes de prometer multisalto:** demostrar que extremos aislados dependen de los intermedios y que la ruptura impide entrega. Tests JVM no habilitan esa afirmación física.
3. **Antes de mostrar recepción central:** persistencia idempotente y comprobante correlacionado; si falta retorno a A, su UI conserva «sin confirmar».
4. **Antes de declarar recuperación tras cortes:** ensayo durable tras reinicio, cuotas y vencimiento. La cola FIFO actual no satisface este hito.
5. **Antes de incorporar personas/datos reales:** revisar seguridad, permisos, minimización, retención, acceso y responsable operativo. Resolver cómo se verifica/atiende una solicitud.
6. **Antes de ampliar con LoRa o funciones de crisis:** la cadena BLE/HTTPS básica debe estar validada; cada extensión conserva identidad, límites y trazabilidad.

## Responsables propuestos y decisiones pendientes

| Frente | Rol propuesto | Decisión necesaria |
| --- | --- | --- |
| Ingeniería base / autoría | Vetrabyte / Nicolás Butterfield | Revisar esta propuesta, estado del repositorio y crédito del código existente |
| Competencia y originalidad | Equipo participante | Desafío, aporte propio, plantilla, rúbrica y condiciones de sede |
| App / mesh / GNSS | Desarrollo Android e integración | Protocolo, fragmentos, capacidades, permisos, continuidad y consumo |
| Gateway / centro | Backend y UX de operaciones | Idempotencia, comprobantes/retorno, roles, mapas y clasificación humana |
| Seguridad / datos | Responsable a designar con institución | Amenazas, claves, consentimiento/modo emergencia, retención y auditoría |
| Validación / sostenibilidad | Pruebas y equipo | Ensayos reproducibles, costos sustentados, usuarios, energía y mantenimiento |

La revisión de la documentación precede a cualquier publicación. En esta intervención se trabaja sobre `main` por pedido del responsable, con cambios locales sin commit/push/PR. Publicar piezas del Rally o el repositorio queda fuera de esta entrega.
