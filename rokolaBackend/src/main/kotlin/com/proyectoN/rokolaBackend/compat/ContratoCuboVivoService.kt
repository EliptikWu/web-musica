package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Cubo en vivo del establecimiento. Calcula TODOS los miembros con CUBE en PostgreSQL,
 * a partir de datos operativos, incluyendo sesiones abiertas y categorías recién corregidas.
 * No combina hechos del pipeline para no contar dos veces sesiones cerradas.
 *
 * El backend solo entrega agregados; nunca expone los identificadores de dispositivos.
 */
@Service
class ContratoCuboVivoService(private val jdbc: JdbcTemplate) {

    @Transactional(readOnly = true)
    fun cubo(barId: Long, ejeParam: String?): Map<String, Any?> {
        val eje = ejeParam?.takeIf(String::isNotBlank) ?: "dia"
        if (eje != "dia" && eje != "evento") {
            throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "El tercer eje es día o evento")
        }
        // El nombre SQL está fijado en el backend, nunca se interpola texto arbitrario.
        val columna = if (eje == "dia") "n.dia" else "n.evento"
        val celdas = jdbc.query(sql(columna), { rs, _ -> celda(rs) }, barId)
        val noches = jdbc.queryForObject(
            "SELECT COUNT(*) FROM public.sesion WHERE id_establecimiento = ?", Int::class.java, barId,
        ) ?: 0
        val abiertas = jdbc.queryForObject(
            "SELECT COUNT(*) FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta'",
            Int::class.java, barId,
        ) ?: 0
        val ahora = OffsetDateTime.now(ZoneOffset.UTC).toString()

        fun miembros(campo: String, orden: (String) -> Int): List<String> {
            val pesos = celdas.filter { it[campo] != "*" }
                .groupBy { it[campo].toString() }
                .mapValues { (_, filas) -> filas.sumOf { it["solicitudes"] as Int } }
            return pesos.keys.sortedWith(
                compareBy<String> { orden(it) }.thenByDescending { pesos.getValue(it) }.thenBy { it }
            )
        }

