package com.proyectoN.rokolaBackend.acceso

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.client.j2se.MatrixToImageWriter
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

/** Convierte una URL corta de Rokola en PNG; no utiliza Python ni ficheros locales. */
@Service
class QrImagenService {
    fun generarPng(url: String): ByteArray {
        val matriz = QRCodeWriter().encode(
            url,
            BarcodeFormat.QR_CODE,
            400,
            400,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
        return ByteArrayOutputStream().use { salida ->
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida)
            salida.toByteArray()
        }
    }
}
