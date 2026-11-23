package com.proyectoN.rokolaBackend.cancion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClasificadorGeneroServiceTest {
    private val clasificador = ClasificadorGeneroService()

    @Test
    fun `reconoce etiquetas exactas sin distinguir mayusculas ni acentos`() {
        assertEquals("Salsa", clasificador.detectar(listOf("Official Video", "SALSA ROMÁNTICA")))
        assertEquals("Urbano", clasificador.detectar(listOf("reguetón", "reggaeton")))
    }

    @Test
    fun `no confunde etiquetas generales con generos`() {
        assertNull(clasificador.detectar(listOf("Latin Music", "Official Music Video")))
        assertNull(clasificador.detectar(emptyList()))
    }

    @Test
    fun `varios generos diferentes quedan para revision humana`() {
        assertNull(clasificador.detectar(listOf("salsa", "bachata")))
    }

    @Test
    fun `solo acepta generos del catalogo al corregir manualmente`() {
        assertEquals("Electrónica", clasificador.canonico("electronica"))
        assertNull(clasificador.canonico("No existe"))
    }
}
