# 05 · Factibilidad, riesgos y privacidad

**Vetrabyte / Nicolás Butterfield · [MIT](../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Validación](Validacion-BeaconChat.md) · [Roadmap](Roadmap-BeaconChat.md)

## Dictamen de prefactibilidad

La propuesta es **apta para una PoC condicionada**, basada en componentes Android existentes y un núcleo lógico simulado. La evidencia actual no permite afirmar que la cadena funcione en teléfonos, sea desplegable en catástrofes, resulte económicamente sostenible o cuente con un centro receptor comprometido. La primera inversión de esfuerzo debe resolver enlaces físicos, protocolo y estado veraz, antes de ampliar funciones.

Este dictamen es una evaluación de ingeniería de alcance limitado, no una certificación ni una evaluación económica/social ya realizada. No existen en este paquete resultados de entrevistas, rescates, ensayos de alcance o cotizaciones.

## Factibilidad técnica

| Aspecto | Base disponible | Supuesto / dificultad | Evidencia para continuar |
| --- | --- | --- | --- |
| BLE directo | E: GATT, advertising y scan, protocolo de 20 bytes | Compatibilidad por fabricante y ciclo de vida | PR-01 física A↔B con permisos y fallos |
| Mesh multisalto | S: TTL/dedupe/cola y topologías JVM | Motor sin BLE; formato rico no cabe en el chat | PR-07/08/09, comparación cadena intacta/rota |
| GNSS offline | El SO puede proveer fix satelital sin datos; no está integrado a incidentes | Señal, espera, permisos y batería; puede no haber fix | PR-05/06 al aire libre/interior, con datos y Wi-Fi apagados |
| Gateway dinámico | P: reutiliza Internet del nodo que lo tenga | No hay cliente HTTPS/API, autenticación ni retorno de comprobantes | PR-10/11 con cortes y dos gateways |
| Centro | P: API, almacenamiento, mapa/lista y operador | Institución receptora, operación y cartografía pendientes | PR-12/14 con servicio de laboratorio |
| Resiliencia prolongada | No hay custodia durable | Reinicios, cuotas, política DTN y reloj incorrecto | PR-13 y límites explícitos; no inferirlo de FIFO temporal |
| Seguridad | No resuelta extremo a extremo | Identidad, claves offline, antiabuso y privacidad | Revisión de amenazas y PR-14 antes de datos reales |
| LoRa externo | P opcional | Hardware, alimentación y compatibilidad; no hay radio en la app | PR-18 solo después de definir equipos y condiciones de radio |

