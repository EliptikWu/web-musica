package com.proyectoN.rokolaBackend.acceso

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

/** URL corta para imprimir en el QR. Validar antes de redirigir, sin insertar una URL arbitraria. */
@RestController
class QrRedirectController(
    private val acceso: AccesoService,
    @Value("\${rokola.client-url:}") private val clientUrl: String,
) {
    @GetMapping("/q/{token}")
    fun abrir(@PathVariable token: String): ResponseEntity<Void> {
        acceso.resolverQr(token) // 404/410 para QR inválidos, revocados o establecimiento inactivo
        if (clientUrl.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar la URL de la aplicación cliente")
        }
        val destino = UriComponentsBuilder.fromUriString(clientUrl)
            .queryParam("qr", token).build().encode().toUri()
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
            .header(HttpHeaders.LOCATION, destino.toASCIIString())
            .header("Referrer-Policy", "no-referrer")
            .build()
    }
}
