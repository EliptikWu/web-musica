package com.proyectoN.rokolaBackend.acceso

import com.proyectoN.rokolaBackend.establecimiento.CodigoQr
import com.proyectoN.rokolaBackend.establecimiento.CodigoQrRepository
import com.proyectoN.rokolaBackend.establecimiento.Establecimiento
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Datos devueltos por la creación; el token original nunca se almacena en la BD. */
data class QrCreado(val id: Long, val url: String, val png: ByteArray)

@Service
class QrCreacionService(
    private val entityManager: EntityManager,
    private val qrRepository: CodigoQrRepository,
    private val tokens: TokenSeguro,
    private val imagen: QrImagenService,
    @Value("\${rokola.qr-base-url:}") private val baseUrl: String,
) {
    @Transactional
    fun crearParaEstablecimiento(idEstablecimiento: Long): QrCreado {
        val raiz = raizConfigurada()
        // Bloquea la fila para serializar dos generaciones simultáneas del mismo establecimiento.
        val establecimiento = entityManager.find(
            Establecimiento::class.java,
            idEstablecimiento,
            LockModeType.PESSIMISTIC_WRITE,
        ) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Establecimiento no encontrado")
        if (!establecimiento.activo) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El establecimiento está inactivo")
        }

        val token = tokens.generar()
        val url = "$raiz/q/$token"
        val png = imagen.generarPng(url)
        val ahora = OffsetDateTime.now(ZoneOffset.UTC)

        // Cada POST rota el QR: revoca los anteriores del mismo establecimiento.
        val anteriores = qrRepository.findByEstablecimientoIdOrderByFechaCreacionDesc(idEstablecimiento)
            .filter { it.activo }
        anteriores.forEach {
            it.activo = false
            if (it.fechaRevocacion == null) {
                it.fechaRevocacion = maxOf(ahora, it.fechaCreacion)
            }
        }
        if (anteriores.isNotEmpty()) qrRepository.saveAll(anteriores)

        val nuevo = qrRepository.saveAndFlush(
            CodigoQr(
                establecimiento = establecimiento,
                tokenHash = tokens.huellaQr(token),
                fechaCreacion = ahora,
                fechaRevocacion = null,
                activo = true,
            ),
        )
        return QrCreado(requireNotNull(nuevo.id), url, png)
    }

    private fun raizConfigurada(): String {
        if (baseUrl.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Configura ROKOLA_QR_BASE_URL con la IP Wi-Fi del computador (http://IP:8080)",
            )
        }
        val uri = try { URI.create(baseUrl.trim()) } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ROKOLA_QR_BASE_URL no es una URL válida")
        }
        if (uri.scheme !in listOf("http", "https") || uri.host == null || uri.rawQuery != null || uri.rawFragment != null || (uri.path != null && uri.path !in listOf("", "/"))) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ROKOLA_QR_BASE_URL debe ser http(s)://host:puerto sin ruta")
        }
        return baseUrl.trim().trimEnd('/')
    }
}
