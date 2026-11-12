package com.proyectoN.rokolaBackend.compat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** RF-062: el volumen que acepta `PUT /api/panel/volumen`. */
class ContratoVolumenTest {
  @Test
  fun `de 0 a 100 entero`() {
    assertEquals(0, ContratoVolumenService.leerVolumen(mapOf("volumen" to 0)))
    assertEquals(100, ContratoVolumenService.leerVolumen(mapOf("volumen" to 100)))
    assertEquals(70, ContratoVolumenService.leerVolumen(mapOf("volumen" to 70.0)))
  }

  @Test
  fun `fuera de rango, decimal o sin volumen se rechaza`() {
    for (cuerpo in listOf(mapOf("volumen" to -1), mapOf("volumen" to 101), mapOf("volumen" to 2.5), mapOf("volumen" to "80"), emptyMap(), null)) {
      val e = assertFailsWith<ContratoApiException> { ContratoVolumenService.leerVolumen(cuerpo) }
      assertEquals("datos_invalidos", e.codigo)
    }
  }
}
