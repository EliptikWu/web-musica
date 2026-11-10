package com.proyectoN.rokolaBackend.solicitud

import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import com.proyectoN.rokolaBackend.cola.ColaService
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.SesionNoEncontradaException
import com.proyectoN.rokolaBackend.sesion.SesionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

/** Consulta privada: solo las solicitudes creadas por el dispositivo de este establecimiento. */
@Service
class MisSolicitudesService(
    private val tokens: TokenSeguro,
    private val sesiones: SesionRepository,
    private val jdbc: JdbcTemplate,
    private val colaService: ColaService,
) {
    @Transactional(readOnly = true)
    fun obtener(idSesion: Long, deviceToken: String): MisSolicitudesResponse {
        if (!tokens.esValido(deviceToken)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identificador de dispositivo no valido")
        }
        val sesion = sesiones.findByIdOrNull(idSesion) ?: throw SesionNoEncontradaException(idSesion)
        val idEstablecimiento = requireNotNull(sesion.establecimiento.id)
        val hash = tokens.huellaDispositivo(idEstablecimiento, deviceToken)
        val idDispositivo = jdbc.query(
            """
            SELECT id_dispositivo, aviso_privacidad_fecha
            FROM public.dispositivo
            WHERE id_establecimiento = ? AND device_hash = ?
            """.trimIndent(),
            { rs, _ ->
                Pair(rs.getLong("id_dispositivo"), rs.getObject("aviso_privacidad_fecha") != null)
            },
            idEstablecimiento, hash,
        ).firstOrNull() ?: throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Dispositivo no registrado en este establecimiento",
        )
        if (!idDispositivo.second) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Debes aceptar el aviso de privacidad")
        }

        // La consulta historica funciona incluso cuando la sesion ya esta cerrada.
        val pendientes = if (sesion.estado == EstadoSesion.ABIERTA && sesion.establecimiento.activo) {
            colaService.obtener(idSesion).canciones.associateBy { it.solicitudId }
        } else emptyMap()

        val solicitudes = jdbc.query(
            """
            SELECT s.id_solicitud, s.id_cancion, s.estado, s.motivo_estado, s.fecha_solicitud,
                   c.youtube_video_id, c.titulo, c.artista, c.miniatura_url,
                   (SELECT COUNT(*) FROM public.voto v
                    WHERE v.id_solicitud = s.id_solicitud AND v.activo = TRUE) AS votos
            FROM public.solicitud s
            JOIN public.cancion c ON c.id_cancion = s.id_cancion
            WHERE s.id_sesion = ? AND s.id_dispositivo_solicitante = ?
            ORDER BY s.fecha_solicitud DESC, s.id_solicitud DESC
            """.trimIndent(),
            { rs, _ ->
                val id = rs.getLong("id_solicitud")
                val cola = pendientes[id]
                MiSolicitudResponse(
                    solicitudId = id,
                    cancionId = rs.getLong("id_cancion"),
                    youtubeVideoId = rs.getString("youtube_video_id"),
                    titulo = rs.getString("titulo"),
                    artista = rs.getString("artista"),
                    miniaturaUrl = rs.getString("miniatura_url"),
                    estado = rs.getString("estado"),
                    motivoEstado = rs.getString("motivo_estado"),
                    fechaSolicitud = rs.getObject("fecha_solicitud", OffsetDateTime::class.java),
                    votos = rs.getLong("votos"),
                    posicionEnCola = cola?.posicion,
                    esperaEstimadaSeg = cola?.esperaEstimadaSeg,
                )
            },
            idSesion, idDispositivo.first,
        )
        return MisSolicitudesResponse(idSesion, solicitudes.size, solicitudes)
    }
}
