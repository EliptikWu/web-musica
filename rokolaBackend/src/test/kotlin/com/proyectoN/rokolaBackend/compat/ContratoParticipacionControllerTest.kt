package com.proyectoN.rokolaBackend.compat

import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import kotlin.test.*

/** CP-009/011/028/035/036/037/038: contrato HTTP sin YouTube ni BD reales.
 * No reemplaza la prueba integral de concurrencia ni la de un teléfono sin conexión.
 */
class ContratoParticipacionControllerTest {
    private val servicio = mock(ContratoParticipacionService::class.java)
    private val controlador = ContratoParticipacionController(servicio)
    private val dispositivo = "550e8400-e29b-41d4-a716-446655440000"
    private val clave = "ef5035dd-2603-47b2-a2b1-2e9c921fd0e2"

    @Test fun `no permite solicitudes sin video id de tipo texto`() {
        for (cuerpo in listOf(emptyMap<String, Any?>(), mapOf("video_id" to null),
                mapOf("video_id" to 4), mapOf("otro" to "abc"))) {
            val error = assertFailsWith<ContratoApiException> {
                controlador.pedir("bar-a", dispositivo, clave, cuerpo)
            }
            assertEquals(HttpStatus.BAD_REQUEST, error.status)
            assertEquals("datos_invalidos", error.codigo)
        }
        verifyNoInteractions(servicio)
    }

    @Test fun `peticion correcta transmite dispositivo idempotencia y video al servicio`() {
        val esperado = ContratoParticipacionService.RespuestaGuardada(201, """{"id":"22"}""")
        `when`(servicio.pedir("bar-a", dispositivo, clave, "dQw4w9WgXcQ")).thenReturn(esperado)
        val r = controlador.pedir("bar-a", dispositivo, clave, mapOf("video_id" to "dQw4w9WgXcQ"))
        assertEquals(HttpStatus.CREATED, r.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, r.headers.contentType)
        assertEquals(esperado.json, r.body)
        verify(servicio, times(1)).pedir("bar-a", dispositivo, clave, "dQw4w9WgXcQ")
    }

    @Test fun `votar traslada los identificadores y no cambia el resultado del servicio`() {
        val esperado = ContratoParticipacionService.RespuestaGuardada(200, """{"votos":2}""")
        `when`(servicio.votar("bar-a", "22", dispositivo, clave)).thenReturn(esperado)
        val r = controlador.votar("bar-a", "22", dispositivo, clave)
        assertEquals(HttpStatus.OK, r.statusCode)
        assertEquals(esperado.json, r.body)
        verify(servicio).votar("bar-a", "22", dispositivo, clave)
    }

    @Test fun `quitar voto mantiene la operacion DELETE del contrato`() {
        val esperado = ContratoParticipacionService.RespuestaGuardada(200, """{"votos":1}""")
        `when`(servicio.quitarVoto("bar-a", "22", dispositivo, clave)).thenReturn(esperado)
        assertEquals(esperado.json, controlador.quitarVoto("bar-a", "22", dispositivo, clave).body)
        verify(servicio).quitarVoto("bar-a", "22", dispositivo, clave)
    }

    @Test fun `retirar pedido propio transmite la misma clave de idempotencia`() {
        val esperado = ContratoParticipacionService.RespuestaGuardada(200, """{"estado":"retirada"}""")
        `when`(servicio.retirarPedido("bar-a", "22", dispositivo, clave)).thenReturn(esperado)
        assertEquals(esperado.json, controlador.retirarPedido("bar-a", "22", dispositivo, clave).body)
        verify(servicio).retirarPedido("bar-a", "22", dispositivo, clave)
    }
}
