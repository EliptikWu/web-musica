package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.cancion.ClasificadorGeneroService
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import org.springframework.transaction.annotation.Transactional

/** Acceso a géneros desde el panel autenticado: catálogo global, consultas filtradas por bar. */
@Service
class ContratoGenerosService(
    private val jdbc: JdbcTemplate,
    private val clasificador: ClasificadorGeneroService,
    private val youtube: YoutubeApiCliente,
    private val bitacora: ContratoBitacora,
    private val panel: CambioPanelPublisher,
) {
    fun catalogo(): List<String> = clasificador.generos()

    /** Canciones usadas por el bar: solicitudes, listas de la casa, bloqueos y reproducciones.
     * El catálogo de canciones sigue siendo global; los permisos se comprueban por establecimiento.
     */
    @Transactional(readOnly = true)
    fun canciones(p: ContratoPanelAcceso.Personal, soloSinCategoria: Boolean, q: String, limite: Int): List<Map<String, Any?>> {
        val filtro = q.trim().take(100).lowercase()
        val maximo = limite.coerceIn(1, 100)
        return jdbc.query(
            """WITH catalogo_bar AS ($CANCIONES_DEL_BAR),
                      pedidos AS (
                         SELECT s.id_cancion, COUNT(*) AS total
                         FROM public.solicitud s
                         JOIN public.sesion se ON se.id_sesion = s.id_sesion
                         WHERE se.id_establecimiento = ?
                         GROUP BY s.id_cancion
                      )
               SELECT c.id_cancion, c.youtube_video_id, c.titulo, c.artista,
                      c.categoria, c.origen_categoria, COALESCE(pedidos.total, 0) AS pedidos
               FROM catalogo_bar cb
               JOIN public.cancion c ON c.id_cancion = cb.id_cancion
               LEFT JOIN pedidos ON pedidos.id_cancion = c.id_cancion
               WHERE (NOT ? OR c.categoria IS NULL)
                 AND (? = '' OR STRPOS(LOWER(c.titulo), ?) > 0 OR STRPOS(LOWER(COALESCE(c.artista, '')), ?) > 0)
               ORDER BY CASE WHEN c.categoria IS NULL THEN 0 ELSE 1 END, pedidos DESC, c.titulo
               LIMIT ?""",
            { rs, _ -> mapOf(
                "id" to rs.getLong("id_cancion").toString(),
                "youtube_video_id" to rs.getString("youtube_video_id"),
                "titulo" to rs.getString("titulo"),
                "artista" to rs.getString("artista"),
                "categoria" to rs.getString("categoria"),
                "origen_categoria" to rs.getString("origen_categoria"),
                "pedidos" to rs.getLong("pedidos"),
            ) },
            p.barId, p.barId, p.barId, p.barId, p.barId,
            soloSinCategoria, filtro, filtro, filtro, maximo,
        )
    }

    /** Protege los bares ajenos al corregir una canción del catálogo compartido. */
    @Transactional
    fun corregir(p: ContratoPanelAcceso.Personal, id: Long, categoriaIngresada: String?): Map<String, Any?> {
        val categoria = categoriaIngresada?.let(clasificador::canonico)
            ?: throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Selecciona un género válido del catálogo")
        val resultados = jdbc.query(
            """UPDATE public.cancion c SET categoria = ?, origen_categoria = 'MANUAL'
               WHERE c.id_cancion = ? AND c.id_cancion IN ($CANCIONES_DEL_BAR)
               RETURNING c.id_cancion, c.categoria, c.origen_categoria""",
            { rs, _ -> mapOf(
                "id" to rs.getLong("id_cancion").toString(),
                "categoria" to rs.getString("categoria"),
                "origen_categoria" to rs.getString("origen_categoria"),
            ) }, categoria, id, p.barId, p.barId, p.barId, p.barId,
        )
        val corregida = resultados.firstOrNull()
            ?: throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Canción no encontrada en el bar")
        bitacora.anotar(p, "editar_categoria", "cancion", id, texto = "Asignó género: $categoria")
        avisarCuboDeCancion(id)
        panel.publicar(p.barSlug, setOf("bitacora"))
        return corregida
    }

    /** Reclasificación a petición del administrador, en lotes de hasta diez videos.
     * No se ejecuta al arrancar, no modifica las revisiones manuales ni repite videos por paginación.
     * Si los metadatos no son concluyentes, la canción permanece sin categoría.
     */
    fun reclasificar(p: ContratoPanelAcceso.Personal, limite: Int, despuesDe: Long): Map<String, Any?> {
        if (despuesDe < 0) throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "despuesDe debe ser >= 0")
        val candidatos = jdbc.query(
            """WITH catalogo_bar AS ($CANCIONES_DEL_BAR)
               SELECT c.id_cancion, c.youtube_video_id FROM catalogo_bar cb
               JOIN public.cancion c ON c.id_cancion = cb.id_cancion
               WHERE c.categoria IS NULL AND c.origen_categoria IS DISTINCT FROM 'MANUAL'
                 AND c.id_cancion > ?
               ORDER BY c.id_cancion LIMIT ?""",
            { rs, _ -> rs.getLong("id_cancion") to rs.getString("youtube_video_id") },
            p.barId, p.barId, p.barId, p.barId, despuesDe, limite.coerceIn(1, 10),
        )
        var ultimo = despuesDe
        var clasificadas = 0
        val idsClasificados = mutableListOf<Long>()
        val porOrigen = mutableMapOf("ETIQUETAS" to 0, "TITULO" to 0, "DESCRIPCION" to 0)
        var sinCoincidencia = 0
        var noDisponibles = 0
        var interrumpida: String? = null
        for ((id, videoId) in candidatos) {
            // Si YouTube falla, el cursor no avanza más allá de ese video: podrá reintentarse.
            val video = try {
                youtube.obtenerVideoPorId(videoId)
            } catch (e: ResponseStatusException) {
                interrumpida = if (e.statusCode.value() == 429) "cuota_agotada" else "youtube_no_disponible"
                break
            }
            if (video == null) {
                noDisponibles++
                ultimo = id
                continue
            }
            val clasificacion = clasificador.clasificar(video.etiquetas, video.titulo, video.descripcion)
            val genero = clasificacion?.genero
            if (genero == null) {
                // El video sí existe: guardamos solo el metadato general, nunca inventamos un género.
                jdbc.update(
                    """UPDATE public.cancion SET categoria_youtube = COALESCE(?, categoria_youtube)
                       WHERE id_cancion = ? AND categoria IS NULL AND origen_categoria IS DISTINCT FROM 'MANUAL'""",
                    video.categoriaYoutube?.take(100), id,
                )
                sinCoincidencia++
            } else {
                val origen = requireNotNull(clasificacion).origen
                val actualizadas = jdbc.update(
                    """UPDATE public.cancion SET categoria = ?, origen_categoria = ?,
                              categoria_youtube = COALESCE(?, categoria_youtube)
                       WHERE id_cancion = ? AND categoria IS NULL AND origen_categoria IS DISTINCT FROM 'MANUAL'
                         AND id_cancion IN ($CANCIONES_DEL_BAR)""",
                    genero, origen, video.categoriaYoutube?.take(100), id,
                    p.barId, p.barId, p.barId, p.barId,
                )
                if (actualizadas > 0) {
                    clasificadas++
                    idsClasificados += id
                    porOrigen[origen] = porOrigen.getValue(origen) + 1
                    bitacora.anotar(p, "editar_categoria", "cancion", id, texto = "Clasificación $origen: $genero")
                }
            }
            ultimo = id
        }
        if (clasificadas > 0) {
            idsClasificados.forEach(::avisarCuboDeCancion)
            panel.publicar(p.barSlug, setOf("bitacora"))
        }
        return mapOf(
            "consultadas" to (clasificadas + sinCoincidencia + noDisponibles),
            "clasificadas" to clasificadas,
            "clasificadas_por_origen" to porOrigen,
            "sin_coincidencia" to sinCoincidencia,
            "no_disponibles" to noDisponibles,
            "siguiente_desde_id" to ultimo,
            "hay_mas" to (interrumpida == null && candidatos.size == limite.coerceIn(1, 10)),
            "interrumpida" to interrumpida,
        )
    }

    /** La corrección se reflejará en el cubo cuando se ejecute de nuevo el pipeline completo. */
    @Transactional(readOnly = true)
    fun estadoAnalitica(@Suppress("UNUSED_PARAMETER") p: ContratoPanelAcceso.Personal): Map<String, Any?> {
        val ultimaCarga = jdbc.queryForObject(
            "SELECT MAX(terminada_en) FROM analitica.carga", OffsetDateTime::class.java,
        )
        val ultimaCorreccion = jdbc.queryForObject(
            // La categoría es global: un administrador de otro bar también puede cambiarla.
            "SELECT MAX(fecha) FROM public.accion_admin WHERE tipo = 'editar_categoria'",
            OffsetDateTime::class.java,
        )
        return mapOf(
            "ultima_carga" to ultimaCarga,
            "ultima_correccion" to ultimaCorreccion,
            "pendiente_actualizacion" to (ultimaCorreccion != null && (ultimaCarga == null || ultimaCorreccion.isAfter(ultimaCarga))),
            "hay_carga" to (ultimaCarga != null),
        )
    }

    /** Una categoría es global: avisa a todos los bares que usan la canción. No envía datos privados. */
    private fun avisarCuboDeCancion(idCancion: Long) {
        val slugs = jdbc.query(
            """SELECT e.slug FROM public.establecimiento e
               WHERE e.activo = TRUE AND (
                 EXISTS (SELECT 1 FROM public.solicitud so JOIN public.sesion se ON se.id_sesion = so.id_sesion
                         WHERE so.id_cancion = ? AND se.id_establecimiento = e.id_establecimiento)
                 OR EXISTS (SELECT 1 FROM public.reproduccion r JOIN public.sesion se ON se.id_sesion = r.id_sesion
                            WHERE r.id_cancion = ? AND se.id_establecimiento = e.id_establecimiento)
                 OR EXISTS (SELECT 1 FROM public.elemento_lista el JOIN public.lista_defecto l ON l.id_lista = el.id_lista
                            WHERE el.id_cancion = ? AND l.id_establecimiento = e.id_establecimiento)
                 OR EXISTS (SELECT 1 FROM public.bloqueo_contenido b
                            WHERE b.id_cancion = ? AND b.id_establecimiento = e.id_establecimiento)
               )""",
            { rs, _ -> rs.getString("slug") },
            idCancion, idCancion, idCancion, idCancion,
        )
        slugs.forEach { panel.publicar(it, setOf("analitica")) }
    }

    /** Agrupaciones separadas: no multiplica solicitudes al contar votos. */
    @Transactional(readOnly = true)
    fun resumen(p: ContratoPanelAcceso.Personal, dias: Int): List<Map<String, Any?>> {
        val ventana = dias.coerceIn(1, 365)
        return jdbc.query(
            """WITH pedidas AS (
                 SELECT COALESCE(c.categoria, 'Sin clasificar') AS genero, COUNT(*) AS solicitudes
                 FROM public.solicitud s
                 JOIN public.sesion se ON se.id_sesion = s.id_sesion
                 JOIN public.cancion c ON c.id_cancion = s.id_cancion
                 WHERE se.id_establecimiento = ?
                   AND s.fecha_solicitud >= CURRENT_TIMESTAMP - (? * INTERVAL '1 day')
                 GROUP BY COALESCE(c.categoria, 'Sin clasificar')
               ), votadas AS (
                 SELECT COALESCE(c.categoria, 'Sin clasificar') AS genero, COUNT(*) AS votos_activos
                 FROM public.voto v
                 JOIN public.solicitud s ON s.id_solicitud = v.id_solicitud
                 JOIN public.sesion se ON se.id_sesion = s.id_sesion
                 JOIN public.cancion c ON c.id_cancion = s.id_cancion
                 WHERE se.id_establecimiento = ? AND v.activo = TRUE
                   AND v.fecha_voto >= CURRENT_TIMESTAMP - (? * INTERVAL '1 day')
                 GROUP BY COALESCE(c.categoria, 'Sin clasificar')
               )
               SELECT COALESCE(p.genero, v.genero) AS genero,
                      COALESCE(p.solicitudes, 0) AS solicitudes,
                      COALESCE(v.votos_activos, 0) AS votos_activos
               FROM pedidas p FULL JOIN votadas v ON v.genero = p.genero
               ORDER BY solicitudes DESC, votos_activos DESC, genero""",
            { rs, _ -> mapOf(
                "genero" to rs.getString("genero"),
                "solicitudes" to rs.getLong("solicitudes"),
                "votos_activos" to rs.getLong("votos_activos"),
            ) }, p.barId, ventana, p.barId, ventana,
        )
    }

    private companion object {
        // Un mismo video se guarda una sola vez, pero puede usarse por varios establecimientos.
        // UNION evita repetir canciones presentes simultáneamente en solicitudes y listas.
        const val CANCIONES_DEL_BAR = """
            SELECT s.id_cancion FROM public.solicitud s
            JOIN public.sesion se ON se.id_sesion = s.id_sesion WHERE se.id_establecimiento = ?
            UNION
            SELECT el.id_cancion FROM public.elemento_lista el
            JOIN public.lista_defecto l ON l.id_lista = el.id_lista WHERE l.id_establecimiento = ?
            UNION
            SELECT b.id_cancion FROM public.bloqueo_contenido b WHERE b.id_establecimiento = ?
            UNION
            SELECT r.id_cancion FROM public.reproduccion r
            JOIN public.sesion se ON se.id_sesion = r.id_sesion WHERE se.id_establecimiento = ?
        """
    }

}
