package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioEfectivoService
import com.proyectoN.rokolaBackend.compat.ContratoServicioService.AccionHorario
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaEvent
import com.proyectoN.rokolaBackend.tiemporeal.CambioServicioEvent
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContratoSesionesTest {
  private val bogota = ZoneId.of("America/Bogota")
  private fun en(dia: Int, hora: Int) = ZonedDateTime.of(2026, 11, dia, hora, 0, 0, 0, bogota).toOffsetDateTime()

  @Test
  fun `el tipo de evento se recorta`() {
    assertEquals("karaoke", ContratoServicioService.leerTipoEvento(mapOf("tipo_evento" to "  karaoke ")))
  }

  @Test
  fun `rechaza un tipo vacio, largo o que no es texto`() {
    listOf(null, emptyMap(), mapOf("tipo_evento" to "   "), mapOf("tipo_evento" to "x".repeat(61)), mapOf("tipo_evento" to 7)).forEach {
      val error = assertFailsWith<ContratoApiException> { ContratoServicioService.leerTipoEvento(it) }
      assertEquals(HttpStatus.BAD_REQUEST, error.status)
    }
  }

  @Test
  fun `solo acepta los modos del contrato`() {
    assertEquals("pausado", ContratoServicioService.leerModo(mapOf("modo_qr" to "pausado")))
    listOf(null, mapOf("modo_qr" to "abierto"), mapOf("modo_qr" to 1)).forEach {
      assertEquals(HttpStatus.BAD_REQUEST, assertFailsWith<ContratoApiException> { ContratoServicioService.leerModo(it) }.status)
    }
  }

  @Test
  fun `la foto sin noche no tiene version menor que la ultima de la noche cerrada`() {
    val versiones = ContratoVersionService()
    val deLaNoche = versiones.actual(7)
    versiones.alCambiar(CambioColaEvent(7, "sesion_cerrada"))
    val alCerrar = versiones.actual(7)
    assertTrue(alCerrar > deLaNoche)
    assertTrue(versiones.actual(null) >= alCerrar)
  }

  @Test
  fun `un cambio de servicio sin noche produce una foto nueva`() {
    val versiones = ContratoVersionService()
    val antes = versiones.actual(null)
    versiones.alCambiarServicio(CambioServicioEvent("demo"))
    assertTrue(versiones.actual(null) > antes)
  }

  @Test
  fun `el horario abre, cierra y respeta el veto y la apertura forzada`() {
    fun accion(noche: Boolean, horario: Boolean, forzado: Boolean = false, veto: Boolean = false) =
      ContratoServicioService.accionHorario(noche, horario, forzado, veto)
    assertEquals(AccionHorario.ABRIR, accion(noche = false, horario = true))
    assertEquals(AccionHorario.NADA, accion(noche = false, horario = true, veto = true))
    assertEquals(AccionHorario.ABRIR, accion(noche = false, horario = false, forzado = true))
    assertEquals(AccionHorario.CERRAR, accion(noche = true, horario = false))
    assertEquals(AccionHorario.NADA, accion(noche = true, horario = false, forzado = true))
    assertEquals(AccionHorario.NADA, accion(noche = false, horario = false))
  }

  @Test
  fun `cerrar a mano cubre las franjas pegadas`() {
    // Viernes 18:00 a 00:00 y sábado 00:00 a 02:00. El 2026-11-20 es viernes.
    val pegadas = listOf(
      HorarioEfectivoService.Franja(5, LocalTime.of(18, 0), LocalTime.MIDNIGHT),
      HorarioEfectivoService.Franja(6, LocalTime.MIDNIGHT, LocalTime.of(2, 0)),
    )
    val ventanas = HorarioEfectivoService.calcular(pegadas, emptyList(), LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 21), bogota)
    assertEquals(en(21, 2), HorarioEfectivoService.vigente(ventanas, en(20, 23))?.fin)
    assertNull(HorarioEfectivoService.vigente(ventanas, en(21, 3)))
  }
}
