package com.proyectoN.rokolaBackend.reproduccion

import java.time.OffsetDateTime
import kotlin.test.*

/** CP-014/044: reglas de avance, lista vacía y reloj; la TV requiere E2E web. */
class ReproduccionAutomaticaIntegrationTest {
    @Test fun `rotacion de lista preserva un elemento y vuelve al inicio`() {
        assertEquals(1, RotacionCasa.siguiente(1, 1))
        assertEquals(1, RotacionCasa.siguiente(5, 5))
        assertEquals(2, RotacionCasa.siguiente(6, 5))
        assertFailsWith<IllegalArgumentException> { RotacionCasa.siguiente(1, 0) }
    }
    @Test fun `un reporte de reproduccion nunca supera duracion`() {
        val inicio = OffsetDateTime.now().minusSeconds(500)
        assertEquals(200L, ReproduccionReloj.posicionSeg(inicio, inicio.plusSeconds(500), 0, null, 0, 200))
    }
    @Test fun `pausa congela el avance del reloj`() {
        val inicio = OffsetDateTime.now().minusSeconds(120)
        val pausa = inicio.plusSeconds(40)
        assertEquals(40L, ReproduccionReloj.posicionSeg(inicio, pausa.plusSeconds(50), 0, pausa, 0, 200))
    }
}
