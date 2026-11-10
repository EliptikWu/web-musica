package com.proyectoN.rokolaBackend.reproduccion

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.cola.ColaRepository
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaEvent
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Asignador de reproducciones (no es un endpoint).
 * - Reacciona a solicitudes/votos/finalizaciones DESPUÉS del commit.
 * - Recupera sesiones abiertas tras reinicio o cambios de horario cada 5 s.
 * - Consulta YouTube FUERA de la transacción de escritura.
 * - La inserción final bloquea la sesión, valida cola y respeta el índice parcial de BD.
 *
 * PMV de una sola instancia Spring. Los candados reales se toman en PostgreSQL.
 */
@Component
class ReproduccionAutomaticaService(
    private val jdbc: JdbcTemplate,
    private val horarios: HorarioActivoService,
    private val cola: ColaRepository,
    private val youtube: YoutubeApiCliente,
    private val gestion: ReproduccionGestionService,
    @Value("\${ROKOLA_AUTOREPRODUCCION_HABILITADA:true}") private val habilitada: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val tareas = Executors.newSingleThreadExecutor { trabajo ->
        Thread(trabajo, "rokola-reproduccion-auto").apply { isDaemon = true }
    }
    private val enProceso = ConcurrentHashMap<Long, AtomicBoolean>()
    private val reintentarDespues = ConcurrentHashMap<Long, Long>()
    private val intervaloErrorMs = 45_000L

    private data class SesionActiva(val id: Long, val barId: Long, val zona: String, val duracionMax: Int)
    private data class ElementoCasa(val id: Long, val videoId: String)
    private data class ListaCasa(val id: Long, val modo: String, val cursor: Int, val elementos: List<ElementoCasa>)

    @Order(200)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun despuesDeCambio(evento: CambioColaEvent) = programar(evento.sesionId)

    @Scheduled(initialDelay = 3_000L, fixedDelay = 5_000L)
    fun recuperarSesiones() {
        if (!habilitada) return
        try {
            // No hay consultas a YouTube si ya existe reproducción en curso.
            val abiertas = jdbc.query(
                """SELECT s.id_sesion FROM public.sesion s
                   JOIN public.establecimiento e ON e.id_establecimiento = s.id_establecimiento
                   JOIN public.configuracion_servicio cfg ON cfg.id_establecimiento = e.id_establecimiento
                   WHERE s.estado = 'abierta' AND e.activo = TRUE AND cfg.estado_manual = 'disponible'
                     AND NOT EXISTS (
                       SELECT 1 FROM public.reproduccion r
                       WHERE r.id_sesion = s.id_sesion AND r.resultado = 'iniciada'
                     )""".trimIndent(),
                { rs, _ -> rs.getLong("id_sesion") },
            )
            abiertas.forEach(::programar)
        } catch (e: Exception) {
            log.warn("No se pudo comprobar la continuidad musical: {}", e.javaClass.simpleName)
        }
    }

    fun programar(idSesion: Long) {
        if (!habilitada) return
        if (System.currentTimeMillis() < (reintentarDespues[idSesion] ?: 0L)) return
        val bandera = enProceso.computeIfAbsent(idSesion) { AtomicBoolean(false) }
        if (!bandera.compareAndSet(false, true)) return
        try {
            tareas.execute {
                try { asignar(idSesion) }
                catch (e: Exception) {
                    reintentarDespues[idSesion] = System.currentTimeMillis() + intervaloErrorMs
                    log.warn("No se pudo asignar reproducción automática para la sesión {}: {}", idSesion, e.javaClass.simpleName)
                } finally { bandera.set(false) }
            }
        } catch (e: Exception) { bandera.set(false); throw e }
    }

    private fun asignar(idSesion: Long) {
        val sesion = jdbc.query(
            """SELECT s.id_sesion, e.id_establecimiento, e.zona_horaria, cfg.duracion_max_seg
               FROM public.sesion s
               JOIN public.establecimiento e ON e.id_establecimiento = s.id_establecimiento
               JOIN public.configuracion_servicio cfg ON cfg.id_establecimiento = e.id_establecimiento
               WHERE s.id_sesion = ? AND s.estado = 'abierta' AND e.activo = TRUE
                 AND cfg.estado_manual = 'disponible'
                 AND NOT EXISTS (
                   SELECT 1 FROM public.reproduccion r
                   WHERE r.id_sesion = s.id_sesion AND r.resultado = 'iniciada'
                 )""".trimIndent(),
            { rs, _ -> SesionActiva(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4)) },
            idSesion,
        ).firstOrNull() ?: return
        if (!horarios.estaEnHorario(sesion.barId, sesion.zona)) return

        // Primero solicitudes por votos; desempate por antigüedad ya fijado en ColaRepository.
        val primera = cola.listarPendientes(idSesion).firstOrNull()
        if (primera != null) {
            val video = youtube.obtenerVideoPorId(primera.youtubeVideoId)
            if (video == null) {
                // Una canción no incrustable no debe bloquear indefinidamente la cola.
                runCatching { gestion.marcarNoDisponible(idSesion, primera.solicitudId) }
                return
            }
            if (video.duracionSeg > sesion.duracionMax) {
                runCatching { gestion.descartarPendiente(idSesion, primera.solicitudId,
                    DescartarSolicitudRequest("saltada", "El vídeo excede la duración máxima actual")) }
                return
            }
            try {
                gestion.iniciar(idSesion, primera.solicitudId, video)
            } catch (e: ResponseStatusException) {
                // La lista de bloqueos pudo cambiar después de la consulta a YouTube.
                if (e.statusCode == HttpStatus.FORBIDDEN) {
                    runCatching { gestion.descartarPendiente(idSesion, primera.solicitudId,
                        DescartarSolicitudRequest("bloqueada", "Canción bloqueada por el establecimiento")) }
                    return
                }
                // Otra transacción pudo mover la cola; el barrido de recuperación reintenta.
                if (e.statusCode != HttpStatus.CONFLICT) throw e
            }
            return
        }

        val lista = buscarLista(sesion) ?: return
        val elegida = if (lista.modo == "aleatorio") lista.elementos[Random.nextInt(lista.elementos.size)]
            else lista.elementos[RotacionCasa.posicion(lista.cursor, lista.elementos.size)]
        val video = youtube.obtenerVideoPorId(elegida.videoId)
        if (video == null) {
            // Disponibilidad verificada por videos.list, no por una presunción del backend.
            gestion.descartarElementoNoDisponible(idSesion, elegida.id)
            return
        }
        try {
            gestion.iniciarDesdeLista(idSesion, lista.id, elegida.id, video)
        } catch (e: ResponseStatusException) {
            if (e.statusCode != HttpStatus.CONFLICT) throw e
        }
    }

    private fun buscarLista(sesion: SesionActiva): ListaCasa? {
        val cabecera = jdbc.query(
            """SELECT l.id_lista, l.modo_rotacion, s.indice_lista_defecto
               FROM public.lista_defecto l JOIN public.sesion s ON s.id_establecimiento = l.id_establecimiento
               WHERE l.id_establecimiento = ? AND l.activa = TRUE AND s.id_sesion = ?
               ORDER BY l.id_lista DESC LIMIT 1""".trimIndent(),
            { rs, _ -> Triple(rs.getLong(1), rs.getString(2), rs.getInt(3)) },
            sesion.barId, sesion.id,
        ).firstOrNull() ?: return null
        val canciones = jdbc.query(
            """SELECT el.id_elemento, c.youtube_video_id FROM public.elemento_lista el
               JOIN public.cancion c ON c.id_cancion = el.id_cancion
               WHERE el.id_lista = ? AND c.disponibilidad = 'disponible' AND c.embebible = TRUE
                 AND NOT EXISTS (SELECT 1 FROM public.bloqueo_contenido b
                   WHERE b.id_establecimiento = ? AND b.id_cancion = c.id_cancion
                     AND b.activo = TRUE AND b.fecha_inicio <= CURRENT_TIMESTAMP
                     AND (b.fecha_fin IS NULL OR b.fecha_fin > CURRENT_TIMESTAMP))
               ORDER BY el.orden, el.id_elemento""".trimIndent(),
            { rs, _ -> ElementoCasa(rs.getLong(1), rs.getString(2)) },
            cabecera.first, sesion.barId,
        )
        if (canciones.isEmpty()) return null
        return ListaCasa(cabecera.first, cabecera.second, cabecera.third, canciones)
    }

    @PreDestroy
    fun detener() {
        tareas.shutdown()
        if (!tareas.awaitTermination(1, TimeUnit.SECONDS)) tareas.shutdownNow()
    }
}
