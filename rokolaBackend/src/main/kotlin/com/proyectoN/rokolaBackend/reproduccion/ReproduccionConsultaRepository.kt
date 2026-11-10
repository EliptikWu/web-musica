package com.proyectoN.rokolaBackend.reproduccion

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.OffsetDateTime

@Repository
class ReproduccionConsultaRepository(private val jdbc: JdbcTemplate) {

    /** El indice parcial uq_reproduccion_activa_sesion garantiza como maximo una fila. */
    fun actual(idSesion: Long, ahora: OffsetDateTime = OffsetDateTime.now()): ReproduccionActualResponse? = jdbc.query(
        """
        SELECT r.id_reproduccion, r.id_sesion, r.id_solicitud, r.id_elemento,
               r.id_cancion, r.fecha_inicio, r.segundos_reproducidos, r.resultado,
               c.youtube_video_id, c.titulo, c.artista, c.miniatura_url, c.duracion_seg
        FROM public.reproduccion r
        JOIN public.cancion c ON c.id_cancion = r.id_cancion
        WHERE r.id_sesion = ? AND r.resultado = 'iniciada'
        ORDER BY r.id_reproduccion DESC
        LIMIT 1
        """.trimIndent(),
        { rs, _ ->
            val inicio = rs.getObject("fecha_inicio", OffsetDateTime::class.java)
            val duracion = rs.getInt("duracion_seg").coerceAtLeast(0).toLong()
            val transcurridoPorReloj = inicio?.let {
                Duration.between(it.toInstant(), ahora.toInstant()).seconds.coerceAtLeast(0L)
            } ?: 0L
            // Estimacion: se supone reproduccion continua; no refleja pausas del reproductor YouTube.
            val transcurridos = maxOf(transcurridoPorReloj, rs.getLong("segundos_reproducidos"))
                .coerceIn(0L, duracion)
            ReproduccionActualResponse(
                reproduccionId = rs.getLong("id_reproduccion"),
                sesionId = rs.getLong("id_sesion"),
                solicitudId = rs.getLong("id_solicitud").takeUnless { rs.wasNull() },
                elementoListaId = rs.getLong("id_elemento").takeUnless { rs.wasNull() },
                origen = if (rs.getObject("id_solicitud") != null) "solicitud" else "lista_defecto",
                cancionId = rs.getLong("id_cancion"),
                youtubeVideoId = rs.getString("youtube_video_id"),
                titulo = rs.getString("titulo"),
                artista = rs.getString("artista"),
                miniaturaUrl = rs.getString("miniatura_url"),
                duracionSeg = duracion.toInt(),
                fechaInicio = inicio,
                segundosTranscurridos = transcurridos,
                segundosRestantes = (duracion - transcurridos).coerceAtLeast(0L),
                resultado = rs.getString("resultado"),
            )
        },
        idSesion,
    ).firstOrNull()
}
