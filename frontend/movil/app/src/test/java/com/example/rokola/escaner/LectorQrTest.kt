package com.example.rokola.escaner

import androidx.camera.core.ImageProxy
import com.example.rokola.viewmodel.Enlace
import com.example.rokola.viewmodel.leerQrEscaneado
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LectorQrTest {
    private val dominio = "rockola.example"
    private val api = "https://api.rockola.example/api"

    private fun leer(texto: String) = leerQrEscaneado(texto, dominio, api)

    @Test
    fun `lee el QR de una imagen en grises y nada de una imagen vacia`() {
        val url = "https://$dominio/q/token-de-prueba_1"
        val matriz = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 320, 320)
        val grises = ByteArray(320 * 320) { i -> if (matriz[i % 320, i / 320]) 0 else -1 }
        assertEquals(url, decodificarQr(grises, 320, 320))
        assertNull(decodificarQr(ByteArray(320 * 320) { -1 }, 320, 320))
    }

    /** Un cuadro como los de CameraX: cada fila del plano Y trae relleno al final. */
    private fun cuadro(grises: ByteArray, ancho: Int, alto: Int, paso: Int): ImageProxy {
        val datos = ByteArray(paso * alto)
        for (fila in 0 until alto) System.arraycopy(grises, fila * ancho, datos, fila * paso, ancho)
        val plano = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ImageProxy.PlaneProxy::class.java)) { _, metodo, _ ->
            when (metodo.name) {
                "getBuffer" -> ByteBuffer.wrap(datos)
                "getRowStride" -> paso
                "getPixelStride" -> 1
                else -> null
            }
        } as ImageProxy.PlaneProxy
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ImageProxy::class.java)) { _, metodo, _ ->
            when (metodo.name) {
                "getPlanes" -> arrayOf(plano)
                "getWidth" -> ancho
                "getHeight" -> alto
                else -> null
            }
        } as ImageProxy
    }

    @Test
    fun `el analizador lee un cuadro de camara con relleno en cada fila`() {
        val url = "https://$dominio/q/token-de-prueba_1"
        val matriz = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 320, 320)
        val grises = ByteArray(320 * 320) { i -> if (matriz[i % 320, i / 320]) 0 else -1 }
        val leidos = mutableListOf<String>()
        AnalizadorQr { leidos += it }.analyze(cuadro(grises, 320, 320, paso = 352))
        assertEquals(listOf(url), leidos)
    }

    @Test
    fun `acepta los enlaces de la web de Rockola`() {
        assertEquals(Enlace.Qr("abc_1-2"), leer("https://$dominio/q/abc_1-2"))
        assertEquals(Enlace.Qr("abc"), leer("  https://ROCKOLA.example/q/abc/ "))
        assertEquals(Enlace.Bar("cafe-vinilo"), leer("https://$dominio/b/cafe-vinilo"))
    }

    @Test
    fun `acepta un QR impreso con la direccion del backend, solo si es un QR`() {
        assertEquals(Enlace.Qr("abc"), leer("https://api.rockola.example/q/abc"))
        assertNull(leer("https://api.rockola.example/b/cafe-vinilo"))
        assertNull(leerQrEscaneado("https://api.rockola.example/q/abc", dominio, ""))
    }

    @Test
    fun `rechaza lo que no es de Rockola`() {
        listOf(
            "http://$dominio/q/abc",
            "https://otro.example/q/abc",
            "https://$dominio.otro.example/q/abc",
            "https://$dominio@otro.example/q/abc",
            "https://usuario@$dominio/q/abc",
            "intent://$dominio/q/abc#Intent;scheme=https;end",
            "javascript:alert(1)",
            "texto cualquiera",
            "",
            "https://$dominio/panel",
            "https://$dominio/q/a/b",
            "https://$dominio/q/%2E%2E",
            "https://$dominio/q/a%2Fb",
            "https://$dominio/q/" + "a".repeat(600),
        ).forEach { assertNull("debió rechazar: $it", leer(it)) }
    }
}
