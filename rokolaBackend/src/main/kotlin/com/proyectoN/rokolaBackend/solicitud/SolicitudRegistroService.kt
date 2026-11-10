package com.proyectoN.rokolaBackend.solicitud

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import com.proyectoN.rokolaBackend.cancion.Cancion
import com.proyectoN.rokolaBackend.cancion.CancionRepository
import com.proyectoN.rokolaBackend.cancion.Disponibilidad
import com.proyectoN.rokolaBackend.dispositivo.Dispositivo
import com.proyectoN.rokolaBackend.establecimiento.ConfiguracionServicioRepository
import com.proyectoN.rokolaBackend.establecimiento.EstadoServicio
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.Sesion
import com.proyectoN.rokolaBackend.sesion.SesionNoEncontradaException
import com.proyectoN.rokolaBackend.voto.Voto
import com.proyectoN.rokolaBackend.voto.VotoRepository
import com.proyectoN.rokolaBackend.youtube.YoutubeVideoResponse
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Registro atómico de canción, solicitud y voto; serializa solicitudes de una sesión. */
@Service
class SolicitudRegistroService(
  private val entityManager: EntityManager,
  private val tokens: TokenSeguro,
  private val configuraciones: ConfiguracionServicioRepository,
  private val horario: HorarioActivoService,
  private val canciones: CancionRepository,
  private val solicitudes: SolicitudRepository,
  private val votos: VotoRepository,
  private val jdbc: JdbcTemplate,
  private val cambios: CambioColaPublisher,
) {
  @Transactional
  fun registrar(idSesion: Long, deviceToken: String, video: YoutubeVideoResponse): CrearSolicitudResponse {
    // Bloqueo pesimista: impide que dos POST simultáneos alteren la misma cola a la vez.
    val sesion = entityManager.find(Sesion::class.java, idSesion, LockModeType.PESSIMISTIC_WRITE)
      ?: throw SesionNoEncontradaException(idSesion)
    val establecimiento = sesion.establecimiento
    val idEstablecimiento = requireNotNull(establecimiento.id)
    if (!establecimiento.activo || sesion.estado != EstadoSesion.ABIERTA) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "El establecimiento o la sesión no están activos")
    }
    val configuracion = configuraciones.findByEstablecimientoId(idEstablecimiento)
      ?: throw ResponseStatusException(HttpStatus.CONFLICT, "El establecimiento no tiene configuración de servicio")
    if (configuracion.estadoManual != EstadoServicio.DISPONIBLE) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "El servicio está pausado o en mantenimiento")
    }
    if (!horario.estaEnHorario(idEstablecimiento, establecimiento.zonaHoraria)) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "El establecimiento está fuera del horario de operación")
    }

    // El token de cliente es secreto; se verifica usando el HMAC que ya utiliza el registro QR.
    val hash = tokens.huellaDispositivo(idEstablecimiento, deviceToken)
    val dispositivo = entityManager.createQuery(
      "SELECT d FROM Dispositivo d WHERE d.establecimiento.id = :establecimiento AND d.deviceHash = :hash",
      Dispositivo::class.java,
    )
      .setParameter("establecimiento", idEstablecimiento)
      .setParameter("hash", hash)
      .setMaxResults(1)
      .resultList.firstOrNull()
      ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dispositivo no registrado en este establecimiento")

    if (dispositivo.avisoPrivacidadFecha == null) {
      throw ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "Debes aceptar el aviso de privacidad antes de realizar solicitudes",
      )
    }
    if (video.duracionSeg <= 0 || video.duracionSeg > configuracion.duracionMaxSeg) {
      throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La canción supera la duración permitida")
    }

    val ahora = OffsetDateTime.now(ZoneOffset.UTC)
    val cancion = canciones.findByYoutubeVideoId(video.youtubeVideoId)?.apply {
      titulo = video.titulo.take(300)
      artista = video.artista?.take(200)
      canal = video.canal.take(200)
      duracionSeg = video.duracionSeg
      miniaturaUrl = video.miniaturaUrl
      embebible = true
      disponibilidad = Disponibilidad.DISPONIBLE
      ultimaValidacion = ahora
    } ?: canciones.saveAndFlush(
      Cancion(
        youtubeVideoId = video.youtubeVideoId,
        titulo = video.titulo.take(300),
        artista = video.artista?.take(200),
        canal = video.canal.take(200),
        duracionSeg = video.duracionSeg,
        miniaturaUrl = video.miniaturaUrl,
        embebible = true,
        disponibilidad = Disponibilidad.DISPONIBLE,
        ultimaValidacion = ahora,
        fechaCreacion = ahora,
      ),
    )
    val idCancion = requireNotNull(cancion.id)

    if (estaBloqueada(idEstablecimiento, idCancion)) {
      throw ResponseStatusException(HttpStatus.FORBIDDEN, "Esta canción está bloqueada en el establecimiento")
    }
    if (seReprodujoRecientemente(idEstablecimiento, idCancion, configuracion.minutosBloqueoRepeticion)) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "Esta canción se reprodujo recientemente")
    }

    val existente = solicitudes.findFirstBySesionIdAndCancionIdAndEstadoIn(
      idSesion,
      idCancion,
      listOf(EstadoSolicitud.PENDIENTE, EstadoSolicitud.REPRODUCIENDO),
    )
    if (existente != null) {
      if (existente.estado != EstadoSolicitud.PENDIENTE) {
        throw ResponseStatusException(HttpStatus.CONFLICT, "Esta canción ya está en reproducción")
      }
      val idSolicitud = requireNotNull(existente.id)
      val idDispositivo = requireNotNull(dispositivo.id)
      if (votos.existsBySolicitudIdAndDispositivoIdAndActivoTrue(idSolicitud, idDispositivo)) {
        throw ResponseStatusException(HttpStatus.CONFLICT, "Este dispositivo ya votó por la canción")
      }
      votos.saveAndFlush(
        Voto(
          solicitud = existente,
          dispositivo = dispositivo,
          fechaVoto = ahora,
          activo = true,
        ),
      )
      cambios.publicar(idSesion, "voto_agregado")
      return respuesta(existente, idSesion, idCancion, video.youtubeVideoId, "voto_agregado")
    }

    // El máximo se aplica a solicitudes NUEVAS creadas por dispositivo durante esta sesión;
    // si está configurado en 0, no se permiten solicitudes nuevas.
    val maximo = configuracion.maxSolicitudesDispositivo
    val creadas = jdbc.queryForObject(
      """SELECT COUNT(*) FROM public.solicitud
               WHERE id_sesion = ? AND id_dispositivo_solicitante = ?""",
      Long::class.javaObjectType,
      idSesion,
      requireNotNull(dispositivo.id),
    ) ?: 0L
    if (creadas >= maximo.toLong()) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "Se alcanzó el límite de solicitudes de este dispositivo")
    }

    val nueva = solicitudes.saveAndFlush(
      Solicitud(
        sesion = sesion,
        cancion = cancion,
        dispositivoSolicitante = dispositivo,
        fechaSolicitud = ahora,
        estado = EstadoSolicitud.PENDIENTE,
      ),
    )
    votos.saveAndFlush(
      Voto(
        solicitud = nueva,
        dispositivo = dispositivo,
        fechaVoto = ahora,
        activo = true,
      ),
    )
    cambios.publicar(idSesion, "solicitud_creada")
    return respuesta(nueva, idSesion, idCancion, video.youtubeVideoId, "solicitud_creada")
  }

  private fun respuesta(
    solicitud: Solicitud,
    idSesion: Long,
    idCancion: Long,
    videoId: String,
    tipo: String,
  ): CrearSolicitudResponse {
    val solicitudId = requireNotNull(solicitud.id)
    return CrearSolicitudResponse(
      solicitudId = solicitudId,
      sesionId = idSesion,
      cancionId = idCancion,
      youtubeVideoId = videoId,
      resultado = tipo,
      estado = solicitud.estado.valor,
      votos = votos.countBySolicitudIdAndActivoTrue(solicitudId),
    )
  }

  private fun estaBloqueada(idEstablecimiento: Long, idCancion: Long): Boolean =
    jdbc.queryForObject(
      """
            SELECT EXISTS (
                SELECT 1 FROM public.bloqueo_contenido
                WHERE id_establecimiento = ? AND id_cancion = ?
                  AND activo = TRUE
                  AND fecha_inicio <= CURRENT_TIMESTAMP
                  AND (fecha_fin IS NULL OR fecha_fin > CURRENT_TIMESTAMP)
            )
            """.trimIndent(),
      Boolean::class.javaObjectType,
      idEstablecimiento,
      idCancion,
    ) ?: false

  private fun seReprodujoRecientemente(idEstablecimiento: Long, idCancion: Long, minutos: Int): Boolean {
    if (minutos <= 0) return false
    return jdbc.queryForObject(
      """
            SELECT EXISTS (
                SELECT 1 FROM public.reproduccion r
                JOIN public.sesion s ON s.id_sesion = r.id_sesion
                WHERE s.id_establecimiento = ? AND r.id_cancion = ?
                  AND r.fecha_inicio > CURRENT_TIMESTAMP - (? * INTERVAL '1 minute')
                  AND r.resultado IN ('iniciada', 'completada')
            )
            """.trimIndent(),
      Boolean::class.javaObjectType,
      idEstablecimiento,
      idCancion,
      minutos,
    ) ?: false
  }
}
