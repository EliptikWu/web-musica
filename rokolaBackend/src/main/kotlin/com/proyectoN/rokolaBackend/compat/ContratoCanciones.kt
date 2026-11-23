package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.cancion.ClasificadorGeneroService
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.sql.ResultSet

/**
 * Canciones para el panel (bloqueos y listas de la casa): la que ya está en la base, o la que
 * YouTube confirma que existe y se puede embeber (`videos.list`), guardada igual que al pedirla.
 */
@Component
class ContratoCanciones(
  private val jdbc: JdbcTemplate,
  private val youtube: YoutubeApiCliente,
  private val clasificador: ClasificadorGeneroService,
) {
  data class Fila(val id: Long, val disponible: Boolean, val contrato: Map<String, Any?>)

  fun enBase(videoId: String): Fila? = jdbc.query(
    "SELECT $COLUMNAS FROM public.cancion c WHERE c.youtube_video_id = ?",
    { rs, _ -> fila(rs) }, videoId,
  ).firstOrNull()

  /** La canción del video, validada con YouTube si la base no la tiene disponible. */
  fun obtenerOValidar(videoIdCrudo: String): Fila {
    val videoId = videoIdCrudo.trim()
    if (!FORMATO_VIDEO.matches(videoId)) ContratoCuerpo.invalido("video_id inválido")
    enBase(videoId)?.takeIf { it.disponible }?.let { return it }
    val video = try {
      youtube.obtenerVideoPorId(videoId)
    } catch (e: ResponseStatusException) {
      val cuota = e.statusCode.value() == 429 || e.reason?.contains("cuota", ignoreCase = true) == true
      throw ContratoApiException(
        HttpStatus.SERVICE_UNAVAILABLE, if (cuota) "cuota_agotada" else "youtube_no_disponible",
        "No fue posible validar el video con YouTube",
      )
    }
    if (video == null || !video.embebible || video.duracionSeg <= 0) {
      throw ContratoApiException(HttpStatus.UNPROCESSABLE_ENTITY, "no_disponible", "Ese video no está disponible")
    }
    val generoDetectado = clasificador.detectar(video.etiquetas)
    jdbc.update(
      """INSERT INTO public.cancion AS c
             (youtube_video_id, titulo, artista, canal, categoria, origen_categoria, categoria_youtube,
              duracion_seg, miniatura_url, embebible, disponibilidad, ultima_validacion)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, 'disponible', CURRENT_TIMESTAMP)
         ON CONFLICT (youtube_video_id) DO UPDATE SET
            titulo = EXCLUDED.titulo, artista = EXCLUDED.artista, canal = EXCLUDED.canal,
            duracion_seg = EXCLUDED.duracion_seg, miniatura_url = EXCLUDED.miniatura_url,
            embebible = TRUE, disponibilidad = 'disponible', ultima_validacion = CURRENT_TIMESTAMP,
            categoria_youtube = COALESCE(EXCLUDED.categoria_youtube, c.categoria_youtube),
            categoria = CASE WHEN c.origen_categoria = 'MANUAL' THEN c.categoria
                             ELSE COALESCE(c.categoria, EXCLUDED.categoria) END,
            origen_categoria = CASE WHEN c.origen_categoria = 'MANUAL' THEN 'MANUAL'
                                    WHEN c.categoria IS NULL AND EXCLUDED.categoria IS NOT NULL
                                    THEN 'ETIQUETAS' ELSE c.origen_categoria END""",
      video.youtubeVideoId, video.titulo.take(300), video.artista?.take(200), video.canal.take(200),
      generoDetectado, if (generoDetectado != null) "ETIQUETAS" else null,
      video.categoriaYoutube?.take(100), video.duracionSeg, video.miniaturaUrl,
    )
    return enBase(video.youtubeVideoId) ?: throw IllegalStateException("No se guardó la canción")
  }

  companion object {
    /** Columnas de `cancion c` que lee [fila]. */
    const val COLUMNAS = "c.id_cancion, c.youtube_video_id, c.titulo, c.artista, c.canal, c.duracion_seg, c.miniatura_url, c.disponibilidad, c.embebible"
    private val FORMATO_VIDEO = Regex("^[A-Za-z0-9_-]{6,30}$")

    fun fila(rs: ResultSet) = Fila(
      id = rs.getLong("id_cancion"),
      disponible = rs.getString("disponibilidad") == "disponible" && rs.getBoolean("embebible"),
      contrato = cancion(rs),
    )

    /** `Cancion` del contrato; el artista sale del canal cuando YouTube no lo da. */
    fun cancion(rs: ResultSet): Map<String, Any?> = mapOf(
      "video_id" to rs.getString("youtube_video_id"),
      "titulo" to rs.getString("titulo"),
      "artista" to (rs.getString("artista")?.takeIf(String::isNotBlank) ?: rs.getString("canal").orEmpty()),
      "duracion_seg" to rs.getInt("duracion_seg").coerceAtLeast(0),
      "miniatura_url" to rs.getString("miniatura_url")?.takeIf(String::isNotBlank),
    )
  }
}
