package com.proyectoN.rokolaBackend.compat

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** RF-036: las reglas del horario, las mismas de `frontend/web/lib/reglas/horario.ts`. */
class ReglasHorarioTest {
  private fun franja(dia: Int, inicio: String, fin: String, activo: Boolean = true) =
    mapOf("dia_semana" to dia, "hora_inicio" to inicio, "hora_fin" to fin, "activo" to activo)

  private fun leer(vararg franjas: Map<String, Any>) = ReglasHorario.leer(mapOf("franjas" to franjas.toList()))

  @Test
  fun `acepta franjas que cruzan la medianoche sin pisarse`() {
    val f = leer(franja(5, "20:00", "02:00"), franja(6, "20:00", "03:00"), franja(1, "08:00", "12:00", activo = false))
    assertEquals(3, f.size)
    assertEquals(LocalTime.of(2, 0), f[0].fin)
  }

  @Test
  fun `empezar y terminar a la misma hora es 400`() {
    val e = assertFailsWith<ContratoApiException> { leer(franja(1, "10:00", "10:00", activo = false)) }
    assertEquals("datos_invalidos", e.codigo)
  }

  @Test
  fun `dos franjas activas que se pisan, aunque una cruce la medianoche, son 409`() {
    // El viernes 22:00–03:00 se mete en el sábado 01:00–04:00.
    val e = assertFailsWith<ContratoApiException> { leer(franja(5, "22:00", "03:00"), franja(6, "01:00", "04:00")) }
    assertEquals("conflicto", e.codigo)
    // El domingo 23:00–01:00 da la vuelta a la semana y pisa el lunes 00:30.
    assertFailsWith<ContratoApiException> { leer(franja(7, "23:00", "01:00"), franja(1, "00:30", "02:00")) }
    // Si una de las dos está apagada, no hay conflicto.
    leer(franja(5, "22:00", "03:00"), franja(6, "01:00", "04:00", activo = false))
  }

  @Test
  fun `formato, rango y campos de más se rechazan`() {
    val malos = listOf(
      mapOf("franjas" to listOf(franja(8, "10:00", "11:00"))),
      mapOf("franjas" to listOf(franja(1, "9:00", "11:00"))),
      mapOf("franjas" to listOf(franja(1, "24:00", "11:00"))),
      mapOf("franjas" to listOf(franja(1, "10:00", "11:00") + ("extra" to 1))),
      mapOf("franjas" to "no"),
      emptyMap(),
      mapOf("franjas" to emptyList<Any>(), "otro" to true),
    )
    for (cuerpo in malos) {
      val e = assertFailsWith<ContratoApiException>("$cuerpo") { ReglasHorario.leer(cuerpo) }
      assertEquals("datos_invalidos", e.codigo)
    }
  }

  @Test
  fun `un horario vacío es válido (el bar no abre por horario)`() {
    assertEquals(0, ReglasHorario.leer(mapOf("franjas" to emptyList<Any>())).size)
  }

  @Test
  fun `el cierre es el fin de la franja vigente, también después de medianoche`() {
    val zona = ZoneId.of("America/Bogota")
    val franjas = leer(franja(5, "20:00", "02:00"))
    // Sábado 21 de noviembre de 2026 a la 1:15 a. m.: sigue la franja del viernes.
    val sabado = ZonedDateTime.of(2026, 11, 21, 1, 15, 0, 0, zona)
    assertEquals(ZonedDateTime.of(2026, 11, 21, 2, 0, 0, 0, zona), ReglasHorario.cierre(franjas, sabado))
    // Viernes a las 21:30 cierra a las 2:00 del sábado.
    assertEquals(ZonedDateTime.of(2026, 11, 21, 2, 0, 0, 0, zona), ReglasHorario.cierre(franjas, sabado.minusHours(3).minusMinutes(45)))
    // Sábado a las 2:00 ya cerró.
    assertNull(ReglasHorario.cierre(franjas, sabado.plusMinutes(45)))
  }
}
