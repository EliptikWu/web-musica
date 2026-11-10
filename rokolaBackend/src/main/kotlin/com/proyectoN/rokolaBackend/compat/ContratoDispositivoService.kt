package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.HexFormat
import java.util.UUID

/** Adaptación de identificación UUID del contrato; NO toca el flujo QR antiguo. */
@Service
class ContratoDispositivoService(private val jdbc: JdbcTemplate) {

  private data class Bar(val id: Long, val maximo: Int)
  private data class Dispositivo(val id: Long, val aviso: OffsetDateTime?)
  private data class Sesion(val id: Long)

  /** Identidad resuelta para los futuros POST contractuales (solicitudes y votos). */
  data class IdentidadContrato(
    val establecimientoId: Long,
    val dispositivoId: Long,
    val avisoVistoEn: OffsetDateTime?,
    val pedidosMaximos: Int,
  )

  private val formatoUuid = Regex(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
  )

  /** El UUID no se guarda ni se devuelve, y no se registra en logs. */
  fun huella(uuidCabecera: String?): String {
    if (uuidCabecera == null || !formatoUuid.matches(uuidCabecera)) {
      throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requiere X-Rockola-Dispositivo como UUID")
    }
    val canonico = try {
      UUID.fromString(uuidCabecera).toString()
    } catch (_: IllegalArgumentException) {
      throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "UUID de dispositivo incorrecto")
    }
    return HexFormat.of().formatHex(
      MessageDigest.getInstance("SHA-256").digest(canonico.toByteArray(Charsets.UTF_8)),
    )
  }

  /** Comprueba slug activo y configuración. Se usa id solo internamente. */
  private fun bar(slug: String): Bar = jdbc.query(
    """
        SELECT e.id_establecimiento, c.max_solicitudes_dispositivo
        FROM public.establecimiento e
        JOIN public.configuracion_servicio c ON c.id_establecimiento = e.id_establecimiento
        WHERE e.slug = ? AND e.activo = TRUE
        """.trimIndent(),
    { rs, _ -> Bar(rs.getLong("id_establecimiento"), rs.getInt("max_solicitudes_dispositivo")) },
    slug,
  ).firstOrNull() ?: throw ContratoApiException(
    HttpStatus.NOT_FOUND, "no_encontrado", "Establecimiento no encontrado",
  )

  private fun dispositivo(idBar: Long, hash: String): Dispositivo? = jdbc.query(
    """
        SELECT id_dispositivo, aviso_privacidad_fecha FROM public.dispositivo
        WHERE id_establecimiento = ? AND device_hash = ?
        """.trimIndent(),
    { rs, _ -> Dispositivo(
      rs.getLong("id_dispositivo"),
      rs.getObject("aviso_privacidad_fecha", OffsetDateTime::class.java),
    ) },
    idBar, hash,
  ).firstOrNull()

  private fun sesionActual(idBar: Long): Sesion? = jdbc.query(
    """
        SELECT id_sesion FROM public.sesion
        WHERE id_establecimiento = ? AND estado = 'abierta'
        ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1
        """.trimIndent(),
    { rs, _ -> Sesion(rs.getLong("id_sesion")) },
    idBar,
  ).firstOrNull()

  /**
   * Lee únicamente dispositivos registrados mediante UUID contractual.
   * No convierte ni asume que una huella HMAC del QR antiguo representa el mismo teléfono.
   */
  @Transactional(readOnly = true)
  fun identificar(slug: String, uuid: String?): IdentidadContrato {
    val hash = huella(uuid)
    val bar = bar(slug)
    val disp = dispositivo(bar.id, hash) ?: throw ContratoApiException(
      HttpStatus.NOT_FOUND, "no_encontrado", "Dispositivo no registrado en el establecimiento",
    )
    return IdentidadContrato(
      establecimientoId = bar.id,
      dispositivoId = disp.id,
      avisoVistoEn = disp.aviso,
      pedidosMaximos = bar.maximo,
    )
  }

  @Transactional
  fun registrar(slug: String, uuid: String?, tipo: String?): Map<String, Any?> {
    val hash = huella(uuid)
    val bar = bar(slug)
    if (tipo !in setOf("movil", "tableta", "otro")) {
      throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "tipo debe ser movil, tableta u otro")
    }
    // Nunca se pone fecha de consentimiento por registrarse: /aviso es una operación aparte.
    jdbc.update(
      """
            INSERT INTO public.dispositivo
                (id_establecimiento, device_hash, tipo, primer_acceso, ultimo_acceso, aviso_privacidad_fecha)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
            ON CONFLICT (id_establecimiento, device_hash)
            DO UPDATE SET tipo = EXCLUDED.tipo, ultimo_acceso = CURRENT_TIMESTAMP
            """.trimIndent(),
      bar.id, hash, tipo,
    )
    return construirYo(bar, requireNotNull(dispositivo(bar.id, hash)))
  }

  @Transactional(readOnly = true)
  fun yo(slug: String, uuid: String?): Map<String, Any?> {
    val hash = huella(uuid)
    val bar = bar(slug)
    val registro = dispositivo(bar.id, hash) ?: throw ContratoApiException(
      HttpStatus.NOT_FOUND, "no_encontrado", "Dispositivo no registrado en el establecimiento",
    )
    return construirYo(bar, registro)
  }

  @Transactional
  fun aviso(slug: String, uuid: String?) {
    val hash = huella(uuid)
    val bar = bar(slug)
    val filas = jdbc.update(
      """
            UPDATE public.dispositivo SET aviso_privacidad_fecha = COALESCE(aviso_privacidad_fecha, CURRENT_TIMESTAMP),
                                          ultimo_acceso = CURRENT_TIMESTAMP
            WHERE id_establecimiento = ? AND device_hash = ?
            """.trimIndent(),
      bar.id, hash,
    )
    if (filas == 0) {
      throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Dispositivo no registrado")
    }
  }

  private fun construirYo(bar: Bar, disp: Dispositivo): Map<String, Any?> {
    val sesion = sesionActual(bar.id)
    if (sesion == null) {
      return mapOf(
        "pedidos_maximos" to bar.maximo,
        "pedidos_restantes" to bar.maximo.coerceAtLeast(0),
        "solicitudes" to emptyList<Any>(),
        "votos" to emptyList<String>(),
        "aviso_visto_en" to disp.aviso,
      )
    }

    val posiciones: Map<Long, Int> = jdbc.query(
      """
            SELECT s.id_solicitud,
                   ROW_NUMBER() OVER (ORDER BY COALESCE(v.total, 0) DESC, s.fecha_solicitud, s.id_solicitud) AS puesto
            FROM public.solicitud s
            LEFT JOIN (
                SELECT id_solicitud, COUNT(*) AS total FROM public.voto WHERE activo = TRUE GROUP BY id_solicitud
            ) v ON v.id_solicitud = s.id_solicitud
            WHERE s.id_sesion = ? AND s.estado = 'pendiente'
            """.trimIndent(),
      { rs, _ -> rs.getLong("id_solicitud") to rs.getInt("puesto") },
      sesion.id,
    ).toMap()

    val solicitudes: List<Map<String, Any?>> = jdbc.query(
      """
            SELECT s.id_solicitud, s.estado, s.fecha_solicitud, s.motivo_estado,
                   c.youtube_video_id, c.titulo, c.artista, c.canal, c.duracion_seg, c.miniatura_url,
                   (SELECT COUNT(*) FROM public.voto v WHERE v.id_solicitud = s.id_solicitud AND v.activo = TRUE) AS votos
            FROM public.solicitud s
            JOIN public.cancion c ON c.id_cancion = s.id_cancion
            WHERE s.id_sesion = ? AND s.id_dispositivo_solicitante = ?
            ORDER BY s.fecha_solicitud DESC, s.id_solicitud DESC
            """.trimIndent(),
      { rs, _ ->
        val id = rs.getLong("id_solicitud")
        mapOf(
          "solicitud_id" to id.toString(),
          "cancion" to mapOf(
            "video_id" to rs.getString("youtube_video_id"),
            "titulo" to rs.getString("titulo"),
            "artista" to (rs.getString("artista") ?: rs.getString("canal") ?: ""),
            "duracion_seg" to rs.getInt("duracion_seg"),
            "miniatura_url" to rs.getString("miniatura_url"),
          ),
          "estado" to rs.getString("estado"),
          "fecha_solicitud" to rs.getObject("fecha_solicitud", OffsetDateTime::class.java),
          "votos_total" to rs.getLong("votos"),
          "puesto" to posiciones[id],
          "motivo" to rs.getString("motivo_estado"),
        )
      },
      sesion.id, disp.id,
    )

    val votos: List<String> = jdbc.query(
      """
            SELECT v.id_solicitud
            FROM public.voto v
            JOIN public.solicitud s ON s.id_solicitud = v.id_solicitud
            WHERE s.id_sesion = ? AND v.id_dispositivo = ? AND v.activo = TRUE
            ORDER BY v.fecha_voto, v.id_voto
            """.trimIndent(),
      { rs, _ -> rs.getLong("id_solicitud").toString() },
      sesion.id, disp.id,
    )
    // RN-01 acordada con el frontend: PENDIENTES + REPRODUCIENDO ocupan cupo.
    val ocupados = ContratoColaCalculos.cuposOcupados(jdbc, sesion.id, disp.id)
    return mapOf(
      "pedidos_maximos" to bar.maximo,
      "pedidos_restantes" to ContratoColaCalculos.cuposRestantes(bar.maximo, ocupados),
      "solicitudes" to solicitudes,
      "votos" to votos,
      "aviso_visto_en" to disp.aviso,
    )
  }
}
