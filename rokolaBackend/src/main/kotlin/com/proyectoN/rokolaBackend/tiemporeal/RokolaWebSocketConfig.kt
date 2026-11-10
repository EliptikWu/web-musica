package com.proyectoN.rokolaBackend.tiemporeal

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/** Origenes explicitos por defecto. Agregar el dominio real del frontend en produccion. */
@Configuration
@EnableWebSocket
class RokolaWebSocketConfig(
    private val handler: RokolaWebSocketHandler,
    @Value("\${rokola.websocket.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
    private val allowedOrigins: String,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        val origins = allowedOrigins.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
        registry.addHandler(handler, "/ws/rokola/cola").setAllowedOrigins(*origins)
    }
}
