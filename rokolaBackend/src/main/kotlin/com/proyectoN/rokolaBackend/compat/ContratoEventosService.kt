package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * El calendario de eventos del bar (RF-045): solo el Administrador, solo su bar. Las reglas son
 * las de la simulación (`frontend/web/simulacion/gestion.ts`): un evento termina después de
 * empezar, de uno que ya terminó solo se corrige la descripción y uno que ya empezó no se elimina.
 *
 * La tabla `evento_calendario` guarda el día y las horas en la zona del bar; el contrato, instantes.
 * Si la hora de fin es menor que la de inicio, el evento termina al día siguiente. Por eso un
 * evento dura menos de 24 h *(propuesto)*: lo que la tabla puede guardar.
 *
 * Un evento cambia el horario de su día (`HorarioEfectivoService`) y la noche de hoy se liga a él
 * (`ContratoServicioService.ligarNocheAbierta`): cada cambio del calendario avisa al teléfono, a
 * la TV y al panel.
 *
 * Hecho por el equipo de frontend (2026-11-21, D14).
 */
@Service
class ContratoEventosService(
  private val jdbc: JdbcTemplate,
  private val bitacora: ContratoBitacora,
  private val panel: CambioPanelPublisher,
  private val servicio: ContratoServicioService,
) {
  private data class Evento(
    val id: Long, val nombre: String, val tipo: String,
    val inicio: OffsetDateTime, val fin: OffsetDateTime, val descripcion: String?,
  ) {
    fun aContrato(): Map<String, Any?> = mapOf(
      "id" to id.toString(), "nombre" to nombre, "tipo_evento" to tipo,
      "inicio" to inicio, "fin" to fin, "descripcion" to descripcion,
    )
  }

  fun listar(p: ContratoPanelAcceso.Personal): Map<String, Any?> =
    mapOf("eventos" to todos(p).sortedBy { it.inicio }.map(Evento::aContrato))

  @Transactional
  fun crear(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, CAMPOS)
    val nombre = ContratoCuerpo.texto(datos, "nombre", max = 150, obligatorio = true)!!
    val tipo = tipo(datos) ?: ContratoCuerpo.invalido("Falta tipo_evento")
    val inicio = instante(datos, "inicio") ?: ContratoCuerpo.invalido("Falta inicio")
    val fin = instante(datos, "fin") ?: ContratoCuerpo.invalido("Falta fin")
    val descripcion = descripcion(datos)
    val (fecha, desde, hasta) = aTabla(inicio, fin, ZoneId.of(p.barZona))
    val id = jdbc.queryForObject(
      """INSERT INTO public.evento_calendario (id_establecimiento, nombre, descripcion, fecha, hora_inicio, hora_fin, tipo)
         VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_evento_calendario""",
      Long::class.javaObjectType, p.barId, nombre, descripcion, fecha, desde, hasta, tipo,
    )!!
    bitacora.anotar(p, "editar_evento", "evento", id, nombre, "creado")
    cambioDeHorario(p)
    return porId(p, id)!!.aContrato()
  }

  @Transactional
  fun editar(p: ContratoPanelAcceso.Personal, idTexto: String, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, CAMPOS)
    val e = existente(p, idTexto, bloquear = true)
    // RF-045: de un evento que ya terminó solo se corrige la descripción.
    if (e.fin.isBefore(OffsetDateTime.now()) && datos.keys.any { it != "descripcion" }) {
      ContratoCuerpo.conflicto("De un evento pasado solo se puede cambiar la descripción")
    }
    val nombre = ContratoCuerpo.texto(datos, "nombre", max = 150) ?: e.nombre
    val tipo = tipo(datos) ?: e.tipo
    val inicio = instante(datos, "inicio") ?: e.inicio
    val fin = instante(datos, "fin") ?: e.fin
    val descripcion = if (datos.containsKey("descripcion")) descripcion(datos) else e.descripcion
    val (fecha, desde, hasta) = aTabla(inicio, fin, ZoneId.of(p.barZona))
    jdbc.update(
      """UPDATE public.evento_calendario SET nombre = ?, descripcion = ?, fecha = ?, hora_inicio = ?, hora_fin = ?, tipo = ?
         WHERE id_evento_calendario = ? AND id_establecimiento = ?""",
      nombre, descripcion, fecha, desde, hasta, tipo, e.id, p.barId,
    )
    bitacora.anotar(p, "editar_evento", "evento", e.id, nombre, "editado")
    cambioDeHorario(p)
    return porId(p, e.id)!!.aContrato()
  }

  @Transactional
  fun eliminar(p: ContratoPanelAcceso.Personal, idTexto: String) {
    val e = existente(p, idTexto, bloquear = true)
    if (e.inicio.isBefore(OffsetDateTime.now())) ContratoCuerpo.conflicto("Un evento que ya empezó no se elimina")
    // La noche abierta ligada a este evento vuelve a ser normal; una ya cerrada conserva su tipo.
    jdbc.update(
      """UPDATE public.sesion SET id_evento_calendario = NULL,
           tipo_evento = CASE WHEN estado = 'abierta' THEN 'normal' ELSE tipo_evento END
         WHERE id_evento_calendario = ?""",
      e.id,
    )
    jdbc.update("DELETE FROM public.evento_calendario WHERE id_evento_calendario = ? AND id_establecimiento = ?", e.id, p.barId)
    bitacora.anotar(p, "editar_evento", "evento", e.id, e.nombre, "eliminado")
    cambioDeHorario(p)
  }

  /** El horario de hoy pudo cambiar: se religa la noche abierta y se avisa a todos. */
  private fun cambioDeHorario(p: ContratoPanelAcceso.Personal) {
    servicio.ligarNocheAbierta(p)
    servicio.avisarHorario(p)
    panel.publicar(p.barSlug, setOf("eventos", "noche", "bitacora"))
  }

  private fun tipo(datos: Map<String, Any?>): String? =
    if (!datos.containsKey("tipo_evento")) null else ContratoCuerpo.opcion(datos["tipo_evento"] as? String, "tipo_evento", TIPOS) { it }

  private fun descripcion(datos: Map<String, Any?>): String? =
    ContratoCuerpo.texto(datos, "descripcion", max = 1000, vacioPermitido = true)?.takeIf { it.isNotEmpty() }

  private fun instante(datos: Map<String, Any?>, campo: String): OffsetDateTime? {
    if (!datos.containsKey(campo)) return null
    val texto = datos[campo] as? String ?: ContratoCuerpo.invalido("$campo debe ser una fecha ISO 8601")
    return try {
      OffsetDateTime.parse(texto)
    } catch (_: DateTimeParseException) {
      ContratoCuerpo.invalido("$campo debe ser una fecha ISO 8601 con zona")
    }
  }

  private fun existente(p: ContratoPanelAcceso.Personal, idTexto: String, bloquear: Boolean): Evento {
    val id = ContratoCuerpo.id(idTexto, "Ese evento no existe")
    return porId(p, id, bloquear) ?: ContratoCuerpo.noEncontrado("Ese evento no existe")
  }

  private fun porId(p: ContratoPanelAcceso.Personal, id: Long, bloquear: Boolean = false): Evento? = jdbc.query(
    "$SELECT AND id_evento_calendario = ?${if (bloquear) " FOR UPDATE" else ""}",
    { rs, _ -> fila(rs, ZoneId.of(p.barZona)) }, p.barId, id,
  ).firstOrNull()

  private fun todos(p: ContratoPanelAcceso.Personal): List<Evento> =
    jdbc.query(SELECT, { rs, _ -> fila(rs, ZoneId.of(p.barZona)) }, p.barId)

  private fun fila(rs: ResultSet, zona: ZoneId): Evento {
    val fecha = rs.getObject("fecha", LocalDate::class.java)
    val desde = rs.getObject("hora_inicio", LocalTime::class.java)
    val hasta = rs.getObject("hora_fin", LocalTime::class.java)
    return Evento(
      id = rs.getLong("id_evento_calendario"),
      nombre = rs.getString("nombre"),
      tipo = rs.getString("tipo"),
      inicio = fecha.atTime(desde).atZone(zona).toOffsetDateTime(),
      fin = (if (hasta.isAfter(desde)) fecha else fecha.plusDays(1)).atTime(hasta).atZone(zona).toOffsetDateTime(),
      descripcion = rs.getString("descripcion"),
    )
  }

  companion object {
    private val CAMPOS = setOf("nombre", "tipo_evento", "inicio", "fin", "descripcion")
    val TIPOS = setOf("normal", "karaoke", "partido", "tematica", "otro")
    // Una fila antigua sin fecha u horas no es un evento del calendario y no debe romper la lista.
    private const val SELECT = """SELECT id_evento_calendario, COALESCE(nombre, 'Sin nombre') AS nombre,
         COALESCE(tipo, 'normal') AS tipo, fecha, hora_inicio, hora_fin, descripcion
       FROM public.evento_calendario
       WHERE fecha IS NOT NULL AND hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND id_establecimiento = ?"""

    /**
     * Instantes del contrato → día y horas de la tabla en la zona del bar. Termina después de
     * empezar y antes de 24 h; si no, 400.
     */
    fun aTabla(inicio: OffsetDateTime, fin: OffsetDateTime, zona: ZoneId): Triple<LocalDate, LocalTime, LocalTime> {
      if (!fin.isAfter(inicio)) ContratoCuerpo.invalido("El evento tiene que terminar después de empezar")
      if (!fin.isBefore(inicio.plusDays(1))) ContratoCuerpo.invalido("Un evento dura menos de 24 horas")
      val i = inicio.atZoneSameInstant(zona)
      val f = fin.atZoneSameInstant(zona)
      val desde = i.toLocalTime().withNano(0)
      val hasta = f.toLocalTime().withNano(0)
      if (desde == hasta) ContratoCuerpo.invalido("El evento tiene que terminar después de empezar")
      return Triple(i.toLocalDate(), desde, hasta)
    }
  }
}
