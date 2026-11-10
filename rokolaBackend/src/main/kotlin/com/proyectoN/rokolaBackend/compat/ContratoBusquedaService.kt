package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.cola.ColaService
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
@Service
class ContratoBusquedaService(
    private val jdbc: JdbcTemplate,
    private val youtube: YoutubeService,
    private val json: JsonMapper,
    private val cola: ColaService,
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

        val (videos, desdeCache) = try {
            val busqueda = youtube.buscar(consulta)
            busqueda.resultados to (busqueda.origen == "cache")
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

        val colaActual = if (sesionId != null) cola.obtener(sesionId).canciones else emptyList()
        val enCola = colaActual.filter {
            it.titulo.contains(consulta, ignoreCase = true) ||
                (it.artista?.contains(consulta, ignoreCase = true) == true)
        }.map { item ->
            mapOf(
                "solicitud_id" to item.solicitudId,
                "cancion" to mapOf(
                    "video_id" to item.youtubeVideoId,
                    "titulo" to item.titulo,
                    "artista" to (item.artista ?: ""),
                    "duracion_seg" to item.duracionSeg,
                    "miniatura_url" to item.miniaturaUrl,
                ),
                "votos_total" to item.votos,
                "fecha_solicitud" to item.fechaSolicitud,
                "puesto" to item.posicion,
                "puesto_anterior" to null,
            )
        }

        val resultados = videos.map { video ->
            val almacenada = consultarCancion(video.youtubeVideoId)
            val bloqueo = almacenada?.let { estaBloqueada(bar.id, it.id) } ?: false
            val repeticionHasta = almacenada?.let { ultimaReproduccion(bar.id, it.id) }
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
                    "miniatura_url" to video.miniaturaUrl,
                ),
                "pedible" to (motivo == null),
                "motivo" to motivo,
                "disponible_desde" to (if (motivo == "reproducida_hace_poco") repeticionHasta else null),
                "genero" to null, // Fuente del género aún es TBD en doc 04.
                "pedidas_en_el_bar" to totalPedidas(bar.id, video.youtubeVideoId),
            )
        }
        return mapOf(
            "consulta" to consulta,
            "en_cola" to enCola,
            "resultados" to resultados,
            "desde_cache" to desdeCache,
        )
    }

    private fun consultarCancion(videoId: String): CancionDb? = jdbc.query(
        """SELECT id_cancion, disponibilidad, embebible FROM public.cancion WHERE youtube_video_id = ?""",
        { rs, _ -> CancionDb(rs.getLong(1), rs.getString(2), rs.getBoolean(3)) }, videoId,
    ).firstOrNull()

    private fun estaBloqueada(idBar: Long, idCancion: Long): Boolean = jdbc.queryForObject(
        """
        SELECT EXISTS(SELECT 1 FROM public.bloqueo_contenido
          WHERE id_establecimiento = ? AND id_cancion = ? AND activo = TRUE
            AND fecha_inicio <= CURRENT_TIMESTAMP AND (fecha_fin IS NULL OR fecha_fin > CURRENT_TIMESTAMP))
        """.trimIndent(), Boolean::class.javaObjectType, idBar, idCancion,
    ) ?: false

    private fun ultimaReproduccion(idBar: Long, idCancion: Long): OffsetDateTime? = jdbc.query(
        """
        SELECT MAX(r.fecha_inicio) AS ultima FROM public.reproduccion r
        JOIN public.sesion s ON s.id_sesion = r.id_sesion
        WHERE s.id_establecimiento = ? AND r.id_cancion = ? AND r.resultado IN ('iniciada', 'completada')
        """.trimIndent(),
        { rs, _ -> rs.getObject("ultima", OffsetDateTime::class.java) }, idBar, idCancion,
    ).firstOrNull()

    private fun totalPedidas(idBar: Long, videoId: String): Long = jdbc.queryForObject(
        """
        SELECT COUNT(*) FROM public.solicitud sol
        JOIN public.sesion s ON s.id_sesion = sol.id_sesion
        JOIN public.cancion c ON c.id_cancion = sol.id_cancion
        WHERE s.id_establecimiento = ? AND c.youtube_video_id = ?
          AND sol.fecha_solicitud >= CURRENT_TIMESTAMP - INTERVAL '28 days'
        """.trimIndent(), Long::class.javaObjectType, idBar, videoId,
    ) ?: 0L

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
