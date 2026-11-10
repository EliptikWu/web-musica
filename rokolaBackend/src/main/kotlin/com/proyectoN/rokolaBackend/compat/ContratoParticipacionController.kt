package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Rutas contractuales; NO reemplaza SolicitudCreacionController ni VotoController legacy. */
@RestController
@RequestMapping("/api/bares/{slug}/solicitudes")
class ContratoParticipacionController(private val servicio: ContratoParticipacionService) {
    @PostMapping
    fun pedir(
        @PathVariable slug: String,
        @RequestHeader(value = "X-Rockola-Dispositivo", required = false) dispositivo: String?,
        @RequestHeader(value = "Idempotency-Key", required = false) clave: String?,
        @RequestBody cuerpo: Map<String, Any?>,
    ): ResponseEntity<String> {
        val video = cuerpo["video_id"] as? String
            ?: throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requiere video_id")
        return resultado(servicio.pedir(slug, dispositivo, clave, video))
    }

    @PostMapping("/{id}/votos")
    fun votar(
        @PathVariable slug: String,
        @PathVariable id: String,
        @RequestHeader(value = "X-Rockola-Dispositivo", required = false) dispositivo: String?,
        @RequestHeader(value = "Idempotency-Key", required = false) clave: String?,
    ): ResponseEntity<String> = resultado(servicio.votar(slug, id, dispositivo, clave))

    private fun resultado(respuesta: ContratoParticipacionService.RespuestaGuardada): ResponseEntity<String> =
        ResponseEntity.status(respuesta.estado)
            .contentType(MediaType.APPLICATION_JSON)
            .body(respuesta.json)
}
