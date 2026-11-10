package com.proyectoN.rokolaBackend.youtube

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.HtmlUtils
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

/** Consulta exclusivamente datos públicos: no necesita OAuth, sí una API key. */
@Component
class   YoutubeApiCliente(
  private val json: JsonMapper,
  @Value("\${youtube.api.key:}") private val claveApi: String,
) {
    private val http = RestClient.builder()
        .baseUrl("https://www.googleapis.com/youtube/v3")
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5_000)
            setReadTimeout(10_000)
        })
        .build()

    fun buscar(consulta: String): List<YoutubeVideoResponse> {
        if (claveApi.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_API_KEY no está configurada")
        }

        val resultadosBusqueda = solicitar {
            http.get().uri { url ->
                url.path("/search")
                    .queryParam("part", "snippet")
                    .queryParam("type", "video")
                    .queryParam("videoEmbeddable", "true")
                    .queryParam("maxResults", 10)
                    .queryParam("regionCode", "CO")
                    .queryParam("relevanceLanguage", "es")
                    .queryParam("q", "{texto}")
                    .queryParam("key", claveApi)
                    .build(consulta)
            }.retrieve().body(String::class.java)
        }

      val ids = json.readTree(resultadosBusqueda)
        .path("items")
        .toList()
        .mapNotNull { nodo ->
          nodo.path("id").path("videoId").asText()
            .takeIf { it.isNotBlank() }
        }
        .distinct()
        if (ids.isEmpty()) return emptyList()

        val detalles = solicitar {
            http.get().uri { url ->
                url.path("/videos")
                    .queryParam("part", "snippet,contentDetails,status")
                    .queryParam("id", "{ids}")
                    .queryParam("key", claveApi)
                    .build(ids.joinToString(","))
            }.retrieve().body(String::class.java)
        }

        val videos = json.readTree(detalles).path("items")
            .mapNotNull { convertir(it) }
            .associateBy { it.youtubeVideoId }
        return ids.mapNotNull(videos::get) // Respeta el orden de relevancia de YouTube
    }

    /** Verifica un video exacto en YouTube antes de aceptar una solicitud musical. */
    fun obtenerVideoPorId(videoId: String): YoutubeVideoResponse? {
        if (claveApi.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_API_KEY no está configurada")
        }
        val detalles = solicitar {
            http.get().uri { url ->
                url.path("/videos")
                    .queryParam("part", "snippet,contentDetails,status")
                    .queryParam("id", videoId)
                    .queryParam("key", claveApi)
                    .build()
            }.retrieve().body(String::class.java)
        }
        return json.readTree(detalles).path("items").toList()
            .firstNotNullOfOrNull { convertir(it) }
    }

    private fun convertir(nodo: JsonNode): YoutubeVideoResponse? {
        val id = nodo.path("id").asText()
        val snippet = nodo.path("snippet")
        val estado = nodo.path("status")
        if (id.isBlank() || !estado.path("embeddable").asBoolean(false)) return null
        if (estado.path("privacyStatus").asText() != "public") return null
        if (snippet.path("liveBroadcastContent").asText() == "live") return null

        val duracion = runCatching {
            Duration.parse(nodo.path("contentDetails").path("duration").asText()).seconds
        }.getOrNull()?.takeIf { it in 1..Int.MAX_VALUE.toLong() } ?: return null

        val titulo = HtmlUtils.htmlUnescape(snippet.path("title").asText())
        val canal = HtmlUtils.htmlUnescape(snippet.path("channelTitle").asText())
        val miniaturas = snippet.path("thumbnails")
        val miniatura = listOf("medium", "high", "default")
            .firstNotNullOfOrNull { tamaño -> miniaturas.path(tamaño).path("url").asText().takeIf(String::isNotBlank) }

        // Heurística explícita, no metadato oficial: "Artista - Canción".
      val artista = Regex("^(.{2,80}?)\\s+[-–—]\\s+.+$")
        .find(titulo)?.groupValues?.getOrNull(1)?.trim()

        return YoutubeVideoResponse(
            youtubeVideoId = id,
            titulo = titulo,
            artista = artista,
            artistaEstimado = artista != null,
            canal = canal,
            duracionSeg = duracion.toInt(),
            miniaturaUrl = miniatura,
            embebible = true,
            url = "https://www.youtube.com/watch?v=$id",
        )
    }

    private fun solicitar(llamada: () -> String?): String = try {
        llamada() ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía de YouTube")
    } catch (e: RestClientResponseException) {
        val cuota = e.statusCode.value() == 429 ||
            (e.statusCode.value() == 403 && e.responseBodyAsString.contains("quotaExceeded"))
        throw ResponseStatusException(
            if (cuota) HttpStatus.TOO_MANY_REQUESTS else HttpStatus.BAD_GATEWAY,
            if (cuota) "YouTube ha agotado su cuota" else "YouTube rechazó la solicitud; revisa la API key y sus restricciones",
        )
    } catch (_: ResourceAccessException) {
        throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "YouTube no respondió a tiempo")
    }
}
