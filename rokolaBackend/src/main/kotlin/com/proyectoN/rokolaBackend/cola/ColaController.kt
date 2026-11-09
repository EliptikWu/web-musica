package com.proyectoN.rokolaBackend.cola

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ColaController(private val servicio: ColaService) {

    @GetMapping("/api/sesiones/{idSesion}/cola")
    fun obtener(@PathVariable idSesion: Long): ColaResponse = servicio.obtener(idSesion)
}
