package com.proyectoN.rokolaBackend.acceso

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/** Endpoint administrativo. Debe requerir autenticación en SecurityConfig. */
@RestController
class QrCreacionController(private val creacion: QrCreacionService) {
    @PostMapping(
        "/api/establecimientos/{id}/codigos-qr/generar",
        produces = [MediaType.IMAGE_PNG_VALUE],
    )
    fun generar(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val creado = creacion.crearParaEstablecimiento(id)
        return ResponseEntity.status(HttpStatus.CREATED)
            .contentType(MediaType.IMAGE_PNG)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"rokola-qr-$id.png\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("X-Rokola-Qr-Id", creado.id.toString())
            .header("X-Rokola-Qr-Url", creado.url)
            .body(creado.png)
    }
}
