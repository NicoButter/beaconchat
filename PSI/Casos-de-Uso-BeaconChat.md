# Documento de Casos de Uso
## BeaconChat - Sistema de Comunicación de Emergencia

**Versión:** 2.0  
**Fecha:** 13 de diciembre de 2025  
**Autor:** NicoButter  
**Estado:** Aprobado

---

## Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Diagrama General de Casos de Uso](#2-diagrama-general-de-casos-de-uso)
3. [Actores del Sistema](#3-actores-del-sistema)
4. [Casos de Uso Detallados](#4-casos-de-uso-detallados)
5. [Matriz de Trazabilidad](#5-matriz-de-trazabilidad)

---

## 1. Introducción

### 1.1 Propósito
Este documento describe los casos de uso del sistema BeaconChat, proporcionando una visión detallada de cómo los diferentes actores interactúan con el sistema para cumplir sus objetivos.

### 1.2 Alcance
Los casos de uso descritos cubren todas las funcionalidades principales de BeaconChat:
- Transmisión de mensajes (luz, vibración, sonido)
- Recepción de mensajes (cámara, acelerómetro, QR)
- Red mesh Bluetooth
- Funciones de emergencia
- Configuración y visualización

### 1.3 Referencias
- ERS-BeaconChat.md - Especificación de Requerimientos de Software v1.0
- IEEE 830-1998 - Software Requirements Specification
- UML 2.5 - Unified Modeling Language

---

## 2. Diagrama General de Casos de Uso

```
┌─────────────────────────────────────────────────────────────────┐
│                      Sistema BeaconChat                         │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │ Transmisión     │    │ Recepción       │                   │
│  │ - SOS/HELP      │    │ - Luz (Cámara)  │                   │
│  │ - Mensaje Texto │    │ - Vibración     │                   │
│  │ - Multi-canal   │    │ - QR Code       │                   │
│  └─────────────────┘    └─────────────────┘                   │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │ Red Mesh        │    │ Configuración   │                   │
│  │ - Descubrir     │    │ - Callsign      │                   │
│  │ - Chat P2P      │    │ - Preferencias  │                   │
│  │ - Radar         │    │ - Visualización │                   │
│  └─────────────────┘    └─────────────────┘                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
         │              │              │              │
         │              │              │              │
    ┌────────┐    ┌──────────┐   ┌──────────┐  ┌──────────┐
    │Víctima │    │Rescatista│   │Usuario   │  │Radio-    │
    │        │    │          │   │General   │  │aficionado│
    └────────┘    └──────────┘   └──────────┘  └──────────┘
```

---

## 3. Actores del Sistema

### 3.1 Actor: Víctima de Emergencia
**Descripción:** Persona en situación de peligro que necesita pedir ayuda urgentemente.

**Características:**
- Puede estar bajo estrés extremo
- Puede tener movilidad limitada
- Requiere interfaz simple de un toque
- Puede no tener conocimientos técnicos

**Objetivos:**
- Enviar señal de socorro rápidamente
- Comunicar ubicación/situación si es posible

---

### 3.2 Actor: Rescatista Profesional
**Descripción:** Personal capacitado en operaciones de rescate y emergencia.

**Características:**
- Entrenado en protocolos de emergencia
- Familiarizado con código Morse básico
- Usa equipo especializado
- Necesita información precisa y rápida

**Objetivos:**
- Detectar señales de emergencia
- Decodificar mensajes de víctimas
- Coordinar con otros rescatistas vía mesh

---

### 3.3 Actor: Usuario General
**Descripción:** Persona que usa BeaconChat como preparación para emergencias.

**Características:**
- Conocimientos técnicos básicos a medios
- Interesado en comunicación offline
- Puede practicar uso de la app

**Objetivos:**
- Aprender a usar el sistema
- Comunicarse con otros usuarios
- Estar preparado para emergencias

---

### 3.4 Actor: Radioaficionado
**Descripción:** Usuario con conocimientos avanzados de comunicaciones y código Morse.

**Características:**
- Experto en código Morse
- Conoce protocolos de comunicación
- Puede tener licencia de radioaficionado
- Interesado en aspectos técnicos

**Objetivos:**
- Experimentar con comunicación VLC
- Participar en red mesh
- Ayudar en situaciones de emergencia

---

## 4. Casos de Uso Detallados

---

## CU-001: Transmitir SOS de Emergencia

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-001 |
| **Nombre** | Transmitir SOS de Emergencia |
| **Actor Principal** | Víctima de Emergencia |
| **Actores Secundarios** | Ninguno |
| **Tipo** | Primario, Crítico |
| **Complejidad** | Baja |
| **Prioridad** | Crítica |

### Descripción
El usuario necesita enviar una señal de socorro SOS inmediatamente usando todos los canales disponibles del dispositivo.

### Precondiciones
- PRE-001.1: La aplicación BeaconChat está instalada
- PRE-001.2: El dispositivo tiene batería suficiente (>5%)
- PRE-001.3: La aplicación está abierta o puede abrirse

### Postcondiciones
- POST-001.1: Señal SOS transmitida continuamente por todos los canales
- POST-001.2: Pantalla muestra feedback visual claro
- POST-001.3: Contador de transmisiones actualizado

### Flujo Principal
1. Usuario abre la aplicación BeaconChat
2. Sistema muestra pantalla de bienvenida con botones grandes
3. Usuario identifica y presiona botón **"SOS"** (rojo, prominente)
4. Sistema valida disponibilidad de hardware
5. Sistema inicia transmisión simultánea:
   - 5.1. Linterna LED parpadea patrón "· · · — — — · · ·"
   - 5.2. Motor de vibración reproduce mismo patrón
   - 5.3. Altavoz emite tonos del mismo patrón
6. Sistema muestra pantalla fullscreen de emergencia con:
   - 6.1. Animación pulsante roja sincronizada
   - 6.2. Texto "TRANSMITIENDO SOS"
   - 6.3. Temporizador con tiempo transcurrido
   - 6.4. Contador de ciclos completados
   - 6.5. Botón "CANCELAR" siempre visible
7. Sistema repite patrón cada 5 segundos automáticamente
8. Usuario presiona botón "CANCELAR" cuando rescate llega
9. Sistema detiene inmediatamente todos los canales
10. Sistema libera recursos de hardware
11. Sistema retorna a pantalla de bienvenida
12. **Fin del caso de uso**

### Flujos Alternativos

**FA-001.1: Sin linterna LED**
- **Punto de activación:** Paso 4
- **Flujo:**
  - 4.1. Sistema detecta que no hay linterna disponible
  - 4.2. Sistema continúa con vibración y sonido únicamente
  - 4.3. Sistema muestra advertencia "⚠️ Sin linterna disponible"
  - 4.4. Continúa en paso 6

**FA-001.2: Sin motor de vibración**
- **Punto de activación:** Paso 4
- **Flujo:**
  - 4.1. Sistema detecta que no hay vibrador disponible
  - 4.2. Sistema continúa con linterna y sonido únicamente
  - 4.3. Continúa en paso 6

**FA-001.3: Batería muy baja (<5%)**
- **Punto de activación:** Paso 4
- **Flujo:**
  - 4.1. Sistema detecta batería crítica
  - 4.2. Sistema muestra advertencia "🔋 Batería baja - transmisión limitada"
  - 4.3. Sistema reduce canales a solo linterna (más eficiente)
  - 4.4. Continúa en paso 6

**FA-001.4: Usuario sale de la app accidentalmente**
- **Punto de activación:** Durante paso 7
- **Flujo:**
  - 7.1. Sistema mantiene transmisión en background
  - 7.2. Sistema muestra notificación persistente "🚨 SOS en curso"
  - 7.3. Usuario toca notificación para volver a pantalla
  - 7.4. Continúa en paso 7

### Flujos de Excepción

**FE-001.1: Error de hardware LED**
- **Punto de activación:** Paso 5.1
- **Flujo:**
  - 5.1.1. Sistema detecta fallo en LED
  - 5.1.2. Sistema registra error en log
  - 5.1.3. Sistema continúa con otros canales
  - 5.1.4. Sistema muestra "⚠️ Error en linterna"
  - 5.1.5. Continúa en paso 6

**FE-001.2: Aplicación crashea**
- **Punto de activación:** Cualquier paso
- **Flujo:**
  - X.1. Sistema detecta crash inminente
  - X.2. Sistema guarda estado de emergencia
  - X.3. Sistema reinicia automáticamente
  - X.4. Sistema restaura transmisión SOS
  - X.5. Continúa desde paso 6

### Requerimientos Relacionados
- RF-010: Transmisión de Emergencia
- RF-001: Transmisión por Luz
- RF-003: Transmisión por Vibración
- RF-005: Transmisión por Sonido
- RNF-003.1: Curva de aprendizaje < 10 segundos

### Notas Adicionales
- Patrón SOS en Morse: `· · · — — — · · ·` (3 cortos, 3 largos, 3 cortos)
- Duración total del patrón: ~7.8 segundos (con timings v2.0: DOT=200ms, DASH=600ms)
- Pausa entre repeticiones: 2.0 segundos
- Total por ciclo: ~10 segundos

---

## CU-002: Transmitir HELP de Emergencia

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-002 |
| **Nombre** | Transmitir HELP de Emergencia |
| **Actor Principal** | Víctima de Emergencia |
| **Actores Secundarios** | Ninguno |
| **Tipo** | Primario, Crítico |
| **Complejidad** | Baja |
| **Prioridad** | Crítica |

### Descripción
El usuario necesita enviar una señal de ayuda HELP para situaciones de peligro que no son extremas como SOS.

### Precondiciones
- PRE-002.1: Aplicación BeaconChat instalada y abierta
- PRE-002.2: Batería suficiente (>5%)

### Postcondiciones
- POST-002.1: Señal HELP transmitida continuamente
- POST-002.2: Feedback visual activo

### Flujo Principal
1. Usuario abre BeaconChat
2. Sistema muestra pantalla de bienvenida
3. Usuario presiona botón **"HELP"** (amarillo/naranja)
4. Sistema valida hardware disponible
5. Sistema inicia transmisión de "HELP" en Morse:
   - Patrón: `· · · · · — · · — — · ·`
6. Sistema muestra pantalla fullscreen con:
   - Animación pulsante amarilla
   - Texto "TRANSMITIENDO HELP"
   - Temporizador
   - Contador de ciclos
   - Botón "CANCELAR"
7. Sistema repite patrón cada 6 segundos
8. Usuario cancela cuando sea necesario
9. Sistema detiene transmisión
10. Sistema retorna a inicio
11. **Fin del caso de uso**

### Flujos Alternativos
(Similares a CU-001)

### Requerimientos Relacionados
- RF-010: Transmisión de Emergencia
- RF-001, RF-003, RF-005

---

## CU-003: Enviar Mensaje Personalizado

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-003 |
| **Nombre** | Enviar Mensaje Personalizado |
| **Actor Principal** | Usuario General |
| **Actores Secundarios** | Ninguno |
| **Tipo** | Primario |
| **Complejidad** | Media |
| **Prioridad** | Alta |

### Descripción
El usuario quiere transmitir un mensaje de texto personalizado usando código Morse a través de un canal específico.

### Precondiciones
- PRE-003.1: Aplicación abierta
- PRE-003.2: Usuario en pantalla "Transmit"

### Postcondiciones
- POST-003.1: Mensaje transmitido correctamente
- POST-003.2: Confirmación visual mostrada

### Flujo Principal
1. Usuario navega a pantalla "Transmit" desde barra inferior
2. Sistema muestra interfaz de transmisión con:
   - Campo de texto vacío (placeholder: "Escribe tu mensaje...")
   - Selector de método (Luz/Vibración/Sonido)
   - Selector de codificación (Morse/Binario)
   - Indicador de alfabeto Morse activo
   - Botones de acción
3. Usuario escribe mensaje en campo de texto
   - Ejemplo: "AYUDA EDIFICIO 5"
4. Sistema muestra contador de caracteres (ej: "18/200")
5. Usuario selecciona método de transmisión (por defecto: Linterna)
6. Sistema muestra vista previa del alfabeto Morse detectado
   - Ejemplo: "🇪🇸 Español (Latino)" con bandera
7. Usuario presiona botón **"ENVIAR ONCE"**
8. Sistema valida mensaje no vacío
9. Sistema codifica texto a secuencia Morse
   - Tiempo de codificación: <100ms
10. Sistema muestra indicador "Transmitiendo..."
11. Sistema ejecuta transmisión:
    - 11.1. Envía marcador START (300-300-900ms)
    - 11.2. Transmite cada carácter en Morse
    - 11.3. Envía marcador END (600-100ms)
12. Sistema completa transmisión
13. Sistema muestra mensaje "✓ Mensaje enviado"
14. Sistema limpia campo de texto
15. **Fin del caso de uso**

### Flujos Alternativos

**FA-003.1: Transmisión Continua**
- **Punto de activación:** Paso 7
- **Flujo:**
  - 7.1. Usuario presiona **"TRANSMITIR CONTINUAMENTE"**
  - 7.2. Sistema inicia loop infinito
  - 7.3. Sistema muestra botón "DETENER"
  - 7.4. Sistema transmite mensaje + pausa 500ms
  - 7.5. Repite desde 7.4 hasta que usuario presiona "DETENER"
  - 7.6. Sistema detiene transmisión
  - 7.7. Continúa en paso 14

**FA-003.2: Usar Botón Rápido**
- **Punto de activación:** Paso 3
- **Flujo:**
  - 3.1. Usuario presiona botón rápido (SOS/HELP/OK/AYUDA)
  - 3.2. Sistema inserta texto predefinido en campo
  - 3.3. Continúa en paso 5

**FA-003.3: Cambiar Método de Transmisión**
- **Punto de activación:** Paso 5
- **Flujo:**
  - 5.1. Usuario selecciona "Vibración" o "Sonido"
  - 5.2. Sistema actualiza icono activo
  - 5.3. Continúa en paso 7

**FA-003.4: Mensaje en Otro Idioma**
- **Punto de activación:** Paso 3
- **Flujo:**
  - 3.1. Usuario escribe "مساعدة" (árabe)
  - 3.2. Sistema detecta caracteres árabes
  - 3.3. Sistema cambia automáticamente a alfabeto árabe
  - 3.4. Sistema muestra "🇸🇦 Árabe"
  - 3.5. Continúa en paso 7

### Flujos de Excepción

**FE-003.1: Mensaje Vacío**
- **Punto de activación:** Paso 8
- **Flujo:**
  - 8.1. Sistema detecta campo vacío
  - 8.2. Sistema muestra toast "⚠️ Escribe un mensaje"
  - 8.3. Retorna a paso 3

**FE-003.2: Mensaje Muy Largo**
- **Punto de activación:** Paso 3
- **Flujo:**
  - 3.1. Usuario intenta escribir más de 200 caracteres
  - 3.2. Sistema bloquea input adicional
  - 3.3. Sistema muestra "200/200 (máximo)"
  - 3.4. Continúa en paso 5

**FE-003.3: Carácter No Soportado**
- **Punto de activación:** Paso 9
- **Flujo:**
  - 9.1. Sistema encuentra carácter sin código Morse
  - 9.2. Sistema lo reemplaza con "?"
  - 9.3. Sistema muestra advertencia "⚠️ Algunos caracteres no soportados"
  - 9.4. Continúa en paso 10

### Requerimientos Relacionados
- RF-001: Transmisión por Luz
- RF-003: Transmisión por Vibración
- RF-005: Transmisión por Sonido
- RF-009: Soporte Multi-Idioma Morse

---

## CU-004: Recibir Mensaje por Luz

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-004 |
| **Nombre** | Recibir Mensaje por Luz |
| **Actor Principal** | Rescatista / Usuario General |
| **Actores Secundarios** | Víctima (emisor) |
| **Tipo** | Primario |
| **Complejidad** | Alta |
| **Prioridad** | Alta |

### Descripción
El usuario apunta su cámara hacia una fuente de luz parpadeante para detectar y decodificar un mensaje en código Morse.

### Precondiciones
- PRE-004.1: Permiso de cámara otorgado
- PRE-004.2: Usuario en pantalla "Receive"
- PRE-004.3: Hay una fuente de luz Morse en rango visual

### Postcondiciones
- POST-004.1: Mensaje decodificado y mostrado
- POST-004.2: Historial de mensaje guardado (temporal)

### Flujo Principal
1. Usuario navega a pantalla "Receive"
2. Sistema solicita permiso de cámara (si no fue otorgado)
3. Usuario otorga permiso
4. Sistema inicializa cámara trasera
5. Sistema muestra vista previa de cámara en tiempo real
6. Sistema inicia análisis de frames a 30 fps
7. Sistema muestra indicador "🔍 Esperando señal..."
8. Usuario apunta cámara hacia luz parpadeante
9. Sistema procesa cada frame:
   - 9.1. Calcula intensidad lumínica promedio
   - 9.2. Aplica filtro suavizado exponencial (α=0.7)
   - 9.3. Calcula umbral dinámico adaptativo
   - 9.4. Detecta transiciones ON/OFF
10. Sistema detecta marcador START (300-300-900ms)
11. Sistema cambia indicador a "📥 RECIBIENDO..."
12. Sistema comienza decodificación en tiempo real:
    - 12.1. Mide duración de cada pulso
    - 12.2. Clasifica como DOT (80-200ms) o DASH (200-500ms)
    - 12.3. Detecta GAPs entre símbolos/letras/palabras
    - 12.4. Traduce Morse a caracteres
13. Sistema muestra mensaje parcial mientras decodifica:
    - Ejemplo: "AY..." → "AYUD..." → "AYUDA"
14. Sistema detecta marcador END (600-100ms)
15. Sistema detiene decodificación
16. Sistema muestra mensaje completo:
    - Texto: "AYUDA EDIFICIO 5"
    - Timestamp: "14:35:22"
    - Estado: "✓ Mensaje completo"
17. Sistema reproduce sonido de confirmación (beep)
18. Sistema mantiene mensaje en pantalla
19. Usuario lee mensaje decodificado
20. **Fin del caso de uso**

### Flujos Alternativos

**FA-004.1: Ver Osciloscopio Óptico**
- **Punto de activación:** Después de paso 5
- **Flujo:**
  - 5.1. Usuario navega a pestaña "Oscilloscope"
  - 5.2. Sistema muestra gráfico de intensidad en tiempo real
  - 5.3. Sistema visualiza símbolos detectados (DOT/DASH/GAP)
  - 5.4. Usuario observa para diagnóstico
  - 5.5. Usuario retorna a vista normal
  - 5.6. Continúa en paso 8

**FA-004.2: Mensaje en Bucle**
- **Punto de activación:** Paso 14
- **Flujo:**
  - 14.1. Transmisor repite mensaje
  - 14.2. Sistema detecta nuevo START
  - 14.3. Sistema verifica si es mismo mensaje
  - 14.4. Si es igual: incrementa contador "Recibido 2x"
  - 14.5. Si es diferente: muestra como nuevo mensaje
  - 14.6. Continúa en paso 12

**FA-004.3: Múltiples Fuentes de Luz**
- **Punto de activación:** Paso 9
- **Flujo:**
  - 9.1. Sistema detecta múltiples variaciones de luz
  - 9.2. Sistema prioriza señal con patrón más claro
  - 9.3. Sistema muestra advertencia "⚠️ Múltiples señales"
  - 9.4. Continúa en paso 10

### Flujos de Excepción

**FE-004.1: Permiso de Cámara Denegado**
- **Punto de activación:** Paso 3
- **Flujo:**
  - 3.1. Usuario deniega permiso
  - 3.2. Sistema muestra diálogo explicativo
  - 3.3. Sistema ofrece ir a Configuración
  - 3.4. Usuario cancela
  - 3.5. Sistema retorna a pantalla anterior
  - 3.6. **Fin del caso de uso**

**FE-004.2: Timeout sin START**
- **Punto de activación:** Paso 10
- **Flujo:**
  - 10.1. Pasan 30 segundos sin detectar START
  - 10.2. Sistema mantiene estado "Esperando..."
  - 10.3. Usuario puede seguir intentando indefinidamente

**FE-004.3: Señal Interrumpida**
- **Punto de activación:** Durante paso 12
- **Flujo:**
  - 12.1. Señal de luz se pierde durante decodificación
  - 12.2. Sistema espera 3 segundos
  - 12.3. Sistema muestra "⚠️ Señal perdida - mensaje incompleto"
  - 12.4. Sistema muestra mensaje parcial decodificado
  - 12.5. Retorna a paso 7

**FE-004.4: Ruido Excesivo**
- **Punto de activación:** Paso 9
- **Flujo:**
  - 9.1. Sistema detecta variaciones irregulares
  - 9.2. Sistema ajusta umbral dinámicamente
  - 9.3. Si no mejora: muestra "⚠️ Señal muy ruidosa"
  - 9.4. Usuario intenta mejorar ángulo/distancia
  - 9.5. Retorna a paso 9

### Requerimientos Relacionados
- RF-002: Recepción por Cámara
- RF-013: Osciloscopio Óptico
- RNF-001.3: Frame Rate 30 fps
- RNF-002.3: Tasa error < 5%

---

## CU-005: Detectar Vibración Morse

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-005 |
| **Nombre** | Detectar Vibración Morse |
| **Actor Principal** | Rescatista |
| **Actores Secundarios** | Víctima enterrada (emisor) |
| **Tipo** | Primario |
| **Complejidad** | Alta |
| **Prioridad** | Media |

### Descripción
El usuario detecta mensajes Morse transmitidos por vibración, útil cuando no hay línea de vista o en oscuridad total.

### Precondiciones
- PRE-005.1: Dispositivo tiene acelerómetro funcional
- PRE-005.2: Usuario en pantalla "Receive"

### Postcondiciones
- POST-005.1: Mensaje de vibración decodificado
- POST-005.2: Datos de aceleración liberados

### Flujo Principal
1. Usuario está en pantalla "Receive"
2. Usuario presiona botón **"Detector de Vibración"**
3. Sistema navega a pantalla "VibrationDetector"
4. Sistema registra listener de acelerómetro
5. Sistema configura muestreo a ~200 Hz
6. Sistema muestra osciloscopio de vibración:
   - Gráfico de magnitud en tiempo real
   - Umbral dinámico visualizado
   - Indicador "🔍 Esperando vibración..."
7. Usuario coloca dispositivo contra superficie vibrante
   - Ejemplo: Pared, suelo, objeto metálico
8. Sistema captura datos del acelerómetro (x, y, z)
9. Sistema procesa cada muestra:
   - 9.1. Elimina componente de gravedad (filtro pasa-alto)
   - 9.2. Calcula magnitud: √(x² + y² + z²)
   - 9.3. Aplica suavizado exponencial
   - 9.4. Actualiza umbral dinámico
10. Sistema detecta pulsos de vibración:
    - ON: magnitud > umbral
    - OFF: magnitud < umbral
11. Sistema clasifica pulsos:
    - 80-200ms: DOT
    - 200-500ms: DASH
    - >500ms: GAP
12. Sistema traduce a caracteres Morse
13. Sistema muestra mensaje mientras decodifica:
    - Ejemplo: "S..." → "SO..." → "SOS"
14. Transmisor detiene vibración (fin de mensaje)
15. Sistema detecta silencio prolongado (>2s)
16. Sistema muestra mensaje completo: "SOS"
17. Sistema reproduce confirmación sonora
18. Usuario lee mensaje
19. **Fin del caso de uso**

### Flujos Alternativos

**FA-005.1: Vibración Muy Débil**
- **Punto de activación:** Paso 10
- **Flujo:**
  - 10.1. Sistema detecta magnitudes muy bajas
  - 10.2. Sistema reduce umbral dinámico
  - 10.3. Sistema muestra "🔉 Señal débil - acerca más"
  - 10.4. Usuario acerca más el dispositivo
  - 10.5. Continúa en paso 10

**FA-005.2: Ruido Ambiental**
- **Punto de activación:** Paso 9
- **Flujo:**
  - 9.1. Sistema detecta vibraciones constantes no-Morse
  - 9.2. Sistema ajusta filtro pasa-alto
  - 9.3. Sistema muestra "⚠️ Ruido ambiente detectado"
  - 9.4. Continúa en paso 10

### Flujos de Excepción

**FE-005.1: Acelerómetro No Disponible**
- **Punto de activación:** Paso 4
- **Flujo:**
  - 4.1. Sistema detecta falta de sensor
  - 4.2. Sistema muestra error "❌ Dispositivo sin acelerómetro"
  - 4.3. Sistema ofrece alternativa "Usa cámara para luz"
  - 4.4. Usuario retorna a pantalla anterior
  - 4.5. **Fin del caso de uso**

### Requerimientos Relacionados
- RF-004: Detección de Vibración
- RF-014: Osciloscopio de Vibración
- RNF-001.4: Sampling 200 Hz

---

## CU-006: Descubrir y Chatear en Red Mesh

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-006 |
| **Nombre** | Descubrir y Chatear en Red Mesh |
| **Actor Principal** | Usuario General / Radioaficionado |
| **Actores Secundarios** | Otros usuarios con BeaconChat |
| **Tipo** | Primario |
| **Complejidad** | Alta |
| **Prioridad** | Media |

### Descripción
El usuario descubre otros dispositivos BeaconChat cercanos vía Bluetooth LE y establece comunicación de chat peer-to-peer.

### Precondiciones
- PRE-006.1: Permisos Bluetooth otorgados
- PRE-006.2: Bluetooth habilitado en dispositivo
- PRE-006.3: Callsign configurado

### Postcondiciones
- POST-006.1: Mensajes intercambiados exitosamente
- POST-006.2: Historial de chat almacenado (temporal)

### Flujo Principal
1. Usuario navega a pantalla "Mesh"
2. Sistema verifica permisos Bluetooth:
   - Android <12: BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION
   - Android ≥12: BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT
3. Si no otorgados: Sistema solicita permisos
4. Usuario otorga permisos
5. Sistema verifica Bluetooth habilitado
6. Sistema solicita callsign si no está configurado:
   - Diálogo: "Ingresa tu identificador (ej: RESCUE01)"
7. Usuario ingresa callsign "RESCUE01"
8. Sistema valida callsign (alfanumérico, 3-10 caracteres)
9. Sistema guarda callsign en preferencias
10. Sistema inicia BLE advertising:
    - Service UUID: 0000BEEF-...
    - Service Data: callsign en bytes
11. Sistema inicia BLE scanning:
    - Filtro: Service UUID BeaconChat
12. Sistema muestra interfaz principal:
    - Lista de dispositivos (vacía inicialmente)
    - Indicador "🔍 Escaneando..."
    - Botón de toggle "Escaneo: ON"
13. Otro usuario (USER02) con BeaconChat aparece en rango (30m)
14. Sistema detecta dispositivo en callback de scan
15. Sistema parsea callsign desde service data: "USER02"
16. Sistema crea objeto MeshPeer:
    - id: MAC address
    - callsign: "USER02"
    - rssi: -65 dBm
    - lastSeen: timestamp actual
17. Sistema agrega a lista de pares
18. Sistema muestra en UI:
    - "USER02"
    - Señal: "Good" (basado en RSSI)
    - Última vez: "Now"
19. Usuario selecciona "USER02" de la lista
20. Sistema abre pantalla de Chat individual
21. Sistema muestra historial vacío
22. Usuario escribe mensaje: "¿Estás bien?"
23. Usuario presiona botón "Enviar"
24. Sistema inicia conexión GATT a USER02:
    - Conecta como cliente
    - Descubre servicios
    - Encuentra Chat Service (0000C4A7-...)
    - Encuentra Message Characteristic (00004D51-...)
25. Sistema escribe mensaje en característica:
    - Formato: "RESCUE01:¿Estás bien?"
    - Encoding: UTF-8
26. Sistema recibe callback onCharacteristicWrite success
27. Sistema muestra mensaje en chat con:
    - Texto: "¿Estás bien?"
    - Timestamp: "14:45"
    - Indicador: "✓"
    - Alineación: derecha (mensaje propio)
28. Sistema desconecta GATT
29. USER02 recibe mensaje en su GATT server
30. USER02 responde: "Sí, gracias"
31. Sistema recibe en GATT server callback
32. Sistema parsea mensaje: callsign="USER02", content="Sí, gracias"
33. Sistema muestra en chat:
    - Texto: "Sí, gracias"
    - Timestamp: "14:46"
    - Alineación: izquierda (mensaje recibido)
34. Usuario continúa conversación
35. **Fin del caso de uso**

### Flujos Alternativos

**FA-006.1: Ver Radar de Dispositivos**
- **Punto de activación:** Paso 12
- **Flujo:**
  - 12.1. Usuario navega a pestaña "Radar"
  - 12.2. Sistema muestra visualización tipo radar
  - 12.3. Sistema anima barrido circular
  - 12.4. Sistema posiciona dispositivos según RSSI
  - 12.5. Usuario observa red visualmente
  - 12.6. Retorna a lista

**FA-006.2: Dispositivo Desaparece**
- **Punto de activación:** Durante paso 18
- **Flujo:**
  - 18.1. USER02 sale de rango o apaga Bluetooth
  - 18.2. Sistema no recibe más advertisements
  - 18.3. Después de 30s: Sistema actualiza "Última vez: 30s"
  - 18.4. Sistema mantiene en lista (puede volver)
  - 18.5. Si pasan 5 minutos: Sistema remueve de lista

**FA-006.3: Múltiples Dispositivos**
- **Punto de activación:** Paso 17
- **Flujo:**
  - 17.1. Sistema detecta RESCUE02, RESCUE03, USER04
  - 17.2. Sistema agrega todos a lista
  - 17.3. Sistema ordena por RSSI (más fuerte primero)
  - 17.4. Usuario elige con cuál chatear
  - 17.5. Continúa en paso 19

### Flujos de Excepción

**FE-006.1: Permisos Bluetooth Denegados**
- **Punto de activación:** Paso 4
- **Flujo:**
  - 4.1. Usuario deniega permisos
  - 4.2. Sistema muestra explicación "Necesario para encontrar dispositivos"
  - 4.3. Sistema ofrece ir a Configuración
  - 4.4. Usuario cancela
  - 4.5. Sistema deshabilita funcionalidad Mesh
  - 4.6. **Fin del caso de uso**

**FE-006.2: Bluetooth Deshabilitado**
- **Punto de activación:** Paso 5
- **Flujo:**
  - 5.1. Sistema detecta Bluetooth OFF
  - 5.2. Sistema muestra botón "Activar Bluetooth"
  - 5.3. Usuario presiona botón
  - 5.4. Sistema lanza Intent para habilitar
  - 5.5. Usuario confirma
  - 5.6. Bluetooth se activa
  - 5.7. Continúa en paso 6

**FE-006.3: Fallo al Enviar Mensaje**
- **Punto de activación:** Paso 24
- **Flujo:**
  - 24.1. Conexión GATT falla (dispositivo fuera de rango)
  - 24.2. Sistema recibe onConnectionStateChange DISCONNECTED
  - 24.3. Sistema muestra "❌ No se pudo enviar - dispositivo fuera de alcance"
  - 24.4. Mensaje queda marcado con "⚠️"
  - 24.5. Usuario puede reintentar más tarde

**FE-006.4: Callsign Inválido**
- **Punto de activación:** Paso 8
- **Flujo:**
  - 8.1. Usuario ingresa "A" (muy corto) o "TOOLONGCALLSIGN123"
  - 8.2. Sistema muestra error "Callsign debe tener 3-10 caracteres alfanuméricos"
  - 8.3. Retorna a paso 7

### Requerimientos Relacionados
- RF-006: Red Mesh Bluetooth
- RF-011: Configuración de Usuario
- RF-012: Radar de Dispositivos

---

## CU-007: Generar Código QR

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-007 |
| **Nombre** | Generar Código QR con Mensaje |
| **Actor Principal** | Usuario General |
| **Actores Secundarios** | Ninguno |
| **Tipo** | Secundario |
| **Complejidad** | Baja |
| **Prioridad** | Baja |

### Descripción
El usuario genera un código QR que contiene un mensaje y su callsign para ser escaneado por otro dispositivo.

### Precondiciones
- PRE-007.1: Usuario en pantalla "Transmit"
- PRE-007.2: Callsign configurado

### Postcondiciones
- POST-007.1: Código QR visible en pantalla
- POST-007.2: QR contiene mensaje y callsign

### Flujo Principal
1. Usuario navega a "Transmit"
2. Usuario escribe mensaje "Ayuda en edificio 5"
3. Usuario selecciona opción "Generar QR"
4. Sistema obtiene callsign del usuario: "RESCUE01"
5. Sistema crea string con formato:
   - `{"callsign":"RESCUE01","message":"Ayuda en edificio 5"}`
6. Sistema genera código QR:
   - Tamaño: 512x512 píxeles
   - Nivel de corrección: Medium
7. Sistema muestra QR en pantalla fullscreen
8. Sistema muestra instrucciones: "Acerca este QR a otra cámara"
9. Usuario posiciona dispositivo contra ventana/superficie visible
10. Otro usuario escanea el QR
11. Usuario presiona "Cerrar" cuando termine
12. **Fin del caso de uso**

### Flujos Alternativos

**FA-007.1: Guardar QR como Imagen**
- **Punto de activación:** Paso 8
- **Flujo:**
  - 8.1. Sistema muestra botón "Guardar"
  - 8.2. Usuario presiona "Guardar"
  - 8.3. Sistema guarda QR en galería
  - 8.4. Sistema muestra "✓ QR guardado en galería"

### Requerimientos Relacionados
- RF-007: Generación de QR

---

## CU-008: Escanear Código QR

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-008 |
| **Nombre** | Escanear Código QR de BeaconChat |
| **Actor Principal** | Usuario General / Rescatista |
| **Actores Secundarios** | Usuario emisor del QR |
| **Tipo** | Secundario |
| **Complejidad** | Media |
| **Prioridad** | Baja |

### Descripción
El usuario escanea un código QR generado por otro BeaconChat para leer el mensaje instantáneamente.

### Precondiciones
- PRE-008.1: Permiso de cámara otorgado
- PRE-008.2: Usuario en pantalla "Receive"

### Postcondiciones
- POST-008.1: Mensaje del QR decodificado y mostrado

### Flujo Principal
1. Usuario navega a "Receive"
2. Usuario presiona botón "Escanear QR"
3. Sistema activa cámara con detector QR
4. Sistema muestra vista previa con marco de guía
5. Usuario apunta cámara hacia código QR
6. Sistema detecta código QR en frame
7. Sistema decodifica contenido:
   - String: `{"callsign":"RESCUE01","message":"Ayuda en edificio 5"}`
8. Sistema parsea JSON
9. Sistema valida formato de BeaconChat
10. Sistema muestra mensaje:
    - "💬 Mensaje de: RESCUE01"
    - "Ayuda en edificio 5"
    - Timestamp de escaneo
11. Sistema reproduce sonido de confirmación
12. Usuario lee mensaje
13. **Fin del caso de uso**

### Flujos de Excepción

**FE-008.1: QR No es de BeaconChat**
- **Punto de activación:** Paso 9
- **Flujo:**
  - 9.1. JSON no tiene campos "callsign" y "message"
  - 9.2. Sistema muestra "⚠️ Este QR no es de BeaconChat"
  - 9.3. Sistema ignora y continúa escaneando

### Requerimientos Relacionados
- RF-008: Escaneo de QR

---

## CU-009: Configurar Callsign

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-009 |
| **Nombre** | Configurar Identificador Personal (Callsign) |
| **Actor Principal** | Usuario General |
| **Actores Secundarios** | Ninguno |
| **Tipo** | Secundario |
| **Complejidad** | Baja |
| **Prioridad** | Media |

### Descripción
El usuario configura su identificador único (callsign) que será usado en mesh y QR.

### Precondiciones
- PRE-009.1: Usuario en pantalla "Settings"

### Postcondiciones
- POST-009.1: Callsign guardado persistentemente
- POST-009.2: Disponible para mesh y QR

### Flujo Principal
1. Usuario navega a "Settings"
2. Sistema muestra configuración actual:
   - Callsign: "UNKNOWN" (si no está configurado)
3. Usuario presiona campo "Callsign"
4. Sistema muestra diálogo de edición
5. Usuario escribe "RESCUE01"
6. Usuario presiona "Guardar"
7. Sistema valida formato:
   - 7.1. Longitud 3-10 caracteres
   - 7.2. Solo alfanuméricos
8. Sistema guarda en SharedPreferences
9. Sistema actualiza UI: "Callsign: RESCUE01"
10. Sistema muestra "✓ Callsign actualizado"
11. **Fin del caso de uso**

### Flujos de Excepción

**FE-009.1: Callsign Inválido**
- **Punto de activación:** Paso 7
- **Flujo:**
  - 7.1. Usuario ingresa "@#$%"
  - 7.2. Sistema detecta caracteres no permitidos
  - 7.3. Sistema muestra "❌ Solo letras y números (3-10 caracteres)"
  - 7.4. Retorna a paso 5

### Requerimientos Relacionados
- RF-011: Configuración de Usuario

---

## CU-010: Visualizar Osciloscopio Óptico

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-010 |
| **Nombre** | Visualizar Osciloscopio Óptico en Tiempo Real |
| **Actor Principal** | Radioaficionado / Usuario Avanzado |
| **Actores Secundarios** | Ninguno |
| **Tipo** | Secundario |
| **Complejidad** | Media |
| **Prioridad** | Media |

### Descripción
El usuario visualiza la intensidad lumínica en tiempo real y los símbolos Morse detectados para diagnóstico y ajuste.

### Precondiciones
- PRE-010.1: Cámara activa en modo recepción
- PRE-010.2: Usuario en pantalla "Oscilloscope"

### Postcondiciones
- POST-010.1: Visualización en tiempo real activa

### Flujo Principal
1. Usuario navega a pantalla "Oscilloscope"
2. Sistema activa análisis de frames
3. Sistema muestra gráfico con:
   - Eje X: tiempo (últimos 5 segundos)
   - Eje Y: intensidad lumínica (0-255)
   - Línea de umbral dinámico (color naranja)
   - Línea de intensidad actual (color verde)
4. Sistema actualiza gráfico a 10 fps
5. Usuario apunta cámara hacia luz
6. Sistema detecta cambios de intensidad
7. Sistema visualiza símbolos en tiempo real:
   - "DOT" cuando detecta pulso ~200ms (≥60ms de tolerancia)
   - "DASH" cuando detecta pulso ~600ms (≥60ms de tolerancia)
   - "GAP" cuando detecta espacio >400ms
8. Sistema muestra buffer de símbolos:
   - Ejemplo: "DOT DOT DOT DASH DASH DASH DOT DOT DOT"
9. Sistema decodifica a letras bajo el gráfico:
   - Ejemplo: "S O S"
10. Usuario observa calidad de señal
11. Usuario ajusta ángulo/distancia según gráfico
12. **Fin del caso de uso**

### Flujos Alternativos

**FA-010.1: Ajustar Sensibilidad**
- **Punto de activación:** Paso 5
- **Flujo:**
  - 5.1. Usuario mueve slider de sensibilidad
  - 5.2. Sistema ajusta multiplicador de umbral (0.3-0.6)
  - 5.3. Sistema actualiza línea de umbral en gráfico
  - 5.4. Continúa en paso 6

### Requerimientos Relacionados
- RF-013: Osciloscopio Óptico

---

## CU-011: Usar Mensajes Rápidos

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-011 |
| **Nombre** | Usar Botones de Mensaje Rápido |
| **Actor Principal** | Víctima / Usuario General |
| **Actores Secundarios** | Ninguno |
| **Tipo** | Primario |
| **Complejidad** | Baja |
| **Prioridad** | Media |

### Descripción
El usuario envía mensajes predefinidos comunes con un solo toque para agilizar comunicación.

### Precondiciones
- PRE-011.1: Usuario en pantalla "Transmit"

### Postcondiciones
- POST-011.1: Mensaje predefinido transmitido

### Flujo Principal
1. Usuario abre "Transmit"
2. Sistema muestra botones rápidos:
   - "SOS" (rojo)
   - "HELP" (amarillo)
   - "OK" (verde)
   - "AYUDA" (naranja)
3. Usuario presiona botón "OK"
4. Sistema inserta "OK" en campo de texto
5. Sistema inicia transmisión automáticamente
6. Sistema transmite "OK" una vez
7. Sistema muestra "✓ OK enviado"
8. **Fin del caso de uso**

### Flujos Alternativos

**FA-011.1: Editar Mensaje Rápido**
- **Punto de activación:** Paso 4
- **Flujo:**
  - 4.1. Usuario edita texto "OK" → "OK EDIFICIO 3"
  - 4.2. Usuario presiona "Enviar"
  - 4.3. Sistema transmite mensaje editado
  - 4.4. Continúa en paso 7

### Requerimientos Relacionados
- RF-001: Transmisión por Luz
- RNF-003.1: Usabilidad

---

## CU-012: Cambiar Alfabeto Morse

### Información General
| Campo | Valor |
|-------|-------|
| **ID** | CU-012 |
| **Nombre** | Cambiar Alfabeto Morse Automáticamente |
| **Actor Principal** | Usuario Internacional |
| **Actores Secundarios** | Ninguno |
| **Tipo** | Primario |
| **Complejidad** | Media |
| **Prioridad** | Alta |

### Descripción
El sistema detecta automáticamente el alfabeto del texto ingresado y usa la tabla Morse correspondiente.

### Precondiciones
- PRE-012.1: Usuario escribiendo mensaje en "Transmit"

### Postcondiciones
- POST-012.1: Alfabeto Morse correcto seleccionado

### Flujo Principal
1. Usuario abre "Transmit"
2. Sistema detecta Locale del dispositivo: Español
3. Sistema selecciona alfabeto Latino por defecto
4. Sistema muestra "🇪🇸 Español (Latino)"
5. Usuario escribe "مساعدة" (árabe: "ayuda")
6. Sistema detecta caracteres árabes en texto
7. Sistema cambia automáticamente a alfabeto Árabe
8. Sistema actualiza UI: "🇸🇦 Árabe"
9. Usuario ve confirmación visual del cambio
10. Usuario transmite mensaje
11. Sistema codifica usando tabla Morse árabe
12. **Fin del caso de uso**

### Flujos Alternativos

**FA-012.1: Texto Mixto**
- **Punto de activación:** Paso 6
- **Flujo:**
  - 6.1. Usuario escribe "HELP مساعدة"
  - 6.2. Sistema detecta múltiples scripts
  - 6.3. Sistema prioriza primer script detectado
  - 6.4. Sistema muestra "⚠️ Texto mixto - usando Latino"

### Requerimientos Relacionados
- RF-009: Soporte Multi-Idioma Morse

---

## 5. Matriz de Trazabilidad

### Tabla de Trazabilidad: Casos de Uso → Requerimientos Funcionales

| Caso de Uso | RF Relacionados | Pantalla Principal | Prioridad |
|-------------|-----------------|-------------------|-----------|
| CU-001: SOS | RF-010, RF-001, RF-003, RF-005 | Welcome, Emergency | Crítica |
| CU-002: HELP | RF-010, RF-001, RF-003, RF-005 | Welcome, Emergency | Crítica |
| CU-003: Mensaje Personalizado | RF-001, RF-003, RF-005, RF-009 | Transmit | Alta |
| CU-004: Recibir Luz | RF-002, RF-013 | Receive, Oscilloscope | Alta |
| CU-005: Detectar Vibración | RF-004, RF-014 | Receive, VibrationDetector | Media |
| CU-006: Mesh Chat | RF-006, RF-011, RF-012 | Mesh | Media |
| CU-007: Generar QR | RF-007 | Transmit | Baja |
| CU-008: Escanear QR | RF-008 | Receive | Baja |
| CU-009: Configurar Callsign | RF-011 | Settings | Media |
| CU-010: Osciloscopio Óptico | RF-013 | Oscilloscope | Media |
| CU-011: Mensajes Rápidos | RF-001 | Transmit | Media |
| CU-012: Cambiar Alfabeto | RF-009 | Transmit | Alta |

### Tabla de Trazabilidad: Actores → Casos de Uso

| Actor | Casos de Uso Primarios | Casos de Uso Secundarios |
|-------|------------------------|--------------------------|
| Víctima de Emergencia | CU-001, CU-002, CU-011 | CU-003, CU-007 |
| Rescatista Profesional | CU-004, CU-005, CU-006 | CU-008, CU-010 |
| Usuario General | CU-003, CU-006 | CU-007, CU-008, CU-009, CU-011 |
| Radioaficionado | CU-003, CU-006, CU-012 | CU-010 |

### Cobertura de Requerimientos

| Requerimiento Funcional | Casos de Uso | Cobertura |
|------------------------|--------------|-----------|
| RF-001: Transmisión Luz | CU-001, CU-002, CU-003, CU-011 | ✅ 100% |
| RF-002: Recepción Luz | CU-004 | ✅ 100% |
| RF-003: Transmisión Vibración | CU-001, CU-002, CU-003 | ✅ 100% |
| RF-004: Detección Vibración | CU-005 | ✅ 100% |
| RF-005: Transmisión Sonido | CU-001, CU-002, CU-003 | ✅ 100% |
| RF-006: Red Mesh | CU-006 | ✅ 100% |
| RF-007: Generación QR | CU-007 | ✅ 100% |
| RF-008: Escaneo QR | CU-008 | ✅ 100% |
| RF-009: Multi-Idioma | CU-003, CU-012 | ✅ 100% |
| RF-010: Emergencias | CU-001, CU-002 | ✅ 100% |
| RF-011: Configuración | CU-006, CU-009 | ✅ 100% |
| RF-012: Radar | CU-006 | ✅ 100% |
| RF-013: Osciloscopio Óptico | CU-004, CU-010 | ✅ 100% |
| RF-014: Osciloscopio Vibración | CU-005 | ✅ 100% |

---

## Apéndice A: Diagramas de Secuencia

### Diagrama: CU-001 Transmisión SOS

```
Usuario          MainActivity    FlashlightCtrl   VibrationCtrl   SoundCtrl
  │                   │                │                │             │
  ├──Presiona SOS────>│                │                │             │
  │                   ├──validate()───>│                │             │
  │                   ├──validate()─────────────────────>│             │
  │                   ├──validate()───────────────────────────────────>│
  │                   │                │                │             │
  │                   ├──encode("SOS")─>│                │             │
  │                   │<──timings[]────┤                │             │
  │                   │                │                │             │
  │                   ├──transmit()───>│                │             │
  │                   ├──transmit()─────────────────────>│             │
  │                   ├──transmit()───────────────────────────────────>│
  │                   │                │                │             │
  │<──UI Update───────┤                │                │             │
  │  "Transmitiendo"  │                │                │             │
  │                   │                │                │             │
  │                   │   [Loop every 5s]                │             │
  │                   ├──transmit()───>│                │             │
  │                   ├──transmit()─────────────────────>│             │
  │                   ├──transmit()───────────────────────────────────>│
  │                   │                │                │             │
  ├──Presiona CANCELAR>│               │                │             │
  │                   ├──stop()───────>│                │             │
  │                   ├──stop()─────────────────────────>│             │
  │                   ├──stop()───────────────────────────────────────>│
  │<──Retorna Home────┤                │                │             │
```

---

## Apéndice B: Glosario de Términos

- **Callsign:** Identificador único del usuario (3-10 caracteres alfanuméricos)
- **DOT:** Pulso corto en Morse (200ms)
- **DASH:** Pulso largo en Morse (600ms)
- **GAP:** Espacio entre símbolos/letras/palabras
- **Mesh:** Red descentralizada peer-to-peer
- **RSSI:** Received Signal Strength Indicator (potencia de señal)
- **GATT:** Generic Attribute Profile (protocolo Bluetooth LE)
- **VLC:** Visible Light Communication
- **BLE:** Bluetooth Low Energy
- **Umbral dinámico:** Valor que se ajusta automáticamente para detectar ON/OFF

---

## Historial de Revisiones

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0 | 2025-12-06 | NicoButter | Versión inicial - 12 casos de uso detallados |

---

## Aprobaciones

| Rol | Nombre | Firma | Fecha |
|-----|--------|-------|-------|
| Autor | NicoButter | | 2025-12-06 |
| Revisor de Casos de Uso | | | |
| Aprobador | | | |

---

**Documento Confidencial - Proyecto BeaconChat**  
**Casos de Uso v1.0**  
**6 de diciembre de 2025**
