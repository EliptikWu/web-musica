package com.proyectoN.rokolaBackend.acceso

import kotlin.test.*

/** CP-003/019/034/051: tokens QR y dispositivos, sin conexión a Railway. */
class TokenSeguroSeguridadTest {
    private val tokens = TokenSeguro("a".repeat(48))
    @Test fun `crea tokens URL seguros unicos y con formato esperado`() {
        val generados = (1..30).map { tokens.generar() }
        assertEquals(30, generados.toSet().size)
        assertTrue(generados.all(tokens::esValido))
        assertFalse(tokens.esValido(" "))
        assertFalse(tokens.esValido("a".repeat(42)))
        assertFalse(tokens.esValido("a".repeat(44)))
    }
    @Test fun `hmac aísla el mismo dispositivo en cada establecimiento`() {
        val a = tokens.huellaDispositivo(1, "dispositivo-prueba")
        val b = tokens.huellaDispositivo(2, "dispositivo-prueba")
        assertEquals(64, a.length)
        assertEquals(a, tokens.huellaDispositivo(1, "dispositivo-prueba"))
        assertNotEquals(a, b)
        assertFalse(a.contains("dispositivo-prueba"))
    }
    @Test fun `la huella QR cambia al modificar el token`() {
        assertEquals(tokens.huellaQr("abc"), tokens.huellaQr("abc"))
        assertNotEquals(tokens.huellaQr("abc"), tokens.huellaQr("abC"))
    }
    @Test fun `no permite secreto hmac de longitud insuficiente`() {
        assertFailsWith<IllegalArgumentException> { TokenSeguro("corto") }
    }
}
