# 06 · Validación y criterios de aceptación

**Vetrabyte / Nicolás Butterfield · [MIT](../../LICENSE) · Borrador para revisión · 16/10/2026**
[Índice](../README.md) · [Requerimientos](02-requerimientos.md) · [Trazabilidad](10-trazabilidad.md)

## Evidencia disponible y límites

| Evidencia | Resultado disponible | Qué no demuestra |
| --- | --- | --- |
| [Informe BLE directo](../../docs/PRUEBA_BLE_DIRECTO.md) | **17 tests JVM reportados**: 13 protocolo, 3 estados y 1 original. Prueba A↔B pendiente; sin dispositivos en aquel informe | Stack Bluetooth, alcance, compatibilidad entre fabricantes ni mesh físico |
| [Informe del núcleo](../../docs/ISSUE_2_MESH_CORE.md) | **48 tests totales reportados**, incluidos 31 del núcleo (22 motor + 9 topologías) y los 17 anteriores | GNSS, radio real, fragmentación, DTN, gateway, backend ni rescate |
| [Código de tests del motor](../../app/src/test/java/com/nicobutter/beaconchat/mesh/core/MeshEngineTest.kt) y [topologías](../../app/src/test/java/com/nicobutter/beaconchat/mesh/core/MeshTopologyTest.kt) | Cadena, ruptura de enlace, triángulo, anillo, caminos duplicados, límites, TTL y expiración mediante red simulada | Probabilidades de pérdida, obstáculos, consumo y restricciones de un teléfono |
| Señalización óptica/vibración/audio/QR/beacon BLE | Componentes presentes en código | Tasas de decodificación o funcionamiento garantizado bajo derrumbes |
| Revisión documental de esta intervención | Consultar [INFORME-REVISION](INFORME-REVISION.md) | No es una ejecución nueva de los ensayos anteriores |

**No se volvió a ejecutar Gradle, APK, tests JVM ni pruebas físicas durante esta tarea documental.** Los 48 incluyen los 31, no son 48 + 31. El informe BLE también registra errores de Lint preexistentes en módulos ópticos; no se presenta un build completo libre de incidencias ni se modifica código para resolverlos.

## Catálogo de pruebas

Los IDs completos son `R26-PR-01` a `R26-PR-19`. Solo PR-19 se ejecuta como revisión documental de esta entrega; PR-01/02 tienen evidencia histórica limitada y el resto queda pendiente. Cada resultado futuro debe señalar simulación, laboratorio físico o campo.

