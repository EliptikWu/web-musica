package com.proyectoN.rokolaBackend.compat

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * La bitácora (`accion_admin`, RF-044, RN-11): escribir una línea y leerla como `AccionAdmin` del
 * contrato. `detalle` es JSONB: `texto` es lo que muestra el panel y `objetivo_nombre` el nombre
 * de lo que no es una canción (un usuario, una lista), que no se puede sacar de otra tabla si
 * después se borra o se renombra. La vista de la noche y `GET /panel/bitacora` leen igual.
 */
@Component
class ContratoBitacora(private val jdbc: JdbcTemplate) {
  fun anotar(
    p: ContratoPanelAcceso.Personal?,
    tipo: String,
    objetivoTipo: String? = null,
    objetivoId: Long? = null,
    objetivoNombre: String? = null,
    texto: String? = null,
    idBar: Long? = p?.barId,
  ) {
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, ?, ?, ?, jsonb_strip_nulls(jsonb_build_object('texto', ?::text, 'objetivo_nombre', ?::text)))""",
      idBar, p?.id, tipo, objetivoTipo, objetivoId, texto, objetivoNombre,
    )
  }

  /** Lo de esta noche (o de las últimas 12 h sin noche abierta), lo más reciente primero. */
  fun recientes(idBar: Long, desde: OffsetDateTime?, limite: Int = 20): List<Map<String, Any?>> =
    consultar(idBar, Filtro(desde = desde ?: OffsetDateTime.now().minusHours(12)), limite)

  /**
   * `GET /panel/bitacora` (RF-044): por fechas, persona (`sistema` = sin persona) y tipo, de 25 en
   * 25. El cursor es el id de la última acción de la página: lo nuevo que llegue mientras se lee
   * no desordena las páginas siguientes.
   */
  fun pagina(idBar: Long, desde: String?, hasta: String?, usuario: String?, tipo: String?, cursor: String?): Map<String, Any?> {
    val filtro = Filtro(
      desde = fecha(desde, "desde"),
      hasta = fecha(hasta, "hasta"),
      usuario = usuario?.takeIf { it.isNotBlank() }?.let {
        if (it == "sistema") SISTEMA else it.toLongOrNull()?.takeIf { n -> n > 0 } ?: ContratoCuerpo.invalido("usuario: un id o «sistema»")
      },
      tipo = tipo?.takeIf { it.isNotBlank() }?.also { if (it !in TIPOS_ACCION) ContratoCuerpo.invalido("tipo de acción desconocido") },
      antesDe = cursor?.takeIf { it.isNotBlank() }?.let { it.toLongOrNull()?.takeIf { n -> n > 0 } ?: ContratoCuerpo.invalido("cursor inválido") },
    )
    // Una de más para saber si hay otra página sin contar todo.
    val filas = consultar(idBar, filtro, POR_PAGINA + 1)
    val acciones = filas.take(POR_PAGINA)
    return mapOf("acciones" to acciones, "siguiente" to if (filas.size > POR_PAGINA) acciones.last()["id"] else null)
  }

  private data class Filtro(
    val desde: OffsetDateTime? = null,
    val hasta: OffsetDateTime? = null,
    val usuario: Long? = null,
    val tipo: String? = null,
    val antesDe: Long? = null,
  )

  private fun consultar(idBar: Long, f: Filtro, limite: Int): List<Map<String, Any?>> {
    val condiciones = mutableListOf("a.id_establecimiento = ?")
    val valores = mutableListOf<Any>(idBar)
    f.desde?.let { condiciones += "a.fecha >= ?"; valores += it }
    // «Hasta» llega con segundos y la fecha guarda microsegundos: incluye todo ese último segundo.
    f.hasta?.let { condiciones += "a.fecha < ?"; valores += it.plusSeconds(1) }
    when (f.usuario) {
      null -> Unit
      SISTEMA -> condiciones += "a.id_usuario IS NULL"
      else -> { condiciones += "a.id_usuario = ?"; valores += f.usuario }
    }
    // Solo los tipos que el contrato conoce, filtrados antes del LIMIT para no mostrar menos.
    if (f.tipo != null) {
      condiciones += "a.tipo = ?"; valores += f.tipo
    } else {
      condiciones += "a.tipo IN (${TIPOS_ACCION.joinToString { "?" }})"; valores.addAll(TIPOS_ACCION)
    }
    f.antesDe?.let {
      condiciones += "(a.fecha, a.id_accion) < (SELECT x.fecha, x.id_accion FROM public.accion_admin x WHERE x.id_accion = ? AND x.id_establecimiento = ?)"
      valores += it; valores += idBar
    }
    return jdbc.query(
      """SELECT a.id_accion, a.fecha, a.tipo, a.objetivo_tipo, a.detalle->>'texto' AS texto,
                a.detalle->>'objetivo_nombre' AS objetivo_nombre,
                u.id_usuario, u.nombre AS usuario, c.titulo, c.artista, c.canal
         FROM public.accion_admin a
         LEFT JOIN public.usuario u ON u.id_usuario = a.id_usuario
         LEFT JOIN public.cancion c ON a.objetivo_tipo = 'cancion' AND c.id_cancion = a.objetivo_id
         WHERE ${condiciones.joinToString(" AND ")}
         ORDER BY a.fecha DESC, a.id_accion DESC LIMIT $limite""",
      { rs, _ -> fila(rs) },
      *valores.toTypedArray(),
    )
  }

  private fun fila(rs: java.sql.ResultSet): Map<String, Any?> {
    val idUsuario = rs.getObject("id_usuario")?.let { (it as Number).toLong() }
    return mapOf(
      "id" to rs.getLong("id_accion").toString(),
      "fecha" to rs.getObject("fecha", OffsetDateTime::class.java),
      "usuario" to idUsuario?.let { mapOf("id" to it.toString(), "nombre" to rs.getString("usuario")) },
      "tipo" to rs.getString("tipo"),
      "objetivo" to (
        rs.getString("titulo")?.let { titulo ->
          mapOf(
            "tipo" to "cancion",
            "nombre" to titulo,
            "detalle" to (rs.getString("artista")?.takeIf(String::isNotBlank) ?: rs.getString("canal")),
          )
        } ?: rs.getString("objetivo_nombre")?.let { nombre ->
          rs.getString("objetivo_tipo")?.takeIf { it in TIPOS_OBJETIVO }?.let { mapOf("tipo" to it, "nombre" to nombre, "detalle" to null) }
        }
      ),
      "detalle" to rs.getString("texto"),
    )
  }

  private fun fecha(texto: String?, campo: String): OffsetDateTime? = texto?.takeIf { it.isNotBlank() }?.let {
    try {
      OffsetDateTime.parse(it)
    } catch (_: DateTimeParseException) {
      ContratoCuerpo.invalido("$campo debe ser una fecha ISO 8601 con zona")
    }
  }

  companion object {
    const val POR_PAGINA = 25
    private const val SISTEMA = -1L

    /** `TipoAccion` del contrato. */
    val TIPOS_ACCION = setOf(
      "saltar", "bloquear", "desbloquear", "extender_bloqueo", "retirar",
      "pausar_solicitudes", "reanudar_solicitudes", "forzar_apertura",
      "pausar_reproduccion", "reanudar_reproduccion", "cambiar_volumen",
      "mantenimiento", "abrir_sesion", "cerrar_sesion",
      "editar_horario", "editar_lista", "activar_lista",
      "crear_usuario", "editar_usuario", "desactivar_usuario", "reactivar_usuario", "restablecer_clave",
      "editar_establecimiento", "regenerar_qr", "editar_evento", "editar_umbrales",
      "vincular_pantalla", "desvincular_pantalla",
      "error_youtube", "intento_no_permitido",
    )

    /** `AccionAdmin.objetivo.tipo` del contrato, salvo la canción (que sale de su tabla). */
    private val TIPOS_OBJETIVO = setOf("usuario", "lista", "horario", "servicio", "sesion", "establecimiento", "evento", "pantalla")
  }
}
