# 09 · Storyboard y pitch para adaptar

**Vetrabyte / Nicolás Butterfield · [MIT](../../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Guía del Rally](00-guia-rally.md) · [Reporte](08-guia-reporte-pautado.md)

## Uso y duración

El equipo debe crear y protagonizar su video propio, ajustar el relato al desafío literal y declarar su innovación. Este guion es un insumo de Vetrabyte; no es una grabación ni una entrega del equipo. La referencia oficial fija **YouTube hasta 2 minutos** y **TikTok hasta 1 minuto** para la interacción internacional. [Competencia](https://www.rallydeinnovacion.org/competencia/).

Se propone un video de **115 segundos**, dejando 5 s de margen dentro de 120 s; cronometrar la grabación final, incluidos créditos/transiciones. Las escenas de cadena completa y centro son P: usar diagramas o mockups rotulados, sin fingir capturas de un producto operativo.

## Storyboard propuesto

| Tiempo | Imagen que produciría el equipo | Mensaje / locución sugerida | Estado visible |
| --- | --- | --- | --- |
| 0–12 s | Contexto ficticio de interrupción y desafío literal | «Tras una catástrofe, un teléfono puede seguir funcionando sin cobertura. Proponemos que una solicitud de ayuda encuentre una salida por dispositivos cercanos.» | Escenario ilustrativo, no testimonio real |
| 12–27 s | Persona registra SOS en mockup y dato GNSS con precisión/edad | «La persona registra un SOS. Intentaríamos obtener ubicación satelital sin datos; si no hay fix, aceptaríamos una referencia manual, una posición reciente identificada o ninguna ubicación.» | P · mockup |
| 27–45 s | Animación A→B→C→D; solo D con símbolo Internet | «Los vecinos retransmitirían por BLE. No todos necesitan Internet: sí necesitan enlaces entre ellos. El primer nodo con una salida útil actuaría como gateway móvil.» | P · red futura |
| 45–60 s | D→API→lista/mapa de prueba | «El gateway enviaría el incidente por HTTPS. El centro lo guardaría sin duplicados y un operador autorizado podría visualizarlo y clasificarlo.» | P · centro conceptual |
| 60–77 s | Código/captura local y simulación claramente separados | «La base BeaconChat, de Vetrabyte y Nicolás Butterfield, ya contiene chat BLE directo y señales locales. Su motor mesh se prueba en simulaciones JVM; todavía no está integrado al BLE.» | E en código / S simulado; físico pendiente |
| 77–92 s | Pantallas de falta de vecinos, fix y salida | «Sin vecinos o gateway no aseguramos llegada al centro. La custodia durable y los reintentos son futuros. Recibir en el centro no confirma lectura, atención ni rescate.» | P · fallos y límites |
| 92–107 s | Aporte propio y plan de prueba sustentado | «Nuestro aporte como equipo es [COMPLETAR]. Lo validaríamos con [ENSAYO DEFINIDO], midiendo llegada, latencia, consumo y comprensión. Costos y operación requieren datos locales.» | Aporte/evidencia del equipo por completar |
| 107–115 s | Créditos legibles y siguiente hito | «Proponemos avanzar a una prueba controlada con datos ficticios, antes de un uso operativo.» Mostrar equipo, fuente BeaconChat y MIT. | P · próximo paso |

No leer literalmente los campos pendientes ni la columna de producción. Adaptar y ensayar para que la locución resulte natural y accesible. Si el equipo obtiene resultados propios, reemplazar el condicional solo en la parte respaldada por esa evidencia; conservar límites y alcance del ensayo.

## Afirmaciones que el material debe revisar

| Expresión a evitar | Formulación respaldada por este paquete |
| --- | --- |
| «Ya tenemos mesh BLE que llega al centro» | «Proponemos integrar mesh BLE y gateway; el núcleo actual se prueba en simulaciones» |
| «GPS exacto incluso bajo escombros» | «Intentaremos GNSS y mostraremos calidad o ausencia de posición» |
| «Enviado equivale a rescatado» | «GATT, recepción central, lectura y atención son confirmaciones distintas» |
| «Todos deben tener Internet» | «Solo el nodo gateway necesita Internet hacia el servicio; los demás necesitan enlaces vecinos» |
| «LoRa viene en el teléfono» | «LoRa requeriría radio/gateway externo opcional» |
| «Solución gratuita/segura/certificada» | «Base MIT; costos de operación y seguridad integral requieren desarrollo y validación» |
| «El equipo creó BeaconChat durante el Rally» | «Base de Vetrabyte / Nicolás Butterfield; aportes propios del equipo identificados» |

## Checklist del video de propuesta

- [ ] Desafío y equipo identificados; aporte propio declarado con hechos.
- [ ] Duración total ≤120 s y locución comprensible; subtítulos/contraste/volumen revisados.
- [ ] Escena central A→vecinos→gateway→centro comprensible, sin Internet obligatorio en todos los teléfonos.
- [ ] Diagramas/mockups P y simulaciones S rotulados; capturas E no implican ensayo físico.
- [ ] Limitaciones, privacidad y veracidad de confirmaciones coherentes con el reporte.
- [ ] Créditos legibles: BeaconChat — Vetrabyte / Nicolás Butterfield — MIT; equipo y material adicional identificados.
- [ ] Música, imágenes y testimonios usados con autorización/licencias aplicables; datos ficticios.
- [ ] Archivo final visto y cronometrado por el equipo antes de publicar; URL verificada según SIRLI/sede.

## Interacción internacional TikTok

Es una pieza lúdica creativa distinta del pitch. La guía pública describe duplas vinculadas en SIRLI y registro de la URL por ambos equipos; ANFEI 2026 identifica `#RallyLatam26`. Confirmar con coordinación los detalles vigentes y la dupla asignada. [Competencia](https://www.rallydeinnovacion.org/competencia/), [ANFEI 2026](https://www.anfei.mx/actividades/registro_sedes_2026/).

Idea adaptable: intercambio cultural de una palabra/gesto y una cadena humana simbólica de colaboración, rotulada como metáfora; no presentarla como demostración de radio ni emitir SOS reales en público. El equipo y su dupla deben concebir y producir la pieza conforme a las reglas.

- [ ] Dupla y comunicación acordadas por los equipos; actividad creativa propia y culturalmente respetuosa.
- [ ] Video total ≤60 s; identidad de ambos equipos/países o culturas y etiqueta de la edición verificadas.
- [ ] Participantes autorizan su aparición; sin datos personales sensibles ni escenas de emergencia reales no consentidas.
- [ ] URL accesible y registrada por ambos equipos según instrucciones de SIRLI/sede; no sustituirla por la URL de YouTube.
- [ ] Registrar responsable, versión revisada y verificación final de duración.

Este documento no autoriza ni ejecuta contactos, grabaciones, publicaciones o cargas por cuenta del equipo. No se produjo ninguno de esos entregables durante la edición documental.
