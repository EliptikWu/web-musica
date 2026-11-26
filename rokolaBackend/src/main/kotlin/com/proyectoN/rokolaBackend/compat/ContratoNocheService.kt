package com.proyectoN.rokolaBackend.compat

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
  private val bitacora: ContratoBitacora,
) {
  private data class SesionFila(val id: Long, val apertura: OffsetDateTime, val tipoEvento: String?, val origen: String)
  private data class Pedido(val hash: String, val fecha: OffsetDateTime)

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
      if (errores > 0) add(mapOf("tipo" to "errores_youtube", "detalle" to detalleErroresYoutube(titulosConError(p.barId), errores)))
    }

    return mapOf(
      "version" to ahora["version"],
      "servidor_ahora" to ahora["servidor_ahora"],
      "sesion" to sesion?.let {
        mapOf("id" to it.id.toString(), "apertura" to it.apertura, "tipo_evento" to (it.tipoEvento ?: ""), "origen" to it.origen)
      },
      "servicio" to servicio,
      "reproduccion" to reproduccion,
      "cola" to cola,
      "volumen" to ahora["volumen"],
      "metricas" to metricas(sesion),
      "registro" to bitacora.recientes(p.barId, sesion?.apertura),
      "horario" to horario(p.barId),
      "lista_casa" to lista,
      "bloqueos" to bloqueos(p.barId),
      "alertas" to alertas,
    )
  }

  private fun sesionAbierta(idBar: Long): SesionFila? = jdbc.query(
    """SELECT id_sesion, fecha_apertura, tipo_evento, origen FROM public.sesion
       WHERE id_establecimiento = ? AND estado = 'abierta'
       ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1""",
    { rs, _ -> SesionFila(rs.getLong(1), rs.getObject(2, OffsetDateTime::class.java), rs.getString(3), rs.getString("origen")) }, idBar,
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

  /** Bloqueos vigentes, con el mismo formato que `GET /panel/bloqueos`. */
  private fun bloqueos(idBar: Long): List<Map<String, Any?>> = jdbc.query(
    "${ContratoBloqueosService.SELECT} WHERE b.id_establecimiento = ? AND ${ContratoBloqueosService.VIGENTE} ORDER BY b.fecha_inicio DESC",
    { rs, _ -> ContratoBloqueosService.fila(rs) },
    idBar,
  )

  private fun erroresYoutube(idBar: Long): Int = jdbc.queryForObject(
    """SELECT COUNT(*) FROM public.error_integracion
       WHERE id_establecimiento = ? AND proveedor = 'YouTube' AND fecha_hora > CURRENT_TIMESTAMP - INTERVAL '5 minutes'""",
    Int::class.javaObjectType, idBar,
  ) ?: 0

  /** Las canciones que no pudo poner YouTube en los últimos 5 minutos, la más reciente primero. */
  private fun titulosConError(idBar: Long): List<String> = jdbc.query(
    """SELECT c.titulo FROM public.accion_admin a
       JOIN public.cancion c ON a.objetivo_tipo = 'cancion' AND c.id_cancion = a.objetivo_id
       WHERE a.id_establecimiento = ? AND a.tipo = 'error_youtube' AND a.fecha > CURRENT_TIMESTAMP - INTERVAL '5 minutes'
       ORDER BY a.fecha DESC, a.id_accion DESC""",
    { rs, _ -> rs.getString(1) }, idBar,
  )

  companion object {
    /**
     * El aviso del panel nombra hasta tres canciones y cuenta el resto (RF-019). [total] sale de
     * `error_integracion`, que también tiene los errores de antes de que se anotara la canción.
     * Igual que `detalleErroresYoutube` de la simulación de la web.
     */
    fun detalleErroresYoutube(titulos: List<String>, total: Int): String {
      val n = maxOf(total, titulos.size)
      val cuenta = "($n en los últimos 5 minutos)"
      if (titulos.isEmpty()) return "algunas canciones $cuenta"
      val nombrados = titulos.take(3)
      val resto = n - nombrados.size
      return nombrados.joinToString(", ") { "«$it»" } + (if (resto > 0) " y $resto más" else "") + " $cuenta"
    }

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
