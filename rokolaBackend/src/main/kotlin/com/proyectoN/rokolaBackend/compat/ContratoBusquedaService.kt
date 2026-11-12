package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.youtube.YoutubeService
import com.proyectoN.rokolaBackend.youtube.YoutubeVideoResponse
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.util.Locale

/** Adaptador a Busqueda / ResultadoBusqueda / ItemCola del OpenAPI. */
/**
 * Si una canción de la cola coincide con lo buscado (título o artista, sin mayúsculas). Una sola
 * regla para decidir si hace falta la foto de la cola y para filtrarla.
 */
internal fun coincide(cancion: Pair<String, String?>, consulta: String): Boolean =
  cancion.first.contains(consulta, ignoreCase = true) || cancion.second?.contains(consulta, ignoreCase = true) == true

@Service
class ContratoBusquedaService(
  private val jdbc: JdbcTemplate,
  private val youtube: YoutubeService,
  private val json: JsonMapper,
  private val estados: ContratoEstadoService,
  private val dispositivos: ContratoDispositivoService,
) {
  private data class Bar(val id: Long, val maxSeg: Int, val noRepeticionMin: Int)
  private data class CancionDb(val id: Long, val disponibilidad: String, val embebible: Boolean)

  fun buscar(slug: String, uuid: String?, consultaOriginal: String): Map<String, Any?> {
    // Solo se comprueba sintaxis del UUID; el frontend puede buscar antes de registrar.
    dispositivos.huella(uuid)
    val consulta = consultaOriginal.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    if (consulta.length !in 2..80) {
      throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "q debe tener entre 2 y 80 caracteres")
    }
    val bar = jdbc.query(
      """
            SELECT e.id_establecimiento, cs.duracion_max_seg, cs.minutos_bloqueo_repeticion
            FROM public.establecimiento e
            JOIN public.configuracion_servicio cs ON cs.id_establecimiento = e.id_establecimiento
            WHERE e.slug = ? AND e.activo = TRUE
            """.trimIndent(),
      { rs, _ -> Bar(rs.getLong(1), rs.getInt(2), rs.getInt(3)) },
      slug,
    ).firstOrNull() ?: throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Bar no encontrado")

    val sesionId = jdbc.query(
      """
            SELECT id_sesion FROM public.sesion
            WHERE id_establecimiento = ? AND estado = 'abierta'
            ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1
            """.trimIndent(),
      { rs, _ -> rs.getLong(1) }, bar.id,
    ).firstOrNull()

    // `desde_cache` en el contrato identifica el respaldo ante una caída de YouTube.
    // Una búsqueda servida normalmente desde una caché todavía válida NO es una caída.
    val (videos, desdeCache) = try {
      val busqueda = youtube.buscar(consulta)
      busqueda.resultados to false
    } catch (error: ResponseStatusException) {
      val guardados = cacheAnterior(consulta)
      if (guardados != null) {
        guardados to true
      } else {
        val cuota = error.statusCode.value() == 429 ||
          (error.reason?.contains("cuota", ignoreCase = true) == true)
        throw ContratoApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          if (cuota) "cuota_agotada" else "youtube_no_disponible",
          if (cuota) "La cuota de YouTube no permite buscar ahora" else "YouTube no está disponible",
        )
      }
    }

    // Los ItemCola salen de la misma foto que `ahora`: así traen espera_seg y puesto_anterior
    // calculados igual que en la cola (antes faltaba espera_seg y la interfaz rechazaba la búsqueda).
    // La foto completa solo si la búsqueda coincide con algo de la cola: casi siempre se busca
    // una canción nueva y `ahora` son varias consultas (rapidez de la búsqueda, 2026-11-12).
    @Suppress("UNCHECKED_CAST")
    val colaActual = if (sesionId != null && titulosEnCola(sesionId).any { coincide(it, consulta) }) {
      estados.ahora(slug)["cola"] as List<Map<String, Any?>>
    } else {
      emptyList()
    }
    val enCola = colaActual.filter { item ->
      val cancion = item["cancion"] as Map<String, Any?>
      coincide(cancion["titulo"] as String to cancion["artista"] as String?, consulta)
    }

    // Cuatro consultas para toda la página, no cuatro por resultado (antes eran 40 con 10).
    val ids = videos.map { it.youtubeVideoId }
    val canciones = consultarCanciones(ids)
    val idsCancion = canciones.values.map { it.id }
    val bloqueadas = bloqueadas(bar.id, idsCancion)
    val ultimas = ultimasReproducciones(bar.id, idsCancion)
    val pedidas = totalesPedidas(bar.id, idsCancion)
    val resultados = videos.map { video ->
      val almacenada = canciones[video.youtubeVideoId]
      val bloqueo = almacenada?.let { it.id in bloqueadas } ?: false
      val repeticionHasta = almacenada?.let { ultimas[it.id] }
        ?.plusMinutes(bar.noRepeticionMin.toLong())
        ?.takeIf { bar.noRepeticionMin > 0 && it.isAfter(OffsetDateTime.now()) }
      val motivo = when {
        bloqueo -> "bloqueada"
        !video.embebible || almacenada?.embebible == false -> "no_embebible"
        almacenada?.disponibilidad in setOf("no_disponible", "restringida") -> "no_disponible"
        video.duracionSeg > bar.maxSeg -> "muy_larga"
        repeticionHasta != null -> "reproducida_hace_poco"
        else -> null
      }
      mapOf(
        "cancion" to mapOf(
          "video_id" to video.youtubeVideoId,
          "titulo" to video.titulo,
          "artista" to (video.artista ?: video.canal),
          "duracion_seg" to video.duracionSeg,
          "miniatura_url" to video.miniaturaUrl?.takeIf(String::isNotBlank),
        ),
        "pedible" to (motivo == null),
        "motivo" to motivo,
        "disponible_desde" to (if (motivo == "reproducida_hace_poco") repeticionHasta else null),
        "genero" to null, // Fuente del género aún es TBD en doc 04.
        "pedidas_en_el_bar" to (almacenada?.let { pedidas[it.id] } ?: 0L),
      )
    }
    return mapOf(
      "consulta" to consulta,
      "en_cola" to enCola,
      "resultados" to resultados,
      "desde_cache" to desdeCache,
    )
  }

  /** `IN (?, ?, …)` con un marcador por valor: los valores van como parámetros, nunca en el texto. */
  private fun marcadores(n: Int) = List(n) { "?" }.joinToString(", ")

  /** Título y artista de lo que espera en la cola: una consulta barata para decidir si hace falta `ahora`. */
  private fun titulosEnCola(idSesion: Long): List<Pair<String, String?>> = jdbc.query(
    """SELECT c.titulo, c.artista FROM public.solicitud s JOIN public.cancion c ON c.id_cancion = s.id_cancion
         WHERE s.id_sesion = ? AND s.estado = 'pendiente'""",
    { rs, _ -> rs.getString(1) to rs.getString(2) }, idSesion,
  )

  private fun consultarCanciones(videoIds: List<String>): Map<String, CancionDb> {
    if (videoIds.isEmpty()) return emptyMap()
    return jdbc.query(
      "SELECT youtube_video_id, id_cancion, disponibilidad, embebible FROM public.cancion WHERE youtube_video_id IN (${marcadores(videoIds.size)})",
      { rs, _ -> rs.getString(1) to CancionDb(rs.getLong(2), rs.getString(3), rs.getBoolean(4)) },
      *videoIds.toTypedArray(),
    ).toMap()
  }

  private fun bloqueadas(idBar: Long, idsCancion: List<Long>): Set<Long> {
    if (idsCancion.isEmpty()) return emptySet()
    return jdbc.query(
      """SELECT DISTINCT id_cancion FROM public.bloqueo_contenido
         WHERE id_establecimiento = ? AND id_cancion IN (${marcadores(idsCancion.size)}) AND activo = TRUE
           AND fecha_inicio <= CURRENT_TIMESTAMP AND (fecha_fin IS NULL OR fecha_fin > CURRENT_TIMESTAMP)""",
      { rs, _ -> rs.getLong(1) }, idBar, *idsCancion.toTypedArray(),
    ).toSet()
  }

  private fun ultimasReproducciones(idBar: Long, idsCancion: List<Long>): Map<Long, OffsetDateTime> {
    if (idsCancion.isEmpty()) return emptyMap()
    return jdbc.query(
      """SELECT r.id_cancion, MAX(r.fecha_inicio) AS ultima FROM public.reproduccion r
         JOIN public.sesion s ON s.id_sesion = r.id_sesion
         WHERE s.id_establecimiento = ? AND r.id_cancion IN (${marcadores(idsCancion.size)})
           AND r.resultado IN ('iniciada', 'completada') AND r.fecha_inicio IS NOT NULL
         GROUP BY r.id_cancion""",
      { rs, _ -> rs.getLong(1) to rs.getObject("ultima", OffsetDateTime::class.java) },
      idBar, *idsCancion.toTypedArray(),
    ).toMap()
  }

  /** Por id de canción: una canción que nunca se guardó no se pudo pedir (0 sin consultar). */
  private fun totalesPedidas(idBar: Long, idsCancion: List<Long>): Map<Long, Long> {
    if (idsCancion.isEmpty()) return emptyMap()
    return jdbc.query(
      """SELECT sol.id_cancion, COUNT(*) FROM public.solicitud sol
         JOIN public.sesion s ON s.id_sesion = sol.id_sesion
         WHERE s.id_establecimiento = ? AND sol.id_cancion IN (${marcadores(idsCancion.size)})
           AND sol.fecha_solicitud >= CURRENT_TIMESTAMP - INTERVAL '28 days'
         GROUP BY sol.id_cancion""",
      { rs, _ -> rs.getLong(1) to rs.getLong(2) }, idBar, *idsCancion.toTypedArray(),
    ).toMap()
  }

  /** Permite degradación con resultados ya guardados, incluso si expiró su TTL. */
  private fun cacheAnterior(consulta: String): List<YoutubeVideoResponse>? {
    val texto = jdbc.query(
      """
            SELECT resultados::text FROM public.busqueda_cache
            WHERE consulta_normalizada = ? AND region = 'CO' AND idioma = 'es'
            ORDER BY fecha_creacion DESC LIMIT 1
            """.trimIndent(),
      { rs, _ -> rs.getString(1) }, consulta,
    ).firstOrNull() ?: return null
    return runCatching {
      json.readTree(texto).toList().map { nodo ->
        YoutubeVideoResponse(
          youtubeVideoId = nodo.path("youtubeVideoId").asText(),
          titulo = nodo.path("titulo").asText(),
          artista = nodo.path("artista").takeUnless { it.isNull }?.asText(),
          artistaEstimado = nodo.path("artistaEstimado").asBoolean(false),
          canal = nodo.path("canal").asText(),
          duracionSeg = nodo.path("duracionSeg").asInt(),
          miniaturaUrl = nodo.path("miniaturaUrl").takeUnless { it.isNull }?.asText(),
          embebible = nodo.path("embebible").asBoolean(false),
          url = nodo.path("url").asText(),
        )
      }.filter { it.youtubeVideoId.isNotBlank() && it.titulo.isNotBlank() }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
  }
}
