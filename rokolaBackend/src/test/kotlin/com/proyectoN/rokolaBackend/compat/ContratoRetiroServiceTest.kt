package com.proyectoN.rokolaBackend.compat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContratoRetiroServiceTest {
  @Test
  fun `acepta los tres motivos del contrato`() {
    assertEquals("Duplicada", ContratoDatosRetiro.validar(mapOf("motivo" to "duplicada", "detalle" to null)).texto)
    assertEquals("Inapropiada", ContratoDatosRetiro.validar(mapOf("motivo" to "inapropiada")).texto)
    assertEquals("Otro: Pedida por error", ContratoDatosRetiro.validar(mapOf("motivo" to "otro", "detalle" to " Pedida por error ")).texto)
  }

  @Test
  fun `rechaza motivo no permitido y campos desconocidos`() {
    val invalido = assertFailsWith<ContratoApiException> {
      ContratoDatosRetiro.validar(mapOf("motivo" to "cancelada", "detalle" to null))
    }
    assertEquals("datos_invalidos", invalido.codigo)
    assertFailsWith<ContratoApiException> {
      ContratoDatosRetiro.validar(mapOf("motivo" to "otro", "detalle" to null, "bar_id" to 5))
    }
  }

  @Test
  fun `rechaza detalle de mas de 200 caracteres y acepta vacio`() {
    assertFailsWith<ContratoApiException> {
      ContratoDatosRetiro.validar(mapOf("motivo" to "duplicada", "detalle" to "x".repeat(201)))
    }
    assertEquals("Duplicada", ContratoDatosRetiro.validar(mapOf("motivo" to "duplicada", "detalle" to "")).texto)
  }
}
