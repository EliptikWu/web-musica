package com.proyectoN.rokolaBackend.youtube

import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.json.JsonMapper

/** Reutiliza la tabla public.busqueda_cache que ya existe en DB_Rokola. */
@Repository
class YoutubeCacheRepository(
    private val jdbc: JdbcTemplate,
    private val json: JsonMapper,
    /**
     * Cuánto se guarda una búsqueda de YouTube (rapidez de la búsqueda, 2026-11-12). Antes 30 min;
     * 6 h cubre una noche de búsquedas repetidas con la cuota diaria (`youtube.busquedas.max-diarias`).
     * La disponibilidad real se vuelve a validar al pedir. *Propuesto*: por confirmar con el backend.
     */
    @Value("\${youtube.cache.horas:6}") private val horasCache: Int,
) {
    private val region = "CO"
    private val idioma = "es"

    fun obtener(consulta: String): List<YoutubeVideoResponse>? {
        val filas = jdbc.query(
            """
            SELECT resultados::text AS json
            FROM public.busqueda_cache
            WHERE consulta_normalizada = ? AND region = ? AND idioma = ?
              AND expira_en > CURRENT_TIMESTAMP
            """.trimIndent(),
            { rs, _ -> rs.getString("json") },
            consulta, region, idioma,
        )
        val resultado = filas.firstOrNull() ?: return null
        val datos = json.readTree(resultado)
      val videos = datos.toList().map { nodo ->
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
        }
        jdbc.update(
            """UPDATE public.busqueda_cache SET aciertos = aciertos + 1
                WHERE consulta_normalizada = ? AND region = ? AND idioma = ?""",
            consulta, region, idioma,
        )
        return videos
    }

    fun guardar(consulta: String, resultados: List<YoutubeVideoResponse>) {
        jdbc.update(
            """
            INSERT INTO public.busqueda_cache
              (consulta_normalizada, region, idioma, resultados, fecha_creacion, expira_en, aciertos)
            VALUES (?, ?, ?, CAST(? AS jsonb), CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP + (? * INTERVAL '1 hour'), 0)
            ON CONFLICT (consulta_normalizada, region, idioma)
            DO UPDATE SET resultados = EXCLUDED.resultados,
                          fecha_creacion = CURRENT_TIMESTAMP,
                          expira_en = EXCLUDED.expira_en,
                          aciertos = 0
            """.trimIndent(),
            consulta, region, idioma, json.writeValueAsString(resultados), horasCache,
        )
    }

    /** Autocompletado con términos anteriores y canciones locales, sin llamar a YouTube. */
    fun sugerencias(texto: String): List<String> = jdbc.query(
        """
        SELECT sugerencia FROM (
            SELECT titulo AS sugerencia FROM public.cancion
              WHERE STRPOS(LOWER(titulo), ?) > 0
            UNION
            SELECT artista AS sugerencia FROM public.cancion
              WHERE artista IS NOT NULL AND STRPOS(LOWER(artista), ?) > 0
            UNION
            SELECT consulta_normalizada AS sugerencia FROM public.busqueda_cache
              -- Solo lo buscado hace poco, como antes de alargar la caché: la caché es de
              -- todos los bares y una búsqueda no debe sugerirse a otro bar durante horas.
              WHERE fecha_creacion > CURRENT_TIMESTAMP - INTERVAL '30 minutes'
                AND STRPOS(LOWER(consulta_normalizada), ?) > 0
        ) AS candidatos
        ORDER BY LOWER(sugerencia)
        LIMIT 8
        """.trimIndent(),
        { rs, _ -> rs.getString("sugerencia") },
        texto, texto, texto,
    )
}
