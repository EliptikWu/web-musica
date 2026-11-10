package com.proyectoN.rokolaBackend.reproduccion

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.cancion.Disponibilidad
import com.proyectoN.rokolaBackend.cola.ColaRepository
import com.proyectoN.rokolaBackend.establecimiento.ConfiguracionServicioRepository
import com.proyectoN.rokolaBackend.establecimiento.EstadoServicio
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.Sesion
import com.proyectoN.rokolaBackend.sesion.SesionNoEncontradaException
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeVideoResponse
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Cambios de estado serializados mediante el mismo lock de sesion que solicitudes y votos. */
@Service
class ReproduccionGestionService(
    private val entityManager: EntityManager,
    private val jdbc: JdbcTemplate,
    private val cola: ColaRepository,
    private val actuales: ReproduccionConsultaRepository,
    private val configuraciones: ConfiguracionServicioRepository,
    private val horario: HorarioActivoService,
    private val cambios: CambioColaPublisher,
) {
    @Transactional
    fun iniciar(idSesion: Long, solicitudEsperada: Long, video: YoutubeVideoResponse): ReproduccionActualResponse {
        val sesion = bloquearSesion(idSesion)
        if (sesion.estado != EstadoSesion.ABIERTA || !sesion.establecimiento.activo) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "La sesion o el establecimiento no estan activos")
        }
        val idEstablecimiento = requireNotNull(sesion.establecimiento.id)
        val config = configuraciones.findByEstablecimientoId(idEstablecimiento)
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Falta configuracion de servicio")
        if (config.estadoManual != EstadoServicio.DISPONIBLE) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El servicio no esta disponible")
        }
        if (!horario.estaEnHorario(idEstablecimiento, sesion.establecimiento.zonaHoraria)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El establecimiento esta fuera de horario")
        }
        if (actuales.actual(idSesion) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una reproduccion en curso")
        }
        val primero = cola.listarPendientes(idSesion).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "No hay solicitudes pendientes")
        if (primero.solicitudId != solicitudEsperada || primero.youtubeVideoId != video.youtubeVideoId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "La cola cambio durante la validacion; vuelve a iniciar")
        }
        if (video.duracionSeg <= 0 || video.duracionSeg > config.duracionMaxSeg) {
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La duracion supera el limite permitido")
        }
        if (estaBloqueada(idEstablecimiento, primero.cancionId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cancion bloqueada en este establecimiento")
        }
        // Los metadatos se revalidaron con videos.list antes de adquirir el bloqueo.
        jdbc.update(
            """UPDATE public.cancion SET duracion_seg = ?, disponibilidad = ?,
               embebible = TRUE, ultima_validacion = CURRENT_TIMESTAMP WHERE id_cancion = ?""",
            video.duracionSeg, Disponibilidad.DISPONIBLE.valor, primero.cancionId,
        )
        val cambiadas = jdbc.update(
            "UPDATE public.solicitud SET estado = 'reproduciendo', motivo_estado = NULL WHERE id_solicitud = ? AND id_sesion = ? AND estado = 'pendiente'",
            primero.solicitudId, idSesion,
        )
        if (cambiadas != 1) throw ResponseStatusException(HttpStatus.CONFLICT, "La solicitud dejo de estar pendiente")
        val idReproduccion = requireNotNull(jdbc.queryForObject(
            """INSERT INTO public.reproduccion
               (id_sesion, id_cancion, id_solicitud, id_elemento, fecha_inicio, segundos_reproducidos, resultado)
               VALUES (?, ?, ?, NULL, CURRENT_TIMESTAMP, 0, 'iniciada')
               RETURNING id_reproduccion""",
            Long::class.javaObjectType,
            idSesion, primero.cancionId, primero.solicitudId,
        ))
        cambios.publicar(idSesion, "reproduccion_iniciada")
        return actuales.actual(idSesion)
            ?: throw IllegalStateException("No se encontro la reproduccion $idReproduccion recien creada")
    }

    @Transactional
    fun marcarNoDisponible(idSesion: Long, idSolicitud: Long) {
        bloquearSesion(idSesion)
        // Si la cola cambio durante la consulta a YouTube, no alteramos otra cancion.
        val primero = cola.listarPendientes(idSesion).firstOrNull()
        if (primero?.solicitudId != idSolicitud) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "La cola cambio durante la validacion")
        }
        jdbc.update(
            """UPDATE public.solicitud SET estado = 'no_disponible',
               motivo_estado = 'Video no disponible o no incrustable en la revalidacion de YouTube'
               WHERE id_solicitud = ? AND estado = 'pendiente'""",
            idSolicitud,
        )
        jdbc.update(
            """UPDATE public.cancion SET disponibilidad = 'no_disponible',
               embebible = FALSE, ultima_validacion = CURRENT_TIMESTAMP WHERE id_cancion = ?""",
            primero.cancionId,
        )
        cambios.publicar(idSesion, "solicitud_no_disponible")
    }

    @Transactional
    fun descartarPendiente(idSesion: Long, idSolicitud: Long, request: DescartarSolicitudRequest) {
        val estado = request.estado.trim().lowercase()
        if (estado !in setOf("saltada", "no_disponible", "bloqueada")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "estado debe ser saltada, no_disponible o bloqueada")
        }
        if (request.motivoEstado != null && request.motivoEstado.length > 500) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "motivoEstado supera 500 caracteres")
        }
        bloquearSesion(idSesion)
        val actualizadas = jdbc.update(
            """UPDATE public.solicitud SET estado = ?, motivo_estado = ?
               WHERE id_solicitud = ? AND id_sesion = ? AND estado = 'pendiente'""",
            estado, request.motivoEstado?.trim(), idSolicitud, idSesion,
        )
        if (actualizadas != 1) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no existe en la sesion o ya no esta pendiente")
        }
        cambios.publicar(idSesion, "solicitud_descartada")
    }

    @Transactional
    fun finalizar(
        idSesion: Long,
        idReproduccion: Long,
        request: FinalizarReproduccionRequest,
    ): ReproduccionFinalizadaResponse {
        val resultado = request.resultado.trim().lowercase()
        val estado = when (resultado) {
            "completada" -> "reproducida"
            "saltada" -> "saltada"
            "fallida" -> "no_disponible"
            else -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "resultado debe ser completada, saltada o fallida",
            )
        }
        if (request.segundosReproducidos != null && request.segundosReproducidos < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "segundosReproducidos debe ser >= 0")
        }
        bloquearSesion(idSesion)
        val actual = actuales.actual(idSesion)
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "No hay reproduccion activa")
        if (actual.reproduccionId != idReproduccion) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "La reproduccion indicada no es la actual")
        }
        val ahora = OffsetDateTime.now(ZoneOffset.UTC)
        val segundos = (request.segundosReproducidos ?: when (resultado) {
            "completada" -> actual.duracionSeg
            else -> actual.fechaInicio?.let { Duration.between(it.toInstant(), ahora.toInstant()).seconds.toInt() } ?: 0
        }).coerceIn(0, actual.duracionSeg)
        val actualizadas = jdbc.update(
            """UPDATE public.reproduccion SET resultado = ?, fecha_fin = CURRENT_TIMESTAMP,
               segundos_reproducidos = ?
               WHERE id_reproduccion = ? AND id_sesion = ? AND resultado = 'iniciada'""",
            resultado, segundos, idReproduccion, idSesion,
        )
        if (actualizadas != 1) throw ResponseStatusException(HttpStatus.CONFLICT, "La reproduccion ya termino")
        if (actual.solicitudId != null) {
            val cambiadas = jdbc.update(
                """UPDATE public.solicitud SET estado = ?, motivo_estado = ?
                   WHERE id_solicitud = ? AND id_sesion = ? AND estado = 'reproduciendo'""",
                estado,
                if (resultado == "fallida") "Error informado por el reproductor" else null,
                actual.solicitudId, idSesion,
            )
            if (cambiadas != 1) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no estaba reproduciendo")
            }
        }
        cambios.publicar(idSesion, "reproduccion_$resultado")
        return ReproduccionFinalizadaResponse(
            reproduccionId = idReproduccion,
            sesionId = idSesion,
            solicitudId = actual.solicitudId,
            cancionId = actual.cancionId,
            resultado = resultado,
            estadoSolicitud = if (actual.solicitudId != null) estado else null,
            fechaFin = ahora,
            segundosReproducidos = segundos,
        )
    }

    private fun bloquearSesion(idSesion: Long): Sesion =
        entityManager.find(Sesion::class.java, idSesion, LockModeType.PESSIMISTIC_WRITE)
            ?: throw SesionNoEncontradaException(idSesion)

    private fun estaBloqueada(idEstablecimiento: Long, idCancion: Long): Boolean =
        jdbc.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1 FROM public.bloqueo_contenido
                WHERE id_establecimiento = ? AND id_cancion = ?
                  AND activo = TRUE AND fecha_inicio <= CURRENT_TIMESTAMP
                  AND (fecha_fin IS NULL OR fecha_fin > CURRENT_TIMESTAMP)
            )
            """.trimIndent(),
            Boolean::class.javaObjectType,
            idEstablecimiento, idCancion,
        ) ?: false
}

data class DescartarSolicitudRequest(val estado: String, val motivoEstado: String? = null)
