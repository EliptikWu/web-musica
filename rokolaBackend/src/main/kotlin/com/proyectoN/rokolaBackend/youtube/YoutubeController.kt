package com.proyectoN.rokolaBackend.youtube

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/youtube")
class YoutubeController(private val servicio: YoutubeService) {
    @GetMapping("/buscar")
    fun buscar(@RequestParam q: String): YoutubeBusquedaResponse = servicio.buscar(q)

    @GetMapping("/sugerencias")
    fun sugerencias(@RequestParam q: String): List<String> = servicio.sugerencias(q)
}
