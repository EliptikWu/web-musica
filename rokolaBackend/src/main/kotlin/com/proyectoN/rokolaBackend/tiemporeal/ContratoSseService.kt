package com.proyectoN.rokolaBackend.tiemporeal

import com.proyectoN.rokolaBackend.compat.ContratoApiException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/** Cambió el servicio de un bar sin noche abierta: no hay sesión por la que avisar. */
data class CambioServicioEvent(val slug: String)

/** El evento panel sólo anuncia recursos (nunca devuelve datos de administración). */
data class CambioPanelEvent(val slug: String, val recursos: Set<String>)

@Component
class CambioPanelPublisher(private val publisher: ApplicationEventPublisher) {
    fun publicar(slug: String, recursos: Set<String>) {
        publisher.publishEvent(CambioPanelEvent(slug, recursos))
    }
}

/**
 * Adaptador de CambioColaEvent -> SSE contractual, transporte usado por web y Android.
 * PMV monoinstancia. La sincronización por bar garantiza que la primera emisión sea 'ahora'.
 */
@Component
@EnableScheduling
class ContratoSseService(private val instantaneas: ContratoSseInstantaneas) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Los avisos salen en un hilo aparte, de a uno y en orden. Los oyentes AFTER_COMMIT corren en
     * el hilo de la petición, que todavía tiene su conexión: armar la foto ahí pedía una segunda,
     * y con 10 votos a la vez (el tamaño del pool) los 10 hilos se quedaban esperando una que
     * nadie soltaba, 30 s hasta que Hikari se rendía (prueba de carga del 2026-11-25, doc 07). Así,
     * además, la respuesta del voto no espera a que se arme y se envíe la foto.
     */
    private val envios = Executors.newSingleThreadExecutor { trabajo ->
        Thread(trabajo, "rokola-sse").apply { isDaemon = true }
    }

    private fun enOtroHilo(que: String, trabajo: () -> Unit) {
        try {
            envios.execute {
                try { trabajo() } catch (e: Exception) { log.warn("No se pudo publicar {} por SSE, tipo={}", que, e.javaClass.simpleName) }
            }
        } catch (e: Exception) {
            log.warn("No se pudo programar {} por SSE, tipo={}", que, e.javaClass.simpleName)
        }
    }

    private class Cliente(val emisor: SseEmitter) {
        var ultimaVersion: Long = -1L
    }
    private class Canal {
        val clientes: MutableSet<Cliente> = LinkedHashSet()
    }
    private val canales = ConcurrentHashMap<String, Canal>()

    fun abrir(slug: String): SseEmitter {
        // Valida que exista el establecimiento ANTES de abrir HTTP 200 streaming.
        instantaneas.ahora(slug)
        val canal = canales.computeIfAbsent(slug) { Canal() }
        val emisor = SseEmitter(0L) // Streaming de larga duración; ping evita inactividad.
        val cliente = Cliente(emisor)
        emisor.onCompletion { desconectar(canal, cliente) }
        emisor.onTimeout { desconectar(canal, cliente); emisor.complete() }
        emisor.onError { desconectar(canal, cliente) }
        synchronized(canal) {
            canal.clientes.add(cliente)
            // Segunda lectura dentro del bloqueo: no pierde el estado entre validación y alta.
            try {
                val estado = instantaneas.ahora(slug)
                enviarAhora(canal, cliente, estado, forzar = true)
            } catch (e: Exception) {
                canal.clientes.remove(cliente)
                throw e
            }
        }
        return emisor
    }

    /** Spring publica CambioColaEvent en la transacción de solicitud, voto o reproducción. */
    @Order(100) // ContratoVersionService (Order(-100)) aumenta la versión primero.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun cambioCola(evento: CambioColaEvent) = enOtroHilo("la cola") {
        val slug = try { instantaneas.slugDeSesion(evento.sesionId) } catch (e: Exception) {
            log.warn("No se pudo identificar el establecimiento para SSE, tipo={}", e.javaClass.simpleName)
            null
        } ?: return@enOtroHilo
        try {
            enviarFoto(slug)
        } catch (e: Exception) {
            log.warn("No se pudo publicar actualización SSE de cola, tipo={}", e.javaClass.simpleName)
        }
        try {
            emitirPanel(slug, setOf("analitica"))
        } catch (e: Exception) {
            log.warn("No se pudo avisar actualización de analítica, tipo={}", e.javaClass.simpleName)
        }
    }

    @Order(100)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun cambioServicio(evento: CambioServicioEvent) = enOtroHilo("el servicio") { enviarFoto(evento.slug) }

    private fun enviarFoto(slug: String) {
        val canal = canales[slug] ?: return
        synchronized(canal) {
            if (canal.clientes.isEmpty()) return
            val estado = instantaneas.ahora(slug)
            canal.clientes.toList().forEach { enviarAhora(canal, it, estado) }
        }
    }

    private val recursosPermitidos = setOf(
        "noche", "yo", "bitacora", "bloqueos", "listas", "horario",
        "usuarios", "establecimiento", "eventos", "pantallas", "analitica",
    )

    /** Preparado para las operaciones del panel; ninguna acción pública lo emite. */
    @Order(100)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun cambioPanel(evento: CambioPanelEvent) = enOtroHilo("el panel") { emitirPanel(evento.slug, evento.recursos) }

    private fun emitirPanel(slug: String, recursosOriginales: Set<String>) {
        val recursos = recursosOriginales.intersect(recursosPermitidos).sorted()
        if (recursos.isEmpty()) return
        val canal = canales[slug] ?: return
        synchronized(canal) {
            canal.clientes.toList().forEach { cliente ->
                emitir(canal, cliente,
                    SseEmitter.event()
                        .name("panel")
                        .id(cliente.ultimaVersion.coerceAtLeast(0).toString())
                        .data(mapOf("tipo" to "panel", "recursos" to recursos), MediaType.APPLICATION_JSON),
                )
            }
        }
    }

    @Scheduled(fixedRate = 15_000L, initialDelay = 15_000L)
    fun ping() {
        for (canal in canales.values) {
            synchronized(canal) {
                canal.clientes.toList().forEach { cliente ->
                    emitir(canal, cliente,
                        SseEmitter.event()
                            .name("ping")
                            .id(cliente.ultimaVersion.coerceAtLeast(0).toString())
                            .data(
                                mapOf("tipo" to "ping", "servidor_ahora" to OffsetDateTime.now(ZoneOffset.UTC)),
                                MediaType.APPLICATION_JSON,
                            ),
                    )
                }
            }
        }
    }

    private fun enviarAhora(canal: Canal, cliente: Cliente, estado: Map<String, Any?>, forzar: Boolean = false) {
        val version = (estado["version"] as? Number)?.toLong() ?: throw ContratoApiException(
            HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El estado no incluye versión",
        )
        // Una sesión cerrada puede reiniciar la versión a 0: también es un cambio real.
        if (!forzar && version == cliente.ultimaVersion) return
        if (emitir(canal, cliente,
                SseEmitter.event()
                    .name("ahora")
                    .id(version.toString())
                    .data(mapOf("tipo" to "ahora", "ahora" to estado), MediaType.APPLICATION_JSON),
            )) {
            cliente.ultimaVersion = version
        }
    }

    private fun emitir(canal: Canal, cliente: Cliente, evento: SseEmitter.SseEventBuilder): Boolean = try {
        cliente.emisor.send(evento)
        true
    } catch (e: Exception) {
        // El cliente desconectado no debe afectar a los demás ni a la operación original.
        canal.clientes.remove(cliente)
        try { cliente.emisor.complete() } catch (_: Exception) {}
        log.debug("Suscripción SSE terminada: {}", e.javaClass.simpleName)
        false
    }

    private fun desconectar(canal: Canal, cliente: Cliente) {
        synchronized(canal) { canal.clientes.remove(cliente) }
    }
}
