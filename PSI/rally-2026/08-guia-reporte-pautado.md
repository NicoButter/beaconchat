# 08 · Guía para completar el Reporte Pautado oficial

**Vetrabyte / Nicolás Butterfield · [MIT](../../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Guía del Rally](00-guia-rally.md) · [Pitch](09-storyboard-y-pitch.md)

## Estado de la plantilla

**No se recibió el formato oficial del equipo.** Este archivo reúne insumos y controles; no reproduce una plantilla oficial, no define sus secciones ni reemplaza el PDF exigido. La página de [competencia](https://www.rallydeinnovacion.org/competencia/) indica completar el Reporte Pautado y subirlo en PDF en SIRLI. El equipo debe obtener el archivo vigente de su sede/SIRLI antes de maquetar o exportar.

| Dato de control | Valor |
| --- | --- |
| Archivo / enlace oficial recibido | PENDIENTE F-02 |
| Edición, versión y fecha del formato | PENDIENTE |
| Fuente: SIRLI / coordinación / sede | PENDIENTE |
| Desafío literal e identificador | PENDIENTE F-01 |
| Equipo, integrantes y sede | PENDIENTE F-03 |
| Límites de páginas/campos, anexos y tamaño | PENDIENTE; no inferidos de ediciones anteriores |
| Rúbrica oficial / condiciones de reutilización | PENDIENTE F-02/04 |
| Responsable de completar / revisor del equipo | PENDIENTE |

## Mapa de insumos, sin imponer estructura al formulario

| Si el formato solicita… | Fuente de este paquete | Qué debe completar o producir el equipo |
| --- | --- | --- |
| Problema/desafío | [01 · Problema](01-problema-y-alcance.md) | Transcripción exacta del desafío y relación con beneficiarios reales; el enunciado de Vetrabyte es orientativo |
| Usuarios y necesidad | [01](01-problema-y-alcance.md), [02 · RU](02-requerimientos.md) | Población/contexto, evidencia y barreras verificadas; no inventar entrevistas |
| Solución e innovación | [03 · Casos](03-casos-de-uso.md), [04 · Arquitectura](04-arquitectura.md) | Aporte propio sobre BeaconChat, comparación sustentada y decisiones originales |
| Funcionamiento | [04](04-arquitectura.md) | Adaptar diagrama A→B/C→D→API→centro y explicar sin fix/sin vecinos/sin salida; rótulos E/S/P visibles |
| Viabilidad técnica | [05 · Técnica](05-factibilidad.md), [06 · Pruebas](06-validacion.md) | Resultados realmente obtenidos y limitaciones; indicar qué fue simulado |
| Viabilidad económica / modelo de sostenimiento | [05](05-factibilidad.md) | Cotizaciones y supuestos locales, operación/mantenimiento, moneda/fecha; no presentar costo cero por usar MIT |
| Impacto social | [05](05-factibilidad.md) | Hipótesis frente a evidencia, acceso/inclusión y quién atiende las solicitudes |
| Sustentabilidad ambiental | [05](05-factibilidad.md), [06 · Métricas](06-validacion.md) | Medir energía, recarga, reutilización y residuos; no inventar ahorros |
| Implementación y próximos pasos | [07 · Roadmap](07-roadmap.md) | Alcance que puede asumir el equipo, recursos, responsables y fechas propias |
| Validación / trazabilidad | [06](06-validacion.md), [10](10-trazabilidad.md) | Ensayos con fichas y evidencia, criterios y desvíos; no usar 48 tests como validación de la cadena completa |
| Créditos, fuentes y anexos | [00](00-guia-rally.md), [LICENSE](../../LICENSE) | Atribución Vetrabyte / Nicolás Butterfield, MIT, autores del equipo y licencias de material adicional |

Esta tabla es una ayuda de redacción. El orden, títulos, extensiones y campos finales serán los de la plantilla oficial. No agregar un canvas, una portada, anexos o una sección supuestamente obligatoria sin verificar que correspondan.

## Texto técnico base para adaptar

> Proponemos registrar un SOS con ubicación GNSS cuando esté disponible y transmitirlo mediante teléfonos vecinos hasta un nodo que tenga Internet. Ese gateway lo remitiría por HTTPS a un servicio central para visualización y clasificación humana. Sin fix se admite una ubicación reciente o manual identificada, o la ausencia de ubicación. La cadena requiere enlaces entre vecinos y una salida efectiva; no garantiza entrega ni rescate. La base BeaconChat, de Vetrabyte / Nicolás Butterfield (MIT), ofrece comunicación BLE directa y señales locales en código, más un motor mesh lógico simulado sin integración BLE. El aporte propio del equipo es [COMPLETAR CON HECHOS].

Usarlo solo si corresponde al desafío y a la propuesta original del equipo. No atribuir al equipo la creación de la base tecnológica ni conservar `[COMPLETAR]` en un archivo entregado.

## Revisión previa a la exportación y carga

- [ ] Formato/versión confirmados con sede y campos/límites respetados.
- [ ] Desafío literal, equipo e innovación propia completos; condiciones de reutilización verificadas.
- [ ] Requerimientos y flujos de fallos coherentes con el alcance descrito.
- [ ] E/S/P visibles en textos, capturas y diagramas; mockups y escenarios sintéticos rotulados.
- [ ] Métricas, costos y validaciones sustentados o marcados como pendientes; ninguna entrevista o aprobación inventada.
- [ ] Créditos y avisos de licencia conservados donde corresponde; contenido del equipo identificado.
- [ ] Sin datos personales, coordenadas/notas reales ni credenciales en material público.
- [ ] PDF exportado desde el formato oficial y revisado visualmente: texto legible, diagramas sin cortes, fuentes y enlaces comprobados.
- [ ] Coherencia con video y registro de quién revisó/fecha; carga y nombre de archivo según SIRLI/sede.

La exportación, revisión visual del PDF y carga son tareas posteriores del equipo cuando facilite el formato. Esta intervención no generó un PDF sustituto, no accedió a SIRLI y no publicó entregables.
