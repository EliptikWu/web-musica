package com.proyectoN.rokolaBackend.cancion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClasificadorGeneroServiceMetadatosTest {
    private val servicio = ClasificadorGeneroService()

    @Test
    fun `tags tienen prioridad sobre titulo y descripcion`() {
        assertEquals(
            ClasificadorGeneroService.Clasificacion("Salsa", "ETIQUETAS"),
            servicio.clasificar(listOf("salsa colombiana"), "Tema (Bachata)", "Género: Rock"),
        )
    }

    @Test
    fun `titulo admite parentesis rótulos y separadores`() {
        assertEquals(
            ClasificadorGeneroService.Clasificacion("Bachata", "TITULO"),
            servicio.clasificar(emptyList(), "Artista - Tema (Bachata)", null),
        )
        assertEquals(
            ClasificadorGeneroService.Clasificacion("Urbano", "TITULO"),
            servicio.clasificar(emptyList(), "Tema | Reggaeton 2026", ""),
        )
        assertEquals(
            ClasificadorGeneroService.Clasificacion("Salsa", "TITULO"),
            servicio.clasificar(emptyList(), "Canción [Salsa Romántica]", ""),
        )
    }

    @Test
    fun `descripcion admite genero explicito frase y hashtag`() {
        assertEquals(
            ClasificadorGeneroService.Clasificacion("Vallenato", "DESCRIPCION"),
            servicio.clasificar(emptyList(), "Artista - Tema", "🎵 Género musical: Vallenato romántico\nVisita mi web"),
        )
        assertEquals(
            ClasificadorGeneroService.Clasificacion("Salsa", "DESCRIPCION"),
            servicio.clasificar(emptyList(), "Artista - Tema", "Nuevo tema de salsa romántica lanzado hoy"),
        )
        assertEquals(
            ClasificadorGeneroService.Clasificacion("Urbano", "DESCRIPCION"),
            servicio.clasificar(emptyList(), "Artista - Tema", "#Reggaeton2026 #VideoOficial"),
        )
    }

    @Test
    fun `no se infiere genero de titulo libre playlists o promos de redes`() {
        assertNull(servicio.clasificar(emptyList(), "Mi salsa favorita (Video Oficial)", "Escúchala hoy"))
        assertNull(servicio.clasificar(emptyList(), "Artista - Tema", "Playlist salsa bachata pop con muchos artistas"))
        assertNull(servicio.clasificar(emptyList(), "Artista - Tema", "Sígueme en Instagram #Salsa"))
        assertNull(servicio.clasificar(emptyList(), "Artista - Tema (Salsa y Bachata)", ""))
    }

    @Test
    fun `las firmas existentes del clasificador se mantienen`() {
        assertEquals("Salsa", servicio.detectar(listOf("#SalsaRomántica")))
        assertEquals("Electrónica", servicio.canonico("electronica"))
    }
}
