# 00 · Guía de uso para el Rally 2026

**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice de ingeniería](../README.md) · [Trazabilidad](Trazabilidad-BeaconChat.md)

## Propósito y fuentes

Este paquete es material de referencia de Vetrabyte para analizar una propuesta SOS + GNSS + mesh + gateway. El equipo participante debe elaborar su propia solución al desafío elegido y declarar sus aportes sobre BeaconChat. No constituye una entrega oficial del equipo ni acredita su elegibilidad.

| Fuente | Consulta y uso |
| --- | --- |
| [Issue #6 actualizado](https://github.com/NicoButter/beaconchat/issues/6) | Consultado el 16/10/2026; actualización `2026-10-17T00:56:57Z`, sin comentarios. Define el paquete 00–10 y el concepto central. |
| [La Competencia](https://www.rallydeinnovacion.org/competencia/) | Información oficial recuperada mediante búsqueda el 16/10/2026; apertura directa intermitente. Resume entregables y dinámica. |
| [ANFEI · convocatoria de sedes 2026](https://www.anfei.mx/actividades/registro_sedes_2026/) | Publicada el 08/07/2026 y consultada el 16/10/2026; confirma edición y participación internacional. |
| [Edición 2026](https://www.rallydeinnovacion.org/edicion_2026/) | Referencia indicada por ANFEI; no se pudo recuperar su contenido. Verificación específica pendiente con la sede. |
| Código de `main` y [fuentes técnicas](Diseño-Arquitectura-BeaconChat.md) | Base local `2e754e315a40533e9b3f61a0a2a292d2db9b96a2`; sustenta qué existe. |

La edición está anunciada para el **16 y 17 de octubre de 2026**, durante 28 horas. La página de competencia indica un video YouTube de **hasta dos minutos**, el **Reporte Pautado en PDF cargado en SIRLI** y una interacción internacional lúdica entre equipos vinculados, plasmada en TikTok de **hasta un minuto**. ANFEI 2026 señala su carácter obligatorio y la etiqueta `#RallyLatam26`. Confirmar horarios, accesibilidad de enlaces y condiciones de sede en SIRLI. [Competencia](https://www.rallydeinnovacion.org/competencia/), [ANFEI](https://www.anfei.mx/actividades/registro_sedes_2026/).

## Responsabilidad del equipo y autoría

BeaconChat y su código preexistente son de **Vetrabyte / Nicolás Butterfield**. [LICENSE](../LICENSE) conserva `Copyright (c) 2025 Nicolás Butterfield` y la licencia MIT. Toda copia o porción sustancial distribuida del software/documentación debe conservar el aviso de copyright y permiso. No se cambia esa licencia, su año ni la titularidad mediante este paquete.

Crédito sugerido, para adaptar con aportes verificables:

> Base tecnológica: BeaconChat, de Vetrabyte / Nicolás Butterfield, licencia MIT. El equipo [PENDIENTE] propone y desarrolla [APORTE PROPIO PENDIENTE]. La integración SOS geolocalizado–mesh–gateway–centro es futura y sus evidencias se identifican por separado.

La información oficial anuncia divulgación pública del material entregado. El equipo debe aclarar con coordinación cómo tratar tecnología preexistente y atribución antes de adjuntar código o documentación. Esa condición de participación no se interpreta aquí como cambio automático de la MIT del repositorio. No incluir datos reales de víctimas en las piezas públicas. [Competencia](https://www.rallydeinnovacion.org/competencia/).

## Preparación de los entregables

1. Registrar el desafío literal, identificador, sede y equipo; contrastarlos con [01](Vision-y-Alcance-BeaconChat.md).
2. Obtener de SIRLI o de coordinación la **plantilla oficial**. Registrar archivo, versión, fecha y restricciones en [08](Reporte-Pautado-Rally-2026.md). No se recibió esa plantilla: no se fabrica ni se adjunta un PDF sustituto.
3. Separar en toda pieza evidencia de código **E**, simulación **S** y propuesta **P**. La clasificación se define en [PSI/README.md](../README.md).
4. Completar el reporte con las fuentes y métricas de [05](Factibilidad-BeaconChat.md) y [06](Validacion-BeaconChat.md); marcar expresamente lo no medido.
5. Adaptar [09](Pitch-Rally-2026.md), grabar material propio y revisar duración/créditos. El equipo gestiona sus publicaciones y cargas.

## Criterios orientativos para preparar la revisión

El Issue #6 pide abordar innovación, impacto social, prefactibilidad económica/social, sustentabilidad ambiental, viabilidad técnica y calidad de presentación. Se ofrece este mapa de trabajo; **no se obtuvo la rúbrica oficial 2026 ni se asignan ponderaciones**. [Issue #6](https://github.com/NicoButter/beaconchat/issues/6).

| Eje | Evidencia que debe producir o completar el equipo |
| --- | --- |
| Innovación | Comparación de alternativas y aporte propio, diferenciando tecnología heredada |
| Impacto social | Beneficiarios, barreras de acceso, validación y efectos adversos |
| Prefactibilidad | Presupuesto con fuentes locales y responsable de operación |
| Ambiente | Energía, reutilización de equipos, mantenimiento y residuos |
| Técnica | Cadena propuesta, fallos, pruebas y límites del prototipo |
| Presentación | Coherencia entre reporte, diagramas y video; estados y créditos visibles |

## Registro de faltantes

| ID | Dato faltante | Responsable sugerido | Efecto |
| --- | --- | --- | --- |
| F-01 | Enunciado literal e ID del desafío | Equipo | El encaje con la competencia permanece provisional |
| F-02 | Plantilla oficial y rúbrica 2026 | Equipo / sede | Pendiente el reporte final y su ajuste formal |
| F-03 | Nombre, miembros y aporte original | Equipo | No atribuirle el código existente ni esta guía |
| F-04 | Condiciones de reutilización, autoría y entrega pública | Equipo / coordinación | Pendiente confirmar condiciones aplicables |
| F-05 | Cotizaciones locales y fuente de financiamiento | Equipo | Sin cifras económicas ni promesa de ahorro |
| F-06 | Datos de ensayos físicos y validación con usuarios | Responsables de pruebas | No afirmar alcance, rescate ni adopción demostrados |
| F-07 | Institución receptora, responsable de datos y operación | Futuro socio operativo | El centro sigue siendo conceptual; sin conexión oficial |

Los faltantes no impiden revisar este paquete de ingeniería. Sí impiden presentarlo como entrega oficial completa o solución validada. No se contactó a terceros ni se publicó material.
