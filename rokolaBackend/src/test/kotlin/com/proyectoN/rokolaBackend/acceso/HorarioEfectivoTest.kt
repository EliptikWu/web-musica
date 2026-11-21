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

/** RF-036 y RF-045: un evento suma horas al horario de siempre y nunca quita. */
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
  fun `un evento dentro del horario de siempre no quita horas`() {
    // Abre a las 8 como siempre; el evento es de 10 p. m. a 1 a. m.
    val v = ventanas(listOf(Evento(7, viernes, LocalTime.of(22, 0), LocalTime.of(1, 0), "karaoke")))
    val noche = HorarioEfectivoService.vigente(v, en(viernes, 20, 30))!!
    assertEquals(en(viernes, 20) to en(viernes.plusDays(1), 2), noche.inicio to noche.fin)
    // Esa noche es la del evento, aunque a las 8:30 todavía no haya empezado.
    assertEquals(7L, noche.evento)
    assertEquals("karaoke", noche.tipoEvento)
    // A la 1:30 sigue abierto por el horario de siempre (el evento acabó a la 1).
    assertEquals(en(viernes.plusDays(1), 2), HorarioEfectivoService.vigente(v, en(viernes.plusDays(1), 1, 30))?.fin)
  }

  @Test
  fun `un evento que empieza antes o termina después suma esas horas`() {
    // Karaoke de 6 p. m. a 3 a. m.: ese viernes abre antes y cierra más tarde.
    val v = ventanas(listOf(Evento(7, viernes, LocalTime.of(18, 0), LocalTime.of(3, 0), "karaoke")))
    val noche = v.single { it.inicio.toLocalDate() == viernes }
    assertEquals(en(viernes, 18) to en(viernes.plusDays(1), 3), noche.inicio to noche.fin)
    // El sábado sigue con su franja, sin evento.
    assertNull(v.single { it.inicio.toLocalDate() == viernes.plusDays(1) }.evento)
  }

  @Test
  fun `un evento en un día sin franjas abre ese día y uno aparte queda en su propia ventana`() {
    // Jueves sin franja: un partido de 3 p. m. a 6 p. m. abre el bar.
    val jueves = viernes.minusDays(1)
    val v = ventanas(listOf(Evento(1, jueves, LocalTime.of(15, 0), LocalTime.of(18, 0), "partido"), Evento(2, viernes, LocalTime.of(14, 0), LocalTime.of(16, 0), "partido")))
    assertEquals(1L, HorarioEfectivoService.vigente(v, en(jueves, 16))?.evento)
    // El viernes, el partido de 2 a 4 p. m. es aparte; entre 4 y 8 p. m. está cerrado.
    assertEquals(2L, HorarioEfectivoService.vigente(v, en(viernes, 15))?.evento)
    assertNull(HorarioEfectivoService.vigente(v, en(viernes, 17)))
    assertEquals(null, HorarioEfectivoService.vigente(v, en(viernes, 21))?.evento)
  }
}
