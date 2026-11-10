package com.proyectoN.rokolaBackend.tiemporeal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.ConcurrentHashMap

/** Socket de SOLO LECTURA: envia instantaneas publicas, nunca recibe ordenes de reproduccion. */
@Component
class RokolaWebSocketHandler(
    private val instantaneas: EstadoMusicalSnapshotService,
    private val json: JsonMapper,
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val suscriptores = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()
    private val sesionesSocket = ConcurrentHashMap<String, Long>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val idSesion = session.uri?.let {
            UriComponentsBuilder.fromUri(it).build().queryParams.getFirst("sesionId")?.toLongOrNull()
        }
        if (idSesion == null || idSesion <= 0L) {
            session.close(CloseStatus.BAD_DATA)
            return
        }
        // Solo permitimos suscripciones a sesiones cuya cola publica esta disponible.
        val inicial = try { instantanea(idSesion, "estado_inicial") } catch (_: Exception) {
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }
        sesionesSocket[session.id] = idSesion
        suscriptores.computeIfAbsent(idSesion) { ConcurrentHashMap.newKeySet<WebSocketSession>() }.add(session)
        enviar(session, inicial)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        quitar(session)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.warn("Error de socket Rokola {}: {}", session.id, exception.message)
        quitar(session)
        if (session.isOpen) session.close(CloseStatus.SERVER_ERROR)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // No aceptar comandos del navegador: solo el REST autenticado escribe la BD.
        session.close(CloseStatus.POLICY_VIOLATION)
    }

    fun notificar(idSesion: Long, tipo: String) {
        val sockets = suscriptores[idSesion] ?: return
        if (sockets.isEmpty()) return
        val mensaje = instantanea(idSesion, tipo)
        sockets.toList().forEach { enviar(it, mensaje) }
    }

    private fun instantanea(idSesion: Long, tipo: String): TextMessage = TextMessage(
        json.writeValueAsString(instantaneas.crear(idSesion, tipo)),
    )

    private fun enviar(session: WebSocketSession, mensaje: TextMessage) {
        if (!session.isOpen) {
            quitar(session)
            return
        }
        try {
            // En Spring WebSocket los envios a la misma conexion deben serializarse.
            synchronized(session) { if (session.isOpen) session.sendMessage(mensaje) }
        } catch (e: Exception) {
            log.debug("No se pudo enviar evento websocket: {}", e.message)
            quitar(session)
            try { if (session.isOpen) session.close(CloseStatus.SERVER_ERROR) } catch (_: Exception) {}
        }
    }

    private fun quitar(session: WebSocketSession) {
        val idSesion = sesionesSocket.remove(session.id) ?: return
        suscriptores[idSesion]?.let { grupo ->
            grupo.remove(session)
            if (grupo.isEmpty()) suscriptores.remove(idSesion, grupo)
        }
    }
}
