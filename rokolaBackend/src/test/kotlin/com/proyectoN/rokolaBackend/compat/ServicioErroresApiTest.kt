package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.*

/** CP-008/035/044: los errores no devuelven trazas internas al cliente. */
class ServicioErroresApiTest {
    private val advice = ContratoErrorAdvice()
    @Test fun `error de negocio preserva codigo pero no expone traza`() {
        val res = advice.errorContrato(ContratoApiException(HttpStatus.CONFLICT, "conflicto", "Solicitud ya registrada"))
        assertEquals(HttpStatus.CONFLICT, res.statusCode)
        assertEquals("conflicto", res.body?.get("codigo"))
        assertFalse(res.body.toString().contains("Exception"))
    }
    @Test fun `error youtube temporal tiene codigo controlado`() {
        val res = advice.estadoHttp(ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "YouTube no respondió"))
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.statusCode)
        assertFalse(res.body.toString().contains("stacktrace"))
    }
}
