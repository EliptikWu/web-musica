package com.proyectoN.rokolaBackend.compat

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** RF-045: los instantes del contrato → día y horas de `evento_calendario`, en la zona del bar. */
class EventosTablaTest {
  private val bogota = ZoneId.of("America/Bogota")

  @Test
  fun `se guarda en la hora del bar aunque llegue en UTC`() {
    val (fecha, desde, hasta) = ContratoEventosService.aTabla(
      OffsetDateTime.parse("2026-11-28T01:00:00Z"), OffsetDateTime.parse("2026-11-28T05:00:00Z"), bogota,
    )
    // 01:00 UTC del 28 son las 20:00 del 27 en Bogotá.
    assertEquals(LocalDate.of(2026, 11, 27), fecha)
    assertEquals(LocalTime.of(20, 0), desde)
    assertEquals(LocalTime.of(0, 0), hasta)
  }

  @Test
  fun `un evento que cruza la medianoche termina al otro día`() {
    val (fecha, desde, hasta) = ContratoEventosService.aTabla(
      OffsetDateTime.parse("2026-11-27T21:00:00-05:00"), OffsetDateTime.parse("2026-11-28T02:30:00-05:00"), bogota,
    )
    assertEquals(LocalDate.of(2026, 11, 27), fecha)
    assertEquals(LocalTime.of(21, 0), desde)
    assertEquals(LocalTime.of(2, 30), hasta)
  }

  @Test
  fun `termina antes de empezar, igual o dura 24 h o más es 400`() {
    val casos = listOf(
      "2026-11-27T21:00:00-05:00" to "2026-11-27T20:00:00-05:00",
      "2026-11-27T21:00:00-05:00" to "2026-11-27T21:00:00-05:00",
      "2026-11-27T21:00:00-05:00" to "2026-11-28T21:00:00-05:00",
      "2026-11-27T21:00:00-05:00" to "2026-11-29T01:00:00-05:00",
    )
    for ((inicio, fin) in casos) {
      val e = assertFailsWith<ContratoApiException>("$inicio → $fin") {
        ContratoEventosService.aTabla(OffsetDateTime.parse(inicio), OffsetDateTime.parse(fin), bogota)
      }
      assertEquals("datos_invalidos", e.codigo)
    }
  }
}
