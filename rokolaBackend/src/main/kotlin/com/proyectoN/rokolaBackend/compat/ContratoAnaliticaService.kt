package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Lee el modelo analítico que carga el pipeline de Spark (ADR-27). No calcula sobre las tablas
 * operativas, salvo la lista activa y los bloqueos vigentes de las recomendaciones (RF-054).
 *
 * Hecho por el equipo de frontend (2026-11-20, D14 y D15) sobre el esquema reconstruido.
 */
@Service
class ContratoAnaliticaService(
  private val jdbc: JdbcTemplate,
  private val panel: CambioPanelPublisher,
) {
  data class Filtro(val noche: String? = null, val desde: String? = null, val hasta: String? = null, val dia: String? = null)

  private data class Noche(
    val id: Long, val inicio: OffsetDateTime, val fin: OffsetDateTime, val fecha: LocalDate, val dia: Int,
    val eventoId: Long?, val eventoNombre: String?, val tipo: String,
  )

  private data class PorNoche(val solicitudes: Int, val votos: Int, val telefonos: Int, val pedidosSeg: Long, val casaSeg: Long, val esperaSuma: Long, val esperaN: Int)

  // ── Noches y origen de los datos (RNF-015) ─────────────────────────────────

  private fun todas(barId: Long): List<Noche> = jdbc.query(
    """SELECT id_sesion, inicio, fin, fecha_local, dia_semana, id_evento, evento_nombre, tipo_evento
       FROM analitica.dim_noche WHERE id_establecimiento = ? ORDER BY inicio""",
    { rs, _ ->
      Noche(
        rs.getLong("id_sesion"), rs.getObject("inicio", OffsetDateTime::class.java), rs.getObject("fin", OffsetDateTime::class.java),
        rs.getObject("fecha_local", LocalDate::class.java), rs.getInt("dia_semana"), rs.getObject("id_evento") as? Long,
        rs.getString("evento_nombre"), rs.getString("tipo_evento"),
      )
    },
    barId,
  )

  private fun fechaFiltro(valor: String?): LocalDate? {
    if (valor.isNullOrBlank()) return null
    if (!FECHA.matches(valor)) throw invalido("Fechas en formato AAAA-MM-DD")
    return try { LocalDate.parse(valor) } catch (_: DateTimeParseException) { throw invalido("Fechas en formato AAAA-MM-DD") }
  }

  private fun seleccionar(lista: List<Noche>, f: Filtro): List<Noche> {
    val desde = fechaFiltro(f.desde)
    val hasta = fechaFiltro(f.hasta)
    val dia = f.dia?.takeIf { it.isNotBlank() }?.let { it.toIntOrNull()?.takeIf { d -> d in 1..7 } ?: throw invalido("Día de la semana de 1 a 7") }
    if (!f.noche.isNullOrBlank()) {
      val id = f.noche.toLongOrNull()
      return listOf(lista.firstOrNull { it.id == id } ?: throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Esa noche no está en el tablero"))
    }
    return lista.filter { (desde == null || it.fecha >= desde) && (hasta == null || it.fecha <= hasta) && (dia == null || it.dia == dia) }
  }

  /** El rótulo es de cada bar: los datos de ejemplo de uno no marcan los registros de otro. */
  private fun origen(barId: Long, elegidas: List<Noche>): Map<String, Any?> {
    val cargada = jdbc.query(
      "SELECT terminada_en FROM analitica.carga ORDER BY id_carga DESC LIMIT 1",
      { rs, _ -> rs.getObject(1, OffsetDateTime::class.java) },
    ).firstOrNull()
    val conEjemplo = jdbc.queryForObject(
      """SELECT EXISTS (
           SELECT 1 FROM analitica.hecho_solicitud h JOIN analitica.dim_cancion c ON c.id_cancion = h.id_cancion
           WHERE h.id_establecimiento = ? AND c.video_id LIKE 'ejemplo-%'
         ) OR EXISTS (
           SELECT 1 FROM analitica.hecho_reproduccion r JOIN analitica.dim_cancion c ON c.id_cancion = r.id_cancion
           WHERE r.id_establecimiento = ? AND c.video_id LIKE 'ejemplo-%'
         )""",
      Boolean::class.java, barId, barId,
    ) == true
    return mapOf(
      "origen" to if (conEjemplo) "ejemplo" else "registrado",
      "actualizado_en" to cargada?.let(::iso),
      "noches" to elegidas.size,
      "desde" to elegidas.firstOrNull()?.let { iso(it.inicio) },
      "hasta" to elegidas.lastOrNull()?.let { iso(it.inicio) },
    )
  }

  private fun evento(n: Noche): Map<String, Any?>? =
    n.eventoId?.let { mapOf("id" to it.toString(), "nombre" to (n.eventoNombre ?: "Evento"), "tipo_evento" to n.tipo) }

  @Transactional(readOnly = true)
  fun noches(p: ContratoPanelAcceso.Personal): Map<String, Any?> {
    val lista = todas(p.barId)
    return mapOf(
      "datos" to origen(p.barId, lista),
      "noches" to lista.map { mapOf("id" to it.id.toString(), "inicio" to iso(it.inicio), "fin" to iso(it.fin), "dia_semana" to it.dia, "evento" to evento(it)) },
    )
  }

  // ── RF-049, RF-050 ─────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  fun resumen(p: ContratoPanelAcceso.Personal, f: Filtro): Map<String, Any?> {
    val elegidas = seleccionar(todas(p.barId), f)
    val ids = idsDe(elegidas)
    val base = jdbc.queryForMap(
      """SELECT COUNT(*) AS solicitudes, COALESCE(SUM(votos), 0) AS votos,
                COUNT(*) FILTER (WHERE causa_no_sono IS NOT NULL) AS no_sonaron,
                ROUND(AVG(espera_seg)) AS promedio, MAX(espera_seg) AS maxima
         FROM analitica.hecho_solicitud WHERE id_sesion IN ($ids)""",
    )
    val telefonos = jdbc.queryForObject(
      """SELECT COUNT(DISTINCT dispositivo_seud) FROM (
           SELECT dispositivo_seud FROM analitica.hecho_solicitud WHERE id_sesion IN ($ids)
           UNION ALL SELECT dispositivo_seud FROM analitica.hecho_voto WHERE id_sesion IN ($ids)) t""",
      Int::class.java,
    ) ?: 0
    val popular = jdbc.query(
      """SELECT $CANCION, SUM(h.votos) AS votos, COUNT(*) AS veces
         FROM analitica.hecho_solicitud h JOIN analitica.dim_cancion c ON c.id_cancion = h.id_cancion
         WHERE h.id_sesion IN ($ids)
         GROUP BY c.id_cancion, c.video_id, c.titulo, c.artista, c.duracion_seg, c.miniatura_url
         ORDER BY votos DESC, veces DESC, c.id_cancion LIMIT 1""",
      { rs, _ -> mapOf("cancion" to cancion(rs), "votos" to rs.getInt("votos"), "solicitudes" to rs.getInt("veces")) },
    ).firstOrNull()
    val tiempos = jdbc.query(
      "SELECT origen, COALESCE(SUM(segundos), 0) FROM analitica.hecho_reproduccion WHERE id_sesion IN ($ids) GROUP BY origen",
      { rs, _ -> rs.getString(1) to rs.getLong(2) },
    ).toMap()
    val historial = if (elegidas.size != 1) emptyList() else jdbc.query(
      """SELECT $CANCION, r.inicio, r.origen, r.votos
         FROM analitica.hecho_reproduccion r JOIN analitica.dim_cancion c ON c.id_cancion = r.id_cancion
         WHERE r.id_sesion IN ($ids) ORDER BY r.inicio""",
      { rs, _ -> mapOf("inicio" to iso(rs.getObject("inicio", OffsetDateTime::class.java)), "cancion" to cancion(rs), "origen" to rs.getString("origen"), "votos" to rs.getInt("votos")) },
    )
    val promedio = (base["promedio"] as Number?)?.toInt()
    return mapOf(
      "datos" to origen(p.barId, elegidas),
      "solicitudes" to (base["solicitudes"] as Number).toInt(),
      "telefonos" to telefonos,
      "votos" to (base["votos"] as Number).toInt(),
      "mas_popular" to popular,
      "espera" to promedio?.let { mapOf("promedio_seg" to it, "maxima_seg" to (base["maxima"] as Number).toInt()) },
      "tiempo" to mapOf("pedidos_seg" to (tiempos["solicitud"] ?: 0L), "lista_casa_seg" to (tiempos["lista_casa"] ?: 0L)),
      "no_sonaron" to (base["no_sonaron"] as Number).toInt(),
      "historial" to historial,
    )
  }

  // ── RF-051 ─────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  fun participacion(p: ContratoPanelAcceso.Personal, f: Filtro): Map<String, Any?> {
    val elegidas = seleccionar(todas(p.barId), f)
    val ids = idsDe(elegidas)
    val generos = jdbc.query(
      """SELECT c.genero, COUNT(*) AS solicitudes, COALESCE(SUM(h.votos), 0) AS votos
         FROM analitica.hecho_solicitud h JOIN analitica.dim_cancion c ON c.id_cancion = h.id_cancion
         WHERE h.id_sesion IN ($ids) GROUP BY c.genero
         ORDER BY (c.genero IS NULL), solicitudes DESC, c.genero""",
      { rs, _ -> mapOf("genero" to rs.getString("genero"), "solicitudes" to rs.getInt("solicitudes"), "votos" to rs.getInt("votos")) },
    )
    val franjas = jdbc.query(
      """SELECT hora_local, SUM(s) AS solicitudes, SUM(v) AS votos, COUNT(DISTINCT dispositivo_seud) AS telefonos FROM (
           SELECT hora_local, dispositivo_seud, 1 AS s, 0 AS v FROM analitica.hecho_solicitud WHERE id_sesion IN ($ids)
           UNION ALL
           SELECT hora_local, dispositivo_seud, 0, 1 FROM analitica.hecho_voto WHERE id_sesion IN ($ids)
         ) t GROUP BY hora_local ORDER BY (hora_local + 12) % 24""",
      { rs, _ -> mapOf("hora" to rs.getInt("hora_local"), "solicitudes" to rs.getInt("solicitudes"), "votos" to rs.getInt("votos"), "telefonos" to rs.getInt("telefonos")) },
    )
    return mapOf("datos" to origen(p.barId, elegidas), "generos" to generos, "franjas" to franjas)
  }

  // ── RF-052 ─────────────────────────────────────────────────────────────────

  fun umbrales(barId: Long): Map<String, Int> = jdbc.query(
    "SELECT votos_minimos, concentracion_pct, participacion_minima FROM analitica.umbrales WHERE id_establecimiento = ?",
    { rs, _ -> mapOf("votos_minimos" to rs.getInt(1), "concentracion_pct" to rs.getInt(2), "participacion_minima" to rs.getInt(3)) },
    barId,
  ).firstOrNull() ?: UMBRALES_POR_DEFECTO

  @Transactional
  fun guardarUmbrales(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Int> {
    val votos = enteroEn(cuerpo, "votos_minimos", 1..500)
    val concentracion = enteroEn(cuerpo, "concentracion_pct", 10..100)
    val participacion = enteroEn(cuerpo, "participacion_minima", 1..1000)
    jdbc.update(
      """INSERT INTO analitica.umbrales (id_establecimiento, votos_minimos, concentracion_pct, participacion_minima)
         VALUES (?, ?, ?, ?)
         ON CONFLICT (id_establecimiento) DO UPDATE SET votos_minimos = EXCLUDED.votos_minimos,
           concentracion_pct = EXCLUDED.concentracion_pct, participacion_minima = EXCLUDED.participacion_minima""",
      p.barId, votos, concentracion, participacion,
    )
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, 'editar_umbrales', NULL, NULL, jsonb_build_object('texto', ?::text))""",
      p.barId, p.id, "$votos votos para «muy pedida», $concentracion % de concentración y $participacion teléfonos por noche",
    )
    panel.publicar(p.barSlug, setOf("bitacora"))
    return mapOf("votos_minimos" to votos, "concentracion_pct" to concentracion, "participacion_minima" to participacion)
  }

  @Transactional(readOnly = true)
  fun diagnostico(p: ContratoPanelAcceso.Personal, f: Filtro): Map<String, Any?> {
    val elegidas = seleccionar(todas(p.barId), f)
    val ids = idsDe(elegidas)
    val limites = umbrales(p.barId)
    val inicioDe = elegidas.associate { it.id to iso(it.inicio) }
    val noSonaron = jdbc.query(
      """SELECT $CANCION, h.id_solicitud, h.votos, h.id_sesion, h.causa_no_sono
         FROM analitica.hecho_solicitud h JOIN analitica.dim_cancion c ON c.id_cancion = h.id_cancion
         JOIN analitica.dim_noche n ON n.id_sesion = h.id_sesion
         WHERE h.id_sesion IN ($ids) AND h.causa_no_sono IS NOT NULL AND h.votos >= ?
         ORDER BY h.votos DESC, n.inicio DESC, h.id_solicitud LIMIT 25""",
      { rs, _ ->
        val noche = rs.getLong("id_sesion")
        mapOf(
          "solicitud_id" to rs.getLong("id_solicitud").toString(), "cancion" to cancion(rs), "votos" to rs.getInt("votos"),
          "noche_id" to noche.toString(), "noche_inicio" to inicioDe.getValue(noche), "causa" to rs.getString("causa_no_sono"),
        )
      },
      limites.getValue("votos_minimos"),
    )
    val votosPorTelefono = mutableMapOf<Long, MutableList<Int>>()
    jdbc.query(
      "SELECT id_sesion, COUNT(*) FROM analitica.hecho_voto WHERE id_sesion IN ($ids) GROUP BY id_sesion, dispositivo_seud",
    ) { rs -> votosPorTelefono.getOrPut(rs.getLong(1)) { mutableListOf() }.add(rs.getInt(2)) }
    val porNoche = porNoche(ids)
    val concentraciones = elegidas.associate { it.id to pctDelDiez(votosPorTelefono[it.id].orEmpty()) }
    val telefonos = jdbc.queryForObject(
      """SELECT COUNT(DISTINCT dispositivo_seud) FROM (
           SELECT dispositivo_seud FROM analitica.hecho_solicitud WHERE id_sesion IN ($ids)
           UNION ALL SELECT dispositivo_seud FROM analitica.hecho_voto WHERE id_sesion IN ($ids)) t""",
      Int::class.java,
    ) ?: 0
    return mapOf(
      "datos" to origen(p.barId, elegidas),
      "umbrales" to limites,
      "no_sonaron" to noSonaron,
      "concentracion" to mapOf(
        "telefonos" to telefonos,
        "votos" to votosPorTelefono.values.sumOf { it.sum() },
        // Por noche: en un rango, los habituales de muchas noches inflarían la concentración.
        "pct_votos_del_10" to if (elegidas.isEmpty()) 0 else (concentraciones.values.sum().toDouble() / elegidas.size).roundToInt(),
      ),
      "noches" to elegidas.map {
        val x = porNoche[it.id]
        mapOf(
          "noche_id" to it.id.toString(), "inicio" to iso(it.inicio), "telefonos" to (x?.telefonos ?: 0),
          "solicitudes" to (x?.solicitudes ?: 0), "pct_votos_del_10" to concentraciones.getValue(it.id),
        )
      },
    )
  }

  private fun porNoche(ids: String): Map<Long, PorNoche> {
    val solicitudes = jdbc.query(
      """SELECT id_sesion, COUNT(*), COALESCE(SUM(votos), 0), COALESCE(SUM(espera_seg), 0), COUNT(espera_seg)
         FROM analitica.hecho_solicitud WHERE id_sesion IN ($ids) GROUP BY id_sesion""",
      { rs, _ -> rs.getLong(1) to listOf(rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)) },
    ).toMap()
    val telefonos = jdbc.query(
      """SELECT id_sesion, COUNT(DISTINCT dispositivo_seud) FROM (
           SELECT id_sesion, dispositivo_seud FROM analitica.hecho_solicitud WHERE id_sesion IN ($ids)
           UNION ALL SELECT id_sesion, dispositivo_seud FROM analitica.hecho_voto WHERE id_sesion IN ($ids)) t GROUP BY id_sesion""",
      { rs, _ -> rs.getLong(1) to rs.getInt(2) },
    ).toMap()
    val tiempos = jdbc.query(
      """SELECT id_sesion, COALESCE(SUM(segundos) FILTER (WHERE origen = 'solicitud'), 0),
                COALESCE(SUM(segundos) FILTER (WHERE origen = 'lista_casa'), 0)
         FROM analitica.hecho_reproduccion WHERE id_sesion IN ($ids) GROUP BY id_sesion""",
      { rs, _ -> rs.getLong(1) to (rs.getLong(2) to rs.getLong(3)) },
    ).toMap()
    return (solicitudes.keys + telefonos.keys + tiempos.keys).associateWith { id ->
      val s = solicitudes[id] ?: listOf(0L, 0L, 0L, 0L)
      val t = tiempos[id] ?: (0L to 0L)
      PorNoche(s[0].toInt(), s[1].toInt(), telefonos[id] ?: 0, t.first, t.second, s[2], s[3].toInt())
    }
  }

  // ── RF-053 ─────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  fun comparar(p: ContratoPanelAcceso.Personal, agrupar: String?, nochesParam: String?, desde: String?, hasta: String?): Map<String, Any?> {
    if (agrupar !in setOf("noche", "dia", "evento")) throw invalido("Agrupa por noche, día o evento")
    val lista = todas(p.barId)
    val pedidas = nochesParam?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    val elegidas = if (agrupar == "noche" && pedidas.isNotEmpty()) {
      val numeros = pedidas.map { it.toLongOrNull() ?: throw invalido("Noches inválidas") }.toSet()
      lista.filter { it.id in numeros }
    } else seleccionar(lista, Filtro(desde = desde, hasta = hasta))
    val datos = porNoche(idsDe(elegidas))
    data class Grupo(val etiqueta: String, val orden: Long, val noches: MutableList<Noche> = mutableListOf())
    val grupos = linkedMapOf<String, Grupo>()
    for (n in elegidas) {
      val tipo = n.tipo
      val (clave, etiqueta, orden) = when (agrupar) {
        "noche" -> Triple(n.id.toString(), etiquetaNoche(n), n.inicio.toEpochSecond())
        "dia" -> Triple(n.dia.toString(), DIAS[n.dia], n.dia.toLong())
        else -> Triple(tipo, TIPOS.getValue(tipo), TIPOS.keys.indexOf(tipo).toLong())
      }
      grupos.getOrPut(clave) { Grupo(etiqueta, orden) }.noches.add(n)
    }
    fun prom(valores: List<Number>, k: Int) = ((valores.sumOf { it.toDouble() } / k) * 10).roundToInt() / 10.0
    return mapOf(
      "datos" to origen(p.barId, elegidas),
      "agrupar" to agrupar,
      "grupos" to grupos.entries.sortedBy { it.value.orden }.map { (clave, g) ->
        val k = g.noches.size
        val filas = g.noches.map { datos[it.id] ?: PorNoche(0, 0, 0, 0, 0, 0, 0) }
        val esperas = filas.sumOf { it.esperaN }
        mapOf(
          "clave" to clave, "etiqueta" to g.etiqueta, "noches" to k,
          "solicitudes" to prom(filas.map { it.solicitudes }, k),
          "telefonos" to prom(filas.map { it.telefonos }, k),
          "votos" to prom(filas.map { it.votos }, k),
          "espera_promedio_seg" to if (esperas > 0) (filas.sumOf { it.esperaSuma }.toDouble() / esperas).roundToInt() else null,
          "pedidos_seg" to prom(filas.map { it.pedidosSeg }, k),
          "lista_casa_seg" to prom(filas.map { it.casaSeg }, k),
        )
      },
    )
  }

  // ── RF-054: sugerencias; ninguna cambia nada ────────────────────────────────

  @Transactional(readOnly = true)
  fun recomendaciones(p: ContratoPanelAcceso.Personal, f: Filtro): Map<String, Any?> {
    val elegidas = seleccionar(todas(p.barId), f)
    return mapOf("datos" to origen(p.barId, elegidas), "recomendaciones" to sugerencias(p.barId, elegidas))
  }

  private data class Pedida(val id: Long, val titulo: String, val artista: String, val genero: String?, val veces: Int, val noches: Int, val votos: Int)

  private fun sugerencias(barId: Long, elegidas: List<Noche>): List<Map<String, Any?>> {
    if (elegidas.isEmpty()) return emptyList()
    val ids = idsDe(elegidas)
    val pedidas = jdbc.query(
      """SELECT c.id_cancion, c.titulo, c.artista, c.genero, COUNT(*) AS veces, COUNT(DISTINCT h.id_sesion) AS noches, SUM(h.votos) AS votos
         FROM analitica.hecho_solicitud h JOIN analitica.dim_cancion c ON c.id_cancion = h.id_cancion
         WHERE h.id_sesion IN ($ids) GROUP BY c.id_cancion, c.titulo, c.artista, c.genero""",
      { rs, _ -> Pedida(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getInt(6), rs.getInt(7)) },
    )
    val total = pedidas.sumOf { it.veces }
    if (total == 0) return emptyList()
    val salida = mutableListOf<Map<String, Any?>>()
    val activa = jdbc.query(
      "SELECT id_lista, nombre FROM public.lista_defecto WHERE id_establecimiento = ? AND activa ORDER BY id_lista DESC LIMIT 1",
      { rs, _ -> rs.getLong(1) to rs.getString(2) }, barId,
    ).firstOrNull()
    val enLista = activa?.let { jdbc.query("SELECT id_cancion FROM public.elemento_lista WHERE id_lista = ?", { rs, _ -> rs.getLong(1) }, it.first) }.orEmpty()
    val bloqueos = jdbc.query(
      """SELECT id_cancion, fecha_fin IS NULL FROM public.bloqueo_contenido
         WHERE id_establecimiento = ? AND activo AND fecha_inicio <= now() AND (fecha_fin IS NULL OR fecha_fin > now())""",
      { rs, _ -> rs.getLong(1) to rs.getBoolean(2) }, barId,
    )
    val bloqueadas = bloqueos.map { it.first }.toSet()

    pedidas.filter { it.id !in enLista && it.id !in bloqueadas && it.veces >= 3 }
      .sortedWith(compareByDescending<Pedida> { it.veces }.thenBy { it.id }).take(2).forEach {
        salida += sugerencia(
          "agregar-${it.id}", "agregar_a_lista", "Agregar «${it.titulo}», de ${it.artista}, a la lista de la casa",
          "La pidieron ${it.veces} veces en ${it.noches} ${if (it.noches == 1) "noche" else "noches"} y no está en «${activa?.second ?: "la lista activa"}».",
          "listas", activa?.first?.toString(),
        )
      }

    if (activa != null && enLista.isNotEmpty()) {
      val generoDe = jdbc.query(
        "SELECT id_cancion, genero FROM analitica.dim_cancion WHERE id_cancion IN (${enLista.joinToString(",")})",
        { rs, _ -> rs.getLong(1) to rs.getString(2) },
      ).toMap()
      val candidato = pedidas.filter { it.genero != null }.groupBy { it.genero!! }
        .map { (genero, filas) ->
          val pedido = pct(filas.sumOf { it.veces }, total)
          Triple(genero, pedido, pct(enLista.count { generoDe[it] == genero }, enLista.size))
        }
        .filter { (_, pedido, lista) -> pedido >= 15 && lista < pedido / 2.0 }
        .maxByOrNull { (_, pedido, lista) -> pedido - lista }
      candidato?.let { (genero, pedido, lista) ->
        salida += sugerencia(
          "genero-$genero", "genero_en_lista", "Sumar canciones de $genero a la lista de la casa",
          "El $pedido % de los pedidos son de $genero; en «${activa.second}» es el $lista %.", "listas", activa.first.toString(),
        )
      }
    }

    val indefinidas = bloqueos.filter { it.second }.map { it.first }.toSet()
    pedidas.filter { it.id in indefinidas && it.veces >= 4 }.maxByOrNull { it.veces }?.let {
      salida += sugerencia(
        "bloqueo-${it.id}", "revisar_bloqueo", "Revisar si «${it.titulo}» debe seguir bloqueada",
        "En estas noches la pidieron ${it.veces} veces y sumó ${it.votos} votos; está bloqueada hasta que alguien lo levante.", "bloqueos", null,
      )
    }

    for (dia in listOf(6, 5)) {
      val delDia = elegidas.filter { it.dia == dia && it.eventoId == null }
      if (delDia.isEmpty()) continue
      val fila = jdbc.queryForMap(
        """SELECT COUNT(*) AS pedidas, COUNT(*) FILTER (WHERE n.fin - h.fecha_solicitud <= INTERVAL '1 hour') AS ultima,
                  COUNT(*) FILTER (WHERE h.causa_no_sono = 'fuera_de_tiempo') AS sin_sonar
           FROM analitica.hecho_solicitud h JOIN analitica.dim_noche n ON n.id_sesion = h.id_sesion
           WHERE h.id_sesion IN (${idsDe(delDia)})""",
      )
      val parte = pct((fila["ultima"] as Number).toInt(), (fila["pedidas"] as Number).toInt())
      val sinSonar = (fila["sin_sonar"] as Number).toInt()
      if (parte >= 12 && sinSonar >= 5) {
        val plural = if (dia == 6) "sábados" else "viernes"
        salida += sugerencia(
          "horario-$dia", "ajustar_horario", "Pensar en cerrar más tarde los $plural",
          "Los $plural, en la última hora llega el $parte % de los pedidos y $sinSonar se quedaron sin sonar al cerrar (${delDia.size} ${if (delDia.size == 1) "noche" else "noches"}).",
          "horario", null,
        )
        break
      }
    }
    return salida
  }

  private fun sugerencia(id: String, tipo: String, titulo: String, base: String, destino: String, lista: String?) =
    mapOf("id" to id, "tipo" to tipo, "titulo" to titulo, "base" to base, "destino" to destino, "lista_id" to lista)

  // ── RF-063: el cubo de KPI ─────────────────────────────────────────────────

  @Transactional(readOnly = true)
  fun cubo(p: ContratoPanelAcceso.Personal, ejeParam: String?): Map<String, Any?> {
    val eje = ejeParam?.takeIf { it.isNotBlank() } ?: "dia"
    if (eje !in setOf("dia", "evento")) throw invalido("El tercer eje es día o evento")
    val lista = todas(p.barId)
    val celdas = jdbc.query(
      """SELECT k.genero, k.franja, k.tercero, k.solicitudes, k.votos, k.telefonos, k.no_sonaron,
                k.espera_promedio_seg, k.espera_maxima_seg, k.pedidos_seg, k.lista_casa_seg,
                k.id_cancion_popular, k.votos_popular, k.solicitudes_popular,
                c.video_id, c.titulo, c.artista, c.duracion_seg, c.miniatura_url
         FROM analitica.cubo_participacion k
         LEFT JOIN analitica.dim_cancion c ON c.id_cancion = k.id_cancion_popular
         WHERE k.id_establecimiento = ? AND k.eje = ?""",
      { rs, _ ->
        mapOf(
          "genero" to rs.getString("genero"), "franja" to rs.getString("franja"), "tercero" to rs.getString("tercero"),
          "solicitudes" to rs.getInt("solicitudes"), "votos" to rs.getInt("votos"), "telefonos" to rs.getInt("telefonos"),
          "no_sonaron" to rs.getInt("no_sonaron"), "espera_promedio_seg" to rs.getObject("espera_promedio_seg") as Int?,
          "espera_maxima_seg" to rs.getObject("espera_maxima_seg") as Int?, "pedidos_seg" to rs.getLong("pedidos_seg"),
          "lista_casa_seg" to rs.getLong("lista_casa_seg"),
          "popular" to if (rs.getObject("id_cancion_popular") == null) null else mapOf(
            "cancion" to cancion(rs), "votos" to rs.getInt("votos_popular"), "solicitudes" to rs.getInt("solicitudes_popular"),
          ),
        )
      },
      p.barId, eje,
    )
    fun miembros(columna: String, orden: (String) -> Int): List<String> {
      val pesos = celdas.filter { it[columna] != TODOS }.groupBy { it[columna] as String }
        .mapValues { (_, filas) -> filas.sumOf { it["solicitudes"] as Int } }
      return pesos.keys.sortedWith(compareBy<String> { orden(it) }.thenByDescending { pesos.getValue(it) }.thenBy { it })
    }
    val ultimoOtros = { m: String -> if (m == "Otros" || m == "Otros días") 1 else 0 }
    return mapOf(
      "datos" to origen(p.barId, lista),
      "eje" to eje,
      "miembros" to mapOf(
        "genero" to miembros("genero", ultimoOtros),
        "franja" to miembros("franja") { FRANJAS.indexOf(it).let { i -> if (i < 0) FRANJAS.size else i } },
        "tercero" to miembros("tercero") { if (eje == "dia") DIAS_CUBO.indexOf(it).let { i -> if (i < 0) DIAS_CUBO.size else i } else ultimoOtros(it) },
      ),
      "celdas" to celdas,
    )
  }

  // ── Comunes ────────────────────────────────────────────────────────────────

  private fun cancion(rs: ResultSet): Map<String, Any?> = mapOf(
    "video_id" to rs.getString("video_id"), "titulo" to rs.getString("titulo"), "artista" to rs.getString("artista"),
    "duracion_seg" to rs.getInt("duracion_seg"), "miniatura_url" to rs.getString("miniatura_url"),
  )

  private fun etiquetaNoche(n: Noche) = "${DIAS[n.dia].take(3).lowercase()} ${n.fecha.dayOfMonth} ${MESES[n.fecha.monthValue - 1]}"
  private fun iso(f: OffsetDateTime) = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(f)
  private fun pct(parte: Int, todo: Int) = if (todo > 0) (parte * 100.0 / todo).roundToInt() else 0
  private fun invalido(mensaje: String) = ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", mensaje)

  private fun enteroEn(cuerpo: Map<String, Any?>?, campo: String, rango: IntRange): Int {
    val n = cuerpo?.get(campo) as? Number
    if (n == null || n.toDouble() != n.toInt().toDouble() || n.toInt() !in rango) throw invalido("$campo debe ser un entero de ${rango.first} a ${rango.last}")
    return n.toInt()
  }

  companion object {
    private const val TODOS = "*"
    private const val CANCION = "c.video_id, c.titulo, c.artista, c.duracion_seg, c.miniatura_url"
    private val FECHA = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val UMBRALES_POR_DEFECTO = mapOf("votos_minimos" to 6, "concentracion_pct" to 40, "participacion_minima" to 15)
    private val DIAS = listOf("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val TIPOS = linkedMapOf("normal" to "Noche normal", "karaoke" to "Karaoke", "partido" to "Partido", "tematica" to "Temática", "otro" to "Otro")
    private val FRANJAS = listOf("Antes de las 9", "9 a 11 p. m.", "11 p. m. a 1 a. m.", "Después de la 1")
    private val DIAS_CUBO = listOf("Jueves", "Viernes", "Sábado", "Otros días")

    /** Los ID vienen de la base como `Long`: nunca hay texto del usuario en el `IN`. */
    private fun idsDe(noches: List<Noche>) = if (noches.isEmpty()) "NULL" else noches.joinToString(",") { it.id.toString() }

    /** Qué parte de los votos de una noche dio el 10 % de los teléfonos que más votaron. */
    internal fun pctDelDiez(votosPorTelefono: List<Int>): Int {
      val total = votosPorTelefono.sum()
      if (total == 0) return 0
      val cuantos = maxOf(1, ceil(votosPorTelefono.size * 0.1).toInt())
      return (votosPorTelefono.sortedDescending().take(cuantos).sum() * 100.0 / total).roundToInt()
    }
  }
}
