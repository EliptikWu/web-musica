package com.proyectoN.rokolaBackend.compat

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Adaptador de la pantalla pública del bar. No modifica los endpoints antiguos. */
@RestController
@RequestMapping("/api/bares/{slug}")
class ContratoPantallaController(private val servicio: ContratoPantallaService) {

    @GetMapping("/qr-publico")
    fun qrPublico(@PathVariable slug: String): Map<String, String> = servicio.qrPublico(slug)

    @PostMapping("/pantalla/reproducciones/{id}/inicio")
    fun inicio(
        @PathVariable slug: String,
        @PathVariable id: String,
        @RequestHeader("Idempotency-Key", required = false) clave: String?,
        @RequestHeader("Authorization", required = false) autorizacion: String?,
        @RequestBody cuerpo: Map<String, Any?>,
    ): ResponseEntity<Void> = reportar(slug, id, "inicio", clave, autorizacion, cuerpo)

    @PostMapping("/pantalla/reproducciones/{id}/fin")
    fun fin(
        @PathVariable slug: String,
        @PathVariable id: String,
        @RequestHeader("Idempotency-Key", required = false) clave: String?,
        @RequestHeader("Authorization", required = false) autorizacion: String?,
        @RequestBody cuerpo: Map<String, Any?>,
    ): ResponseEntity<Void> = reportar(slug, id, "fin", clave, autorizacion, cuerpo)

    @PostMapping("/pantalla/reproducciones/{id}/error")
    fun error(
        @PathVariable slug: String,
        @PathVariable id: String,
        @RequestHeader("Idempotency-Key", required = false) clave: String?,
        @RequestHeader("Authorization", required = false) autorizacion: String?,
        @RequestBody cuerpo: Map<String, Any?>,
    ): ResponseEntity<Void> = reportar(slug, id, "error", clave, autorizacion, cuerpo)

    private fun reportar(
        slug: String, id: String, accion: String, clave: String?, autorizacion: String?,
        cuerpo: Map<String, Any?>,
    ): ResponseEntity<Void> {
        servicio.reportar(slug, id, accion, clave, autorizacion, cuerpo)
        return ResponseEntity.noContent().build()
    }
}
