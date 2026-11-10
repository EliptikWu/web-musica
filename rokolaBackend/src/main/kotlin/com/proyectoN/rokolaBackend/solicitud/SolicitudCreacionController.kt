package com.proyectoN.rokolaBackend.solicitud

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sesiones")
class SolicitudCreacionController(private val servicio: SolicitudCreacionService) {

    @PostMapping("/{idSesion}/solicitudes")
    fun crear(
        @PathVariable idSesion: Long,
        @RequestBody request: CrearSolicitudRequest,
    ): ResponseEntity<CrearSolicitudResponse> {
        val respuesta = servicio.crear(idSesion, request)
        // Nueva solicitud = 201; canción pendiente ya existente = 200 y voto adicional.
        return if (respuesta.resultado == "solicitud_creada") {
            ResponseEntity.status(201).body(respuesta)
        } else {
            ResponseEntity.ok(respuesta)
        }
    }
}
