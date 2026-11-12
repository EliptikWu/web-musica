package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.cola.ColaService
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionActualResponse
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionConsultaRepository
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/** Lee el estado del bar, sin introducir campos fuera de los esquemas Ahora/Destacadas. */
@Service
class ContratoEstadoService(
  private val jdbc: JdbcTemplate,
  private val cola: ColaService,
  private val reproducciones: ReproduccionConsultaRepository,
  private val versiones: ContratoVersionService,
  private val posiciones: ContratoPosicionesService,
) {
  private data class Bar(
    val id: Long,
    val zona: String,
    val modoActual: String?,
    /** RF-062 (migración V4); sin configuración, 80 como el contrato. */
    val volumen: Int,
  )
  private data class Franja(
    val dia: Int,
    val inicio: LocalTime,
    val fin: LocalTime,
  )
  private data class Ventana(val inicio: OffsetDateTime, val fin: OffsetDateTime)
  private data class DestacadaFila(
    val solicitudId: Long,
    val estado: String,
    val videoId: String,
    val titulo: String,
    val artista: String?,
    val canal: String?,
    val duracionSeg: Int,
    val miniaturaUrl: String?,
    val votos: Long,
    val fechaFin: OffsetDateTime?,
  )

  @Transactional(readOnly = true)
  fun ahora(slug: String): Map<String, Any?> {
    val bar = bar(slug)
    val idSesion = sesionAbierta(bar.id)
    val ahora = OffsetDateTime.now(ZoneId.of(bar.zona))
    val horario = ventanas(bar.id, ahora, ZoneId.of(bar.zona))
    val vigente = horario.firstOrNull { !ahora.isBefore(it.inicio) && ahora.isBefore(it.fin) }
    val proxima = horario.firstOrNull { it.inicio.isAfter(ahora) }

    val servicio = servicio(bar, idSesion, vigente, proxima)
    val reproduccionActual = idSesion?.let { reproducciones.actual(it, ahora) }
    val canciones = idSesion?.let { cola.obtener(it).canciones } ?: emptyList()
    // La espera se calcula en el MISMO instante que servidor_ahora: duración
    // restante de lo que está sonando + canciones pendientes anteriores.
    val esperas = ContratoColaCalculos.esperasSeg(
      canciones.map { it.duracionSeg }, reproduccionActual?.segundosRestantes ?: 0L,
    )
    val anteriores = if (idSesion != null) {
      posiciones.anteriores(idSesion, canciones.associate { it.solicitudId to it.posicion })
    } else emptyMap()

    val items = canciones.mapIndexed { indice, item ->
      mapOf(
        "solicitud_id" to item.solicitudId.toString(),
        "cancion" to cancion(item.youtubeVideoId, item.titulo, item.artista, null, item.duracionSeg, item.miniaturaUrl),
        "votos_total" to item.votos,
        "fecha_solicitud" to item.fechaSolicitud,
        "puesto" to item.posicion,
        "puesto_anterior" to anteriores[item.solicitudId],
        "espera_seg" to esperas[indice],
      )
    }
    return mapOf(
      "version" to versiones.actual(idSesion),
      "servidor_ahora" to ahora,
      "servicio" to servicio,
      "reproduccion" to reproduccionActual?.let { reproduccion(it) },
      "cola" to items,
      "lista_casa" to listaCasa(bar.id),
      // RF-062 (migración V4): lo fija el panel; la TV lo aplica al reproductor.
      "volumen" to bar.volumen,
    )
  }

  @Transactional(readOnly = true)
  fun destacadas(slug: String): Map<String, Any> {
    val bar = bar(slug)
    val idSesion = sesionAbierta(bar.id) ?: return mapOf("destacadas" to emptyList<Any>())
    val actualId = reproducciones.actual(idSesion)?.solicitudId

    // No se suman solicitudes y votos: la solicitud inicial YA genera el primer voto.
    val filas: List<DestacadaFila> = jdbc.query(
      """
            SELECT s.id_solicitud, s.estado, c.youtube_video_id, c.titulo, c.artista,
                   c.canal, c.duracion_seg, c.miniatura_url,
                   COALESCE(v.total, 0) AS votos,
                   (SELECT MAX(r.fecha_fin) FROM public.reproduccion r
                    WHERE r.id_solicitud = s.id_solicitud AND r.resultado = 'completada') AS fecha_fin
            FROM public.solicitud s
            JOIN public.cancion c ON c.id_cancion = s.id_cancion
            LEFT JOIN (
                SELECT id_solicitud, COUNT(*) AS total FROM public.voto
                WHERE activo = TRUE GROUP BY id_solicitud
            ) v ON v.id_solicitud = s.id_solicitud
            WHERE s.id_sesion = ?
              AND s.estado IN ('pendiente', 'reproduciendo', 'reproducida')
            ORDER BY COALESCE(v.total, 0) DESC, s.fecha_solicitud ASC, s.id_solicitud ASC
            """.trimIndent(),
      { rs, _ ->
        DestacadaFila(
          solicitudId = rs.getLong("id_solicitud"),
          estado = rs.getString("estado"),
          videoId = rs.getString("youtube_video_id"),
          titulo = rs.getString("titulo"),
          artista = rs.getString("artista"),
          canal = rs.getString("canal"),
          duracionSeg = rs.getInt("duracion_seg"),
          miniaturaUrl = rs.getString("miniatura_url"),
          votos = rs.getLong("votos"),
          fechaFin = rs.getObject("fecha_fin", OffsetDateTime::class.java),
        )
      }, idSesion,
    )
    return mapOf("destacadas" to filas
      .filter { it.estado != "reproduciendo" || it.solicitudId == actualId }
      // Las tres más votadas, como el prototipo: con todas, la bienvenida crecía con la noche y
      // había que deslizar para llegar a «Entrar y votar» (corrección del 2026-11-12).
      .take(3)
      .map { fila ->
        // El contrato solo tiene tres contextos. No se presentan solicitudes descartadas.
        val contexto = when {
          fila.estado == "reproducida" -> "sono"
          fila.estado == "reproduciendo" && fila.solicitudId == actualId -> "sonando"
          else -> "en_cola"
        }
        mapOf(
          "cancion" to cancion(fila.videoId, fila.titulo, fila.artista, fila.canal, fila.duracionSeg, fila.miniaturaUrl),
          "votos" to fila.votos,
          "contexto" to contexto,
          "hora" to (if (contexto == "sono") fila.fechaFin else null),
        )
      })
  }

  private fun bar(slug: String): Bar = jdbc.query(
    """
        SELECT e.id_establecimiento, e.zona_horaria, c.estado_manual, COALESCE(c.volumen, 80) AS volumen
        FROM public.establecimiento e
        LEFT JOIN public.configuracion_servicio c ON c.id_establecimiento = e.id_establecimiento
        WHERE e.slug = ? AND e.activo = TRUE
        """.trimIndent(),
    { rs, _ -> Bar(
      rs.getLong("id_establecimiento"),
      rs.getString("zona_horaria"),
      rs.getString("estado_manual"),
      rs.getInt("volumen"),
    ) }, slug,
  ).firstOrNull()?.also {
    if (it.modoActual == null) throw ContratoApiException(
      HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración de servicio",
    )
  } ?: throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Bar no encontrado")

  private fun sesionAbierta(idBar: Long): Long? = jdbc.query(
    """SELECT id_sesion FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta'
           ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1""",
    { rs, _ -> rs.getLong("id_sesion") }, idBar,
  ).firstOrNull()

  /** Una franja nocturna termina en el día siguiente, según la zona horaria real del bar. */
  private fun ventanas(idBar: Long, ahora: OffsetDateTime, zona: ZoneId): List<Ventana> {
    val franjas: List<Franja> = jdbc.query(
      """SELECT dia_semana, hora_inicio, hora_fin FROM public.horario_operacion
               WHERE id_establecimiento = ? AND activo = TRUE""",
      { rs, _ -> Franja(
        rs.getInt("dia_semana"),
        rs.getObject("hora_inicio", LocalTime::class.java),
        rs.getObject("hora_fin", LocalTime::class.java),
      ) }, idBar,
    )
    val hoy = ahora.toLocalDate()
    return (-1L..8L).flatMap { dias ->
      val fecha = hoy.plusDays(dias)
      franjas.asSequence().filter { it.dia == fecha.dayOfWeek.value && it.inicio != it.fin }
        .map { franja ->
          val diaFin: LocalDate = if (franja.fin.isAfter(franja.inicio)) fecha else fecha.plusDays(1)
          Ventana(
            fecha.atTime(franja.inicio).atZone(zona).toOffsetDateTime(),
            diaFin.atTime(franja.fin).atZone(zona).toOffsetDateTime(),
          )
        }.toList()
    }.sortedBy { it.inicio }
  }

  private fun servicio(
    bar: Bar, idSesion: Long?,
    vigente: Ventana?, proxima: Ventana?,
  ): Map<String, Any?> {
    val modo = bar.modoActual
    val estado = when {
      modo == "mantenimiento" -> "mantenimiento"
      modo == "pausado" -> "en_pausa"
      idSesion == null -> "en_pausa" // Sin noche abierta no se pueden aceptar pedidos.
      vigente == null -> "fuera_de_horario"
      else -> "abierto"
    }
    val modoQr = if (modo == "pausado") "pausado" else "automatico"
    return mapOf(
      "estado" to estado,
      "modo_qr" to modoQr,
      "desde" to if (estado == "abierto") vigente?.inicio else null,
      "hasta" to if (estado == "abierto") vigente?.fin else null,
      "proxima_apertura" to if (estado == "fuera_de_horario") proxima?.inicio else null,
    )
  }

  private fun listaCasa(idBar: Long): Map<String, Any>? = jdbc.query(
    """
        SELECT l.nombre, COUNT(el.id_elemento)::integer AS total_canciones
        FROM public.lista_defecto l
        LEFT JOIN public.elemento_lista el ON el.id_lista = l.id_lista
        WHERE l.id_establecimiento = ? AND l.activa = TRUE
        GROUP BY l.id_lista, l.nombre ORDER BY l.id_lista DESC LIMIT 1
        """.trimIndent(),
    { rs, _ -> mapOf(
      "nombre" to rs.getString("nombre"),
      "total_canciones" to rs.getInt("total_canciones"),
    ) }, idBar,
  ).firstOrNull()

  private fun reproduccion(actual: ReproduccionActualResponse): Map<String, Any?> {
    val inicio = actual.fechaInicio ?: throw ContratoApiException(
      HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "La reproducción no tiene fecha de inicio",
    )
    val votos = actual.solicitudId?.let { id -> jdbc.queryForObject(
      "SELECT COUNT(*) FROM public.voto WHERE id_solicitud = ? AND activo = TRUE",
      Long::class.javaObjectType, id,
    ) ?: 0L }
    return mapOf(
      "id" to actual.reproduccionId.toString(),
      "cancion" to cancion(actual.youtubeVideoId, actual.titulo, actual.artista, null, actual.duracionSeg, actual.miniaturaUrl),
      "origen" to if (actual.origen == "solicitud") "solicitud" else "lista_casa",
      "solicitud_id" to actual.solicitudId?.toString(),
      "votos" to votos,
      "inicio" to inicio,
      "posicion_seg" to actual.segundosTranscurridos,
      // RF-040: con la pausa del panel la TV detiene el video y la barra de avance se queda quieta.
      "estado" to if (actual.pausada) "pausada" else "sonando",
    )
  }

  private fun cancion(
    videoId: String, titulo: String, artista: String?, canal: String?,
    duracionSeg: Int, miniaturaUrl: String?,
  ): Map<String, Any?> = mapOf(
    "video_id" to videoId,
    "titulo" to titulo,
    "artista" to (artista?.takeIf(String::isNotBlank) ?: canal.orEmpty()),
    "duracion_seg" to duracionSeg.coerceAtLeast(0),
    "miniatura_url" to miniaturaUrl?.takeIf(String::isNotBlank),
  )
}
