package com.proyectoN.rokolaBackend.compat

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Lista del equipo (2026-11-25): «Barns Courtney - Kicks | Lyrics» dio el error 150 y se podía
 * volver a pedir. Con 101 o 150 la canción queda restringida; con 100, no disponible.
 */
class ErrorReproductorTest {
  @Test
  fun `101 y 150 restringen la canción y 100 la deja no disponible`() {
    assertEquals("restringida", ContratoPantallaService.disponibilidadPorError(150))
    assertEquals("restringida", ContratoPantallaService.disponibilidadPorError(101))
    assertEquals("no_disponible", ContratoPantallaService.disponibilidadPorError(100))
    assertNull(ContratoPantallaService.disponibilidadPorError(5))
    assertNull(ContratoPantallaService.disponibilidadPorError(null))
  }

  @Test
  fun `el texto dice qué pasó en palabras del personal`() {
    assertTrue(ContratoPantallaService.textoErrorReproductor(150).startsWith("Quien subió el video no deja ponerlo fuera de YouTube"))
    assertEquals("El video ya no existe o es privado (error 100)", ContratoPantallaService.textoErrorReproductor(100))
    assertEquals("El reproductor dio el error 5", ContratoPantallaService.textoErrorReproductor(5))
  }
}
