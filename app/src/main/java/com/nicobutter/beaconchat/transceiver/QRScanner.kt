package com.nicobutter.beaconchat.transceiver

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * Scans QR codes from camera frames using CameraX and ZXing.
 *
 * This analyzer processes camera image frames to detect and decode QR codes
 * in real-time. It implements debouncing to prevent duplicate detections and
 * handles image processing efficiently with proper resource cleanup.
 */
class QRScanner(private val onQRCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader =
            MultiFormatReader().apply {
                val hints =
                        mapOf<DecodeHintType, Any>(
                                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
                        )
                setHints(hints)
            }

    private var lastScannedContent: String? = null
    private var lastScanTime = 0L
    private val SCAN_DELAY_MS = 1000L // Avoid spamming the same code

    /**
     * Analyzes a camera image frame for QR codes.
     *
     * Processes the ImageProxy to extract YUV data, converts it to a format
     * suitable for ZXing, and attempts to decode any QR codes present.
     * Implements debouncing to prevent repeated callbacks for the same code.
     *
     * @param image The camera image frame to analyze
     */
    override fun analyze(image: ImageProxy) {
        try {
            val buffer = image.planes[0].buffer
            val data = toByteArray(buffer)

            val width = image.width
            val height = image.height

            // Create a LuminanceSource from the YUV data
            // Note: CameraX images might be rotated, but QR codes are rotation invariant mostly,
            // or ZXing handles it. However, for portrait mode, the image data is usually rotated 90
            // degrees
            // relative to the screen. ZXing's HybridBinarizer handles some of this, but
            // PlanarYUVLuminanceSource just takes the raw data.

            val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)

            val bitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decodeWithState(bitmap)
                val content = result.text

                val currentTime = System.currentTimeMillis()
                if (content != null &&
                                (content != lastScannedContent ||
                                        currentTime - lastScanTime > SCAN_DELAY_MS)
                ) {
                    lastScannedContent = content
                    lastScanTime = currentTime
                    Log.d(TAG, "QR Code detected: $content")
                    onQRCodeDetected(content)
                }
            } catch (e: Exception) {
                // NotFoundException is common when no QR code is in the frame
                // We don't need to log it to avoid spam
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image for QR code", e)
        } finally {
            image.close()
        }
    }

    /**
     * Converts a ByteBuffer to a byte array for image processing.
     *
     * Extracts all remaining bytes from the buffer into a new array,
     * rewinding the buffer position to the beginning first.
     *
     * @param buffer The ByteBuffer to convert
     * @return Byte array containing the buffer's data
     */
    private fun toByteArray(buffer: ByteBuffer): ByteArray {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }

    companion object {
        private const val TAG = "QRScanner"
    }
}
