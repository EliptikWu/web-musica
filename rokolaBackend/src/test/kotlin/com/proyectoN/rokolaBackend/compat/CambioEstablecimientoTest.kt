package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RF-034, RF-059: lo que acepta `PATCH /api/panel/establecimiento`, con los límites del contrato. */
class CambioEstablecimientoTest {
  private fun rechaza(cuerpo: Map<String, Any?>) {
    val e = assertFailsWith<ContratoApiException> { CambioEstablecimiento.desde(cuerpo) }
    assertEquals(HttpStatus.BAD_REQUEST, e.status)
    assertEquals("datos_invalidos", e.codigo)
  }

  @Test
  fun `solo el tema cambia el tema y nada mas`() {
    val c = CambioEstablecimiento.desde(mapOf("tema" to "afiche"))
    assertEquals("afiche", c.tema)
    assertNull(c.nombre)
    assertEquals("tema=afiche", c.resumen())
  }

  @Test
  fun `un tema que no existe se rechaza`() {
    rechaza(mapOf("tema" to "neon"))
    rechaza(mapOf("tema" to 3))
  }

  @Test
  fun `los limites son los del contrato`() {
    assertEquals(10, CambioEstablecimiento.desde(mapOf("pedidos_maximos" to 10)).pedidosMaximos)
    rechaza(mapOf("pedidos_maximos" to 0))
    rechaza(mapOf("pedidos_maximos" to 11))
    rechaza(mapOf("pedidos_maximos" to 2.5))
    rechaza(mapOf("duracion_max_seg" to 59))
    rechaza(mapOf("ventana_no_repeticion_min" to 601))
    rechaza(mapOf("nombre" to "   "))
    rechaza(mapOf("zona_horaria" to "Marte/Olympus"))
    assertEquals("America/Bogota", CambioEstablecimiento.desde(mapOf("zona_horaria" to "America/Bogota")).zonaHoraria)
  }

  @Test
  fun `no se pueden cambiar el id el slug ni el estado`() {
    rechaza(mapOf("slug" to "otro-bar"))
    rechaza(mapOf("id" to "1", "tema" to "vidrio"))
    rechaza(mapOf("estado" to "inactivo"))
  }

  @Test
  fun `un cuerpo vacio no cambia nada`() {
    assertTrue(CambioEstablecimiento.desde(emptyMap()).vacio)
  }
}
