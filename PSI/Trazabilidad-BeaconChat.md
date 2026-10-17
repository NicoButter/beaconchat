# 10 · Trazabilidad de la propuesta

**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Requerimientos](ERS-BeaconChat.md) · [Casos](Casos-de-Uso-BeaconChat.md) · [Pruebas](Validacion-BeaconChat.md) · [Etapas](Roadmap-BeaconChat.md)

## Convención

Se omite `R26-` en las celdas compactas: `RU-01` significa `R26-RU-01`, `PR-01` significa `R26-PR-01`, etc. Los IDs pertenecen a esta edición. Las relaciones pueden recorrerse en ambas direcciones filtrando las columnas; son cobertura documental, no porcentaje de implementación ni pruebas aprobadas. E/S/P se definen en [PSI/README](../README.md).

## Matriz RU ↔ RF ↔ CU ↔ prueba ↔ etapa

| RF | RU de origen | CU relacionado | Prueba / criterio verificable | Etapa | Estado / evidencia o componente |
| --- | --- | --- | --- | --- | --- |
| RF-01 | RU-03, RU-08 | CU-07, CU-08 | PR-01 | E0 | E: DirectBleProtocol, BLEMeshController; 17 JVM históricos reportados, físico pendiente |
| RF-02 | RU-08 | CU-08 | PR-03 | E0 | E: EmergencyManager, emisores, receptores y QR; ensayo por canal pendiente |
| RF-03 | RU-02 | CU-03 | PR-02 | E0 | S: MeshEngine, MeshMessage y SimulatedNetwork; 31 JVM históricos reportados |
| RF-04 | RU-01 | CU-01, CU-02 | PR-04 | E1 diseño, E3 implementación | P: registro SOS y validación de carga |
| RF-05 | RU-01 | CU-01 | PR-05 | E3 | P: proveedor GNSS de incidentes |
| RF-06 | RU-01 | CU-02 | PR-06 | E3 | P: fallback reciente/manual/sin ubicación |
| RF-07 | RU-02 | CU-03 | PR-07, PR-08, PR-09 | E2 | P: transporte mesh BLE real y protocolo versionado |
| RF-08 | RU-03 | CU-01, CU-04, CU-06, CU-07 | PR-10, PR-11, PR-15 | E3; recuperación E4 | P: estados y retorno de comprobante central |
| RF-09 | RU-04 | CU-04, CU-06 | PR-10 | E3; reintento durable E4 | P: gateway dinámico HTTPS |
| RF-10 | RU-04, RU-05 | CU-04, CU-05, CU-06 | PR-11 | E3 | P: API, idempotencia y confirmación persistente |
| RF-11 | RU-05 | CU-02, CU-05 | PR-12 | E3 | P: mapa/lista, filtros y clasificación humana |
| RF-12 | RU-06 | CU-03, CU-04, CU-06 | PR-09, PR-10, PR-13 | E4 | P: custodia durable, cuotas, expiración y reintentos |
| RF-13 | RU-07 | CU-01, CU-03, CU-04, CU-05, CU-09 | PR-14, PR-15 | E3–E5 | P: privacidad, permisos, autenticación, protección y roles |
| RF-14 | RU-09 | CU-10 | PR-17 | E4–E5 | P: recursos, brigadistas, rutas y prioridad asistida humana |
| RF-15 | RU-09 | CU-10 | PR-18 | E5 opcional | P: LoRa con dispositivo/gateway externo |

Los componentes actuales enlazados y sus límites están en [04 · Fuentes](Diseño-Arquitectura-BeaconChat.md). Un caso de uso con varias funciones E/S/P no hereda el estado más avanzado: CU-03 solo tiene lógica S y no tránsito físico; CU-07 integral sigue P aunque use RF-01 como antecedente de confirmación local.

## Cobertura de atributos y restricciones

