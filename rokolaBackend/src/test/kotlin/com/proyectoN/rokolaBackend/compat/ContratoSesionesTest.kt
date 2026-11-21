package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.compat.ContratoSesionesService.AccionHorario
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaEvent
import com.proyectoN.rokolaBackend.tiemporeal.CambioServicioEvent
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContratoSesionesTest {
  private val bogota = ZoneId.of("America/Bogota")
  // Viernes de 8 p. m. a 2 a. m. El 2026-11-20 es viernes.
  private val franjas = listOf(ReglasHorario.Franja(5, LocalTime.of(20, 0), LocalTime.of(2, 0), true))
  private fun en(dia: Int, hora: Int) = ZonedDateTime.of(2026, 11, dia, hora, 0, 0, 0, bogota)

  @Test
  fun `el tipo de evento se recorta`() {
    assertEquals("karaoke", ContratoSesionesService.leerTipoEvento(mapOf("tipo_evento" to "  karaoke ")))
  }

  @Test
  fun `rechaza un tipo vacio, largo o que no es texto`() {
    listOf(null, emptyMap(), mapOf("tipo_evento" to "   "), mapOf("tipo_evento" to "x".repeat(61)), mapOf("tipo_evento" to 7)).forEach {
      val error = assertFailsWith<ContratoApiException> { ContratoSesionesService.leerTipoEvento(it) }
      assertEquals(HttpStatus.BAD_REQUEST, error.status)
    }
  }

  @Test
  fun `solo acepta los modos del contrato`() {
    assertEquals("pausado", ContratoSesionesService.leerModo(mapOf("modo_qr" to "pausado")))
    listOf(null, mapOf("modo_qr" to "abierto"), mapOf("modo_qr" to 1)).forEach {
      assertEquals(HttpStatus.BAD_REQUEST, assertFailsWith<ContratoApiException> { ContratoSesionesService.leerModo(it) }.status)
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
      ContratoSesionesService.accionHorario(noche, horario, forzado, veto)
    assertEquals(AccionHorario.ABRIR, accion(noche = false, horario = true))
    assertEquals(AccionHorario.NADA, accion(noche = false, horario = true, veto = true))
    assertEquals(AccionHorario.ABRIR, accion(noche = false, horario = false, forzado = true))
    assertEquals(AccionHorario.CERRAR, accion(noche = true, horario = false))
    assertEquals(AccionHorario.NADA, accion(noche = true, horario = false, forzado = true))
    assertEquals(AccionHorario.NADA, accion(noche = false, horario = false))
  }

  @Test
  fun `el fin de la franja sirve antes y despues de medianoche`() {
    assertEquals(en(21, 2), ReglasHorario.cierreContiguo(franjas, en(20, 23)))
    assertEquals(en(21, 2), ReglasHorario.cierreContiguo(franjas, en(21, 1)))
    assertNull(ReglasHorario.cierreContiguo(franjas, en(21, 3)))
    assertNull(ReglasHorario.cierreContiguo(franjas, en(20, 19)))
  }

  @Test
  fun `cerrar a mano cubre las franjas pegadas`() {
    val pegadas = listOf(
      ReglasHorario.Franja(5, LocalTime.of(18, 0), LocalTime.MIDNIGHT, true),
      ReglasHorario.Franja(6, LocalTime.MIDNIGHT, LocalTime.of(2, 0), true),
    )
    assertEquals(en(21, 2), ReglasHorario.cierreContiguo(pegadas, en(20, 23)))
  }

  @Test
  fun `un horario de toda la semana no deja el calculo sin terminar`() {
    val siempre = (1..7).map { ReglasHorario.Franja(it, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.minusMinutes(1), true) }
    assertTrue(ReglasHorario.cierreContiguo(siempre, en(20, 23)) != null)
  }
}
