package com.proyectoN.rokolaBackend.acceso

import com.proyectoN.rokolaBackend.acceso.HorarioEfectivoService.Evento
import com.proyectoN.rokolaBackend.acceso.HorarioEfectivoService.Franja
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** RF-036 y RF-045: el día con evento abre con las horas del evento; los demás, con sus franjas. */
class HorarioEfectivoTest {
  private val bogota = ZoneId.of("America/Bogota")
  // Viernes 27 de noviembre de 2026.
  private val viernes = LocalDate.of(2026, 11, 27)
  private val franjas = listOf(
    Franja(5, LocalTime.of(20, 0), LocalTime.of(2, 0)), // viernes 20:00–02:00
    Franja(6, LocalTime.of(20, 0), LocalTime.of(2, 0)), // sábado 20:00–02:00
  )

  private fun en(dia: LocalDate, h: Int, m: Int = 0): OffsetDateTime = dia.atTime(h, m).atZone(bogota).toOffsetDateTime()

  private fun ventanas(eventos: List<Evento>) =
    HorarioEfectivoService.calcular(franjas, eventos, viernes.minusDays(1), viernes.plusDays(2), bogota)

  @Test
  fun `sin eventos valen las franjas, también después de medianoche`() {
    val v = ventanas(emptyList())
    assertEquals(listOf(en(viernes, 20) to en(viernes.plusDays(1), 2), en(viernes.plusDays(1), 20) to en(viernes.plusDays(2), 2)), v.map { it.inicio to it.fin })
    assertEquals(en(viernes.plusDays(1), 2), HorarioEfectivoService.vigente(v, en(viernes.plusDays(1), 1, 30))?.fin)
    assertNull(HorarioEfectivoService.vigente(v, en(viernes.plusDays(1), 2)))
  }

  @Test
  fun `el evento reemplaza las franjas de su día y no las de los demás`() {
    // Karaoke el viernes de 18:00 a 03:00: ese día abre antes y cierra más tarde.
    val v = ventanas(listOf(Evento(7, viernes, LocalTime.of(18, 0), LocalTime.of(3, 0), "karaoke")))
    val viernesV = v.single { it.inicio.toLocalDate() == viernes }
    assertEquals(en(viernes, 18) to en(viernes.plusDays(1), 3), viernesV.inicio to viernesV.fin)
    assertEquals(7L, viernesV.evento)
    assertEquals("karaoke", viernesV.tipoEvento)
    // A las 19:00 del viernes está abierto por el evento; el sábado sigue con su franja.
    assertEquals(7L, HorarioEfectivoService.vigente(v, en(viernes, 19))?.evento)
    assertEquals(null, v.single { it.inicio.toLocalDate() == viernes.plusDays(1) }.evento)
  }

  @Test
  fun `un evento en un día sin franjas abre ese día y uno más corto cierra antes`() {
    // Jueves sin franja: un partido de 15:00 a 18:00 abre el bar.
    val jueves = viernes.minusDays(1)
    val v = ventanas(listOf(Evento(1, jueves, LocalTime.of(15, 0), LocalTime.of(18, 0), "partido"), Evento(2, viernes, LocalTime.of(20, 0), LocalTime.of(23, 0), "tematica")))
    assertEquals(1L, HorarioEfectivoService.vigente(v, en(jueves, 16))?.evento)
    // El viernes el evento termina a las 23:00, antes que la franja normal (02:00).
    assertNull(HorarioEfectivoService.vigente(v, en(viernes, 23, 30)))
  }
}
