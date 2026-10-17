# Ingeniería de software de BeaconChat

**Vetrabyte / Nicolás Butterfield · Licencia [MIT](../LICENSE)**
Estado de esta edición: **borrador para revisión**, 16 de octubre de 2026 (Argentina).

Este índice reúne los antecedentes y la propuesta del [Issue #6](https://github.com/NicoButter/beaconchat/issues/6). Toda la documentación nueva de ingeniería de esta intervención reside en `PSI/`. Los archivos existentes en `docs/` se consultan como evidencia técnica, sin duplicarlos ni modificarlos.

La propuesta central es registrar un SOS con ubicación local, transmitirlo por vecinos de una red mesh y alcanzar un teléfono que tenga Internet en ese momento. Ese teléfono actuaría como gateway hacia un servicio central para un centro de emergencias. **La cadena completa es una propuesta futura**: hoy existen chat BLE directo y señalización local, más un motor mesh lógico probado en simulaciones y desconectado del transporte BLE.

## Antecedentes conservados

Los tres documentos se revisaron y se mantienen íntegros. Su marca histórica «Aprobado» no aprueba esta propuesta ni acredita el comportamiento actual. Sus identificadores RU/RF/CU, versiones y cifras no se trasladan automáticamente a la edición 2026.

| Documento histórico | Valor como antecedente | Diferencias que debe considerar el lector |
| --- | --- | --- |
| [ERS-BeaconChat.md](ERS-BeaconChat.md) | Requerimientos y canales originales | API mínima 26, MTU 512, SharedPreferences, alcances y métricas no acreditadas; el código declara `minSdk 24`, paquetes directos de 20 bytes y DataStore. Contiene timings ópticos contradictorios. |
| [Casos-de-Uso-BeaconChat.md](Casos-de-Uso-BeaconChat.md) | Actores, SOS local y escenarios de recepción | Incluye continuidad en background, reinicio tras crash y otros flujos sin evidencia de implementación. «Mesh» describe principalmente proximidad; no prueba multisalto. |
| [Diseño-Arquitectura-BeaconChat.md](Diseño-Arquitectura-BeaconChat.md) | Organización inicial y decisiones de diseño | Describe MainActivity como ViewModel y métricas de calidad sin medición verificable; la vista nueva parte de clases reales y separa núcleo lógico, hardware y futuro backend. |

Las referencias históricas IEEE/NIST/ITU, el rótulo «Confidencial» y las cifras de rendimiento no constituyen certificación, licencia nueva ni resultados de ensayo. Para reutilización rige el archivo [LICENSE](../LICENSE), cuyo aviso de copyright se preserva.

## Paquete Rally 2026

| Documento | Contenido |
| --- | --- |
| [00 · Guía del Rally](rally-2026/00-guia-rally.md) | Fuentes, entregables, autoría, licencia, faltantes y plantilla oficial |
| [01 · Problema y alcance](rally-2026/01-problema-y-alcance.md) | Objetivos, usuarios, escenarios, matriz AS-IS/TO-BE |
| [02 · Requerimientos](rally-2026/02-requerimientos.md) | RU, RF, RNF y RS priorizados, estados y aceptación |
| [03 · Casos de uso](rally-2026/03-casos-de-uso.md) | Actores, flujos normales, errores y diagramas Mermaid |
| [04 · Arquitectura](rally-2026/04-arquitectura.md) | Arquitectura actual y futura, secuencias, estados y datos |
| [05 · Factibilidad](rally-2026/05-factibilidad.md) | Técnica, económica, social, ambiental, riesgos y privacidad |
| [06 · Validación](rally-2026/06-validacion.md) | Evidencia existente, plan de pruebas y métricas futuras |
| [07 · Roadmap](rally-2026/07-roadmap.md) | Rally/PoC, mesh real, crisis y centro de operaciones |
| [08 · Guía del Reporte Pautado](rally-2026/08-guia-reporte-pautado.md) | Insumos para completar el formato oficial del equipo |
| [09 · Storyboard y pitch](rally-2026/09-storyboard-y-pitch.md) | Guion adaptable de hasta dos minutos y checklist TikTok |
| [10 · Trazabilidad](rally-2026/10-trazabilidad.md) | RU ↔ RF ↔ CU ↔ prueba ↔ etapa; RNF y RS |
| [Informe de revisión](rally-2026/INFORME-REVISION.md) | Alcance del cambio, comprobaciones y decisiones pendientes |

## Cómo leer los estados

- **E — Existente en código:** comportamiento identificable en `main`; no implica validación física.
- **S — Simulado:** lógica existente ejercitada mediante transporte/topologías JVM; no es mesh operativo en teléfonos.
- **P — Propuesto:** requiere diseño, implementación y validación posteriores. Un diagrama, mockup o guion no implementa el sistema.

Base local inspeccionada: `main`, commit `2e754e315a40533e9b3f61a0a2a292d2db9b96a2`. El Issue #6 fue consultado en su actualización `2026-10-17T00:56:57Z`, equivalente al 16 de octubre en Argentina. Prevalece la instrucción del responsable para esta intervención: trabajar directamente en `main`, sin rama/worktree, commit, push, PR ni publicación. Esta edición no modifica código, manifiesto, dependencias ni los antecedentes.
