package com.proyectoN.rokolaBackend.tiemporeal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CambioColaWebSocketListener(private val sockets: RokolaWebSocketHandler) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun enviar(evento: CambioColaEvent) {
        // Un socket desconectado no debe convertir una transaccion confirmada en error HTTP.
        try {
            sockets.notificar(evento.sesionId, evento.tipo)
        } catch (e: Exception) {
            log.warn("La actualizacion WebSocket no pudo enviarse para sesion {}: {}", evento.sesionId, e.message)
        }
    }
}
