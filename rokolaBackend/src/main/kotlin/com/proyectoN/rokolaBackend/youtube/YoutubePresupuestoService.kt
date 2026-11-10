package com.proyectoN.rokolaBackend.youtube

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.ZoneId

/** Límite preventivo por instancia. No sustituye el contador de cuotas de Google Cloud. */
@Service
class YoutubePresupuestoService(
    @Value("\${youtube.busquedas.max-diarias:80}") private val limite: Int,
) {
    private val pacifico = ZoneId.of("America/Los_Angeles")
    private var fecha = LocalDate.now(pacifico)
    private var consumidas = 0

    @Synchronized
    fun reservarBusqueda() {
        val hoy = LocalDate.now(pacifico)
        if (hoy != fecha) {
            fecha = hoy
            consumidas = 0
        }
        if (consumidas >= limite) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Se alcanzó el límite preventivo diario de búsquedas. Reintenta más tarde.",
            )
        }
        consumidas++
    }
}