La descripción de GNSS se basa en [Android GPS_PROVIDER](https://developer.android.com/reference/android/location/LocationManager#GPS_PROVIDER) y las limitaciones de recepción en [GPS.gov](https://www.gps.gov/gps-accuracy). No se extrapolan cifras de precisión de esas referencias a BeaconChat.

La densidad de participantes debe tratarse como variable del ensayo: sin vecinos compatibles no hay tránsito; sin ningún gateway con Internet útil no hay recepción central. Distancia, obstáculos, interferencia, movilidad y política energética afectan los enlaces. No se fija un radio BLE universal ni se afirma comunicación a través de escombros. Luz, audio, QR y vibración son alternativas locales con condiciones propias, no una red mesh automáticamente interoperable.

## Factibilidad económica

Reutilizar teléfonos podría disminuir compras iniciales, pero no demuestra costo cero. Deben presupuestarse desarrollo, hardware prestado/comprado, energía, conectividad del gateway, servicio central, seguridad, pruebas, capacitación, soporte y reposición. LoRa es un costo adicional opcional, separado del MVP.

Modelo para completar con cotizaciones, sin cifras inventadas:

`Costo inicial = equipos + desarrollo/integración + pruebas + seguridad + capacitación inicial`

`Costo operativo mensual = hosting + datos + energía + mantenimiento + soporte + personal + reposiciones prorrateadas`

`Costo total de N meses = costo inicial + N × costo operativo mensual`

| Rubro | Cantidad / unidad a levantar | Fuente y fecha | Importe / moneda |
| --- | --- | --- | --- |
| Teléfonos compatibles, propios o prestados | Inventario y costo de oportunidad | PENDIENTE F-05 | PENDIENTE |
| Baterías externas y recarga | Equipos × sesiones de autonomía | PENDIENTE F-05 | PENDIENTE |
| Horas de desarrollo y validación | Rol × horas × tarifa sustentada | PENDIENTE F-05 | PENDIENTE |
| Hosting/API/almacenamiento/mapas | Tráfico, usuarios, retención y licencias | PENDIENTE F-05 | PENDIENTE |
| Datos de gateway y soporte | Nodos activos × consumo/mes | PENDIENTE F-05 | PENDIENTE |
| Operadores y formación | Turnos, capacidad y mantenimiento | PENDIENTE F-07 | PENDIENTE |
| Seguridad, auditoría y recuperación | Alcance acordado y reposición | PENDIENTE F-05 | PENDIENTE |
| LoRa externo opcional | Modelo, cantidad, accesorios y alimentación | PENDIENTE, fuera del MVP | PENDIENTE |

Registrar impuestos, vigencia y tipo de cambio con fuente cuando se usen. No mezclar monedas ni tratar horas donadas como esfuerzo inexistente. Comparar costo total y sensibilidad para pocos/muchos gateways, baja/alta participación y retención corta/larga. Sin esos datos, la prefactibilidad económica permanece abierta; no se ofrece retorno de inversión calculado.

Opciones a comparar por el equipo: señalización de proximidad sin centro, comunicación comunitaria por medios ya disponibles y propuesta mesh con gateway. Evaluar cobertura efectiva, necesidad de equipos adicionales, capacitación, autonomía y responsable de operación. No se afirma superioridad frente a alternativas no ensayadas.

## Factibilidad social y organizacional

Beneficiarios potenciales: personas aisladas y centros que necesitan consolidar solicitudes. Hipótesis de impacto: el tránsito colaborativo podría hacer llegar solicitudes que el chat directo no alcanzaría. Debe medirse primero llegada al servicio y comprensión del estado; no convertir esa métrica en vidas salvadas o tiempo de rescate reducido.

Barreras: app instalada previamente, disponibilidad/compatibilidad de teléfonos, energía, permisos, confianza en retransmitir, alfabetización digital, idioma, discapacidad y ausencia de una institución que atienda. La exclusión de quien no tiene teléfono debe discutirse, por ejemplo mediante acompañante que registre la solicitud, sin dar por implementado ese mecanismo.

Validación propuesta: sesiones consentidas con usuarios de prueba y operadores, tareas con datos ficticios, medición de comprensión, errores y tiempo; registrar tamaño de muestra y limitaciones. Son actividades pendientes, sin entrevistas ni avales atribuidos a organizaciones. Deben acordarse quién opera el centro, cuándo se atiende, cómo se verifica un SOS y cómo se comunican incidentes duplicados/falsos. Una app no crea capacidad de respuesta por sí sola.

## Factibilidad ambiental

La reutilización de dispositivos es una hipótesis favorable que exige inventario y análisis de vida útil. GNSS, escaneo continuo, cámara, linterna y reintentos consumen energía; no se presupone que todos los canales simultáneos sean más eficientes. Proponer sesiones de medición contra baseline, cuotas y pausas configurables sin ocultar el efecto sobre entrega.

Registrar energía por hora de participación, energía por SOS intentado/entregado, necesidades de recarga y desgaste de batería. Incluir reparación, disposición de baterías y residuos de radios externos si se añade LoRa. No se calculan huella de carbono, ahorro ni sustentabilidad comprobada sin metodología y datos. La prueba inicial debe hacerse en un entorno controlado, sin provocar emisiones SOS públicas reales.

## Registro de riesgos

Evaluación cualitativa inicial: probabilidad **desconocida** hasta ensayos; impacto estimado de ingeniería. Los responsables son roles propuestos, no compromisos asumidos por terceros.

| ID | Riesgo y efecto | Impacto estimado | Mitigación / evidencia | Responsable propuesto |
| --- | --- | --- | --- | --- |
| K-01 | Pocos vecinos, enlaces rotos o ningún gateway: SOS no llega | Alto | Estado sin salida, PR-08/09/10; explorar custodia E4 | Integración mesh |
| K-02 | GNSS ausente/viejo o manual mal interpretado | Alto | Calidad/edad visibles, sin marcador falso; PR-05/06/12 | App / UX |
| K-03 | GATT o handoff presentado como recepción central | Alto | Comprobante verificable y estados separados; PR-11/15 | App / backend |
| K-04 | App pausada, restricciones Android, batería agotada | Alto | Matriz por SO/modelo, PR-01/08/13/16; no prometer continuidad | Android / pruebas |
| K-05 | Saturación, bucles, replay y fragmentos incompletos | Alto | Cuotas, TTL, dedupe y pruebas de abuso PR-07/09/14 | Protocolo / seguridad |
| K-06 | Filtración de ubicación/notas o suplantación | Alto | Minimización, cuerpo protegido, roles, claves y revisión PR-14 | Seguridad / responsable de datos |
| K-07 | Servidor guarda pero respuesta se pierde | Alto | Idempotencia y comprobantes; origen sin confirmar si no vuelve ACK central | Backend / gateway |
| K-08 | Incidentes falsos o gravedad manipulada saturan atención | Alto | Límites y autenticación compatible con acceso de emergencia; revisión humana, PR-14 | Operación / seguridad |
| K-09 | Sin presupuesto ni centro receptor comprometido | Alto | Cotizaciones, costos operativos y acuerdo de responsabilidad F-05/07 | Responsable del proyecto |
| K-10 | Presentar tecnología heredada como aporte del equipo | Alto | Créditos MIT y declaración de aportes; revisión F-03/04 | Equipo / coordinación |
| K-11 | Consumo excesivo o descarte de equipos | Medio | Medir baseline y plan de reutilización/reparación; PR-16 | Pruebas / operación |
| K-12 | Datos de rutas incorrectos o prioridad asistida tomada como orden | Alto | Fuente/edad y decisión humana; PR-17 antes de operación | Coordinación |

## Privacidad y amenazas: política futura

Los datos de ubicación y situación de una víctima son sensibles. La propuesta deja de ser exclusivamente local al incorporar gateway/centro. Las afirmaciones históricas «sin datos personales» y «confidencialidad no aplicable» no sirven como política de esta ampliación.

| Frontera | Amenaza | Control requerido, aún P |
| --- | --- | --- |
| Emisor → vecinos | Observación o alteración de coordenadas/notas; falso origen | Cuerpo protegido para el centro, autenticación de emisor y reglas offline; metadatos mínimos de tránsito |
| Vecino → vecino | Replay, bucles, restauración de TTL, uso abusivo de memoria/radio | Dedupe temporal más reglas durables/anti-replay, cuotas, expiración y límites por peer |
| Gateway → API | Gateway suplantado, respuesta falsa, múltiples entregas | HTTPS con validación correcta, credenciales revocables, comprobante correlacionado, idempotencia |
| API/base → operador | Acceso indebido, exportación o clasificación no autorizada | Roles mínimos, auditoría de acceso/cambios, gestión de credenciales y revisión |
| Dispositivo perdido / copias | Lectura local, backups, retención indefinida | Almacenamiento protegido, exclusión de copias sensibles, plazos y borrado verificados |
| Reporte/video/logs | Publicación irrestricta de coordenadas o notas | Datos sintéticos, logs sin carga sensible, capturas revisadas antes de difundir |

Minimizar: no exigir nombre, DNI, contactos ni historia clínica para un SOS básico. Una nota libre puede contener datos personales: advertirlo y limitarla. Relays no necesitan ver el contenido sensible; la identidad seudónima y las direcciones de radio también pueden revelar patrones, por lo que se debe revisar el modelo de amenazas completo.

La política de consentimiento debe explicar finalidad/destino y permitir elegir colaboración. Un modo de emergencia necesita tratamiento específico para situaciones donde el consentimiento habitual no sea viable; responsable y condiciones se definirán con la institución antes de datos reales. No se afirma cumplimiento legal de una jurisdicción por escribir esta guía.

Definir retención por fase/local/centro, quién autoriza acceso/borrado, cómo se maneja retiro de participación y cómo se auditan incidentes. Los plazos están pendientes, no son ilimitados por defecto. Borrar una copia local no elimina las remotas ya transmitidas. Cifrado y autenticación no garantizan que un SOS sea verdadero; la atención requiere verificación humana y una política antiabuso que no excluya injustificadamente a quien solicita ayuda.

## Decisión de continuidad

Continuar con E1 documental y PoC sintética; avanzar a E2/E3 solo con protocolo y pruebas físicas satisfactorias. Antes de un piloto con datos reales se requieren controles de seguridad, política de datos/retención, institución receptora, presupuesto y revisión de riesgos con responsables. Las dependencias no resueltas quedan visibles en F-01 a F-07 y en el [roadmap](Roadmap-BeaconChat.md), sin atribuir aprobaciones inexistentes.
