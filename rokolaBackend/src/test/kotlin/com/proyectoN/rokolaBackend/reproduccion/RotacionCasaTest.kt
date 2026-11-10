package com.proyectoN.rokolaBackend.reproduccion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RotacionCasaTest {
    @Test
    fun `inicia en el primer elemento y rota secuencialmente`() {
        assertEquals(0, RotacionCasa.posicion(1, 3))
        assertEquals(2, RotacionCasa.siguiente(1, 3))
        assertEquals(1, RotacionCasa.siguiente(3, 3))
    }

    @Test
    fun `lista de un elemento se repite sin salirse`() {
        assertEquals(0, RotacionCasa.posicion(1, 1))
        assertEquals(1, RotacionCasa.siguiente(1, 1))
    }

    @Test
    fun `cursor antiguo sigue siendo valido si cambia el numero de canciones`() {
        assertEquals(2, RotacionCasa.posicion(9, 3))
    }

    @Test
    fun `lista vacia se rechaza`() {
        assertFailsWith<IllegalArgumentException> { RotacionCasa.posicion(1, 0) }
    }
}
