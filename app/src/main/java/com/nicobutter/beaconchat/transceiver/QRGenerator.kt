package com.nicobutter.beaconchat.transceiver

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QRGenerator {

    companion object {
        private const val TAG = "QRGenerator"
        private const val DEFAULT_SIZE = 512
    }

    /**
     * Generate a QR code bitmap from text
     * @param text The text to encode
     * @param size The size of the QR code (width and height)
     * @param errorCorrectionLevel Error correction level (default: HIGH for better detection)
     * @return Bitmap of the QR code
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

    /** Generate QR code with optimal size based on text length */
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

    /** Estimate if text can be encoded in QR with given error correction */
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
