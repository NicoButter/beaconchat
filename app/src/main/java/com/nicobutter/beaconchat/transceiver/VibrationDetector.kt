package com.nicobutter.beaconchat.transceiver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detector de vibración usando acelerómetro
 * 
 * Detecta patrones de vibración Morse mediante el análisis de la magnitud
 * de aceleración. Funciona colocando el dispositivo receptor en contacto
 * directo con el dispositivo transmisor que está vibrando.
 * 
 * Principio de funcionamiento:
 * - El motor de vibración genera aceleración detectable (~2-5 m/s²)
 * - Calculamos magnitud vectorial: √(x² + y² + z²)
 * - Filtro pasa-alto elimina gravedad constante (9.8 m/s²)
 * - Umbral dinámico detecta pulsos ON/OFF
 * - Decodifica DOT/DASH usando timing Morse estándar
 * 
 * @param context Contexto de Android para acceder al SensorManager
 * @param onVibrationMagnitudeChanged Callback con magnitud de vibración normalizada (0.0-1.0)
 */
class VibrationDetector(
    private val context: Context,
    private val onVibrationMagnitudeChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    // Filtro de gravedad para eliminar aceleración constante
    private var gravity = FloatArray(3) { 0f }
    private val alpha = 0.8f // Factor de filtro pasa-bajo para gravedad
    
    // Histórico de magnitudes para calcular umbral dinámico
    private val magnitudeHistory = mutableListOf<Float>()
    private val maxHistorySize = 100 // ~3 segundos a 30Hz
    
    // Filtro suavizado exponencial para eliminar jitter
    private var previousMagnitude = 0f
    private val smoothingFactor = 0.7f
    
    var isDetecting = false
        private set

    /**
     * Inicia la detección de vibración
     */
    fun startDetection() {
        if (isDetecting) return
        
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST // ~200Hz para captura precisa
            )
            isDetecting = true
            
            // Reset estado
            gravity = FloatArray(3) { 0f }
            magnitudeHistory.clear()
            previousMagnitude = 0f
        }
    }

    /**
     * Detiene la detección de vibración
     */
    fun stopDetection() {
        if (!isDetecting) return
        
        sensorManager.unregisterListener(this)
        isDetecting = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 1. Filtro pasa-alto: eliminar componente de gravedad
        // Gravedad = alpha × gravedad_anterior + (1-alpha) × aceleración_actual
        gravity[0] = alpha * gravity[0] + (1 - alpha) * x
        gravity[1] = alpha * gravity[1] + (1 - alpha) * y
        gravity[2] = alpha * gravity[2] + (1 - alpha) * z

        // Aceleración lineal = aceleración_total - gravedad
        val linearX = x - gravity[0]
        val linearY = y - gravity[1]
        val linearZ = z - gravity[2]

        // 2. Calcular magnitud de vibración (aceleración lineal)
        val magnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        // 3. Filtro suavizado exponencial (reducir jitter)
        val smoothedMagnitude = smoothingFactor * previousMagnitude + (1 - smoothingFactor) * magnitude
        previousMagnitude = smoothedMagnitude

        // 4. Actualizar histórico para umbral dinámico
        magnitudeHistory.add(smoothedMagnitude)
        if (magnitudeHistory.size > maxHistorySize) {
            magnitudeHistory.removeAt(0)
        }

        // 5. Normalizar magnitud (0.0 - 1.0)
        val normalizedMagnitude = if (magnitudeHistory.isNotEmpty()) {
            val min = magnitudeHistory.minOrNull() ?: 0f
            val max = magnitudeHistory.maxOrNull() ?: 1f
            val range = max - min
            
            if (range > 0.1f) { // Evitar división por cero
                ((smoothedMagnitude - min) / range).coerceIn(0f, 1f)
            } else {
                0f
            }
        } else {
            0f
        }

        // 6. Notificar cambio de magnitud
        onVibrationMagnitudeChanged(normalizedMagnitude)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesitamos manejar cambios de precisión
    }

    /**
     * Obtiene estadísticas de la señal de vibración
     */
    fun getStats(): VibrationStats {
        return if (magnitudeHistory.isNotEmpty()) {
            VibrationStats(
                min = magnitudeHistory.minOrNull() ?: 0f,
                max = magnitudeHistory.maxOrNull() ?: 0f,
                avg = magnitudeHistory.average().toFloat(),
                current = previousMagnitude
            )
        } else {
            VibrationStats(0f, 0f, 0f, 0f)
        }
    }

    /**
     * Limpia el estado interno del detector
     */
    fun reset() {
        gravity = FloatArray(3) { 0f }
        magnitudeHistory.clear()
        previousMagnitude = 0f
    }
}

/**
 * Estadísticas de señal de vibración
 */
data class VibrationStats(
    val min: Float,
    val max: Float,
    val avg: Float,
    val current: Float
)
