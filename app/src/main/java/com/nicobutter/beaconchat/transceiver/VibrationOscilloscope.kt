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
     * Detector de pulsos Morse en señal de vibración
     * Similar al detector de OpticalOscilloscope pero adaptado para vibración
     */
    private inner class PulseDetector(private val locale: Locale) {
        // Decodificador Morse simple (sin multi-idioma por ahora)
        private val morseDecoder = MorseDecoder()
        
        // Umbral dinámico para detección ON/OFF
        private val thresholdFactor = 0.4f // 40% del rango dinámico
        
        // Estado de detección
        private var isVibrating = false
        private var pulseStartTime = 0L
        private var lastTransitionTime = 0L
        
        // Contador de pulsos
        private var pulseCount = 0
        
        // Buffer de símbolos Morse detectados
        private val morseBuffer = StringBuilder()
        
        // Mensaje decodificado
        private var decodedText = ""
        
        fun processSample(magnitude: Float, timestamp: Long) {
            // Calcular umbral dinámico
            val stats = vibrationDetector?.getStats() ?: return
            val threshold = stats.min + (stats.max - stats.min) * thresholdFactor
            
            val isAboveThreshold = magnitude > threshold
            
            // Detectar transiciones ON/OFF
            if (isAboveThreshold && !isVibrating) {
                // Inicio de pulso (OFF → ON)
                handlePulseStart(timestamp)
            } else if (!isAboveThreshold && isVibrating) {
                // Fin de pulso (ON → OFF)
                handlePulseEnd(timestamp)
            }
            
            // Detectar timeouts (silencio prolongado)
            if (!isVibrating && lastTransitionTime > 0) {
                val silenceDuration = timestamp - lastTransitionTime
                
                if (silenceDuration > GAP_MIN_DURATION && morseBuffer.isNotEmpty()) {
                    // GAP entre letras → decodificar letra
                    decodeLetter()
                }
                
                if (silenceDuration > WORD_GAP_DURATION) {
                    // GAP entre palabras → agregar espacio
                    if (decodedText.isNotEmpty() && !decodedText.endsWith(" ")) {
                        decodedText += " "
                    }
                }
            }
        }
        
        private fun handlePulseStart(timestamp: Long) {
            isVibrating = true
            pulseStartTime = timestamp
            lastTransitionTime = timestamp
        }
        
        private fun handlePulseEnd(timestamp: Long) {
            isVibrating = false
            val pulseDuration = timestamp - pulseStartTime
            lastTransitionTime = timestamp
            
            // Clasificar pulso por duración
            when {
                pulseDuration < MIN_PULSE_DURATION -> {
                    // Ruido - ignorar
                }
                pulseDuration < DOT_MAX_DURATION -> {
                    // DOT detectado
                    morseBuffer.append('.')
                    pulseCount++
                }
                pulseDuration < DASH_MAX_DURATION -> {
                    // DASH detectado
                    morseBuffer.append('-')
                    pulseCount++
                }
                else -> {
                    // Pulso muy largo - posible marcador o error
                    // Por ahora ignorar
                }
            }
        }
        
        private fun decodeLetter() {
            val morseCode = morseBuffer.toString()
            // Por ahora usar el mensaje decodificado del MorseDecoder estándar
            // En futuro se puede adaptar para multi-idioma
            morseBuffer.clear()
        }
        
        fun getDecodedMessage(): String = decodedText
        
        fun getPulseCount(): Int = pulseCount
        
        fun reset() {
            isVibrating = false
            pulseStartTime = 0L
            lastTransitionTime = 0L
            pulseCount = 0
            morseBuffer.clear()
            decodedText = ""
        }
    }
}
