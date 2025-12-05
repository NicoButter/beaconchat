package com.nicobutter.beaconchat.transceiver

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates QR codes for BeaconChat data transmission.
 *
 * This utility class creates QR code bitmaps from text data using the ZXing
 * library. It supports configurable size and error correction levels for
 * optimal scanning reliability in various conditions.
 */
class QRGenerator {

    companion object {
        private const val TAG = "QRGenerator"
        private const val DEFAULT_SIZE = 512
    }

    /**
     * Generates a QR code bitmap from the provided text.
     *
     * Creates a QR code bitmap with specified dimensions and error correction level.
     * Uses UTF-8 encoding and minimal margins for maximum data density.
     *
     * @param text The text content to encode in the QR code
     * @param size The width and height of the generated bitmap in pixels (default: 512)
     * @param errorCorrectionLevel Error correction level for robustness (default: High)
     * @return Bitmap containing the QR code, or null if generation fails
     */
    fun generateQRCode(
            text: String,
            size: Int = DEFAULT_SIZE,
            errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.H
    ): Bitmap? {
        return try {
            Log.d(TAG, "Generating QR code for text: $text (length: ${text.length})")

            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = errorCorrectionLevel
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1 // Minimal margin for maximum QR size

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            Log.d(TAG, "QR code generated successfully: ${width}x${height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a QR code with optimal size based on text length.
     *
     * Automatically selects an appropriate bitmap size based on the input text
     * length to balance readability and data capacity. Larger texts get larger
     * QR codes for better scanning reliability.
     *
     * @param text The text content to encode in the QR code
     * @return Bitmap containing the optimally sized QR code, or null if generation fails
     */
    fun generateOptimalQRCode(text: String): Bitmap? {
        val optimalSize =
                when {
                    text.length < 50 -> 256
                    text.length < 100 -> 384
                    text.length < 200 -> 512
                    else -> 768
                }

        Log.d(TAG, "Using optimal size $optimalSize for text length ${text.length}")
        return generateQRCode(text, optimalSize)
    }

    /**
     * Checks if the given text can be encoded in a QR code with the specified error correction.
     *
     * Estimates encoding capacity based on QR code specifications for Version 40
     * (maximum size) with the given error correction level. Considers UTF-8 byte
     * length for accurate capacity calculation.
     *
     * @param text The text to check for QR encoding capability
     * @param errorCorrectionLevel Error correction level to check capacity for (default: High)
     * @return True if the text can be encoded, false if it exceeds capacity
     */
    fun canEncode(
            text: String,
            errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.H
    ): Boolean {
        // QR Code capacity varies by version and error correction level
        // Version 40 with High error correction can hold ~1852 alphanumeric chars
        // or ~1273 bytes
        val maxCapacity =
                when (errorCorrectionLevel) {
                    ErrorCorrectionLevel.L -> 2953 // Low
                    ErrorCorrectionLevel.M -> 2331 // Medium
                    ErrorCorrectionLevel.Q -> 1663 // Quartile
                    ErrorCorrectionLevel.H -> 1273 // High
                }

        return text.toByteArray(Charsets.UTF_8).size <= maxCapacity
    }
}
