package com.example.rokola.escaner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

private val PISTAS = mapOf(
    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    DecodeHintType.TRY_HARDER to true,
)

/**
 * El texto del QR que se vea en una imagen en grises, o null si no hay ninguno legible (RF-066).
 * Corre en el teléfono: la imagen no se guarda ni sale de él.
 */
fun decodificarQr(luminancia: ByteArray, ancho: Int, alto: Int): String? {
    val fuente = PlanarYUVLuminanceSource(luminancia, ancho, alto, 0, 0, ancho, alto, false)
    return try {
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(fuente)), PISTAS).text
    } catch (_: ReaderException) {
        null
    }
}

/** Analiza cada cuadro de la cámara. El contenido del QR no se registra en ningún log. */
class AnalizadorQr(private val alLeer: (String) -> Unit) : ImageAnalysis.Analyzer {
    private var grises = ByteArray(0)

    override fun analyze(imagen: ImageProxy) {
        imagen.use {
            val plano = it.planes[0]
            val ancho = it.width
            val alto = it.height
            if (grises.size != ancho * alto) grises = ByteArray(ancho * alto)
            val origen = plano.buffer
            // Cada fila puede traer relleno al final: se copia solo lo que es imagen.
            for (fila in 0 until alto) {
                origen.position(fila * plano.rowStride)
                origen.get(grises, fila * ancho, ancho)
            }
            decodificarQr(grises, ancho, alto)?.let(alLeer)
        }
    }
}