| ID | Preparación y procedimiento | Criterio de aceptación / evidencia | Estado |
| --- | --- | --- | --- |
| R26-PR-01 | Ejecutar tests de protocolo/estado y protocolo manual A↔B del informe BLE: permisos, Unicode, 20 bytes exactos/exceso, desconexión, pausa y timeout. | Tests del protocolo correctos; ambos teléfonos registran texto íntegro, sin truncar ni éxito falso; guardar modelos/SO/logs. | JVM histórico reportado; físico pendiente |
| R26-PR-02 | Revisar y reproducir suite del núcleo: cadena intacta/rota, TTL cero, triángulo/anillo/diamante, dedupe/colas/reloj y fallos de transporte. | Resultado lógico esperado y propagación termina; transporte no disponible consume intento sin reencolar ni ACK. | 31 JVM históricos reportados; sin nueva ejecución |
| R26-PR-03 | En dispositivos compatibles, probar cada emisor/receptor óptico, vibración, audio, QR y beacon; registrar luz/ruido/acoplamiento, permisos y detención. | Emisión/decodificación observada por canal y condición; errores y limitaciones explícitos, sin atribuir alcance genérico ni geolocalización al beacon. | Pendiente físico |
| R26-PR-04 | Con implementación futura, registrar SOS sintéticos válidos/invalidables y doble toque/reintento. | Esquema validado, ID/carga estable para reintentos, sin incidente accidental doble; nota/límites UTF-8 comprobados. | Pendiente |
| R26-PR-05 | Desactivar datos/Wi-Fi; ensayar GNSS al aire libre y en interior, con permisos, proveedor apagado y límite de espera configurado. Registrar tiempo hasta fix y precisión reportada. | No exige Internet, captura/edad/fuente visibles; timeout no fabrica posición ni bloquea SOS. No exigir fix en toda condición. | Pendiente físico |
| R26-PR-06 | Forzar sin fix, permiso denegado, posición antigua, dato manual inválido y ausencia completa de ubicación. | Admite SOS sin posición, rechaza rangos inválidos, rotula manual/reciente y no dibuja posición ficticia; antigüedad aumenta y reloj incierto se indica. | Pendiente |
| R26-PR-07 | Probar serializer/fragmentos futuros: pérdida, duplicación, desorden, corrupción, exceso, timeout, mezcla de versiones y peer legado. | Roundtrip íntegro solo con carga completa/válida; memoria acotada, liberación al vencer y chat legado intacto; TTL no se descuenta por fragmento. | Pendiente |
| R26-PR-08 | A-B-C-D físicos, D único gateway, A sin enlace directo con C/D. Demostrar aislamiento retirando B/C y verificar que no haya salida IP en A/B/C. | Recorrido de IDs/saltos registrado; 10/10 SOS de prueba en cadena estable llegan en ≤120 s cada uno, objetivo provisional. Cadena rota no informa entrega falsa. | Pendiente; hipótesis de laboratorio |
| R26-PR-09 | Quitar vecinos/salida, agotar TTL, llenar dedupe/cola y luego restaurar enlaces. Separar motor actual de custodia futura. | Motor actual no promete recuperación; se observan descartes y límites. Futuro sistema muestra sin salida y aplica política explícita, sin bucle ni TTL restaurado indefinidamente. | Lógica parcial histórica; sistema pendiente |
| R26-PR-10 | Cortar Internet/DNS/API del gateway antes, durante y después del guardado; red Wi-Fi sin Internet; luego reconectar y probar otro gateway. | Sin confirmación central falsa, autenticación verificada y error/estado incierto claro. Reintento durable solo en E4; ID preservado. | Pendiente |
| R26-PR-11 | Enviar mismo SOS por dos gateways, perder respuesta tras guardado, repetir y provocar fallo de base. Ensayar retorno de comprobante al origen. | Un incidente por clave; confirmar solo después de persistir. Respuesta perdida no duplica. Sin retorno, A sigue sin confirmar; mismo ID con cuerpo distinto se rechaza/audita. | Pendiente |
| R26-PR-12 | Operador de prueba entra con distintos roles y observa incidentes actuales/viejos/manuales/sin ubicación; filtra y clasifica. | Lista completa, mapa sin coordenadas ficticias, incertidumbre visible, acceso mínimo y cambios auditables. | Pendiente |
| R26-PR-13 | En fase durable, reiniciar/matar proceso durante custodia, transferencia y recepción; simular disco lleno, vencimiento y reloj civil desajustado. | Custodia aceptada/vigente recuperable, reintento acotado, sin duplicado central; expirados no salen y falta de escritura nunca se declara guardada. | Pendiente E4 |
| R26-PR-14 | Revisar amenazas e intentar emisor/gateway falsos, replay, alteración, acceso sin rol, revocación, extracción de logs/backups y vencimiento/borrado. | Contenido protegido y controles observables; exposición pública cero en muestras sintéticas; retención y autenticación verificadas según política aprobada. No basta HTTPS para declarar protección de los saltos BLE. | Pendiente |
| R26-PR-15 | Tareas con usuarios de prueba, lector de pantalla/texto ampliado y fallos inducidos; comparar evidencia contra etiquetas de estado. | Meta ≤3 acciones y feedback ≤1 s en configuración registrada; cero confirmaciones centrales falsas; medir errores/comprensión sin inventar satisfacción. | Pendiente |
| R26-PR-16 | Ensayar carga/saturación y energía contra baseline en el mismo equipo: scan, GNSS, señales, cámara y reintentos; bloquear/pasar a background por SO. | Cumplir cuotas/timers definidos; informar pérdidas y continuidad real. Reportar consumo y diferencia observada, sin meta de ahorro ficticia. | Pendiente |
| R26-PR-17 | Simulación de mesa futura de recursos, brigadistas y rutas con fuentes contradictorias/viejas. | Trazabilidad de decisiones humanas y fuente/edad visibles; prioridad asistida no despacha automáticamente ni certifica rutas. | Pendiente E4–E5 |
| R26-PR-18 | Si se decide LoRa, definir equipos externos/plan de enlace y repetir intercambio/duplicados/fallo de radio. | Funciona solo con equipo externo documentado; sin radio se informa limitación y no se exige LoRa al flujo BLE básico. | Pendiente E5 opcional |
| R26-PR-19 | Revisar paquete 00–10, enlaces, IDs/trazabilidad, fences/diagramas Mermaid, autoría y límites de cambios en Git. | Sin enlace local roto ni requisito huérfano; Mermaid válido; solo altas documentales en PSI y antecedentes íntegros. | Ejecutada; detalle en informe |

