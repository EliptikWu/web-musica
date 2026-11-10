package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Endpoints nuevos para Next.js/Android. Los antiguos de /api/acceso no se modifican. */
@RestController
@RequestMapping("/api/bares/{slug}/dispositivo")
class ContratoDispositivoController(private val servicio: ContratoDispositivoService) {

    @PostMapping
    fun registrar(
        @PathVariable slug: String,
        @RequestHeader(value = "X-Rockola-Dispositivo", required = false) uuid: String?,
        @RequestBody cuerpo: Map<String, Any?>,
    ): Map<String, Any?> = servicio.registrar(slug, uuid, cuerpo["tipo"] as? String)

    @GetMapping
    fun yo(
        @PathVariable slug: String,
        @RequestHeader(value = "X-Rockola-Dispositivo", required = false) uuid: String?,
    ): Map<String, Any?> = servicio.yo(slug, uuid)

    @PostMapping("/aviso")
    fun aviso(
        @PathVariable slug: String,
        @RequestHeader(value = "X-Rockola-Dispositivo", required = false) uuid: String?,
    ): ResponseEntity<Void> {
        servicio.aviso(slug, uuid)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
