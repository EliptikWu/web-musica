package com.proyectoN.rokolaBackend.compat

import kotlin.test.Test
import kotlin.test.assertEquals

class ContratoColaCalculosTest {
    @Test
    fun `sin reproduccion la primera de la cola espera cero`() {
        assertEquals(listOf(0, 180, 420), ContratoColaCalculos.esperasSeg(listOf(180, 240, 210), 0L))
    }

    @Test
    fun `con reproduccion suma el restante antes de las pendientes`() {
        assertEquals(listOf(75, 255, 495), ContratoColaCalculos.esperasSeg(listOf(180, 240, 210), 75L))
    }

    @Test
    fun `no se generan segundos negativos`() {
        assertEquals(listOf(0, 0, 10), ContratoColaCalculos.esperasSeg(listOf(-40, 10, 20), -1L))
    }

    @Test
    fun `los cupos incluyen la cancion sonando`() {
        // El conteo SQL compartido incluye pendiente y reproduciendo.
        assertEquals(0, ContratoColaCalculos.cuposRestantes(3, 3L))
        assertEquals(1, ContratoColaCalculos.cuposRestantes(3, 2L))
        assertEquals(3, ContratoColaCalculos.cuposRestantes(3, 0L))
        assertEquals(0, ContratoColaCalculos.cuposRestantes(3, 4L))
    }
}