El objetivo de PR-08 es un umbral de laboratorio propuesto para hacer falsable la PoC, no una tasa obtenida ni una promesa de campo. Si falla, registrar resultados y revisar parámetros/alcance; no borrar intentos fallidos ni cambiar el criterio después del ensayo sin dejar constancia.

## Protocolo reproducible de ensayo futuro

1. Registrar responsable, fecha/zona horaria, commit/APK y configuración, modelo/SO, permisos, batería, cuotas, TTL y topología. Documentar cómo se aisló el enlace A-C/D; no basta colocar etiquetas a los teléfonos.
2. Usar solicitudes sintéticas con IDs conocidos. Separar el modo demostración del SOS visible/audible público. No ensayar enviando alertas a un centro oficial no acordado.
3. Observar cada enlace/estado y correlacionar origen→relays→gateway→API→comprobante. Guardar evidencia sin notas/coordenadas personales.
4. Ejecutar casos normales y fallos del catálogo; conservar todos los intentos. Para el umbral PR-08 usar 10 intentos declarados; para estimaciones estadísticas posteriores justificar una muestra mayor y sus intervalos.
5. Comparar con criterios predefinidos, registrar desviaciones y repetir únicamente tras cambios identificados. Asignar a un revisor el dictamen de cada prueba.

Comandos de reproducción JVM, **no ejecutados en esta intervención**, para un entorno ya preparado con JDK/SDK/dependencias del proyecto:

```bash
./gradlew testDebugUnitTest --tests 'com.nicobutter.beaconchat.mesh.core.*'
./gradlew testDebugUnitTest
```

No usar esos comandos como prueba de radio ni agregar aquí dependencias para ensayos futuros.

## Métricas y registro

| Métrica | Definición propuesta | Precaución |
| --- | --- | --- |
| Entrega central | SOS únicos persistidos / SOS únicos generados, por escenario y ventana | Incluir intentos sin ruta y expirados; no igualar con handoff |
| Latencia central | Generación hasta persistencia confirmada, con método de reloj declarado | No restar relojes de teléfonos distintos sin sincronización o incertidumbre |
| Confirmación al origen | Comprobantes válidos recibidos en origen / SOS generados | Puede ser menor que entrega central por falla del retorno |
| Duplicación | Incidentes centrales extra por una misma clave SOS | Objetivo de diseño: cero con idempotencia durable |
| GNSS | Tiempo hasta fix, tasa sin fix, precisión estimada y edad | Separar cielo abierto/interior/manual; no confundir estimación con error real medido |
| Recursos | Pico de cola/reensamblado, descartes, CPU/memoria | Registrar valores y límites aprobados antes de prueba |
| Energía | mWh por hora y por SOS, o proxy documentado si no hay instrumental | % de batería no equivale a energía comparable entre modelos |
| Usabilidad | Acciones, tiempos, errores y comprensión por tarea | Muestra y método explícitos; sin entrevistas inventadas |
| Seguridad/privacidad | Intentos rechazados, exposición y cumplimiento de retención | Un test exitoso no certifica ausencia de vulnerabilidades |

Ficha para cada ensayo: `ID de prueba | fecha | responsable/revisor | versión | entorno/topología | datos sintéticos | esperado | observado | métricas/método | evidencia | desvíos | resultado (aprobado/fallido/no ejecutado)`.

No hay valores observados nuevos para estas métricas de sistema. La aprobación documental de PR-19 no cambia el estado P del producto integral ni el carácter S de las topologías existentes.
