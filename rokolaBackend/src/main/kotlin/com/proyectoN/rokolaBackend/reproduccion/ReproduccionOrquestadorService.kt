package com.proyectoN.rokolaBackend.reproduccion

import com.proyectoN.rokolaBackend.cola.ColaService
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/** La llamada de red a YouTube ocurre ANTES de la transaccion de escritura. */
@Service
class ReproduccionOrquestadorService(
    private val cola: ColaService,
    private val youtube: YoutubeApiCliente,
    private val gestion: ReproduccionGestionService,
) {
    fun iniciarSiguiente(idSesion: Long): ReproduccionActualResponse {
        val siguiente = cola.obtener(idSesion).canciones.firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "No hay solicitudes pendientes")
        val video = youtube.obtenerVideoPorId(siguiente.youtubeVideoId)
        if (video == null) {
            gestion.marcarNoDisponible(idSesion, siguiente.solicitudId)
            throw ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "El video no puede reproducirse; la solicitud se marco no_disponible. Intenta iniciar la siguiente",
            )
        }
        return gestion.iniciar(idSesion, siguiente.solicitudId, video)
    }
}
