package com.proyectoN.rokolaBackend.solicitud

import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class MisSolicitudesController(private val servicio: MisSolicitudesService) {

    @GetMapping("/api/sesiones/{idSesion}/mis-solicitudes")
    fun obtener(
        @PathVariable idSesion: Long,
        @RequestHeader("X-Rokola-Device-Token") deviceToken: String,
    ): ResponseEntity<MisSolicitudesResponse> = ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(servicio.obtener(idSesion, deviceToken))
}
