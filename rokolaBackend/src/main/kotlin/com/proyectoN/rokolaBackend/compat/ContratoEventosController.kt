package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.ContratoSseService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/** SSE público: EventSource no permite añadir X-Rockola-Dispositivo. */
@RestController
@RequestMapping("/api/bares/{slug}")
class ContratoEventosController(private val sse: ContratoSseService) {
    @GetMapping(value = ["/eventos"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun eventos(@PathVariable slug: String): ResponseEntity<SseEmitter> = ResponseEntity.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
        .header("X-Accel-Buffering", "no")
        .body(sse.abrir(slug))
}
