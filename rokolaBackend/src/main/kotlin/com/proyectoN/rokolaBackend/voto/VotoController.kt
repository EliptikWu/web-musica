package com.proyectoN.rokolaBackend.voto

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/solicitudes")
class VotoController(
    private val servicio: VotoService,
) {
    @PostMapping("/{idSolicitud}/votos")
    fun votar(
        @PathVariable idSolicitud: Long,
        @RequestBody solicitud: RegistrarVotoRequest,
    ): ResponseEntity<RegistrarVotoResponse> = ResponseEntity
        .status(HttpStatus.CREATED)
        .body(servicio.registrar(idSolicitud, solicitud))
}
