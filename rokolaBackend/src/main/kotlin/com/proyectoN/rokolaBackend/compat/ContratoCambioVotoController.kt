package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** PUT contractual RF-014: mismo dispositivo y sesión que las API de solicitud y voto. */
@RestController
@RequestMapping("/api/bares/{slug}/dispositivo/voto")
class ContratoCambioVotoController(private val servicio: ContratoParticipacionService) {
    @PutMapping
    fun cambiar(
      @PathVariable slug: String,
      @RequestHeader(value = "X-Rockola-Dispositivo", required = false) dispositivo: String?,
      @RequestHeader(value = "Idempotency-Key", required = false) clave: String?,
      @RequestBody cuerpo: Map<String, Any?>,
    ): ResponseEntity<String> {
        val desde = cuerpo["desde"] as? String
            ?: throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requiere desde como cadena")
        val hacia = cuerpo["hacia"] as? String
            ?: throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requiere hacia como cadena")
        val r = servicio.cambiarVoto(slug, dispositivo, clave, desde, hacia)
        return ResponseEntity.status(r.estado).contentType(MediaType.APPLICATION_JSON).body(r.json)
    }
}
