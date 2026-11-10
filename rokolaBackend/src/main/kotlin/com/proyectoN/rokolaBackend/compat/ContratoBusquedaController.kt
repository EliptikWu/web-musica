package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bares/{slug}")
class ContratoBusquedaController(private val servicio: ContratoBusquedaService) {
    @GetMapping("/busqueda")
    fun buscar(
        @PathVariable slug: String,
        @RequestHeader(value = "X-Rockola-Dispositivo", required = false) uuid: String?,
        @RequestParam(value = "q", required = false) q: String?,
    ): Map<String, Any?> {
        if (q == null) {
            throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requiere el parámetro q")
        }
        return servicio.buscar(slug, uuid, q)
    }
}
