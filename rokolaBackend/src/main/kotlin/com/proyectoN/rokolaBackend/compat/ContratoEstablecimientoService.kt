package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DateTimeException
import java.time.ZoneId

/**
 * `GET/PATCH /api/panel/establecimiento` (RF-034, RF-059): los datos del bar y las reglas de los
 * pedidos, como `Establecimiento` del contrato. Solo el Administrador, y solo su propio bar: el
 * id sale de la sesión, nunca de la petición. Cada cambio va a la bitácora.
 *
 * Hecho por el equipo de frontend (2026-11-12) sobre el esquema reconstruido del código: la base
 * de Railway no estaba disponible para verificarlo (doc 04, «Pendiente del backend»).
 */
@Service
class ContratoEstablecimientoService(
  private val jdbc: JdbcTemplate,
  private val panel: CambioPanelPublisher,
) {
  fun leer(p: ContratoPanelAcceso.Personal): Map<String, Any?> = jdbc.query(
    """SELECT e.id_establecimiento, e.slug, e.nombre, e.direccion, e.zona_horaria, e.activo, e.tema,
              c.max_solicitudes_dispositivo, c.duracion_max_seg, c.minutos_bloqueo_repeticion
       FROM public.establecimiento e
       LEFT JOIN public.configuracion_servicio c ON c.id_establecimiento = e.id_establecimiento
       WHERE e.id_establecimiento = ?""",
    { rs, _ ->
      fun entero(columna: String): Int = (rs.getObject(columna) as? Number)?.toInt()
        ?: throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio")
      mapOf(
        "id" to rs.getLong("id_establecimiento").toString(),
        "slug" to rs.getString("slug"),
        "nombre" to rs.getString("nombre"),
        "direccion" to (rs.getString("direccion") ?: ""),
        "zona_horaria" to rs.getString("zona_horaria"),
        "estado" to if (rs.getBoolean("activo")) "activo" else "inactivo",
        "pedidos_maximos" to entero("max_solicitudes_dispositivo"),
        "duracion_max_seg" to entero("duracion_max_seg"),
        "ventana_no_repeticion_min" to entero("minutos_bloqueo_repeticion"),
        "tema" to rs.getString("tema"),
      )
    },
    p.barId,
  ).firstOrNull() ?: throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Bar no encontrado")

  /** Aplica solo los campos que llegan (PATCH). Valida todo antes de escribir nada. */
  @Transactional
  fun editar(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val cambio = CambioEstablecimiento.desde(cuerpo ?: emptyMap())
    if (cambio.vacio) return leer(p)
    val bar = p.barId
    actualizar("establecimiento", cambio.columnasEstablecimiento(), bar)
    val config = cambio.columnasConfiguracion()
    if (config.isNotEmpty() && actualizar("configuracion_servicio", config + ("fecha_modificacion" to Ahora), bar) == 0) {
      throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio")
    }
    // `detalle` es JSONB y la bitácora lee `detalle->>'texto'` (ContratoNocheService).
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, 'editar_establecimiento', 'establecimiento', ?, jsonb_build_object('texto', ?::text))""",
      bar, p.id, bar, cambio.resumen(),
    )
    // `yo` lleva el nombre y la zona del bar; `bitacora`, la línea nueva.
    panel.publicar(p.barSlug, setOf("establecimiento", "yo", "bitacora"))
    return leer(p)
  }

  /**
   * Un solo UPDATE por tabla. Tabla y columnas salen de listas fijas del código (nunca de la
   * petición): el SQL no se arma con datos; los valores van como parámetros.
   */
  private fun actualizar(tabla: String, columnas: Map<String, Any>, bar: Long): Int {
    if (columnas.isEmpty()) return 0
    require(tabla in TABLAS && columnas.keys.all { it in COLUMNAS })
    val asignaciones = columnas.entries.joinToString(", ") { (c, v) -> if (v === Ahora) "$c = CURRENT_TIMESTAMP" else "$c = ?" }
    val valores = columnas.values.filter { it !== Ahora } + bar
    return jdbc.update("UPDATE public.$tabla SET $asignaciones WHERE id_establecimiento = ?", *valores.toTypedArray())
  }

  /** Marca para «la hora del servidor de base de datos» en un UPDATE. */
  private object Ahora

  private companion object {
    val TABLAS = setOf("establecimiento", "configuracion_servicio")
    val COLUMNAS = setOf(
      "nombre", "direccion", "zona_horaria", "tema",
      "max_solicitudes_dispositivo", "duracion_max_seg", "minutos_bloqueo_repeticion", "fecha_modificacion",
    )
  }
}

/**
 * El cuerpo de `CambioEstablecimiento` (contrato), con los mismos límites que el contrato y el
 * formulario del panel. Aparte del servicio para probarlo sin base de datos.
 */
data class CambioEstablecimiento(
  val nombre: String? = null,
  val direccion: String? = null,
  val zonaHoraria: String? = null,
  val pedidosMaximos: Int? = null,
  val duracionMaxSeg: Int? = null,
  val ventanaMin: Int? = null,
  val tema: String? = null,
) {
  val vacio: Boolean get() = this == CambioEstablecimiento()

  fun columnasEstablecimiento(): Map<String, Any> = listOfNotNull(
    nombre?.let { "nombre" to it }, direccion?.let { "direccion" to it },
    zonaHoraria?.let { "zona_horaria" to it }, tema?.let { "tema" to it },
  ).toMap()

  fun columnasConfiguracion(): Map<String, Any> = listOfNotNull(
    pedidosMaximos?.let { "max_solicitudes_dispositivo" to it },
    duracionMaxSeg?.let { "duracion_max_seg" to it },
    ventanaMin?.let { "minutos_bloqueo_repeticion" to it },
  ).toMap()

  /** Qué cambió, para el detalle de la bitácora; el diseño va con su nombre. */
  fun resumen(): String = listOfNotNull(
    nombre?.let { "nombre" }, direccion?.let { "dirección" }, zonaHoraria?.let { "zona horaria" },
    pedidosMaximos?.let { "pedidos por teléfono" }, duracionMaxSeg?.let { "duración máxima" },
    ventanaMin?.let { "tiempo sin repetir" }, tema?.let { "diseño ${it.replaceFirstChar(Char::uppercase)}" },
  ).joinToString(", ")

  companion object {
    val TEMAS = setOf("rockola", "vidrio", "afiche")
    private val CAMPOS = setOf("nombre", "direccion", "zona_horaria", "pedidos_maximos", "duracion_max_seg", "ventana_no_repeticion_min", "tema")

    fun desde(cuerpo: Map<String, Any?>): CambioEstablecimiento {
      val desconocidos = cuerpo.keys - CAMPOS
      if (desconocidos.isNotEmpty()) invalido("Campos que no se pueden cambiar: ${desconocidos.sorted().joinToString()}")
      return CambioEstablecimiento(
        nombre = texto(cuerpo, "nombre", max = 150)?.also { if (it.isEmpty()) invalido("El nombre no puede quedar vacío") },
        direccion = texto(cuerpo, "direccion", max = 250),
        zonaHoraria = texto(cuerpo, "zona_horaria", max = 60)?.also {
          try { ZoneId.of(it) } catch (_: DateTimeException) { invalido("Zona horaria desconocida") }
        },
        pedidosMaximos = entero(cuerpo, "pedidos_maximos", 1, 10),
        duracionMaxSeg = entero(cuerpo, "duracion_max_seg", 60, 3600),
        ventanaMin = entero(cuerpo, "ventana_no_repeticion_min", 0, 600),
        tema = texto(cuerpo, "tema", max = 10)?.also { if (it !in TEMAS) invalido("Diseño desconocido") },
      )
    }

    /** Sin los espacios de los lados, y entonces se mide (un nombre pegado con espacios no falla). */
    private fun texto(cuerpo: Map<String, Any?>, campo: String, max: Int): String? {
      if (!cuerpo.containsKey(campo)) return null
      val v = (cuerpo[campo] as? String ?: invalido("$campo debe ser texto")).trim()
      if (v.length > max) invalido("$campo: máximo $max caracteres")
      return v
    }

    private fun entero(cuerpo: Map<String, Any?>, campo: String, min: Int, max: Int): Int? {
      if (!cuerpo.containsKey(campo)) return null
      val n = cuerpo[campo] as? Number ?: invalido("$campo debe ser un número entero")
      val d = n.toDouble()
      if (d != Math.floor(d) || d < min || d > max) invalido("$campo: de $min a $max")
      return d.toInt()
    }

    private fun invalido(mensaje: String): Nothing =
      throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", mensaje)
  }
}
