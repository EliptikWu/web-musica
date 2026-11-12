package com.proyectoN.rokolaBackend.reproduccion

import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

/** RF-040: en pausa, la barra de avance no corre. */
class ReproduccionRelojTest {
  private val t0 = OffsetDateTime.of(2026, 11, 12, 21, 0, 0, 0, ZoneOffset.ofHours(-5))
  private fun en(seg: Long) = t0.plusSeconds(seg)

  @Test
  fun `sin pausas corre con el reloj`() {
    assertEquals(90, ReproduccionReloj.posicionSeg(t0, en(90), 0, null, 0, 300))
  }

  @Test
  fun `en pausa la posicion queda quieta`() {
    // Sonó 60 s y se pausó: a los 60, a los 100 y a los 500 s del inicio sigue en 60.
    assertEquals(60, ReproduccionReloj.posicionSeg(t0, en(60), 0, en(60), 0, 300))
    assertEquals(60, ReproduccionReloj.posicionSeg(t0, en(100), 0, en(60), 0, 300))
    assertEquals(60, ReproduccionReloj.posicionSeg(t0, en(500), 0, en(60), 0, 300))
  }

  @Test
  fun `al reanudar sigue desde donde quedo`() {
    // Pausa de 40 s ya terminada: a los 130 s del inicio lleva 90.
    assertEquals(90, ReproduccionReloj.posicionSeg(t0, en(130), 40, null, 0, 300))
  }

  @Test
  fun `lo reportado por el reproductor es un piso y la duracion un techo`() {
    assertEquals(120, ReproduccionReloj.posicionSeg(t0, en(30), 0, null, 120, 300))
    assertEquals(300, ReproduccionReloj.posicionSeg(t0, en(900), 0, null, 0, 300))
    assertEquals(0, ReproduccionReloj.posicionSeg(null, en(30), 0, null, 0, 300))
  }
}
