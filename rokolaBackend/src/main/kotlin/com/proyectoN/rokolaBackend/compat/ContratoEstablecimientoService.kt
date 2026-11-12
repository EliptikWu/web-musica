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
    cambio.nombre?.let { jdbc.update("UPDATE public.establecimiento SET nombre = ? WHERE id_establecimiento = ?", it, bar) }
    cambio.direccion?.let { jdbc.update("UPDATE public.establecimiento SET direccion = ? WHERE id_establecimiento = ?", it, bar) }
    cambio.zonaHoraria?.let { jdbc.update("UPDATE public.establecimiento SET zona_horaria = ? WHERE id_establecimiento = ?", it, bar) }
    cambio.tema?.let { jdbc.update("UPDATE public.establecimiento SET tema = ? WHERE id_establecimiento = ?", it, bar) }
    cambio.pedidosMaximos?.let { configuracion("max_solicitudes_dispositivo", it, bar) }
    cambio.duracionMaxSeg?.let { configuracion("duracion_max_seg", it, bar) }
    cambio.ventanaMin?.let { configuracion("minutos_bloqueo_repeticion", it, bar) }
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, 'editar_establecimiento', 'establecimiento', ?, ?)""",
      bar, p.id, bar, cambio.resumen(),
    )
    // `yo` lleva el nombre y la zona del bar; `bitacora`, la línea nueva.
    panel.publicar(p.barSlug, setOf("establecimiento", "yo", "bitacora"))
    return leer(p)
  }

  /** La columna sale de una lista fija (nunca de la petición): no hay SQL armado con datos. */
  private fun configuracion(columna: String, valor: Int, bar: Long) {
    require(columna in setOf("max_solicitudes_dispositivo", "duracion_max_seg", "minutos_bloqueo_repeticion"))
    val filas = jdbc.update("UPDATE public.configuracion_servicio SET $columna = ? WHERE id_establecimiento = ?", valor, bar)
    if (filas == 0) throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio")
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

  /** Qué campos cambiaron, para el detalle de la bitácora; el tema va con su valor. */
  fun resumen(): String = listOfNotNull(
    nombre?.let { "nombre" }, direccion?.let { "direccion" }, zonaHoraria?.let { "zona_horaria" },
    pedidosMaximos?.let { "pedidos_maximos" }, duracionMaxSeg?.let { "duracion_max_seg" },
    ventanaMin?.let { "ventana_no_repeticion_min" }, tema?.let { "tema=$it" },
  ).joinToString(", ")

  companion object {
    val TEMAS = setOf("rockola", "vidrio", "afiche")
    private val CAMPOS = setOf("nombre", "direccion", "zona_horaria", "pedidos_maximos", "duracion_max_seg", "ventana_no_repeticion_min", "tema")

    fun desde(cuerpo: Map<String, Any?>): CambioEstablecimiento {
      val desconocidos = cuerpo.keys - CAMPOS
      if (desconocidos.isNotEmpty()) invalido("Campos que no se pueden cambiar: ${desconocidos.sorted().joinToString()}")
      return CambioEstablecimiento(
        nombre = texto(cuerpo, "nombre", max = 150)?.trim()?.also { if (it.isEmpty()) invalido("El nombre no puede quedar vacío") },
        direccion = texto(cuerpo, "direccion", max = 250)?.trim(),
        zonaHoraria = texto(cuerpo, "zona_horaria", max = 60)?.also {
          try { ZoneId.of(it) } catch (_: DateTimeException) { invalido("Zona horaria desconocida") }
        },
        pedidosMaximos = entero(cuerpo, "pedidos_maximos", 1, 10),
        duracionMaxSeg = entero(cuerpo, "duracion_max_seg", 60, 3600),
        ventanaMin = entero(cuerpo, "ventana_no_repeticion_min", 0, 600),
        tema = texto(cuerpo, "tema", max = 10)?.also { if (it !in TEMAS) invalido("Diseño desconocido") },
      )
    }

    private fun texto(cuerpo: Map<String, Any?>, campo: String, max: Int): String? {
      if (!cuerpo.containsKey(campo)) return null
      val v = cuerpo[campo] as? String ?: invalido("$campo debe ser texto")
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
