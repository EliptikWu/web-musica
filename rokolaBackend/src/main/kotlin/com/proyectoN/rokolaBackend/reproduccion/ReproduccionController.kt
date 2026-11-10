package com.proyectoN.rokolaBackend.reproduccion

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ReproduccionController(
    private val consulta: ReproduccionConsultaService,
    private val orquestador: ReproduccionOrquestadorService,
    private val gestion: ReproduccionGestionService,
) {
    /** Publico: indica que cancion esta sonando, o actual=null. */
    @GetMapping("/api/sesiones/{idSesion}/reproduccion/actual")
    fun actual(@PathVariable idSesion: Long): EstadoReproduccionResponse = consulta.obtener(idSesion)

    /** Solo operador autenticado. Inicia la primera solicitud pendiente por votos. */
    @PostMapping("/api/sesiones/{idSesion}/reproduccion/iniciar")
    fun iniciar(@PathVariable idSesion: Long): ResponseEntity<ReproduccionActualResponse> =
        ResponseEntity.status(201).body(orquestador.iniciarSiguiente(idSesion))

    /** Solo operador autenticado. El frontend avisa cuando YouTube termino, fallo o fue saltado. */
    @PostMapping("/api/sesiones/{idSesion}/reproducciones/{idReproduccion}/finalizar")
    fun finalizar(
        @PathVariable idSesion: Long,
        @PathVariable idReproduccion: Long,
        @RequestBody request: FinalizarReproduccionRequest,
    ): ReproduccionFinalizadaResponse = gestion.finalizar(idSesion, idReproduccion, request)

    /** Resuelve un video pendiente que debe excluirse de la cola sin iniciar reproduccion. */
    @PostMapping("/api/sesiones/{idSesion}/solicitudes/{idSolicitud}/descartar")
    fun descartar(
        @PathVariable idSesion: Long,
        @PathVariable idSolicitud: Long,
        @RequestBody request: DescartarSolicitudRequest,
    ): ResponseEntity<Void> {
        gestion.descartarPendiente(idSesion, idSolicitud, request)
        return ResponseEntity.noContent().build()
    }
}
