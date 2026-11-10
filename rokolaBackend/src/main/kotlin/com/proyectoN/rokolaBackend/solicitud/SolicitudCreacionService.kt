package com.proyectoN.rokolaBackend.solicitud

import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/** Hace la llamada externa ANTES de abrir la transacción que bloqueará la sesión. */
@Service
class SolicitudCreacionService(
    private val youtube: YoutubeApiCliente,
    private val tokens: TokenSeguro,
    private val registro: SolicitudRegistroService,
) {
    private val videoIdValido = Regex("^[A-Za-z0-9_-]{11}$")

    fun crear(idSesion: Long, request: CrearSolicitudRequest): CrearSolicitudResponse {
        if (idSesion <= 0L) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "idSesion debe ser mayor que cero")
        }
        if (!tokens.esValido(request.deviceToken)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identificador de dispositivo no válido")
        }
        if (!videoIdValido.matches(request.youtubeVideoId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "youtubeVideoId no tiene el formato válido")
        }
        val video = youtube.obtenerVideoPorId(request.youtubeVideoId)
            ?: throw ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "El video no existe, es privado, no es incrustable o no tiene duración válida",
            )
        return registro.registrar(idSesion, request.deviceToken, video)
    }
}
