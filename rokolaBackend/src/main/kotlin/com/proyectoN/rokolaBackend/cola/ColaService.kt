package com.proyectoN.rokolaBackend.cola

import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.SesionNoEncontradaException
import com.proyectoN.rokolaBackend.sesion.SesionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class ColaService(
    private val sesiones: SesionRepository,
    private val cola: ColaRepository,
) {
    fun obtener(idSesion: Long): ColaResponse {
        val sesion = sesiones.findByIdOrNull(idSesion)
            ?: throw SesionNoEncontradaException(idSesion)

        // La cola pública solo está disponible durante sesiones abiertas y locales activos.
        if (sesion.estado != EstadoSesion.ABIERTA || !sesion.establecimiento.activo) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cola pública no disponible")
        }

        val canciones = cola.listarPendientes(idSesion).mapIndexed { indice, fila ->
            CancionEnColaResponse(
                posicion = indice + 1,
                solicitudId = fila.solicitudId,
                cancionId = fila.cancionId,
                youtubeVideoId = fila.youtubeVideoId,
                titulo = fila.titulo,
                artista = fila.artista,
                duracionSeg = fila.duracionSeg,
                miniaturaUrl = fila.miniaturaUrl,
                votos = fila.votos,
                fechaSolicitud = fila.fechaSolicitud,
            )
        }
        return ColaResponse(
            sesionId = idSesion,
            totalSolicitudes = canciones.size,
            canciones = canciones,
        )
    }
}
