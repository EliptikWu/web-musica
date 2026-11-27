package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import kotlin.test.*

/** CP-002/019/034: verificaciones criptográficas antes de resolver un QR en la BD. */
class QrEstablecimientoTest {
    @Test fun `dos códigos QR tienen diferentes huellas`() {
        val seguro = TokenSeguro("x".repeat(48))
        val qrs = (1..20).map { seguro.generar() }
        assertEquals(20, qrs.map(seguro::huellaQr).toSet().size)
    }
    @Test fun `un QR alterado produce huella distinta`() {
        val seguro = TokenSeguro("x".repeat(48))
        val original = seguro.generar()
        val alterado = (if (original[0] == 'a') "b" else "a") + original.drop(1)
        assertNotEquals(seguro.huellaQr(original), seguro.huellaQr(alterado))
    }
}
