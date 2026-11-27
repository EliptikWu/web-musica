package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.json.JsonMapper
import kotlin.test.*

/** CP-007/008: sin salida de red ni consumir cuota; validaciones y falta de API key. */
class ValidacionYoutubeClienteTest {
    private val mapper = JsonMapper.builder().build()
    @Test fun `sin api key rechaza busquedas y detalle antes de enviar HTTP`() {
        val api = YoutubeApiCliente(mapper, "")
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertFailsWith<ResponseStatusException> { api.buscar("salsa") }.statusCode)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertFailsWith<ResponseStatusException> { api.obtenerVideoPorId("dQw4w9WgXcQ") }.statusCode)
    }
    @Test fun `filtro musical excluye otras categorias`() {
        assertTrue(YoutubeApiCliente.esMusica(mapper.readTree("""{"categoryId":"10"}""")))
        assertFalse(YoutubeApiCliente.esMusica(mapper.readTree("""{"categoryId":"22"}""")))
    }
    @Test fun `filtro vertical rechaza videos mas altos que anchos`() {
        assertTrue(YoutubeApiCliente.esVertical(mapper.readTree("""{"embedWidth":360,"embedHeight":640}""")))
        assertFalse(YoutubeApiCliente.esVertical(mapper.readTree("""{"embedWidth":640,"embedHeight":360}""")))
    }
}
