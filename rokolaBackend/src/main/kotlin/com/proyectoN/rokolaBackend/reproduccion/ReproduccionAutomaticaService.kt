package com.proyectoN.rokolaBackend.reproduccion

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.cola.ColaRepository
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaEvent
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import com.proyectoN.rokolaBackend.youtube.YoutubeVideoResponse
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
    fun despuesDeCambio(evento: CambioColaEvent) {
        // Un cambio de volumen (RF-062) solo refresca la foto: no decide qué suena.
        if (evento.tipo != "volumen") programar(evento.sesionId)
    }

    @Scheduled(initialDelay = 3_000L, fixedDelay = 5_000L)
    fun recuperarSesiones() {
        if (!habilitada) return
        try {
            // No hay consultas a YouTube si ya existe reproducción en curso.
            val abiertas = jdbc.query(
                """SELECT s.id_sesion FROM public.sesion s
                   JOIN public.establecimiento e ON e.id_establecimiento = s.id_establecimiento
                   JOIN public.configuracion_servicio cfg ON cfg.id_establecimiento = e.id_establecimiento
                   WHERE s.estado = 'abierta' AND e.activo = TRUE AND cfg.estado_manual IN ('disponible', 'pausado')
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

    /**
     * El video de un pedido, listo para sonar. Pedir ya lo validó con YouTube; si fue hace poco,
     * salen los datos guardados y no se espera otra consulta: al saltar, la siguiente arranca sin la
     * ida y vuelta a YouTube y no gasta cuota (doc 04, punto 27; T10, 2026-11-25). Si no, se consulta
     * YouTube como siempre. Solo para la cola: `iniciar` vuelve a marcar la validación al sonar, y en
     * una lista de la casa corta eso encadenaría reproducciones sin preguntar nunca a YouTube.
     */
    private fun videoVerificado(videoId: String): YoutubeVideoResponse? =
        jdbc.query(
            """SELECT youtube_video_id, titulo, artista, canal, duracion_seg, miniatura_url FROM public.cancion
               WHERE youtube_video_id = ? AND embebible AND disponibilidad = 'disponible' AND duracion_seg > 0
                 AND ultima_validacion > CURRENT_TIMESTAMP - make_interval(mins => ?)""",
            { rs, _ ->
                val id = rs.getString("youtube_video_id")
                YoutubeVideoResponse(
                    youtubeVideoId = id,
                    titulo = rs.getString("titulo"),
                    artista = rs.getString("artista"),
                    artistaEstimado = false,
                    canal = rs.getString("canal").orEmpty(),
                    duracionSeg = rs.getInt("duracion_seg"),
                    miniaturaUrl = rs.getString("miniatura_url"),
                    embebible = true,
                    url = "https://www.youtube.com/watch?v=$id",
                )
            },
            videoId, VALIDEZ_MIN,
        ).firstOrNull() ?: youtube.obtenerVideoPorId(videoId)

    private fun asignar(idSesion: Long) {
        val sesion = jdbc.query(
            """SELECT s.id_sesion, e.id_establecimiento, e.zona_horaria, cfg.duracion_max_seg
               FROM public.sesion s
               JOIN public.establecimiento e ON e.id_establecimiento = s.id_establecimiento
               JOIN public.configuracion_servicio cfg ON cfg.id_establecimiento = e.id_establecimiento
               WHERE s.id_sesion = ? AND s.estado = 'abierta' AND e.activo = TRUE
                 -- Con las solicitudes en pausa la cola y la lista de la casa siguen sonando
                 -- (RF-039, decisión D24): solo el mantenimiento la calla.
                 AND cfg.estado_manual IN ('disponible', 'pausado')
                 AND NOT EXISTS (
                   SELECT 1 FROM public.reproduccion r
                   WHERE r.id_sesion = s.id_sesion AND r.resultado = 'iniciada'
                 )""".trimIndent(),
            { rs, _ -> SesionActiva(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4)) },
            idSesion,
        ).firstOrNull() ?: return
        if (!horarios.atiende(sesion.barId, sesion.zona)) return

        // Primero solicitudes por votos; desempate por antigüedad ya fijado en ColaRepository.
        val primera = cola.listarPendientes(idSesion).firstOrNull()
        if (primera != null) {
            val video = videoVerificado(primera.youtubeVideoId)
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
        // Una de la lista que la TV ya no pudo poner (error 101 o 150) se descarta como no disponible.
        val restringida = jdbc.query(
            "SELECT 1 FROM public.cancion WHERE youtube_video_id = ? AND disponibilidad = 'restringida'",
            { _, _ -> true }, elegida.videoId,
        ).isNotEmpty()
        val video = if (restringida) null else youtube.obtenerVideoPorId(elegida.videoId)
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

    companion object {
        /** Cuánto vale una validación de YouTube para poner la canción sin volver a preguntar *(propuesto)*. */
        internal const val VALIDEZ_MIN = 30
    }
}
