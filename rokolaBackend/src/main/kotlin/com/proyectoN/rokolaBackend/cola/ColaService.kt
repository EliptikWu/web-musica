package com.proyectoN.rokolaBackend.cola

import com.proyectoN.rokolaBackend.reproduccion.ReproduccionConsultaRepository
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.SesionNoEncontradaException
import com.proyectoN.rokolaBackend.sesion.SesionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
@Transactional(readOnly = true)
class ColaService(
  private val sesiones: SesionRepository,
  private val cola: ColaRepository,
  private val reproducciones: ReproduccionConsultaRepository,
) {
  fun obtener(idSesion: Long): ColaResponse {
    val sesion = sesiones.findByIdOrNull(idSesion) ?: throw SesionNoEncontradaException(idSesion)
    if (sesion.estado != EstadoSesion.ABIERTA || !sesion.establecimiento.activo) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cola publica no disponible")
    }

    val ahora = OffsetDateTime.now(ZoneOffset.UTC)
    val actual = reproducciones.actual(idSesion, ahora)
    var esperaAcumulada = actual?.segundosRestantes ?: 0L
    // El orden ya existente es: votos DESC, fecha de solicitud ASC, id ASC.
    val canciones = cola.listarPendientes(idSesion).mapIndexed { indice, fila ->
      val espera = esperaAcumulada
      esperaAcumulada += fila.duracionSeg.coerceAtLeast(0).toLong()
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
        esperaEstimadaSeg = espera,
        inicioEstimado = ahora.plusSeconds(espera),
      )
    }
    return ColaResponse(idSesion, ahora, canciones.size, actual?.segundosRestantes, canciones)
  }
}
