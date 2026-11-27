package com.proyectoN.rokolaBackend.compat

import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-003 y CP-051: usa UUID contractual, NO tokens Firebase. */
class DispositivoIdentidadTest {
    private val servicio = ContratoDispositivoService(mock(JdbcTemplate::class.java))
    @Test fun `huella uuid es estable y no revela el uuid`() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val digest = servicio.huella(uuid)
        assertEquals(64, digest.length)
        assertEquals(digest, servicio.huella(uuid.uppercase()))
        assertFalse(digest.contains(uuid))
        assertNotEquals(digest, servicio.huella("550e8400-e29b-41d4-a716-446655440001"))
    }
    @Test fun `rechaza ausencia y uuid malformado antes de consultar la base`() {
        for (valor in listOf(null, "", "firebase-token", "123", "550e8400-e29b-41d4-a716-44665544000z")) {
            val error = assertFailsWith<ContratoApiException> { servicio.huella(valor) }
            assertEquals("datos_invalidos", error.codigo)
        }
    }
}
