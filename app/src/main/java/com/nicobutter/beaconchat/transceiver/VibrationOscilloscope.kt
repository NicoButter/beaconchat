package com.nicobutter.beaconchat.transceiver

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Timing para clasificación de pulsos (mismo que protocolo óptico)
 */
private const val MIN_PULSE_DURATION = 80L    // Rechazar ruido < 80ms
private const val DOT_MAX_DURATION = 250L     // DOT: 80-250ms
private const val DASH_MAX_DURATION = 600L    // DASH: 250-600ms
private const val GAP_MIN_DURATION = 600L     // GAP entre letras: >600ms
private const val WORD_GAP_DURATION = 1500L   // GAP entre palabras: >1500ms

/**
 * Osciloscopio de Vibración - Análisis de señales táctiles Morse
 * 
 * Analiza las vibraciones captadas por el acelerómetro y decodifica
 * mensajes Morse transmitidos mediante el motor de vibración.
 * 
 * Pipeline de procesamiento:
 * 1. VibrationDetector captura magnitud de vibración (0.0-1.0)
 * 2. Detección de pulsos ON/OFF con umbral dinámico
 * 3. Clasificación temporal: DOT (80-250ms), DASH (250-600ms), GAP (>600ms)
 * 4. Decodificación Morse multi-idioma
 * 5. Reconstrucción de mensaje de texto
 * 
 * Similar a OpticalOscilloscope pero para señales táctiles
 */
class VibrationOscilloscope(
    private val context: Context,
    private val locale: Locale = Locale.getDefault()
) {
    // Detector de vibración
    private var vibrationDetector: VibrationDetector? = null
    
    // Estado de análisis
    var isAnalyzing by mutableStateOf(false)
        private set
    
    var currentMagnitude by mutableStateOf(0f)
        private set
    
    var decodedMessage by mutableStateOf("")
        private set
    
    var detectedPulses by mutableStateOf(0)
        private set
    
    // Histórico de magnitudes para visualización
    private val magnitudeHistory = mutableListOf<Float>()
    private val maxHistorySize = 100
    
    // Detector de pulsos Morse
    private var pulseDetector: PulseDetector? = null
    
    /**
     * Inicia el análisis de vibración
     */
    fun startAnalysis() {
        if (isAnalyzing) return
        
        // Crear detector de pulsos
        pulseDetector = PulseDetector(locale)
        
        // Crear y arrancar detector de vibración
        vibrationDetector = VibrationDetector(context) { magnitude ->
            analyzeMagnitude(magnitude)
        }
        vibrationDetector?.startDetection()
        
        isAnalyzing = true
        magnitudeHistory.clear()
        detectedPulses = 0
        decodedMessage = ""
    }
    
    /**
     * Detiene el análisis de vibración
     */
    fun stopAnalysis() {
        if (!isAnalyzing) return
        
        vibrationDetector?.stopDetection()
        vibrationDetector = null
        pulseDetector = null
        
        isAnalyzing = false
    }
    
    /**
     * Analiza un valor de magnitud de vibración
     */
    private fun analyzeMagnitude(magnitude: Float) {
        currentMagnitude = magnitude
        
        // Actualizar histórico para gráfico
        magnitudeHistory.add(magnitude)
        if (magnitudeHistory.size > maxHistorySize) {
            magnitudeHistory.removeAt(0)
        }
        
        // Detectar pulsos Morse
        pulseDetector?.let { detector ->
            val timestamp = System.currentTimeMillis()
            detector.processSample(magnitude, timestamp)
            
            // Actualizar mensaje decodificado
            decodedMessage = detector.getDecodedMessage()
            detectedPulses = detector.getPulseCount()
        }
    }
    
    /**
     * Obtiene el histórico de magnitudes para visualización
     */
    fun getMagnitudeHistory(): List<Float> {
        return magnitudeHistory.toList()
    }
    
    /**
     * Obtiene estadísticas de la señal
     */
    fun getStats(): VibrationStats {
        return vibrationDetector?.getStats() ?: VibrationStats(0f, 0f, 0f, 0f)
    }
    
    /**
     * Reinicia el estado del osciloscopio
     */
    fun reset() {
        vibrationDetector?.reset()
        pulseDetector?.reset()
        magnitudeHistory.clear()
        currentMagnitude = 0f
        decodedMessage = ""
        detectedPulses = 0
    }
    
    /**
     * Detector de pulsos Morse en señal de vibración.
     *
     * Detecta transiciones ON/OFF en la magnitud normalizada y delega la
     * decodificación Morse completa (incluyendo preámbulo) a [MorseDecoder].
     * Cada vez que la señal cruza el umbral dinámico se notifica al
     * decodificador con la duración del estado que acaba de terminar.
     */
    private inner class PulseDetector(private val locale: Locale) {

        private val morseDecoder = MorseDecoder()
        private val thresholdFactor = 0.4f

        private var isVibrating = false
        private var pulseStartTime = 0L   // marca de tiempo al inicio del ON
        private var silenceStartTime = 0L // marca de tiempo al inicio del OFF
        private var pulseCount = 0

        fun processSample(magnitude: Float, timestamp: Long) {
            val stats = vibrationDetector?.getStats() ?: return
            val range = stats.max - stats.min
            // No hay señal suficiente todavía
            if (range < 0.05f) return

            val threshold = stats.min + range * thresholdFactor
            val isAboveThreshold = magnitude > threshold

            if (isAboveThreshold && !isVibrating) {
                // Transición OFF → ON: notificar duración del silencio previo
                isVibrating = true
                val offDuration = if (silenceStartTime > 0L) timestamp - silenceStartTime else 0L
                if (silenceStartTime > 0L) {
                    // isLightOn=true  ← nuevo estado; offDuration ← estado que terminó
                    morseDecoder.onLightStateChanged(true, offDuration)
                }
                pulseStartTime = timestamp

            } else if (!isAboveThreshold && isVibrating) {
                // Transición ON → OFF: notificar duración del pulso previo
                isVibrating = false
                val onDuration = timestamp - pulseStartTime
                silenceStartTime = timestamp
                if (onDuration >= MIN_PULSE_DURATION) {
                    // isLightOn=false ← nuevo estado; onDuration ← estado que terminó
                    morseDecoder.onLightStateChanged(false, onDuration)
                    pulseCount++
                }
            }
        }

        fun getDecodedMessage(): String = morseDecoder.decodedMessage

        fun getPulseCount(): Int = pulseCount

        fun reset() {
            isVibrating = false
            pulseStartTime = 0L
            silenceStartTime = 0L
            pulseCount = 0
            morseDecoder.reset()
        }
    }
}
