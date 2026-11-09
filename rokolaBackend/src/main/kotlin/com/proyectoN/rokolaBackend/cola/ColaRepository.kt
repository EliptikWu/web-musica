package com.proyectoN.rokolaBackend.cola

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

/** Consulta única para obtener la cola sin cargar entidades ni hacer N+1 consultas. */
@Repository
class ColaRepository(private val jdbc: JdbcTemplate) {

    internal data class FilaCola(
        val solicitudId: Long,
        val cancionId: Long,
        val youtubeVideoId: String,
        val titulo: String,
        val artista: String?,
        val duracionSeg: Int,
        val miniaturaUrl: String?,
        val votos: Long,
        val fechaSolicitud: OffsetDateTime,
    )

    internal fun listarPendientes(idSesion: Long): List<FilaCola> = jdbc.query(
        """
        SELECT s.id_solicitud,
               c.id_cancion,
               c.youtube_video_id,
               c.titulo,
               c.artista,
               c.duracion_seg,
               c.miniatura_url,
               s.fecha_solicitud,
               COALESCE(conteo.total_votos, 0) AS total_votos
        FROM public.solicitud s
        JOIN public.cancion c ON c.id_cancion = s.id_cancion
        LEFT JOIN (
            SELECT id_solicitud, COUNT(*) AS total_votos
            FROM public.voto
            WHERE activo = TRUE
            GROUP BY id_solicitud
        ) conteo ON conteo.id_solicitud = s.id_solicitud
        WHERE s.id_sesion = ?
          AND s.estado = 'pendiente'
        ORDER BY COALESCE(conteo.total_votos, 0) DESC,
                 s.fecha_solicitud ASC,
                 s.id_solicitud ASC
        """.trimIndent(),
        { rs, _ ->
            FilaCola(
                solicitudId = rs.getLong("id_solicitud"),
                cancionId = rs.getLong("id_cancion"),
                youtubeVideoId = rs.getString("youtube_video_id"),
                titulo = rs.getString("titulo"),
                artista = rs.getString("artista"),
                duracionSeg = rs.getInt("duracion_seg"),
                miniaturaUrl = rs.getString("miniatura_url"),
                votos = rs.getLong("total_votos"),
                fechaSolicitud = rs.getObject("fecha_solicitud", OffsetDateTime::class.java),
            )
        },
        idSesion,
    )
}
