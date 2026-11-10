package com.proyectoN.rokolaBackend.tiemporeal

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/** Publicar DENTRO de la transaccion; emitir por WebSocket solo DESPUES del commit. */
data class CambioColaEvent(val sesionId: Long, val tipo: String)

@Component
class CambioColaPublisher(private val publisher: ApplicationEventPublisher) {
    fun publicar(idSesion: Long, tipo: String) {
        publisher.publishEvent(CambioColaEvent(idSesion, tipo))
    }
}
