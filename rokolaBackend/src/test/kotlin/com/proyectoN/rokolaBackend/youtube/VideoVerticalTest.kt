package com.proyectoN.rokolaBackend.youtube

import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Los shorts no se piden ni suenan (RN-06, lista del equipo 2026-11-25): se reconocen por ser verticales. */
class VideoVerticalTest {
  private val json = JsonMapper.builder().build()
  private fun player(texto: String) = json.readTree(texto)

  @Test
  fun `un video más alto que ancho es un short`() {
    assertTrue(YoutubeApiCliente.esVertical(player("""{"embedWidth": 360, "embedHeight": 640}""")))
  }

  @Test
  fun `un video horizontal o cuadrado se acepta`() {
    assertFalse(YoutubeApiCliente.esVertical(player("""{"embedWidth": 640, "embedHeight": 360}""")))
    assertFalse(YoutubeApiCliente.esVertical(player("""{"embedWidth": 480, "embedHeight": 480}""")))
  }

  @Test
  fun `sin las medidas del reproductor se acepta, como antes`() {
    assertFalse(YoutubeApiCliente.esVertical(player("""{"embedHtml": "<iframe></iframe>"}""")))
    assertFalse(YoutubeApiCliente.esVertical(player("{}")))
  }
}
