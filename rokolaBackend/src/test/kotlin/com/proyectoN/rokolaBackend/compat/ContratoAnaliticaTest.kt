package com.proyectoN.rokolaBackend.compat

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ContratoAnaliticaTest {
  @Test
  fun `sin votos la concentracion es cero`() {
    assertEquals(0, ContratoAnaliticaService.pctDelDiez(emptyList()))
  }

  @Test
  fun `el diez por ciento de pocos telefonos es al menos uno`() {
    assertEquals(50, ContratoAnaliticaService.pctDelDiez(listOf(5, 3, 2)))
  }

  @Test
  fun `toma el diez por ciento que mas voto aunque lleguen desordenados`() {
    val votos = listOf(1, 1, 1, 1, 1, 1, 1, 1, 12, 10, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    assertEquals(55, ContratoAnaliticaService.pctDelDiez(votos))
  }
}
