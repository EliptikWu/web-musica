package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaEvent
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ContratoSesionesTest {
  @Test
  fun `el tipo de evento se recorta`() {
    assertEquals("karaoke", ContratoSesionesService.leerTipoEvento(mapOf("tipo_evento" to "  karaoke ")))
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
  fun `rechaza un tipo vacio, largo o que no es texto`() {
    listOf(null, emptyMap(), mapOf("tipo_evento" to "   "), mapOf("tipo_evento" to "x".repeat(61)), mapOf("tipo_evento" to 7)).forEach {
      val error = assertFailsWith<ContratoApiException> { ContratoSesionesService.leerTipoEvento(it) }
      assertEquals(HttpStatus.BAD_REQUEST, error.status)
    }
  }
}