| RNF / RS | RF al que restringe o da soporte | CU de observación | Prueba | Etapa y estado |
| --- | --- | --- | --- | --- |
| RNF-01 | RF-08, RF-09, RF-10 | CU-04, CU-05, CU-07 | PR-10, PR-11, PR-15 | E3 / P |
| RNF-02 | RF-04, RF-05, RF-06, RF-08, RF-13 | CU-01, CU-02, CU-07, CU-09 | PR-15 | E3 / P |
| RNF-03 | RF-05, RF-07, RF-09, RF-10 | CU-01, CU-03, CU-04 | PR-05, PR-08 | E2–E3 / P; meta de PoC |
| RNF-04 | RF-03, RF-07, RF-12 | CU-03, CU-06 | PR-02, PR-07, PR-09, PR-16 | E2–E4 / P integral; antecedente S |
| RNF-05 | RF-13 | CU-09 | PR-14 | E3–E5 / P |
| RNF-06 | RF-07, RF-09, RF-10, RF-13 | CU-03, CU-04, CU-05, CU-09 | PR-14 | E3–E5 / P |
| RNF-07 | RF-01, RF-07 | CU-03, CU-08 | PR-01, PR-07 | E2 / P integral |
| RNF-08 | RF-10, RF-12 | CU-04, CU-06 | PR-11, PR-13 | E4 / P |
| RNF-09 | RF-01, RF-03, RF-07, RF-10 | CU-03, CU-05, CU-08 | PR-19 | E1 y cada cambio futuro / P continuo |
| RNF-10 | RF-02, RF-05, RF-07, RF-12 | CU-01, CU-03, CU-06, CU-08 | PR-16 | E2–E4 / P |
| RS-01 | RF-01, RF-02, RF-07 | CU-03, CU-08 | PR-01, PR-03, PR-08 | E0 configuración E; compatibilidad futura P |
| RS-02 | RF-01, RF-05, RF-06, RF-13 | CU-01, CU-02, CU-08, CU-09 | PR-01, PR-05, PR-06, PR-15 | E0 BLE E; E3 GNSS P |
| RS-03 | RF-05, RF-06 | CU-01, CU-02 | PR-05, PR-06 | E3 / P |
| RS-04 | RF-07 | CU-03 | PR-07 | E2 / P |
| RS-05 | RF-01, RF-02, RF-07, RF-12 | CU-03, CU-06, CU-08 | PR-01, PR-08, PR-13, PR-16 | E2–E4 continuidad P; pausa actual E |
| RS-06 | RF-10, RF-11, RF-13 | CU-05, CU-09 | PR-11, PR-12, PR-14 | E3–E5 / P |
| RS-07 | RF-09 | CU-04, CU-06 | PR-10 | E3 / P |
| RS-08 | RF-15 | CU-10 | PR-18 | E5 opcional / P |
| RS-09 | RF-03, RF-07 | CU-03 | PR-02, PR-07, PR-09 | E0 contrato S; E2 host BLE P |

## Escenarios críticos del Issue #6

| Escenario / condición | Requerimientos | Caso y prueba | Cobertura documental |
| --- | --- | --- | --- |
| SOS con posición local sin Internet en origen | RU-01, RF-04, RF-05, RS-03 | CU-01 / PR-04, PR-05 | 01, 02, 03, 04, 06 |
| GNSS ausente, dato viejo, manual o sin ubicación | RF-06, RNF-02, RS-02 | CU-02 / PR-06, PR-15 | 01, 03, 04, 06 |
| A→B/C→D, gateway distinto de retransmisor | RU-02, RU-04, RF-07, RF-09, RS-07 | CU-03, CU-04 / PR-08, PR-10 | 01, 03, 04 |
| Sin vecinos o sin gateway | RF-08, RF-12, RNF-01 | CU-03, CU-06, CU-07 / PR-09, PR-13, PR-15 | 03, 04, 06 |
| Gateway pierde conectividad, dos gateways, ACK perdido | RF-08, RF-09, RF-10, RNF-08 | CU-04, CU-05, CU-07 / PR-10, PR-11 | 03, 04, 06 |
| Recuperar conexión y reinicio con persistencia futura | RU-06, RF-12 | CU-06 / PR-13 | 03, 04, 06, 07; expresamente P |
| Privacidad, falsos SOS y acceso restringido | RU-07, RF-13, RNF-05, RNF-06 | CU-09 / PR-14 | 02, 04, 05, 06 |
| Clasificación humana, mapas y funciones de crisis | RU-05, RU-09, RF-11, RF-14 | CU-05, CU-10 / PR-12, PR-17 | 01, 04, 07 |
| LoRa opcional externo | RF-15, RS-08 | CU-10 / PR-18 | 01, 04, 05, 07 |
| Código heredado, antecedentes intactos y Rally propio | RNF-09 | PR-19 | PSI/README, 00, 08, 09 e informe |

## Control de cobertura y cambios

Esta edición define **9 RU, 15 RF, 10 RNF y 9 RS** (43 requerimientos), **10 CU**, **19 pruebas catalogadas** y **6 etapas** (E0–E5). Los 15 RF tienen al menos RU, CU, prueba y etapa; RNF/RS tienen vínculos adicionales. PR-19 verifica documentación, no funcionalidades Android.

Para una revisión posterior: actualizar primero el estado/evidencia del componente, después los requisitos y casos, luego pruebas/roadmap y finalmente esta matriz. Una función pasa de P/S a E solo con código identificable; su validación física se registra por separado. No cerrar pendientes por cambiar rótulos o por contar enlaces como si fueran tests aprobados.

Las decisiones F-01–F-07 están en [00](Guia-Rally-2026.md); los riesgos K-01–K-12 en [05](Factibilidad-BeaconChat.md). Ningún requisito aquí reemplaza el desafío literal ni declara aprobado el uso operativo o la publicación.
