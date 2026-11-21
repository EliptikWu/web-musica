package com.proyectoN.rokolaBackend.acceso

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * El horario que vale de verdad (RF-036, RF-045): las franjas semanales, salvo el día que tiene un
 * evento en el calendario, que abre y cierra con las horas del evento (las franjas de ese día no
 * cuentan). Un día con varios eventos abre en cada uno. Lo usan `ahora` (lo que ven el teléfono, la
 * TV y el panel), pedir, la reproducción automática y «hasta el fin de la noche» de los bloqueos.
 *
 * Una franja o un evento que cruza la medianoche pertenece al día en que empieza. Todo en la zona
 * del bar. Hecho por el equipo de frontend (2026-11-21, D14).
 */
@Service
class HorarioEfectivoService(private val jdbc: JdbcTemplate) {
  /** Las ventanas abiertas desde ayer hasta dentro de 8 días, en orden. */
  fun ventanas(idBar: Long, zona: ZoneId, clock: Clock = Clock.systemUTC()): List<Ventana> {
    val hoy = clock.instant().atZone(zona).toLocalDate()
    val desde = hoy.minusDays(1)
    val hasta = hoy.plusDays(8)
    val franjas = jdbc.query(
      "SELECT dia_semana, hora_inicio, hora_fin FROM public.horario_operacion WHERE id_establecimiento = ? AND activo = TRUE",
      { rs, _ -> Franja(rs.getInt(1), rs.getObject(2, LocalTime::class.java), rs.getObject(3, LocalTime::class.java)) },
      idBar,
    )
    val eventos = jdbc.query(
      """SELECT id_evento_calendario, fecha, hora_inicio, hora_fin, tipo FROM public.evento_calendario
         WHERE id_establecimiento = ? AND fecha BETWEEN ? AND ?""",
      { rs, _ ->
        Evento(rs.getLong(1), rs.getObject(2, LocalDate::class.java), rs.getObject(3, LocalTime::class.java), rs.getObject(4, LocalTime::class.java), rs.getString(5))
      },
      idBar, desde, hasta,
    )
    return calcular(franjas, eventos, desde, hasta, zona)
  }

  fun estaAbierto(idBar: Long, zona: ZoneId, clock: Clock = Clock.systemUTC()): Boolean =
    vigente(ventanas(idBar, zona, clock), OffsetDateTime.now(clock)) != null

  /** Cuándo cierra la ventana abierta ahora, o null si está cerrado. */
  fun cierre(idBar: Long, zona: ZoneId, clock: Clock = Clock.systemUTC()): OffsetDateTime? =
    vigente(ventanas(idBar, zona, clock), OffsetDateTime.now(clock))?.fin?.atZoneSameInstant(zona)?.toOffsetDateTime()

  /**
   * El evento de esta noche, para ligarle la noche: el que está en curso o, si no hay, el próximo
   * que empieza hoy (fecha del bar). Null si hoy no hay eventos por delante.
   */
  fun eventoDeHoy(idBar: Long, zona: ZoneId, clock: Clock = Clock.systemUTC()): EventoDeHoy? {
    val ahora = OffsetDateTime.now(clock)
    val candidatos = ventanas(idBar, zona, clock).filter { it.evento != null && ahora.isBefore(it.fin) }
    val hoy = ahora.atZoneSameInstant(zona).toLocalDate()
    val elegido = candidatos.firstOrNull { !ahora.isBefore(it.inicio) }
      ?: candidatos.firstOrNull { it.inicio.atZoneSameInstant(zona).toLocalDate() == hoy }
      ?: return null
    return EventoDeHoy(elegido.evento!!, elegido.tipoEvento!!)
  }

  data class Franja(val dia: Int, val inicio: LocalTime, val fin: LocalTime)
  data class Evento(val id: Long, val fecha: LocalDate, val inicio: LocalTime, val fin: LocalTime, val tipo: String)
  data class EventoDeHoy(val id: Long, val tipo: String)

  /** Una ventana abierta; `evento` es el del calendario si la abre un evento. */
  data class Ventana(val inicio: OffsetDateTime, val fin: OffsetDateTime, val evento: Long? = null, val tipoEvento: String? = null)

  companion object {
    fun vigente(ventanas: List<Ventana>, ahora: OffsetDateTime): Ventana? =
      ventanas.firstOrNull { !ahora.isBefore(it.inicio) && ahora.isBefore(it.fin) }

    /** Regla pura: por cada día, los eventos de ese día o, si no tiene, sus franjas. */
    fun calcular(franjas: List<Franja>, eventos: List<Evento>, desde: LocalDate, hasta: LocalDate, zona: ZoneId): List<Ventana> {
      val porDia = eventos.groupBy { it.fecha }
      return generateSequence(desde) { it.plusDays(1) }.takeWhile { !it.isAfter(hasta) }.flatMap { dia ->
        val delDia = porDia[dia]
        if (delDia != null) {
          delDia.filter { it.inicio != it.fin }.map { ventana(dia, it.inicio, it.fin, zona).copy(evento = it.id, tipoEvento = it.tipo) }
        } else {
          franjas.filter { it.dia == dia.dayOfWeek.value && it.inicio != it.fin }.map { ventana(dia, it.inicio, it.fin, zona) }
        }
      }.sortedBy { it.inicio }.toList()
    }

    private fun ventana(dia: LocalDate, inicio: LocalTime, fin: LocalTime, zona: ZoneId) = Ventana(
      dia.atTime(inicio).atZone(zona).toOffsetDateTime(),
      (if (fin.isAfter(inicio)) dia else dia.plusDays(1)).atTime(fin).atZone(zona).toOffsetDateTime(),
    )
  }
}
