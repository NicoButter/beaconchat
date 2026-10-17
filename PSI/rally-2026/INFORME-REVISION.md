# Informe para revisión de la documentación · Issue #6

**Vetrabyte / Nicolás Butterfield · [MIT](../../LICENSE) · Borrador para revisión · 16/10/2026**

[Índice general](../README.md) · [Guía del paquete](00-guia-rally.md)

## Resultado y alcance

Se preparó el paquete documental del [Issue #6 actualizado](https://github.com/NicoButter/beaconchat/issues/6): **13 archivos Markdown nuevos**, exclusivamente dentro de `PSI/`: el índice general, los once documentos numerados 00–10 y este informe. Está listo para revisión del responsable; **no se publicó** ni se registra aprobación editorial o del equipo del Rally.

La propuesta explica la cadena **persona con SOS y ubicación opcional → vecinos mesh → nodo móvil con Internet → API → centro de emergencias**. Distingue la posición GNSS local de una conexión de datos, retransmisor de gateway, y confirmación GATT de recepción central, lectura y atención. Incluye degradación sin fix, sin vecinos, sin salida o con respuesta perdida.

Se trabajó directamente en `main`, con HEAD conservado en `2e754e315a40533e9b3f61a0a2a292d2db9b96a2`. La instrucción del responsable para esta intervención prevaleció sobre la rama/PR que mencionaba el issue. **No se crearon ramas ni worktrees, no se modificó código, dependencias, manifiesto ni archivos preexistentes, y no se hizo commit, push, PR, comentario al issue ni carga a servicios externos.** Los cambios nuevos permanecen sin agregar al índice de Git.

## Entregables revisables

| Archivo | Contenido principal |
| --- | --- |
| [PSI/README.md](../README.md) | Índice de ingeniería, antecedentes y diferencias con el código actual |
| [00 · Guía del Rally](00-guia-rally.md) | Fuentes oficiales consultadas, entregables, autoría/MIT y faltantes |
| [01 · Problema y alcance](01-problema-y-alcance.md) | Objetivos, usuarios, escenarios y matriz AS-IS/TO-BE |
| [02 · Requerimientos](02-requerimientos.md) | 9 RU, 15 RF, 10 RNF y 9 RS, con prioridades/estados y criterios |
| [03 · Casos de uso](03-casos-de-uso.md) | 10 casos con actores, precondiciones, pasos, errores y aceptación |
| [04 · Arquitectura](04-arquitectura.md) | Fuentes de código, arquitectura actual/futura, secuencias, datos, estados y decisiones pendientes |
| [05 · Factibilidad](05-factibilidad.md) | Evaluación condicionada técnica/económica/social/ambiental, presupuesto por completar y 12 riesgos |
| [06 · Validación](06-validacion.md) | Evidencia histórica delimitada, 19 pruebas catalogadas, protocolo y métricas futuras |
| [07 · Roadmap](07-roadmap.md) | 6 etapas E0–E5, puertas de salida, responsabilidades propuestas y recortes |
| [08 · Reporte Pautado](08-guia-reporte-pautado.md) | Mapa de insumos para la plantilla oficial, sin inventar formato ni PDF |
| [09 · Storyboard/pitch](09-storyboard-y-pitch.md) | Guion adaptable de 115 s, margen de 5 s, checklist de video/TikTok y créditos |
| [10 · Trazabilidad](10-trazabilidad.md) | RU ↔ RF ↔ CU ↔ prueba ↔ etapa; vínculos RNF/RS y escenarios del issue |

## Revisión de los tres antecedentes

Se leyeron los tres documentos completos y se preservaron byte por byte. Se señalan sus diferencias desde el índice nuevo, sin reescribir su historia ni sustituirlos como documentos aprobados de otra época.

| Antecedente | Hallazgo relevante para esta edición |
| --- | --- |
| [ERS](../ERS-BeaconChat.md) | Mezcla timings ópticos; menciona API 26/MTU 512/SharedPreferences y cifras de alcance/rendimiento no acreditadas. La base actual declara API mínima 24, límite directo 20 bytes y DataStore. |
| [Casos de uso](../Casos-de-Uso-BeaconChat.md) | Incluye background, reinicio automático y otros escenarios sin respaldo actual; «mesh» no prueba reenvío físico. El nuevo catálogo usa IDs R26 independientes. |
| [Diseño y arquitectura](../Diseño-Arquitectura-BeaconChat.md) | Presenta MainActivity como ViewModel y métricas sin medición verificable. La vista nueva identifica clases reales y la separación del núcleo lógico respecto del BLE. |

Sus referencias a IEEE/NIST/ITU y rótulos «Aprobado»/«Confidencial» no se convierten en certificaciones, aprobación de la propuesta ni restricciones de licencia nuevas. Se conserva [LICENSE](../../LICENSE), incluido `Copyright (c) 2025 Nicolás Butterfield`.

## Separación del estado real

| Estado | Qué se documenta |
| --- | --- |
| **E · Existente en código** | Chat BLE directo GATT con 20 bytes totales; señalización SOS, óptica/Morse, vibración/audio, QR y beacon BLE; preferencias en DataStore. Validación física insuficiente, A↔B pendiente. |
| **S · Simulado** | MeshEngine Kotlin con identidad lógica, TTL, dedupe y cola temporal; topologías JVM, sin integración BLE. Un intento por item, sin persistencia ni reintentos durables. |
| **P · Propuesto** | GNSS de incidentes, SOS rico, serializer/fragmentos, mesh BLE real, custodia durable/DTN, gateway HTTPS, API/base central, mapa/clasificación, seguridad integral y funciones de crisis. LoRa únicamente como opción con radio/gateway externo. |

Los **48 tests** se citan como resultado histórico reportado y contienen los **31** del núcleo. No se ejecutaron nuevamente ni se presentan como pruebas de campo, GNSS, gateway, centro o rescate. Las métricas cuantitativas de PoC están rotuladas como metas de laboratorio pendientes, no resultados.

## Comprobaciones de esta intervención

| Comprobación | Resultado |
| --- | --- |
| Consulta del Issue #6 | Título/cuerpo actualizado y comentarios consultados; actualización `2026-10-17T00:56:57Z`, sin comentarios. Fecha local 16/10/2026. |
| Fuentes del Rally | Competencia y convocatoria ANFEI 2026 recuperadas mediante herramientas web; la página de edición 2026 no se recuperó. Plantilla y rúbrica oficial específicas siguen pendientes. Referencias y límites en 00. |
| Integridad del alcance | Solo altas Markdown en `PSI/`; `git diff HEAD` sin cambios en archivos versionados; rama y HEAD conservados, sin cambios staged. |
| Preservación | Hash Git de cada antecedente y LICENSE igual al blob de HEAD; valores abajo. |
| Enlaces y formato | Enlaces locales resueltos, fences equilibrados y comprobación de whitespace sobre todos los archivos nuevos, incluidos los no rastreados. |
| Cobertura | IDs definidos: 9 RU, 15 RF, 10 RNF, 9 RS, 10 CU y 19 PR. Los 15 RF tienen RU/CU/prueba/etapa; RNF/RS y todos los CU/PR figuran en la matriz. Sin referencias a IDs inexistentes en ella. |
| Mermaid | **9 de 9** bloques parseados y renderizados con Mermaid **11.12.0** en Chromium headless; imágenes inspeccionadas, con revisión ampliada de actividad y secuencia normal. Se mejoró disposición y ajuste de etiquetas. |
| Código / pruebas de producto | Sin cambios y sin nueva ejecución Gradle, JVM, APK, instrumentación ni hardware. Esto verifica documentación, no valida el sistema futuro. |

Para QA se utilizó un paquete temporal de Mermaid en `/tmp` y Playwright ya disponible en el entorno. No se añadieron paquetes, lockfiles ni dependencias al proyecto. Los renders e instrumentos temporales no forman parte de los entregables; los diagramas fuente permanecen en los Markdown de PSI. La renderización verificada no supone una exportación PDF de la plantilla oficial ni garantiza idéntico layout en otra versión de Mermaid.

Blobs Git preservados:

| Archivo | Hash igual a HEAD |
| --- | --- |
| `PSI/ERS-BeaconChat.md` | `89d38ea7b152df740854e03188e7b3a82417de17` |
| `PSI/Casos-de-Uso-BeaconChat.md` | `1ea1d04a88947f8148953c40dfea1e7e0db06759` |
| `PSI/Diseño-Arquitectura-BeaconChat.md` | `4056840826a9646ce78c3c76cb70208561d71ddb` |
| `LICENSE` | `0fae33f0a3717a9eb16beec611b7dc639e1e72b4` |

## Puntos para decidir antes de publicar o adaptar

1. **Encaje con el Rally:** completar desafío literal, equipo y aporte original. El concepto de Vetrabyte no sustituye el enunciado oficial ni el trabajo propio estudiantil.
2. **Formato/reglas:** obtener plantilla y rúbrica oficial, y confirmar condiciones de reutilización/atribución y divulgación con la coordinación. El paquete no contiene un Reporte Pautado oficial ni un video publicado.
3. **Alcance y prioridades:** revisar RU/RF/RNF/RS y etapas; decidir cuánto asumirá el equipo sin presentar funciones P como disponibles.
4. **Diseño pendiente:** aprobar esquema, límites de fragmentación/reensamblado, retorno de comprobantes, cuotas, espera GNSS, criterio de posición reciente, retención y política durable/TTL antes de implementarlos.
5. **Factibilidad:** incorporar costos locales sustentados, validación física/usuarios y responsable de operación/datos del centro. No existen esos resultados ni acuerdos en esta entrega.
6. **Comunicación pública:** conservar Vetrabyte / Nicolás Butterfield y avisos MIT, declarar la base heredada y aportes propios, y usar únicamente datos sintéticos en diagramas/capturas públicas.

Recomendación de lectura: índice → 01/04 para evaluar la propuesta → 02/03/10 para revisar consistencia → 05/06/07 para valorar viabilidad y límites → 00/08/09 para preparar material propio del equipo. La documentación queda local, disponible para tu revisión antes de cualquier publicación.
