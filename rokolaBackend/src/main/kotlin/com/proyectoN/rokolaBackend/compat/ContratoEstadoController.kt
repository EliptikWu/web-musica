package com.proyectoN.rokolaBackend.compat

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Adaptadores de lectura: solo campos definidos en el OpenAPI del frontend. */
@RestController
@RequestMapping("/api/bares/{slug}")
class ContratoEstadoController(
    private val estados: ContratoEstadoService,
    private val dispositivos: ContratoDispositivoService,
) {
    @GetMapping("/ahora")
    fun ahora(
        @PathVariable slug: String,
        @RequestHeader("X-Rockola-Dispositivo", required = false) uuid: String?,
    ): Map<String, Any?> {
        dispositivos.huella(uuid) // Valida UUID, sin registrar ni revelar el identificador.
        return estados.ahora(slug)
    }

    @GetMapping("/destacadas")
    fun destacadas(
        @PathVariable slug: String,
        @RequestHeader("X-Rockola-Dispositivo", required = false) uuid: String?,
    ): Map<String, Any> {
        dispositivos.huella(uuid)
        return estados.destacadas(slug)
    }
}
