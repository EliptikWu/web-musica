package com.proyectoN.rokolaBackend.acceso

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Tokens aleatorios; nunca se almacena el token original en la base de datos. */
@Component
class TokenSeguro(
    @Value("\${rokola.device-hmac-secret}") private val deviceHmacSecret: String,
) {
    private val random = SecureRandom()
    private val tokenRegex = Regex("^[A-Za-z0-9_-]{43}$") // Base64URL de 32 bytes sin '='

    init {
        require(deviceHmacSecret.length >= 32) {
            "ROKOLA_DEVICE_HMAC_SECRET debe tener al menos 32 caracteres aleatorios"
        }
    }

    fun esValido(token: String) = tokenRegex.matches(token)

    fun generar(): String = ByteArray(32).also(random::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

    /** SHA-256 de la cadena UTF-8 del token público que se encuentra en el QR. */
    fun huellaQr(token: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)),
    )

    /** HMAC restringido a cada establecimiento para los secretos de dispositivos. */
    fun huellaDispositivo(idEstablecimiento: Long, token: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(deviceHmacSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return HexFormat.of().formatHex(mac.doFinal("$idEstablecimiento:$token".toByteArray(Charsets.UTF_8)))
    }
}
