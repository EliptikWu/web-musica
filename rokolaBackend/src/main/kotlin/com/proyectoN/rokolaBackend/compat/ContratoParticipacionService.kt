package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.cola.ColaService
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.Sesion
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import com.proyectoN.rokolaBackend.youtube.YoutubeVideoResponse
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.HexFormat
import java.util.UUID

/**
 * Adaptación contractual de solicitudes y votos.
 * - Comparte las tablas y bloqueo de sesion con los endpoints antiguos.
 * - Registra los resultados HTTP exitosos por Idempotency-Key dentro de la MISMA transacción.
 * - No guarda UUID ni llave API, solo SHA-256 y clave de idempotencia.
 * - Nunca confía en un id_dispositivo enviado por el cliente.
 */
@Service
class ContratoParticipacionService(
  private val jdbc: JdbcTemplate,
  private val em: EntityManager,
  private val dispositivos: ContratoDispositivoService,
  private val horarios: HorarioActivoService,
  private val youtube: YoutubeApiCliente,
  private val cola: ColaService,
  private val cambios: CambioColaPublisher,
  private val json: JsonMapper,
) {
  data class RespuestaGuardada(val estado: Int, val json: String)
  private data class Config(val maximo: Int, val duracion: Int, val repeticion: Int, val estado: String)
  private data class Existente(val id: Long, val estado: String)
  private data class Cancion(val id: Long, val disponible: String, val embebible: Boolean)
  private data class RegistroIdempotente(val operacion: String, val huella: String, val respuesta: RespuestaGuardada)
  private val videoValido = Regex("^[A-Za-z0-9_-]{11}$")

  /** Para llamadas nuevas y repetidas: replay antes de revisar sesion/horario/YouTube. */
  @Transactional
  fun pedir(slug: String, uuid: String?, claveCabecera: String?, videoId: String): RespuestaGuardada {
    if (!videoValido.matches(videoId)) error(HttpStatus.BAD_REQUEST, "datos_invalidos", "video_id debe tener 11 caracteres válidos")
    val key = clave(claveCabecera)
    val identidad = dispositivos.identificar(slug, uuid)
    val hashDispositivo = dispositivos.huella(uuid)
    val alcance = "pedir|$slug|$videoId"
    val huella = sha256(alcance)
    bloquearClave(identidad.establecimientoId, hashDispositivo, key)
    repeticion(identidad.establecimientoId, hashDispositivo, key, "pedir", huella)?.let { return it }

    comprobarAviso(identidad)
    val sesion = sesionAbiertaConBloqueo(identidad.establecimientoId)
    val idSesion = requireNotNull(sesion.id)
    val config = comprobarServicio(sesion)

    // Validacion con YouTube: la API no se consulta en replays.
    // NOTA: mientras se valida el video se mantiene el lock de sesion; se debe optimizar al escalar.
    val video = validarVideo(videoId)
    if (!video.embebible) error(HttpStatus.UNPROCESSABLE_ENTITY, "no_embebible", "El video no admite reproducción incrustada")
    if (video.duracionSeg > config.duracion || video.duracionSeg <= 0)
      error(HttpStatus.UNPROCESSABLE_ENTITY, "muy_larga", "El video supera la duración máxima del bar")

    val idCancion = guardarCancion(video)
    if (bloqueada(identidad.establecimientoId, idCancion))
      error(HttpStatus.UNPROCESSABLE_ENTITY, "bloqueada", "La canción está bloqueada por el bar")
    if (repetida(identidad.establecimientoId, idCancion, config.repeticion))
      error(HttpStatus.UNPROCESSABLE_ENTITY, "reproducida_hace_poco", "La canción sonó hace poco")

    val existente = buscarSolicitud(idSesion, idCancion)
    val idSolicitud: Long
    val tipo: String
    if (existente != null) {
      if (existente.estado != "pendiente") error(HttpStatus.GONE, "no_pendiente", "La canción ya está en reproducción")
      idSolicitud = existente.id
      comprobarVotoNuevo(idSolicitud, identidad.dispositivoId)
      insertarVoto(idSolicitud, identidad.dispositivoId)
      tipo = "votada"
    } else {
      // RN-01: las canciones propias que están sonando también ocupan cupo.
      val ocupados = ContratoColaCalculos.cuposOcupados(jdbc, idSesion, identidad.dispositivoId)
      if (ocupados >= config.maximo)
        error(HttpStatus.CONFLICT, "limite_pedidos", "Se alcanzó el cupo de solicitudes pendientes o sonando")
      idSolicitud = jdbc.queryForObject(
        """INSERT INTO public.solicitud
                   (id_sesion,id_cancion,id_dispositivo_solicitante,fecha_solicitud,estado)
                   VALUES (?,?,?,CURRENT_TIMESTAMP,'pendiente') RETURNING id_solicitud""",
        Long::class.javaObjectType, idSesion, idCancion, identidad.dispositivoId,
      ) ?: throw IllegalStateException("No se registró la solicitud")
      insertarVoto(idSolicitud, identidad.dispositivoId)
      tipo = "creada"
    }
    val puestoYEspera = cola.obtener(idSesion).canciones.firstOrNull { it.solicitudId == idSolicitud }
      ?: throw IllegalStateException("La solicitud no figura en la cola recién actualizada")
    val restantes = ContratoColaCalculos.cuposRestantes(
      config.maximo, ContratoColaCalculos.cuposOcupados(jdbc, idSesion, identidad.dispositivoId),
    )
    val resultado = respuesta(201, mapOf(
      "resultado" to tipo,
      "solicitud_id" to idSolicitud.toString(), // OpenAPI Id: string, NO bigint en JSON.
      "puesto" to puestoYEspera.posicion,
      "espera_seg" to puestoYEspera.esperaEstimadaSeg.coerceAtLeast(0),
      "pedidos_restantes" to restantes,
    ))
    guardarClave(identidad.establecimientoId, hashDispositivo, key, "pedir", huella, resultado)
    cambios.publicar(idSesion, if (tipo == "creada") "solicitud_creada" else "voto_agregado")
    return resultado
  }

  @Transactional
  fun votar(slug: String, idExterno: String, uuid: String?, claveCabecera: String?): RespuestaGuardada {
    val idSolicitud = idExterno.toLongOrNull()?.takeIf { it > 0L }
      ?: error(HttpStatus.BAD_REQUEST, "datos_invalidos", "El identificador de la solicitud no es válido")
    val key = clave(claveCabecera)
    val identidad = dispositivos.identificar(slug, uuid)
    val hashDispositivo = dispositivos.huella(uuid)
    val huella = sha256("votar|$slug|$idSolicitud")
    bloquearClave(identidad.establecimientoId, hashDispositivo, key)
    repeticion(identidad.establecimientoId, hashDispositivo, key, "votar", huella)?.let { return it }

    comprobarAviso(identidad)
    val idSesion = jdbc.query(
      """SELECT s.id_sesion FROM public.solicitud s JOIN public.sesion se ON se.id_sesion = s.id_sesion
               WHERE s.id_solicitud=? AND se.id_establecimiento=?""",
      { rs, _ -> rs.getLong(1) }, idSolicitud, identidad.establecimientoId,
    ).firstOrNull() ?: error(HttpStatus.NOT_FOUND, "no_encontrado", "Solicitud no encontrada en este bar")
    val sesion = em.find(Sesion::class.java, idSesion, LockModeType.PESSIMISTIC_WRITE)
      ?: error(HttpStatus.NOT_FOUND, "no_encontrado", "Sesión no encontrada")
    comprobarServicio(sesion)
    val fila = jdbc.query(
      """SELECT s.estado, c.disponibilidad, c.embebible, c.id_cancion
               FROM public.solicitud s JOIN public.cancion c ON c.id_cancion=s.id_cancion
               WHERE s.id_solicitud=? AND s.id_sesion=?""",
      { rs, _ -> Pair(rs.getString("estado"),
        Cancion(rs.getLong("id_cancion"),rs.getString("disponibilidad"),rs.getBoolean("embebible"))) }, idSolicitud, idSesion,
    ).firstOrNull() ?: error(HttpStatus.NOT_FOUND, "no_encontrado", "Solicitud no encontrada")
    if (fila.first != "pendiente") error(HttpStatus.GONE, "no_pendiente", "La solicitud ya no está pendiente")
    if (fila.second.disponible != "disponible") error(HttpStatus.UNPROCESSABLE_ENTITY, "no_disponible", "El video no está disponible")
    if (!fila.second.embebible) error(HttpStatus.UNPROCESSABLE_ENTITY, "no_embebible", "El video no es incrustable")
    if (bloqueada(identidad.establecimientoId, fila.second.id))
      error(HttpStatus.UNPROCESSABLE_ENTITY, "bloqueada", "La canción está bloqueada")
    comprobarVotoNuevo(idSolicitud, identidad.dispositivoId)
    insertarVoto(idSolicitud, identidad.dispositivoId)
    val total = totalVotos(idSolicitud)
    val resultado = respuesta(201, mapOf("solicitud_id" to idSolicitud.toString(), "votos_total" to total))
    guardarClave(identidad.establecimientoId, hashDispositivo, key, "votar", huella, resultado)
    cambios.publicar(idSesion, "voto_registrado")
    return resultado
  }

  private data class SolicitudParaCambio(
    val id: Long,
    val estado: String,
    val solicitanteId: Long,
    val cancionId: Long,
    val disponibilidad: String,
    val embebible: Boolean,
    val videoId: String,
  )

  /**
   * RF-014: traslada un voto adicional entre dos solicitudes pendientes.
   * No se permite trasladar el voto inicial que acompaña a una solicitud propia.
   * Misma transacción: cancelar voto origen, insertar destino, registrar evento e idempotencia.
   * El bloqueo PESSIMISTIC_WRITE de sesion serializa cambios con pedir/votar legacy y contrato.
   */
  @Transactional
  fun cambiarVoto(
    slug: String,
    uuid: String?,
    claveCabecera: String?,
    desdeExterno: String,
    haciaExterno: String,
  ): RespuestaGuardada {
    val desde = idPositivo(desdeExterno)
    val hacia = idPositivo(haciaExterno)
    if (desde == hacia) error(HttpStatus.BAD_REQUEST, "datos_invalidos", "Origen y destino deben ser distintos")

    val key = clave(claveCabecera)
    val identidad = dispositivos.identificar(slug, uuid)
    val hashDispositivo = dispositivos.huella(uuid)
    val huella = sha256("cambiar_voto|$slug|$desde|$hacia")
    bloquearClave(identidad.establecimientoId, hashDispositivo, key)
    repeticion(identidad.establecimientoId, hashDispositivo, key, "cambiar_voto", huella)?.let { return it }

    comprobarAviso(identidad)
    // El id de la sesion de origen solo se consulta para poder bloquearla.
    val idSesion = jdbc.query(
      """SELECT s.id_sesion FROM public.solicitud s
               JOIN public.sesion se ON se.id_sesion = s.id_sesion
               WHERE s.id_solicitud = ? AND se.id_establecimiento = ?""",
      { rs, _ -> rs.getLong(1) }, desde, identidad.establecimientoId,
    ).firstOrNull() ?: error(HttpStatus.NOT_FOUND, "no_encontrado", "Solicitud de origen no encontrada en el bar")

    val sesion = em.find(Sesion::class.java, idSesion, LockModeType.PESSIMISTIC_WRITE)
      ?: error(HttpStatus.NOT_FOUND, "no_encontrado", "Sesión no encontrada")
    comprobarServicio(sesion)

    // Releer DESPUÉS del bloqueo: tanto estado como pertenencia deben ser consistentes.
    val solicitudes: Map<Long, SolicitudParaCambio> = jdbc.query(
      """SELECT s.id_solicitud, s.estado, s.id_dispositivo_solicitante,
                      c.id_cancion, c.disponibilidad, c.embebible, c.youtube_video_id
               FROM public.solicitud s JOIN public.cancion c ON c.id_cancion=s.id_cancion
               WHERE s.id_solicitud IN (?,?) AND s.id_sesion = ?""",
      { rs, _ -> SolicitudParaCambio(
        rs.getLong("id_solicitud"), rs.getString("estado"),
        rs.getLong("id_dispositivo_solicitante"), rs.getLong("id_cancion"),
        rs.getString("disponibilidad"), rs.getBoolean("embebible"),
        rs.getString("youtube_video_id"),
      ) }, desde, hacia, idSesion,
    ).associateBy { it.id }
    val origen = solicitudes[desde]
      ?: error(HttpStatus.NOT_FOUND, "no_encontrado", "Solicitud de origen no encontrada")
    val destino = solicitudes[hacia]
      ?: error(HttpStatus.BAD_REQUEST, "datos_invalidos", "Ambas solicitudes deben pertenecer a la misma sesión")
    if (origen.estado != "pendiente" || destino.estado != "pendiente")
      error(HttpStatus.GONE, "no_pendiente", "Solo se puede mover el voto entre solicitudes pendientes")
    if (origen.solicitanteId == identidad.dispositivoId)
      error(HttpStatus.BAD_REQUEST, "datos_invalidos", "No se puede mover el voto inicial de una solicitud propia")
    if (destino.disponibilidad != "disponible")
      error(HttpStatus.UNPROCESSABLE_ENTITY, "no_disponible", "La canción de destino no está disponible")
    if (!destino.embebible)
      error(HttpStatus.UNPROCESSABLE_ENTITY, "no_embebible", "La canción de destino no admite reproducción incrustada")
    if (bloqueada(identidad.establecimientoId, destino.cancionId))
      error(HttpStatus.UNPROCESSABLE_ENTITY, "bloqueada", "La canción de destino está bloqueada")

    val votoOrigen = jdbc.query(
      """SELECT id_voto FROM public.voto
               WHERE id_solicitud = ? AND id_dispositivo = ? AND activo = TRUE""",
      { rs, _ -> rs.getLong(1) }, desde, identidad.dispositivoId,
    ).firstOrNull() ?: error(HttpStatus.BAD_REQUEST, "datos_invalidos", "No existe un voto activo tuyo en el origen")
    comprobarVotoNuevo(hacia, identidad.dispositivoId)

    val actualizados = jdbc.update(
      """UPDATE public.voto SET activo = FALSE, fecha_retiro = CURRENT_TIMESTAMP
               WHERE id_voto = ? AND activo = TRUE""", votoOrigen,
    )
    if (actualizados != 1) error(HttpStatus.CONFLICT, "conflicto", "El voto de origen cambió; intenta nuevamente")
    insertarVoto(hacia, identidad.dispositivoId)

    // Evento RF-046. Solo se almacena la huella, nunca el UUID completo.
    val detalle = json.writeValueAsString(mapOf(
      "desde" to desde.toString(), "hacia" to hacia.toString(),
    ))
    jdbc.update(
      """INSERT INTO public.evento_analitico
               (nombre_evento, id_establecimiento, id_sesion, device_hash,
                youtube_video_id, origen_app, propiedades)
               VALUES ('cambio_voto', ?, ?, ?, ?, 'cliente', CAST(? AS jsonb))""",
      identidad.establecimientoId, idSesion, hashDispositivo, destino.videoId, detalle,
    )
    val resultado = respuesta(200, mapOf(
      "solicitud_id" to hacia.toString(), "votos_total" to totalVotos(hacia),
    ))
    guardarClave(identidad.establecimientoId, hashDispositivo, key, "cambiar_voto", huella, resultado)
    cambios.publicar(idSesion, "cambio_voto")
    return resultado
  }

  /** La solicitud bloqueada con su noche, para quitar un voto o retirarla (RF-060, RF-061). */
  private data class SolicitudBloqueada(val idSesion: Long, val fila: SolicitudParaCambio, val config: Config)

  private fun solicitudConNocheBloqueada(id: Long, idBar: Long): SolicitudBloqueada {
    val idSesion = jdbc.query(
      """SELECT s.id_sesion FROM public.solicitud s
               JOIN public.sesion se ON se.id_sesion = s.id_sesion
               WHERE s.id_solicitud = ? AND se.id_establecimiento = ?""",
      { rs, _ -> rs.getLong(1) }, id, idBar,
    ).firstOrNull() ?: error(HttpStatus.GONE, "no_pendiente", "Esa canción ya no está en la cola")
    val sesion = em.find(Sesion::class.java, idSesion, LockModeType.PESSIMISTIC_WRITE)
      ?: error(HttpStatus.GONE, "no_pendiente", "Esa canción ya no está en la cola")
    val config = comprobarServicio(sesion)
    val fila = jdbc.query(
      """SELECT s.id_solicitud, s.estado, s.id_dispositivo_solicitante,
                      c.id_cancion, c.disponibilidad, c.embebible, c.youtube_video_id
               FROM public.solicitud s JOIN public.cancion c ON c.id_cancion = s.id_cancion
               WHERE s.id_solicitud = ? AND s.id_sesion = ?""",
      { rs, _ -> SolicitudParaCambio(
        rs.getLong("id_solicitud"), rs.getString("estado"),
        rs.getLong("id_dispositivo_solicitante"), rs.getLong("id_cancion"),
        rs.getString("disponibilidad"), rs.getBoolean("embebible"),
        rs.getString("youtube_video_id"),
      ) }, id, idSesion,
    ).firstOrNull() ?: error(HttpStatus.GONE, "no_pendiente", "Esa canción ya no está en la cola")
    return SolicitudBloqueada(idSesion, fila, config)
  }

  /**
   * RF-060 (frontend, 2026-11-12, D14): quitar mi voto de una solicitud pendiente que pidió otro.
   * Idempotente: sin voto activo responde igual. El voto inicial de un pedido propio no se quita
   * (eso es retirar el pedido, RF-061).
   */
  @Transactional
  fun quitarVoto(slug: String, idExterno: String, uuid: String?, claveCabecera: String?): RespuestaGuardada {
    val idSolicitud = idPositivo(idExterno)
    val key = clave(claveCabecera)
    val identidad = dispositivos.identificar(slug, uuid)
    val hashDispositivo = dispositivos.huella(uuid)
    val huella = sha256("quitar_voto|$slug|$idSolicitud")
    bloquearClave(identidad.establecimientoId, hashDispositivo, key)
    repeticion(identidad.establecimientoId, hashDispositivo, key, "quitar_voto", huella)?.let { return it }
    comprobarAviso(identidad)

    val (idSesion, solicitud) = solicitudConNocheBloqueada(idSolicitud, identidad.establecimientoId)
    if (solicitud.estado != "pendiente") error(HttpStatus.GONE, "no_pendiente", "Esa canción ya no está en la cola")
    if (solicitud.solicitanteId == identidad.dispositivoId)
      error(HttpStatus.BAD_REQUEST, "datos_invalidos", "El voto de tu propio pedido no se quita: retira el pedido")
    val quitados = jdbc.update(
      """UPDATE public.voto SET activo = FALSE, fecha_retiro = CURRENT_TIMESTAMP
               WHERE id_solicitud = ? AND id_dispositivo = ? AND activo = TRUE""",
      idSolicitud, identidad.dispositivoId,
    )
    if (quitados > 0) {
      jdbc.update(
        """INSERT INTO public.evento_analitico
                 (nombre_evento, id_establecimiento, id_sesion, device_hash, youtube_video_id, origen_app, propiedades)
                 VALUES ('quitar_voto', ?, ?, ?, ?, 'cliente', CAST(? AS jsonb))""",
        identidad.establecimientoId, idSesion, hashDispositivo, solicitud.videoId,
        json.writeValueAsString(mapOf("solicitud" to idSolicitud.toString())),
      )
    }
    val resultado = respuesta(200, mapOf("solicitud_id" to idSolicitud.toString(), "votos_total" to totalVotos(idSolicitud)))
    guardarClave(identidad.establecimientoId, hashDispositivo, key, "quitar_voto", huella, resultado)
    if (quitados > 0) cambios.publicar(idSesion, "quitar_voto")
    return resultado
  }

  /**
   * RF-061 (frontend, 2026-11-12, D14): retirar mi pedido si todavía no suena. Pasa a `retirada`,
   * sus votos se desactivan y el teléfono recupera el cupo (RN-01 cuenta pendiente y sonando).
   * Solo quien la pidió; un id de otro teléfono es `sin_permiso`.
   */
  @Transactional
  fun retirarPedido(slug: String, idExterno: String, uuid: String?, claveCabecera: String?): RespuestaGuardada {
    val idSolicitud = idPositivo(idExterno)
    val key = clave(claveCabecera)
    val identidad = dispositivos.identificar(slug, uuid)
    val hashDispositivo = dispositivos.huella(uuid)
    val huella = sha256("retirar_pedido|$slug|$idSolicitud")
    bloquearClave(identidad.establecimientoId, hashDispositivo, key)
    repeticion(identidad.establecimientoId, hashDispositivo, key, "retirar_pedido", huella)?.let { return it }
    comprobarAviso(identidad)

    val (idSesion, solicitud, config) = solicitudConNocheBloqueada(idSolicitud, identidad.establecimientoId)
    if (solicitud.solicitanteId != identidad.dispositivoId)
      error(HttpStatus.FORBIDDEN, "sin_permiso", "Solo puedes retirar las canciones que pediste tú")
    // Idempotente (contrato): si ya la retiró este teléfono (retirada sin motivo), responde igual.
    val yaLaRetiraste = solicitud.estado == "retirada" && jdbc.queryForObject(
      "SELECT motivo_estado IS NULL FROM public.solicitud WHERE id_solicitud = ?", Boolean::class.javaObjectType, idSolicitud,
    ) == true
    if (!yaLaRetiraste) retirar(idSesion, idSolicitud, solicitud, identidad, hashDispositivo)
    val usados = jdbc.queryForObject(
      """SELECT COUNT(*) FROM public.solicitud
               WHERE id_sesion = ? AND id_dispositivo_solicitante = ? AND estado IN ('pendiente', 'reproduciendo')""",
      Long::class.javaObjectType, idSesion, identidad.dispositivoId,
    ) ?: 0L
    val resultado = respuesta(200, mapOf(
      "solicitud_id" to idSolicitud.toString(),
      "pedidos_restantes" to ContratoColaCalculos.cuposRestantes(config.maximo, usados),
    ))
    guardarClave(identidad.establecimientoId, hashDispositivo, key, "retirar_pedido", huella, resultado)
    if (!yaLaRetiraste) cambios.publicar(idSesion, "retirar_pedido")
    return resultado
  }

  private fun retirar(
    idSesion: Long, idSolicitud: Long, solicitud: SolicitudParaCambio,
    identidad: ContratoDispositivoService.IdentidadContrato, hashDispositivo: String,
  ) {
    if (solicitud.estado != "pendiente") error(HttpStatus.GONE, "no_pendiente", "Esa canción ya suena o ya salió de la cola")
    val retiradas = jdbc.update(
      "UPDATE public.solicitud SET estado = 'retirada' WHERE id_solicitud = ? AND estado = 'pendiente'", idSolicitud,
    )
    if (retiradas != 1) error(HttpStatus.CONFLICT, "conflicto", "La solicitud cambió; intenta nuevamente")
    jdbc.update(
      """UPDATE public.voto SET activo = FALSE, fecha_retiro = CURRENT_TIMESTAMP
               WHERE id_solicitud = ? AND activo = TRUE""", idSolicitud,
    )
    jdbc.update(
      """INSERT INTO public.evento_analitico
               (nombre_evento, id_establecimiento, id_sesion, device_hash, youtube_video_id, origen_app, propiedades)
               VALUES ('retirar_pedido', ?, ?, ?, ?, 'cliente', CAST(? AS jsonb))""",
      identidad.establecimientoId, idSesion, hashDispositivo, solicitud.videoId,
      json.writeValueAsString(mapOf("solicitud" to idSolicitud.toString())),
    )
  }

  private fun idPositivo(texto: String): Long = texto.toLongOrNull()?.takeIf { it > 0L }
    ?: error(HttpStatus.BAD_REQUEST, "datos_invalidos", "Los identificadores deben ser cadenas de enteros positivos")

  private fun comprobarAviso(i: ContratoDispositivoService.IdentidadContrato) {
    if (i.avisoVistoEn == null) error(HttpStatus.CONFLICT, "conflicto", "Falta registrar la visualización del aviso de datos")
  }
  private fun sesionAbiertaConBloqueo(idBar: Long): Sesion {
    val id = jdbc.query(
      """SELECT id_sesion FROM public.sesion WHERE id_establecimiento=? AND estado='abierta'
               ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1""",
      { rs, _ -> rs.getLong(1) }, idBar,
    ).firstOrNull() ?: error(HttpStatus.LOCKED, "servicio_no_disponible", "No hay sesión abierta")
    return em.find(Sesion::class.java, id, LockModeType.PESSIMISTIC_WRITE)
      ?: error(HttpStatus.LOCKED, "servicio_no_disponible", "La sesión ya no está disponible")
  }
  private fun comprobarServicio(sesion: Sesion): Config {
    val bar = sesion.establecimiento
    val idBar = requireNotNull(bar.id)
    if (sesion.estado != EstadoSesion.ABIERTA || !bar.activo)
      error(HttpStatus.LOCKED, "servicio_no_disponible", "El bar o la sesión están cerrados")
    val config = jdbc.query(
      """SELECT estado_manual,max_solicitudes_dispositivo,duracion_max_seg,minutos_bloqueo_repeticion
               FROM public.configuracion_servicio WHERE id_establecimiento=?""",
      { rs, _ -> Config(rs.getInt("max_solicitudes_dispositivo"),rs.getInt("duracion_max_seg"),
        rs.getInt("minutos_bloqueo_repeticion"),rs.getString("estado_manual")) }, idBar,
    ).firstOrNull() ?: error(HttpStatus.LOCKED, "servicio_no_disponible", "Servicio sin configuración")
    if (config.estado != "disponible")
      error(HttpStatus.LOCKED, "servicio_no_disponible", "El servicio está pausado o en mantenimiento")
    if (!horarios.estaEnHorario(idBar, bar.zonaHoraria))
      error(HttpStatus.LOCKED, "servicio_no_disponible", "Fuera del horario de operación")
    return config
  }
  private fun validarVideo(id: String): YoutubeVideoResponse = try {
    youtube.obtenerVideoPorId(id)
      ?: error(HttpStatus.UNPROCESSABLE_ENTITY, "no_disponible", "Video no disponible en YouTube")
  } catch (e: ResponseStatusException) {
    val cuota = e.statusCode.value() == 429 || e.reason?.contains("cuota", ignoreCase = true) == true
    error(HttpStatus.SERVICE_UNAVAILABLE, if (cuota) "cuota_agotada" else "youtube_no_disponible",
      "No fue posible validar el video con YouTube")
  }
  private fun guardarCancion(v: YoutubeVideoResponse): Long = jdbc.queryForObject(
    """INSERT INTO public.cancion
           (youtube_video_id,titulo,artista,canal,duracion_seg,miniatura_url,embebible,disponibilidad,ultima_validacion)
           VALUES (?,?,?,?,?,?,TRUE,'disponible',CURRENT_TIMESTAMP)
           ON CONFLICT(youtube_video_id) DO UPDATE SET
              titulo=EXCLUDED.titulo, artista=EXCLUDED.artista, canal=EXCLUDED.canal,
              duracion_seg=EXCLUDED.duracion_seg, miniatura_url=EXCLUDED.miniatura_url,
              embebible=TRUE, disponibilidad='disponible', ultima_validacion=CURRENT_TIMESTAMP
           RETURNING id_cancion""",
    Long::class.javaObjectType, v.youtubeVideoId, v.titulo.take(300), v.artista?.take(200),
    v.canal.take(200), v.duracionSeg, v.miniaturaUrl,
  ) ?: throw IllegalStateException("No se guardó la canción")
  private fun buscarSolicitud(idSesion: Long, idCancion: Long): Existente? = jdbc.query(
    """SELECT id_solicitud,estado FROM public.solicitud WHERE id_sesion=? AND id_cancion=?
           AND estado IN ('pendiente','reproduciendo') ORDER BY id_solicitud ASC LIMIT 1""",
    { rs, _ -> Existente(rs.getLong(1),rs.getString(2)) }, idSesion,idCancion,
  ).firstOrNull()
  private fun bloquearClave(idBar: Long, hash: String, key: UUID) {
    // El lock dura solo hasta commit/rollback; otra petición idéntica espera y lee el resultado guardado.
    jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", RowCallbackHandler { }, "$idBar|$hash|$key")
  }
  private fun repeticion(idBar: Long, hash: String, key: UUID, tipo: String, huella: String): RespuestaGuardada? {
    val r = jdbc.query(
      """SELECT operacion,huella_peticion,respuesta_http,respuesta_json
               FROM public.api_idempotencia WHERE id_establecimiento=? AND device_hash=? AND clave=?""",
      { rs, _ -> RegistroIdempotente(rs.getString(1),rs.getString(2),
        RespuestaGuardada(rs.getInt(3),rs.getString(4))) }, idBar,hash,key,
    ).firstOrNull() ?: return null
    if (r.operacion != tipo || r.huella != huella)
      error(HttpStatus.CONFLICT, "conflicto", "Idempotency-Key ya se utilizó para otra operación")
    return r.respuesta
  }
  private fun guardarClave(idBar: Long, hash: String, key: UUID, tipo: String, huella: String, r: RespuestaGuardada) {
    jdbc.update(
      """INSERT INTO public.api_idempotencia
               (id_establecimiento,device_hash,clave,operacion,huella_peticion,respuesta_http,respuesta_json)
               VALUES (?,?,?,?,?,?,?)""",
      idBar,hash,key,tipo,huella,r.estado,r.json,
    )
  }
  private fun comprobarVotoNuevo(idSolicitud: Long, idDispositivo: Long) {
    val previo = jdbc.queryForObject(
      """SELECT EXISTS(SELECT 1 FROM public.voto
               WHERE id_solicitud=? AND id_dispositivo=? AND activo=TRUE)""",
      Boolean::class.javaObjectType,idSolicitud,idDispositivo,
    ) ?: false
    if (previo) error(HttpStatus.CONFLICT, "ya_votaste", "Ya votaste por esta canción")
  }
  private fun insertarVoto(idSolicitud: Long, idDispositivo: Long) {
    jdbc.update(
      """INSERT INTO public.voto (id_solicitud,id_dispositivo,fecha_voto,activo)
               VALUES (?,?,CURRENT_TIMESTAMP,TRUE)""",idSolicitud,idDispositivo,
    )
  }
  private fun totalVotos(idSolicitud: Long): Long = jdbc.queryForObject(
    "SELECT COUNT(*) FROM public.voto WHERE id_solicitud=? AND activo=TRUE",
    Long::class.javaObjectType,idSolicitud,
  ) ?: 0L
  private fun bloqueada(idBar: Long, idCancion: Long) = jdbc.queryForObject(
    """SELECT EXISTS(SELECT 1 FROM public.bloqueo_contenido
           WHERE id_establecimiento=? AND id_cancion=? AND activo=TRUE
           AND fecha_inicio<=CURRENT_TIMESTAMP AND (fecha_fin IS NULL OR fecha_fin>CURRENT_TIMESTAMP))""",
    Boolean::class.javaObjectType,idBar,idCancion,
  ) ?: false
  private fun repetida(idBar: Long, idCancion: Long, minutos: Int): Boolean {
    if (minutos <= 0) return false
    return jdbc.queryForObject(
      """SELECT EXISTS(SELECT 1 FROM public.reproduccion r
               JOIN public.sesion s ON s.id_sesion=r.id_sesion
               WHERE s.id_establecimiento=? AND r.id_cancion=?
               AND r.fecha_inicio > CURRENT_TIMESTAMP - (? * INTERVAL '1 minute')
               AND r.resultado IN ('iniciada','completada'))""",
      Boolean::class.javaObjectType,idBar,idCancion,minutos,
    ) ?: false
  }
  private fun clave(raw: String?): UUID = try {
    if (raw == null || raw != UUID.fromString(raw).toString()) throw IllegalArgumentException("bad key")
    UUID.fromString(raw)
  } catch (_: IllegalArgumentException) {
    error(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requiere Idempotency-Key como UUID")
  }
  private fun respuesta(estado: Int, cuerpo: Map<String, Any?>): RespuestaGuardada =
    RespuestaGuardada(estado, json.writeValueAsString(cuerpo))
  private fun sha256(texto: String): String = HexFormat.of().formatHex(
    MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8)),
  )
  private fun error(estado: HttpStatus, codigo: String, mensaje: String): Nothing =
    throw ContratoApiException(estado,codigo,mensaje)
}
