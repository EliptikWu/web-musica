package com.proyectoN.rokolaBackend.compat

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** La regla con que la búsqueda encuentra lo que ya está en la cola («Ya está en la cola»). */
class CoincideBusquedaTest {
  @Test
  fun `por titulo o artista, sin mayusculas y con tildes`() {
    assertTrue(coincide("Ángel" to "Romeo Santos", "ángel"))
    assertTrue(coincide("Monaco" to "Bad Bunny", "bad bunny"))
    assertTrue(coincide("NUEVAYoL" to "Bad Bunny", "nuevayol"))
  }

  @Test
  fun `sin artista o sin coincidencia no aparece`() {
    assertFalse(coincide("Monaco" to null, "bad bunny"))
    assertFalse(coincide("Monaco" to "Bad Bunny", "salsa"))
  }
}