        return mapOf(
            "datos" to mapOf(
                "origen" to "registrado",
                "fuente" to "operativa",
                "en_vivo" to true,
                "actualizado_en" to ahora,
                "noches" to noches,
                "sesiones_abiertas" to abiertas,
                "desde" to null,
                "hasta" to ahora,
            ),
            "eje" to eje,
            "miembros" to mapOf(
                "genero" to miembros("genero") { if (it == "Sin clasificar" || it == "Otros") 1 else 0 },
                "franja" to miembros("franja") { FRANJAS.indexOf(it).let { i -> if (i == -1) 999 else i } },
                "tercero" to miembros("tercero") {
                    val valores = if (eje == "dia") DIAS else EVENTOS
                    valores.indexOf(it).let { i -> if (i == -1) 999 else i }
                },
            ),
            "celdas" to celdas,
        )
    }

    private fun celda(rs: ResultSet): Map<String, Any?> {
        val popularId = rs.getObject("id_cancion_popular") as? Number
        val popular = if (popularId == null) null else mapOf(
            "cancion" to mapOf(
                "video_id" to rs.getString("video_id"),
                "titulo" to rs.getString("titulo"),
                "artista" to rs.getString("artista").orEmpty(),
                "duracion_seg" to rs.getInt("duracion_seg"),
                "miniatura_url" to rs.getString("miniatura_url")?.takeIf(String::isNotBlank),
            ),
            "votos" to rs.getInt("votos_popular"),
            "solicitudes" to rs.getInt("solicitudes_popular"),
        )
        return mapOf(
            "genero" to rs.getString("genero"),
            "franja" to rs.getString("franja"),
            "tercero" to rs.getString("tercero"),
            "solicitudes" to rs.getInt("solicitudes"),
            "votos" to rs.getInt("votos"),
            "telefonos" to rs.getInt("telefonos"),
            "no_sonaron" to rs.getInt("no_sonaron"),
            "espera_promedio_seg" to (rs.getObject("espera_promedio_seg") as? Number)?.toInt(),
            "espera_maxima_seg" to (rs.getObject("espera_maxima_seg") as? Number)?.toInt(),
            "pedidos_seg" to rs.getLong("pedidos_seg"),
            "lista_casa_seg" to rs.getLong("lista_casa_seg"),
            "popular" to popular,
        )
    }

    private fun sql(columna: String) = """
      WITH n AS (
        SELECT s.id_sesion, s.fecha_cierre, e.zona_horaria,
               CASE EXTRACT(ISODOW FROM s.fecha_apertura AT TIME ZONE e.zona_horaria)::int
                   WHEN 1 THEN 'Lunes' WHEN 2 THEN 'Martes' WHEN 3 THEN 'Miércoles'
                   WHEN 4 THEN 'Jueves' WHEN 5 THEN 'Viernes' WHEN 6 THEN 'Sábado'
                   ELSE 'Domingo' END AS dia,
               CASE LOWER(COALESCE(NULLIF(ev.tipo, ''), NULLIF(s.tipo_evento, ''), 'normal'))
                   WHEN 'normal' THEN 'Normal' WHEN 'karaoke' THEN 'Karaoke'
                   WHEN 'partido' THEN 'Partido' WHEN 'tematica' THEN 'Temática'
                   ELSE 'Otro' END AS evento
        FROM public.sesion s
        JOIN public.establecimiento e ON e.id_establecimiento = s.id_establecimiento
        LEFT JOIN public.evento_calendario ev ON ev.id_evento_calendario = s.id_evento_calendario
        WHERE s.id_establecimiento = ?
      ),
      primera AS (
        SELECT r.id_solicitud, MIN(r.fecha_inicio) AS fecha_inicio
        FROM public.reproduccion r
        JOIN n ON n.id_sesion = r.id_sesion
        WHERE r.id_solicitud IS NOT NULL AND r.resultado <> 'fallida'
        GROUP BY r.id_solicitud
      ),
      hechos AS (
        SELECT so.id_cancion,
               COALESCE(NULLIF(BTRIM(c.categoria), ''), 'Sin clasificar') AS genero,
               $FRANJA_SOLICITUD AS franja,
               $columna AS tercero,
               so.id_dispositivo_solicitante AS dispositivo,
               1 AS es_solicitud, 0 AS es_voto,
               CASE WHEN n.fecha_cierre IS NOT NULL AND so.estado NOT IN ('reproducida', 'reproduciendo')
                    THEN 1 ELSE 0 END AS no_sono,
               CASE WHEN pr.fecha_inicio IS NOT NULL
                    THEN GREATEST(0, EXTRACT(EPOCH FROM (pr.fecha_inicio - so.fecha_solicitud))::int)
                    ELSE NULL END AS espera_seg,
               0::bigint AS pedidos_seg, 0::bigint AS lista_casa_seg
        FROM public.solicitud so
        JOIN n ON n.id_sesion = so.id_sesion
        JOIN public.cancion c ON c.id_cancion = so.id_cancion
        LEFT JOIN primera pr ON pr.id_solicitud = so.id_solicitud
        UNION ALL
        SELECT so.id_cancion,
               COALESCE(NULLIF(BTRIM(c.categoria), ''), 'Sin clasificar'),
               $FRANJA_VOTO, $columna,
               v.id_dispositivo, 0, 1, 0, NULL::int, 0::bigint, 0::bigint
        FROM public.voto v
        JOIN public.solicitud so ON so.id_solicitud = v.id_solicitud
        JOIN n ON n.id_sesion = so.id_sesion
        JOIN public.cancion c ON c.id_cancion = so.id_cancion
        WHERE v.activo = TRUE
        UNION ALL
        SELECT r.id_cancion,
               COALESCE(NULLIF(BTRIM(c.categoria), ''), 'Sin clasificar'),
               $FRANJA_REPRODUCCION, $columna,
               NULL::bigint, 0, 0, 0, NULL::int,
               CASE WHEN r.id_solicitud IS NOT NULL THEN GREATEST(0, COALESCE(r.segundos_reproducidos, 0)) ELSE 0 END,
               CASE WHEN r.id_solicitud IS NULL THEN GREATEST(0, COALESCE(r.segundos_reproducidos, 0)) ELSE 0 END
        FROM public.reproduccion r
        JOIN n ON n.id_sesion = r.id_sesion
        JOIN public.cancion c ON c.id_cancion = r.id_cancion
      ),
      agrupado AS (
        SELECT CASE WHEN GROUPING(genero) = 1 THEN '*' ELSE genero END AS genero,
               CASE WHEN GROUPING(franja) = 1 THEN '*' ELSE franja END AS franja,
               CASE WHEN GROUPING(tercero) = 1 THEN '*' ELSE tercero END AS tercero,
               SUM(es_solicitud)::int AS solicitudes,
               SUM(es_voto)::int AS votos,
               COUNT(DISTINCT dispositivo)::int AS telefonos,
               SUM(no_sono)::int AS no_sonaron,
               ROUND(AVG(espera_seg))::int AS espera_promedio_seg,
               MAX(espera_seg) AS espera_maxima_seg,
               SUM(pedidos_seg)::bigint AS pedidos_seg,
               SUM(lista_casa_seg)::bigint AS lista_casa_seg
        FROM hechos GROUP BY CUBE(genero, franja, tercero)
      ),
      popular_agrupado AS (
        SELECT CASE WHEN GROUPING(genero) = 1 THEN '*' ELSE genero END AS genero,
               CASE WHEN GROUPING(franja) = 1 THEN '*' ELSE franja END AS franja,
               CASE WHEN GROUPING(tercero) = 1 THEN '*' ELSE tercero END AS tercero,
               id_cancion,
               SUM(es_voto)::int AS votos_popular,
               SUM(es_solicitud)::int AS solicitudes_popular
        FROM hechos
        WHERE es_solicitud = 1 OR es_voto = 1
        GROUP BY id_cancion, CUBE(genero, franja, tercero)
      ),
      populares AS (
        SELECT *, ROW_NUMBER() OVER (
            PARTITION BY genero, franja, tercero
            ORDER BY votos_popular DESC, solicitudes_popular DESC, id_cancion
        ) AS posicion
        FROM popular_agrupado
      )
      SELECT a.*, p.id_cancion AS id_cancion_popular,
             COALESCE(p.votos_popular, 0) AS votos_popular,
             COALESCE(p.solicitudes_popular, 0) AS solicitudes_popular,
             c.youtube_video_id AS video_id, c.titulo, c.artista, c.duracion_seg, c.miniatura_url
      FROM agrupado a
      LEFT JOIN populares p ON p.genero = a.genero AND p.franja = a.franja
                            AND p.tercero = a.tercero AND p.posicion = 1
      LEFT JOIN public.cancion c ON c.id_cancion = p.id_cancion
      ORDER BY a.genero, a.franja, a.tercero
    """.trimIndent()

    companion object {
        private val FRANJAS = listOf("Antes de las 9", "9 a 11 p. m.", "11 p. m. a 1 a. m.", "Después de la 1")
        private val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        private val EVENTOS = listOf("Normal", "Karaoke", "Partido", "Temática", "Otro")

        private fun expresionFranja(fecha: String) = """
            CASE WHEN EXTRACT(HOUR FROM $fecha AT TIME ZONE n.zona_horaria)::int IN (21, 22)
                  THEN '9 a 11 p. m.'
                 WHEN EXTRACT(HOUR FROM $fecha AT TIME ZONE n.zona_horaria)::int IN (23, 0)
                  THEN '11 p. m. a 1 a. m.'
                 WHEN EXTRACT(HOUR FROM $fecha AT TIME ZONE n.zona_horaria)::int BETWEEN 1 AND 5
                  THEN 'Después de la 1'
                 ELSE 'Antes de las 9' END
        """.trimIndent()
        private val FRANJA_SOLICITUD = expresionFranja("so.fecha_solicitud")
        private val FRANJA_VOTO = expresionFranja("v.fecha_voto")
        private val FRANJA_REPRODUCCION = expresionFranja("r.fecha_inicio")
    }
}
