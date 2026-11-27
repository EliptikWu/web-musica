package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeService
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.text.Normalizer
import java.time.OffsetDateTime

/**
 * Listas de la casa (RF-027 a RF-033) y la búsqueda del panel para armarlas (RF-031): solo el
 * Administrador, solo las de su bar. Las reglas son las de la simulación de la web
 * (`frontend/web/simulacion/gestion.ts`): el nombre no se repite (sin distinguir mayúsculas ni
 * tildes), la lista activa no se elimina ni se queda vacía, una lista vacía no se activa y una
 * canción bloqueada o que YouTube no deja embeber no entra.
 *
 * La base dice «aleatorio» y el contrato «aleatoria». Hecho por el equipo de frontend
 * (2026-11-21, D14).
 */
@Service
class ContratoListasService(
  private val jdbc: JdbcTemplate,
  private val canciones: ContratoCanciones,
  private val youtube: YoutubeService,
  private val bitacora: ContratoBitacora,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
) {
  private data class Cabecera(val id: Long, val nombre: String, val modo: String, val activa: Boolean, val actualizada: OffsetDateTime)

  fun listar(p: ContratoPanelAcceso.Personal, q: String?): Map<String, Any?> {
    val texto = q?.let(::normalizar)?.take(60)?.takeIf { it.isNotEmpty() }
    val listas = jdbc.query(
      """SELECT l.id_lista, l.nombre, l.modo_rotacion, l.activa, l.fecha_modificacion,
                COUNT(el.id_elemento)::integer AS total, COALESCE(SUM(c.duracion_seg), 0)::integer AS segundos
         FROM public.lista_defecto l
         LEFT JOIN public.elemento_lista el ON el.id_lista = l.id_lista
         LEFT JOIN public.cancion c ON c.id_cancion = el.id_cancion
         WHERE l.id_establecimiento = ?
         GROUP BY l.id_lista ORDER BY l.activa DESC, l.nombre""",
      { rs, _ ->
        resumen(
          Cabecera(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getObject(5, OffsetDateTime::class.java)),
          rs.getInt("total"), rs.getInt("segundos"),
        )
      },
      p.barId,
    ).filter { texto == null || normalizar(it["nombre"] as String).contains(texto) }
    return mapOf("listas" to listas)
  }

  fun detalle(p: ContratoPanelAcceso.Personal, idTexto: String): Map<String, Any?> =
    detalle(cabecera(p, idTexto, bloquear = false))

  @Transactional
  fun crear(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val nombre = nombre(cuerpo)
    bloquearBar(p.barId)
    if (nombreUsado(p.barId, nombre, excepto = null)) ContratoCuerpo.conflicto("Ya hay una lista con ese nombre")
    // La columna `activa` empieza en TRUE en la base: una lista nueva nunca reemplaza a la activa.
    val id = jdbc.queryForObject(
      "INSERT INTO public.lista_defecto (id_establecimiento, nombre, modo_rotacion, activa) VALUES (?, ?, 'secuencial', FALSE) RETURNING id_lista",
      Long::class.javaObjectType, p.barId, nombre,
    )!!
    bitacora.anotar(p, "editar_lista", "lista", id, nombre, "creada")
    avisar(p, activa = false)
    return detalle(cabeceraPorId(p.barId, id)!!)
  }

  @Transactional
  fun renombrar(p: ContratoPanelAcceso.Personal, idTexto: String, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val nombre = nombre(cuerpo)
    bloquearBar(p.barId)
    val l = cabecera(p, idTexto, bloquear = true)
    if (nombreUsado(p.barId, nombre, excepto = l.id)) ContratoCuerpo.conflicto("Ya hay una lista con ese nombre")
    jdbc.update("UPDATE public.lista_defecto SET nombre = ?, fecha_modificacion = CURRENT_TIMESTAMP WHERE id_lista = ?", nombre, l.id)
    bitacora.anotar(p, "editar_lista", "lista", l.id, nombre, "renombrada")
    avisar(p, l.activa)
    return detalle(cabeceraPorId(p.barId, l.id)!!)
  }

  @Transactional
  fun eliminar(p: ContratoPanelAcceso.Personal, idTexto: String) {
    val l = cabecera(p, idTexto, bloquear = true)
    if (l.activa) ContratoCuerpo.conflicto("No se puede eliminar la lista activa")
    val sonando = jdbc.queryForObject(
      """SELECT EXISTS(SELECT 1 FROM public.reproduccion r JOIN public.elemento_lista el ON el.id_elemento = r.id_elemento
         WHERE el.id_lista = ? AND r.resultado = 'iniciada')""",
      Boolean::class.javaObjectType, l.id,
    ) == true
    if (sonando) ContratoCuerpo.conflicto("Una canción de esta lista está sonando ahora; elimínala cuando termine")
    // Sus elementos se borran en cascada; lo que ya sonó queda en la historia sin el vínculo (V5).
    jdbc.update("DELETE FROM public.lista_defecto WHERE id_lista = ? AND id_establecimiento = ?", l.id, p.barId)
    bitacora.anotar(p, "editar_lista", "lista", l.id, l.nombre, "eliminada")
    avisar(p, activa = false)
  }

  @Transactional
  fun agregar(p: ContratoPanelAcceso.Personal, idTexto: String, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("video_id"))
    val videoId = ContratoCuerpo.texto(datos, "video_id", max = 30, obligatorio = true)!!
    val l = cabecera(p, idTexto, bloquear = true)
    val cancion = canciones.obtenerOValidar(videoId)
    val bloqueada = jdbc.queryForObject(
      "SELECT EXISTS(SELECT 1 FROM public.bloqueo_contenido b WHERE b.id_establecimiento = ? AND b.id_cancion = ? AND ${ContratoBloqueosService.VIGENTE})",
      Boolean::class.javaObjectType, p.barId, cancion.id,
    ) == true
    if (bloqueada) throw ContratoApiException(HttpStatus.UNPROCESSABLE_ENTITY, "bloqueada", "Esa canción está bloqueada")
    val yaEsta = jdbc.queryForObject(
      "SELECT EXISTS(SELECT 1 FROM public.elemento_lista WHERE id_lista = ? AND id_cancion = ?)",
      Boolean::class.javaObjectType, l.id, cancion.id,
    ) == true
    if (yaEsta) ContratoCuerpo.conflicto("Esa canción ya está en la lista")
    jdbc.update(
      """INSERT INTO public.elemento_lista (id_lista, id_cancion, orden)
         VALUES (?, ?, (SELECT COALESCE(MAX(orden), 0) + 1 FROM public.elemento_lista WHERE id_lista = ?))""",
      l.id, cancion.id, l.id,
    )
    tocar(l.id)
    bitacora.anotar(p, "editar_lista", "lista", l.id, l.nombre, "agregó ${cancion.contrato["titulo"]}")
    avisar(p, l.activa)
    return detalle(cabeceraPorId(p.barId, l.id)!!)
  }

  @Transactional
  fun quitar(p: ContratoPanelAcceso.Personal, idTexto: String, elementoTexto: String): Map<String, Any?> {
    val l = cabecera(p, idTexto, bloquear = true)
    val elemento = ContratoCuerpo.id(elementoTexto, "Esa canción no está en la lista")
    val titulo = jdbc.query(
      """SELECT c.titulo FROM public.elemento_lista el JOIN public.cancion c ON c.id_cancion = el.id_cancion
         WHERE el.id_elemento = ? AND el.id_lista = ?""",
      { rs, _ -> rs.getString(1) }, elemento, l.id,
    ).firstOrNull() ?: ContratoCuerpo.noEncontrado("Esa canción no está en la lista")
    // RF-032: la lista activa no se queda vacía.
    if (l.activa && total(l.id) <= 1) ContratoCuerpo.conflicto("La lista activa no puede quedar vacía")
    val sonando = jdbc.queryForObject(
      "SELECT EXISTS(SELECT 1 FROM public.reproduccion WHERE id_elemento = ? AND resultado = 'iniciada')",
      Boolean::class.javaObjectType, elemento,
    ) == true
    if (sonando) ContratoCuerpo.conflicto("Esa canción está sonando ahora; quítala cuando termine")
    jdbc.update("DELETE FROM public.elemento_lista WHERE id_elemento = ? AND id_lista = ?", elemento, l.id)
    renumerar(l.id, ordenActual(l.id))
    tocar(l.id)
    bitacora.anotar(p, "editar_lista", "lista", l.id, l.nombre, "quitó $titulo")
    avisar(p, l.activa)
    return detalle(cabeceraPorId(p.barId, l.id)!!)
  }

  @Transactional
  fun ordenar(p: ContratoPanelAcceso.Personal, idTexto: String, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("elementos"))
    val pedidos = (datos["elementos"] as? List<*> ?: ContratoCuerpo.invalido("Falta elementos"))
      .map { (it as? String)?.toLongOrNull() ?: ContratoCuerpo.invalido("Cada elemento es un id") }
    val l = cabecera(p, idTexto, bloquear = true)
    val actuales = ordenActual(l.id)
    if (pedidos.size != actuales.size || pedidos.toSet() != actuales.toSet()) {
      ContratoCuerpo.invalido("El orden no corresponde a las canciones de la lista")
    }
    renumerar(l.id, pedidos)
    tocar(l.id)
    bitacora.anotar(p, "editar_lista", "lista", l.id, l.nombre, "reordenada")
    avisar(p, l.activa)
    return detalle(cabeceraPorId(p.barId, l.id)!!)
  }

  @Transactional
  fun activar(p: ContratoPanelAcceso.Personal, idTexto: String, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("modo_rotacion"))
    val modo = ContratoCuerpo.opcion(datos["modo_rotacion"] as? String, "modo_rotacion", setOf("secuencial", "aleatoria")) {
      if (it == "aleatoria") "aleatorio" else "secuencial"
    }
    bloquearBar(p.barId)
    val l = cabecera(p, idTexto, bloquear = true)
    if (total(l.id) == 0) ContratoCuerpo.conflicto("Una lista vacía no se puede activar")
    // Primero se apagan las demás: el índice único `uq_lista_activa_por_establecimiento` se
    // comprueba fila por fila y un solo UPDATE chocaría al encender la nueva antes de apagar la vieja.
    jdbc.update("UPDATE public.lista_defecto SET activa = FALSE WHERE id_establecimiento = ? AND id_lista <> ? AND activa", p.barId, l.id)
    jdbc.update(
      "UPDATE public.lista_defecto SET activa = TRUE, modo_rotacion = ?, fecha_modificacion = CURRENT_TIMESTAMP WHERE id_lista = ?",
      modo, l.id,
    )
    // La rotación de la noche empieza desde la primera canción de la lista nueva.
    jdbc.update("UPDATE public.sesion SET indice_lista_defecto = 1 WHERE id_establecimiento = ? AND estado = 'abierta'", p.barId)
    bitacora.anotar(p, "activar_lista", "lista", l.id, l.nombre, if (modo == "aleatorio") "aleatoria" else "secuencial")
    avisar(p, activa = true)
    return detalle(cabeceraPorId(p.barId, l.id)!!)
  }

  /** `GET /panel/busqueda` (RF-031): videos de YouTube que se pueden embeber, para agregarlos. */
  fun buscar(q: String?): Map<String, Any?> {
    val consulta = q?.trim()?.replace(Regex("\\s+"), " ")?.lowercase() ?: ""
    if (consulta.length !in 2..80) ContratoCuerpo.invalido("q debe tener entre 2 y 80 caracteres")
    val videos = try {
      youtube.buscar(consulta).resultados
    } catch (e: ResponseStatusException) {
      val cuota = e.statusCode.value() == 429 || e.reason?.contains("cuota", ignoreCase = true) == true
      throw ContratoApiException(
        HttpStatus.SERVICE_UNAVAILABLE, if (cuota) "cuota_agotada" else "youtube_no_disponible",
        if (cuota) "La cuota de YouTube no permite buscar ahora" else "YouTube no está disponible",
      )
    }
    return mapOf(
      "consulta" to consulta,
      "resultados" to videos.filter { it.embebible && it.duracionSeg > 0 }.map {
        mapOf(
          "video_id" to it.youtubeVideoId,
          "titulo" to it.titulo,
          "artista" to (it.artista?.takeIf(String::isNotBlank) ?: it.canal),
          "duracion_seg" to it.duracionSeg,
          "miniatura_url" to it.miniaturaUrl?.takeIf(String::isNotBlank),
        )
      },
    )
  }

  private fun detalle(l: Cabecera): Map<String, Any?> {
    val elementos = jdbc.query(
      """SELECT el.id_elemento, ${ContratoCanciones.COLUMNAS},
                (SELECT COUNT(*) FROM public.reproduccion r WHERE r.id_elemento = el.id_elemento)::integer AS veces,
                (SELECT MAX(r.fecha_inicio) FROM public.reproduccion r WHERE r.id_elemento = el.id_elemento) AS ultima
         FROM public.elemento_lista el JOIN public.cancion c ON c.id_cancion = el.id_cancion
         WHERE el.id_lista = ? ORDER BY el.orden, el.id_elemento""",
      { rs, i ->
        mapOf(
          "elemento_id" to rs.getLong("id_elemento").toString(),
          "cancion" to ContratoCanciones.cancion(rs),
          "orden" to i + 1,
          "veces_reproducida" to rs.getInt("veces"),
          "ultima_reproduccion" to rs.getObject("ultima", OffsetDateTime::class.java),
        )
      },
      l.id,
    )
    val segundos = elementos.sumOf { ((it["cancion"] as Map<*, *>)["duracion_seg"] as Int) }
    return resumen(l, elementos.size, segundos) + ("canciones" to elementos)
  }

  private fun resumen(l: Cabecera, total: Int, segundos: Int): Map<String, Any?> = mapOf(
    "id" to l.id.toString(),
    "nombre" to l.nombre,
    "modo_rotacion" to if (l.modo.startsWith("aleatori")) "aleatoria" else "secuencial",
    "activa" to l.activa,
    "total_canciones" to total,
    "duracion_total_seg" to segundos,
    "actualizada_en" to l.actualizada,
  )

  private fun cabecera(p: ContratoPanelAcceso.Personal, idTexto: String, bloquear: Boolean): Cabecera {
    val id = ContratoCuerpo.id(idTexto, "Esa lista no existe")
    return cabeceraPorId(p.barId, id, bloquear) ?: ContratoCuerpo.noEncontrado("Esa lista no existe")
  }

  private fun cabeceraPorId(idBar: Long, id: Long, bloquear: Boolean = false): Cabecera? = jdbc.query(
    """SELECT id_lista, nombre, modo_rotacion, activa, fecha_modificacion FROM public.lista_defecto
       WHERE id_lista = ? AND id_establecimiento = ?${if (bloquear) " FOR UPDATE" else ""}""",
    { rs, _ -> Cabecera(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getObject(5, OffsetDateTime::class.java)) },
    id, idBar,
  ).firstOrNull()

  private fun nombre(cuerpo: Map<String, Any?>?): String {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("nombre"))
    return ContratoCuerpo.texto(datos, "nombre", max = 60, obligatorio = true)!!
  }

  private fun nombreUsado(idBar: Long, nombre: String, excepto: Long?): Boolean {
    val buscado = normalizar(nombre)
    return jdbc.query(
      "SELECT nombre FROM public.lista_defecto WHERE id_establecimiento = ? AND id_lista <> ?",
      { rs, _ -> rs.getString(1) }, idBar, excepto ?: -1L,
    ).any { normalizar(it) == buscado }
  }

  private fun total(idLista: Long): Int =
    jdbc.queryForObject("SELECT COUNT(*) FROM public.elemento_lista WHERE id_lista = ?", Int::class.javaObjectType, idLista) ?: 0

  private fun ordenActual(idLista: Long): List<Long> = jdbc.query(
    "SELECT id_elemento FROM public.elemento_lista WHERE id_lista = ? ORDER BY orden, id_elemento",
    { rs, _ -> rs.getLong(1) }, idLista,
  )

  /** Deja el orden 1..n según [elementos]. Primero lejos, para no chocar con UNIQUE (id_lista, orden). */
  private fun renumerar(idLista: Long, elementos: List<Long>) {
    if (elementos.isEmpty()) return
    jdbc.update("UPDATE public.elemento_lista SET orden = orden + $DESPLAZAMIENTO WHERE id_lista = ?", idLista)
    jdbc.batchUpdate(
      "UPDATE public.elemento_lista SET orden = ? WHERE id_elemento = ? AND id_lista = ?",
      elementos.mapIndexed { i, id -> arrayOf<Any>(i + 1, id, idLista) },
    )
  }

  private fun tocar(idLista: Long) {
    jdbc.update("UPDATE public.lista_defecto SET fecha_modificacion = CURRENT_TIMESTAMP WHERE id_lista = ?", idLista)
  }

  private fun bloquearBar(idBar: Long) {
    jdbc.query("SELECT 1 FROM public.establecimiento WHERE id_establecimiento = ? FOR UPDATE", { _, _ -> }, idBar)
  }

  /** El panel vuelve a pedir las listas; si cambió la activa, también el teléfono y la TV. */
  private fun avisar(p: ContratoPanelAcceso.Personal, activa: Boolean) {
    if (activa) {
      jdbc.query(
        "SELECT id_sesion FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta'",
        { rs, _ -> rs.getLong(1) }, p.barId,
      ).forEach { cola.publicar(it, "lista_casa") }
    }
    panel.publicar(p.barSlug, setOf("listas", "noche", "bitacora"))
  }

  internal companion object {
    const val DESPLAZAMIENTO = 1_000_000

    /** Sin mayúsculas, tildes ni espacios de más: «Clásicos» y «clasicos » son el mismo nombre. */
    fun normalizar(texto: String): String = Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ")
  }
}
