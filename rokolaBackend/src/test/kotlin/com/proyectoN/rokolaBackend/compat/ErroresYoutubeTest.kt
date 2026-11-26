package com.proyectoN.rokolaBackend.compat

import kotlin.test.Test
import kotlin.test.assertEquals

/** RF-019: el aviso del panel dice qué canciones no pudo poner YouTube, no solo cuántas. */
class ErroresYoutubeTest {
  @Test
  fun `nombra hasta tres y cuenta el resto`() {
    assertEquals("«Monaco» (1 en los últimos 5 minutos)", ContratoNocheService.detalleErroresYoutube(listOf("Monaco"), 1))
    assertEquals(
      "«A», «B», «C» y 2 más (5 en los últimos 5 minutos)",
      ContratoNocheService.detalleErroresYoutube(listOf("A", "B", "C", "D", "E"), 5),
    )
  }

  @Test
  fun `los errores de antes sin cancion cuentan igual`() {
    assertEquals("algunas canciones (2 en los últimos 5 minutos)", ContratoNocheService.detalleErroresYoutube(emptyList(), 2))
    assertEquals("«A» y 1 más (2 en los últimos 5 minutos)", ContratoNocheService.detalleErroresYoutube(listOf("A"), 2))
  }
}
