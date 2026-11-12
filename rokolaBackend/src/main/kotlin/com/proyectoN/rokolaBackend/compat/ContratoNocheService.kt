package com.proyectoN.rokolaBackend.compat

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * `GET /panel/noche` (RF-037, RF-044, RF-049): la foto pública `ahora` más lo que solo ve el
 * personal. Lo que el modelo de datos aún no guarda sale vacío o null, nunca inventado.
 */
@Service
class ContratoNocheService(
  private val jdbc: JdbcTemplate,
  private val estados: ContratoEstadoService,
) {
  private data class SesionFila(val id: Long, val apertura: OffsetDateTime, val tipoEvento: String?)
  private data class Pedido(val hash: String, val fecha: OffsetDateTime)

  private val tiposAccion = setOf(
    "saltar", "bloquear", "desbloquear", "extender_bloqueo", "retirar",
    "pausar_solicitudes", "reanudar_solicitudes", "forzar_apertura",
    "pausar_reproduccion", "reanudar_reproduccion", "cambiar_volumen",
    "mantenimiento", "abrir_sesion", "cerrar_sesion",
    "editar_horario", "editar_lista", "activar_lista",
    "crear_usuario", "editar_usuario", "desactivar_usuario", "reactivar_usuario", "restablecer_clave",
    "editar_establecimiento", "regenerar_qr", "editar_evento", "editar_umbrales",
    "vincular_pantalla", "desvincular_pantalla",
    "error_youtube", "intento_no_permitido",
  )

  @Suppress("UNCHECKED_CAST")
  @Transactional(readOnly = true)
  fun noche(p: ContratoPanelAcceso.Personal): Map<String, Any?> {
    val ahora = estados.ahora(p.barSlug)
    val sesion = sesionAbierta(p.barId)
    val pedidos = sesion?.let { pedidos(it.id) } ?: emptyMap()

    val reproduccion = (ahora["reproduccion"] as Map<String, Any?>?)?.let { r ->
      val pedido = (r["solicitud_id"] as String?)?.toLongOrNull()?.let(pedidos::get)
      r + mapOf("pedida_por" to pedido?.let { codigoTelefono(it.hash) }, "pedida_en" to pedido?.fecha)
    }
    val cola = (ahora["cola"] as List<Map<String, Any?>>).map { item ->
      val pedido = pedidos[(item["solicitud_id"] as String).toLong()]
      val cancion = item["cancion"] as Map<String, Any?>
      item + mapOf(
        "pedida_por" to (pedido?.let { codigoTelefono(it.hash) } ?: "----"),
        "duracion_seg" to cancion["duracion_seg"],
      )
    }
    val servicio = ahora["servicio"] as Map<String, Any?>
    val lista = listaActiva(p.barId)

    val alertas = buildList {
      if (lista == null) add(mapOf("tipo" to "sin_lista_activa", "detalle" to "Si la cola se vacía, no sonará nada."))
      if (sesion == null) add(mapOf("tipo" to "sin_sesion", "detalle" to null))
      val errores = erroresYoutube(p.barId)
      if (errores > 0) add(mapOf("tipo" to "errores_youtube", "detalle" to "$errores en la última media hora"))
    }

    return mapOf(
      "version" to ahora["version"],
      "servidor_ahora" to ahora["servidor_ahora"],
      "sesion" to sesion?.let {
        // El modelo no distingue cómo se abrió la noche: hoy solo se abre a mano.
        mapOf("id" to it.id.toString(), "apertura" to it.apertura, "tipo_evento" to (it.tipoEvento ?: ""), "origen" to "manual")
      },
      "servicio" to servicio,
      "reproduccion" to reproduccion,
      "cola" to cola,
      "volumen" to ahora["volumen"],
      "metricas" to metricas(sesion),
      "registro" to registro(p.barId, sesion?.apertura),
      "horario" to horario(p.barId),
      "lista_casa" to lista,
      "bloqueos" to bloqueos(p.barId),
      "alertas" to alertas,
    )
  }

  private fun sesionAbierta(idBar: Long): SesionFila? = jdbc.query(
    """SELECT id_sesion, fecha_apertura, tipo_evento FROM public.sesion
       WHERE id_establecimiento = ? AND estado = 'abierta'
       ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1""",
    { rs, _ -> SesionFila(rs.getLong(1), rs.getObject(2, OffsetDateTime::class.java), rs.getString(3)) }, idBar,
  ).firstOrNull()

  private fun pedidos(idSesion: Long): Map<Long, Pedido> = jdbc.query(
    """SELECT s.id_solicitud, d.device_hash, s.fecha_solicitud
       FROM public.solicitud s JOIN public.dispositivo d ON d.id_dispositivo = s.id_dispositivo_solicitante
       WHERE s.id_sesion = ? AND s.estado IN ('pendiente', 'reproduciendo')""",
    { rs, _ -> rs.getLong(1) to Pedido(rs.getString(2), rs.getObject(3, OffsetDateTime::class.java)) }, idSesion,
  ).toMap()

  private fun metricas(sesion: SesionFila?): Map<String, Any?> {
    if (sesion == null) {
      return mapOf("desde" to null, "solicitudes" to 0, "telefonos" to 0, "votos" to 0, "espera_promedio_seg" to null)
    }
    return jdbc.query(
      """SELECT
           (SELECT COUNT(*) FROM public.solicitud WHERE id_sesion = ?) AS solicitudes,
           (SELECT COUNT(DISTINCT x.id) FROM (
              SELECT id_dispositivo_solicitante AS id FROM public.solicitud WHERE id_sesion = ?
              UNION SELECT v.id_dispositivo FROM public.voto v JOIN public.solicitud s ON s.id_solicitud = v.id_solicitud
                    WHERE s.id_sesion = ?) x) AS telefonos,
           (SELECT COUNT(*) FROM public.voto v JOIN public.solicitud s ON s.id_solicitud = v.id_solicitud
              WHERE s.id_sesion = ? AND v.activo = TRUE) AS votos,
           (SELECT ROUND(AVG(EXTRACT(EPOCH FROM (r.fecha_inicio - s.fecha_solicitud))))
              FROM public.reproduccion r JOIN public.solicitud s ON s.id_solicitud = r.id_solicitud
              WHERE r.id_sesion = ? AND r.fecha_inicio IS NOT NULL) AS espera""",
      { rs, _ ->
        mapOf(
          "desde" to sesion.apertura,
          "solicitudes" to rs.getInt("solicitudes"),
          "telefonos" to rs.getInt("telefonos"),
          "votos" to rs.getInt("votos"),
          "espera_promedio_seg" to rs.getObject("espera")?.let { (it as Number).toInt().coerceAtLeast(0) },
        )
      },
      sesion.id, sesion.id, sesion.id, sesion.id, sesion.id,
    ).first()
  }

  /** Lo de esta noche (o de las últimas 12 h sin noche abierta), lo más reciente primero. */
  private fun registro(idBar: Long, desde: OffsetDateTime?): List<Map<String, Any?>> = jdbc.query(
    // Solo los tipos que el contrato conoce, filtrados antes del LIMIT para no mostrar menos de 20.
    """SELECT a.id_accion, a.fecha, a.tipo, a.objetivo_tipo, a.detalle->>'texto' AS texto,
              u.id_usuario, u.nombre AS usuario, c.titulo, c.artista, c.canal
       FROM public.accion_admin a
       LEFT JOIN public.usuario u ON u.id_usuario = a.id_usuario
       LEFT JOIN public.cancion c ON a.objetivo_tipo = 'cancion' AND c.id_cancion = a.objetivo_id
       WHERE a.id_establecimiento = ? AND a.fecha >= ? AND a.tipo IN (${tiposAccion.joinToString { "?" }})
       ORDER BY a.fecha DESC, a.id_accion DESC LIMIT 20""",
    { rs, _ ->
      val tipo = rs.getString("tipo")
      val idUsuario = rs.getObject("id_usuario")?.let { (it as Number).toLong() }
      mapOf(
        "id" to rs.getLong("id_accion").toString(),
        "fecha" to rs.getObject("fecha", OffsetDateTime::class.java),
        "usuario" to idUsuario?.let { mapOf("id" to it.toString(), "nombre" to rs.getString("usuario")) },
        "tipo" to tipo,
        "objetivo" to rs.getString("titulo")?.let { titulo ->
          mapOf(
            "tipo" to "cancion",
            "nombre" to titulo,
            "detalle" to (rs.getString("artista")?.takeIf(String::isNotBlank) ?: rs.getString("canal")),
          )
        },
        "detalle" to rs.getString("texto"),
      )
    },
    idBar, desde ?: OffsetDateTime.now().minusHours(12), *tiposAccion.toTypedArray(),
  )

  private fun horario(idBar: Long): List<Map<String, Any>> {
    val formato = DateTimeFormatter.ofPattern("HH:mm")
    return jdbc.query(
      """SELECT dia_semana, hora_inicio, hora_fin, activo FROM public.horario_operacion
         WHERE id_establecimiento = ? ORDER BY dia_semana, hora_inicio""",
      { rs, _ ->
        mapOf(
          "dia_semana" to rs.getInt(1),
          "hora_inicio" to rs.getObject(2, LocalTime::class.java).format(formato),
          "hora_fin" to rs.getObject(3, LocalTime::class.java).format(formato),
          "activo" to rs.getBoolean(4),
        )
      },
      idBar,
    )
  }

  private fun listaActiva(idBar: Long): Map<String, Any>? = jdbc.query(
    """SELECT l.id_lista, l.nombre, l.modo_rotacion, COUNT(el.id_elemento)::integer AS total
       FROM public.lista_defecto l LEFT JOIN public.elemento_lista el ON el.id_lista = l.id_lista
       WHERE l.id_establecimiento = ? AND l.activa = TRUE
       GROUP BY l.id_lista, l.nombre, l.modo_rotacion ORDER BY l.id_lista DESC LIMIT 1""",
    { rs, _ ->
      mapOf(
        "id" to rs.getLong(1).toString(),
        "nombre" to rs.getString(2),
        // La base dice «aleatorio»; el contrato, «aleatoria».
        "total_canciones" to rs.getInt(4),
        "modo_rotacion" to if (rs.getString(3).startsWith("aleatori")) "aleatoria" else "secuencial",
      )
    },
    idBar,
  ).firstOrNull()

  /** Bloqueos vigentes. El modelo no guarda «hasta el fin de la noche»: sale como horas o indefinido. */
  private fun bloqueos(idBar: Long): List<Map<String, Any?>> = jdbc.query(
    """SELECT b.id_bloqueo, b.motivo, b.fecha_inicio, b.fecha_fin, u.nombre AS creador,
              c.youtube_video_id, c.titulo, c.artista, c.canal, c.duracion_seg, c.miniatura_url
       FROM public.bloqueo_contenido b
       JOIN public.cancion c ON c.id_cancion = b.id_cancion
       LEFT JOIN public.usuario u ON u.id_usuario = b.id_usuario
       WHERE b.id_establecimiento = ? AND b.activo = TRUE AND b.fecha_inicio <= CURRENT_TIMESTAMP
         AND (b.fecha_fin IS NULL OR b.fecha_fin > CURRENT_TIMESTAMP)
       ORDER BY b.fecha_inicio DESC""",
    { rs, _ ->
      val inicio = rs.getObject("fecha_inicio", OffsetDateTime::class.java)
      val fin = rs.getObject("fecha_fin", OffsetDateTime::class.java)
      val horas = fin?.let { Duration.between(inicio, it).toMinutes().let { m -> ((m + 59) / 60).toInt().coerceIn(1, 168) } }
      mapOf(
        "id" to rs.getLong("id_bloqueo").toString(),
        "cancion" to mapOf(
          "video_id" to rs.getString("youtube_video_id"),
          "titulo" to rs.getString("titulo"),
          "artista" to (rs.getString("artista")?.takeIf(String::isNotBlank) ?: rs.getString("canal").orEmpty()),
          "duracion_seg" to rs.getInt("duracion_seg").coerceAtLeast(0),
          "miniatura_url" to rs.getString("miniatura_url")?.takeIf(String::isNotBlank),
        ),
        "motivo" to rs.getString("motivo").orEmpty(),
        "duracion" to if (fin == null) "indefinido" else "horas",
        "horas" to horas,
        "inicio" to inicio,
        "fin" to fin,
        "creado_por" to (rs.getString("creador") ?: "Sistema"),
        "vigente" to true,
      )
    },
    idBar,
  )

  private fun erroresYoutube(idBar: Long): Int = jdbc.queryForObject(
    """SELECT COUNT(*) FROM public.error_integracion
       WHERE id_establecimiento = ? AND proveedor = 'YouTube' AND fecha_hora > CURRENT_TIMESTAMP - INTERVAL '30 minutes'""",
    Int::class.javaObjectType, idBar,
  ) ?: 0

  companion object {
    private const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Código corto del teléfono (p. ej. 7F3K) derivado de su huella SHA-256: identifica el
     * pedido en el panel sin mostrar el identificador (RNF-007, Ley 1581).
     */
    fun codigoTelefono(huellaHex: String): String {
      val bits = huellaHex.take(5).toLong(16) // 20 bits = 4 letras de 5 bits
      return (0 until 4).joinToString("") { i -> ALFABETO[((bits shr (i * 5)) and 31).toInt()].toString() }
    }
  }
}
