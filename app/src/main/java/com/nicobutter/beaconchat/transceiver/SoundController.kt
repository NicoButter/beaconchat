package com.nicobutter.beaconchat.transceiver

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Controls ultrasound transmission for BeaconChat signaling.
 *
 * This controller generates ultrasonic audio signals (18.5kHz) to transmit
 * timing-based signals. It converts timing sequences into audio on/off
 * patterns suitable for acoustic communication, using frequencies that
 * are nearly inaudible to humans.
 */
class SoundController {
    private val transmissionMutex = Mutex()
    private var audioTrack: AudioTrack? = null

    /** Callback for visual debugging of transmission state. */
    var onStateChange: ((Boolean, Int, Int) -> Unit)? = null

    // Configuración de audio para ultrasonido
    private val sampleRate = 48000 // Increased from 44100 for better ultrasound quality
    private val frequency = 18500.0 // 18.5kHz ultrasound (nearly inaudible to humans)

    init {
        Log.d(
                TAG,
                "SoundController initialized with ultrasound frequency: ${frequency}Hz at ${sampleRate}Hz sample rate"
        )
    }

    /**
     * Transmits a sequence of timing durations using ultrasound signals.
     *
     * Converts the timing list into audio on/off states where even indices
     * represent ultrasound tone periods and odd indices represent silence periods.
     * Uses 18.5kHz ultrasonic frequency for nearly inaudible transmission.
     *
     * @param timings List of durations in milliseconds, alternating tone/silence periods
     */
    suspend fun transmit(timings: List<Long>) {
        // Prevent concurrent transmissions
        transmissionMutex.withLock {
            Log.d(TAG, "Starting sound transmission with ${timings.size} timings")

            withContext(Dispatchers.IO) {
                try {
                    for (i in timings.indices) {
                        val duration = timings[i]
                        val isSound = i % 2 == 0 // Even indices are ON, Odd are OFF

                        if (isSound) {
                            Log.d(TAG, "[$i/${timings.size}] BEEP for ${duration}ms")
                            playTone(duration)
                            onStateChange?.invoke(true, i, timings.size)
                        } else {
                            Log.d(TAG, "[$i/${timings.size}] SILENCE for ${duration}ms")
                            onStateChange?.invoke(false, i, timings.size)
                            delay(duration)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during sound transmission", e)
                    e.printStackTrace()
                    onStateChange?.invoke(false, -1, timings.size)
                } finally {
                    // ALWAYS ensure audio is stopped when done
                    Log.d(TAG, "Sound transmission complete")
                    stopAudio()
                    onStateChange?.invoke(false, timings.size, timings.size)
                }
            }
        }
    }

    /**
     * Generates and plays an ultrasonic tone for the specified duration.
     *
     * Creates a sine wave at 18.5kHz frequency, configures an AudioTrack
     * with appropriate attributes for ultrasound transmission, and plays
     * the tone for the specified duration.
     *
     * @param durationMs Duration to play the tone in milliseconds
     */
    private suspend fun playTone(durationMs: Long) {
        try {
            val numSamples = (durationMs * sampleRate / 1000).toInt()
            val samples = ShortArray(numSamples)

            // Generate sine wave
            for (i in samples.indices) {
                val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
                samples[i] = (sin(angle) * Short.MAX_VALUE).toInt().toShort()
            }

            // Create and configure AudioTrack
            val bufferSize =
                    AudioTrack.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    )

            audioTrack =
                    AudioTrack.Builder()
                            .setAudioAttributes(
                                    AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_ALARM)
                                            .setContentType(
                                                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                                            )
                                            .build()
                            )
                            .setAudioFormat(
                                    AudioFormat.Builder()
                                            .setSampleRate(sampleRate)
                                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                            .build()
                            )
                            .setBufferSizeInBytes(bufferSize)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build()

            audioTrack?.let { track ->
                track.write(samples, 0, samples.size)
                track.play()

                // Wait for playback to complete
                delay(durationMs)

                track.stop()
                track.release()
                audioTrack = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing tone for ${durationMs}ms", e)
            e.printStackTrace()
            stopAudio()
        }
    }

    /**
     * Stops any currently playing audio and releases the AudioTrack resources.
     *
     * Safely stops playback if active and releases the audio track to free
     * system resources. Handles exceptions gracefully to prevent crashes.
     */
    private fun stopAudio() {
        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
                audioTrack = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }

    /**
     * Cleans up audio resources and ensures no audio is playing.
     *
     * This method should be called when the controller is no longer needed
     * to ensure proper resource cleanup and prevent audio from continuing
     * to play.
     */
    fun cleanup() {
        try {
            stopAudio()
            // Give hardware time to release
            Thread.sleep(50)
            Log.d(TAG, "Audio cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
        // Reset callback
        onStateChange = null
    }

    companion object {
        private const val TAG = "SoundController"
    }
}
