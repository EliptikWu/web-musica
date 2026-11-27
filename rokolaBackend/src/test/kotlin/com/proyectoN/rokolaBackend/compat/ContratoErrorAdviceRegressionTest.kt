package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.*

/** CP-007/008/036/039/045 y RNF-006: los mensajes HTTP no revelan datos internos. */
class ContratoErrorAdviceRegressionTest {
    private val handler = ContratoErrorAdvice()

    @Test fun `el contrato expone solo codigo mensaje y detalle autorizado`() {
        val r = handler.errorContrato(ContratoApiException(HttpStatus.CONFLICT, "conflicto", "No permitido", mapOf("reintentar_en" to 10)))
        assertEquals(HttpStatus.CONFLICT, r.statusCode)
        assertEquals(setOf("codigo", "mensaje", "detalle"), r.body!!.keys)
        assertEquals("conflicto", r.body!!["codigo"])
        assertEquals(mapOf("reintentar_en" to 10), r.body!!["detalle"])
    }

    @Test fun `cuota youtube se convierte en 503 y codigo estable`() {
        val r = handler.estadoHttp(ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Cuota temporal"))
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.statusCode)
        assertEquals("cuota_agotada", r.body!!["codigo"])
    }

    @Test fun `fallo inesperado no filtra passwords sql ni stacktrace`() {
        val textoSecreto = "password=secreto host=base-de-datos SELECT * FROM usuario"
        val r = handler.imprevisto(IllegalStateException(textoSecreto))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.statusCode)
        assertEquals("error_interno", r.body!!["codigo"])
        assertFalse(r.body.toString().contains("secreto"))
        assertFalse(r.body.toString().contains("SELECT"))
    }

    @Test fun `errores 404 y 409 mantienen sus codigos de contrato`() {
        val noExiste = handler.estadoHttp(ResponseStatusException(HttpStatus.NOT_FOUND, "No existe"))
        val conflicto = handler.estadoHttp(ResponseStatusException(HttpStatus.CONFLICT, "Conflicto"))
        assertEquals("no_encontrado", noExiste.body!!["codigo"])
        assertEquals("conflicto", conflicto.body!!["codigo"])
    }
}
