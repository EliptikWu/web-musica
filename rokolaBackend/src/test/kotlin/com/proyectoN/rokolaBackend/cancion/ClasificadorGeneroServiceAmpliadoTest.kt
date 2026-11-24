package com.proyectoN.rokolaBackend.cancion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClasificadorGeneroServiceAmpliadoTest {
    private val clasificador = ClasificadorGeneroService()

    @Test
    fun `conserva compatibilidad con los generos anteriores`() {
        assertEquals("Salsa", clasificador.detectar(listOf("Official Video", "SALSA ROMÁNTICA")))
        assertEquals("Urbano", clasificador.detectar(listOf("reguetón", "reggaeton")))
        assertEquals("Electrónica", clasificador.canonico("electronica"))
        assertTrue(clasificador.generos().containsAll(listOf("Salsa", "Pop", "Tropical", "Urbano")))
    }

    @Test
    fun `detecta expresiones largas y hashtags`() {
        assertEquals("Salsa", clasificador.detectar(listOf("salsa colombiana 2026")))
        assertEquals("Salsa", clasificador.detectar(listOf("#SalsaRomántica")))
        assertEquals("Salsa", clasificador.detectar(listOf("#salsaromantica")))
        assertEquals("Urbano", clasificador.detectar(listOf("#Reggaeton2026")))
        assertEquals("Vallenato", clasificador.detectar(listOf("vallenato colombiano en vivo")))
        assertEquals("Música Popular", clasificador.detectar(listOf("música popular colombiana")))
    }

    @Test
    fun `prefiere genero concreto en lugar de categoria generica`() {
        assertEquals("Salsa", clasificador.detectar(listOf("salsa", "tropical music")))
        assertEquals("House", clasificador.detectar(listOf("house music", "edm")))
        assertEquals("Corridos", clasificador.detectar(listOf("corridos tumbados", "regional mexicano")))
        assertEquals("Punk", clasificador.detectar(listOf("pop punk")))
        assertEquals("K-Pop", clasificador.detectar(listOf("k-pop")))
    }

    @Test
    fun `evita falsos positivos y conflictos`() {
        assertNull(clasificador.detectar(listOf("Latin Music", "Official Music Video")))
        assertNull(clasificador.detectar(listOf("popular songs", "rocket man")))
        assertNull(clasificador.detectar(listOf("salsa", "bachata")))
        assertNull(clasificador.detectar(emptyList()))
        assertNull(clasificador.canonico("inventado"))
    }

    @Test
    fun `acepta un genero con varias etiquetas de respaldo`() {
        assertEquals("Salsa", clasificador.detectar(listOf("salsa", "salsa romantica", "bachata")))
    }
}
