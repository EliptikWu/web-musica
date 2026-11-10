package com.proyectoN.rokolaBackend.youtube

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Locale

@Service
class YoutubeService(
    private val youtube: YoutubeApiCliente,
    private val cache: YoutubeCacheRepository,
    private val presupuesto: YoutubePresupuestoService,
) {
    fun buscar(q: String): YoutubeBusquedaResponse {
        val consulta = normalizar(q)
        cache.obtener(consulta)?.let {
            return YoutubeBusquedaResponse(consulta, "cache", it)
        }

        presupuesto.reservarBusqueda()
        val resultados = youtube.buscar(consulta)
        cache.guardar(consulta, resultados)
        return YoutubeBusquedaResponse(consulta, "youtube", resultados)
    }

    fun sugerencias(q: String): List<String> = cache.sugerencias(normalizar(q))

    private fun normalizar(texto: String): String {
        val consulta = texto.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
        if (consulta.length !in 2..80) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "q debe tener entre 2 y 80 caracteres")
        }
        return consulta
    }
}
