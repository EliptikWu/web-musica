package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import kotlin.test.*

/** CP-036, CP-041, CP-042: límites y tipos de los contratos HTTP. */
class ContratoValidacionesTest {
    @Test fun `no acepta campos inesperados`() {
        val e = assertFailsWith<ContratoApiException> {
            ContratoCuerpo.exigirCampos(mapOf("nombre" to "bar", "id_establecimiento" to 2), setOf("nombre"))
        }
        assertEquals(HttpStatus.BAD_REQUEST, e.status)
    }
    @Test fun `texto obligatorio se recorta y valida`() {
        assertEquals("Salsa", ContratoCuerpo.texto(mapOf("q" to " Salsa "), "q", 10, obligatorio = true))
        assertFailsWith<ContratoApiException> { ContratoCuerpo.texto(emptyMap(), "q", 10, obligatorio = true) }
        assertFailsWith<ContratoApiException> { ContratoCuerpo.texto(mapOf("q" to " "), "q", 10) }
        assertFailsWith<ContratoApiException> { ContratoCuerpo.texto(mapOf("q" to "123456"), "q", 5) }
        assertFailsWith<ContratoApiException> { ContratoCuerpo.texto(mapOf("q" to 123), "q", 10) }
    }
    @Test fun `entero rechaza fracciones y rangos invalidos`() {
        assertEquals(3, ContratoCuerpo.entero(mapOf("cantidad" to 3), "cantidad", 0, 5))
        for (v in listOf(-1, 6, 1.5, "3")) {
            assertFailsWith<ContratoApiException> { ContratoCuerpo.entero(mapOf("cantidad" to v), "cantidad", 0, 5) }
        }
    }
    @Test fun `ids deben ser positivos y tipos booleanos correctos`() {
        assertEquals(7L, ContratoCuerpo.id("7", "no existe"))
        for (v in listOf("0", "-1", "abc", "1.2")) {
            assertEquals(HttpStatus.NOT_FOUND, assertFailsWith<ContratoApiException> { ContratoCuerpo.id(v, "no existe") }.status)
        }
        assertTrue(ContratoCuerpo.booleano(mapOf("activo" to true), "activo"))
        assertFailsWith<ContratoApiException> { ContratoCuerpo.booleano(mapOf("activo" to "true"), "activo") }
    }
}
