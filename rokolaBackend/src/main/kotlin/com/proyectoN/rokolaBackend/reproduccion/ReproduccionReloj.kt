package com.proyectoN.rokolaBackend.reproduccion

import java.time.Duration
import java.time.OffsetDateTime

/**
 * Cuánto lleva sonando una reproducción (RF-040). El reloj corre desde `fecha_inicio`, menos el
 * tiempo en pausa: el de pausas terminadas (`segundos_en_pausa`) y, si está pausada ahora, el de
 * la pausa en curso. Así, en pausa la posición queda quieta. Lo que reportó el reproductor
 * (`segundos_reproducidos`) es un piso: nunca se informa menos de lo que ya se escuchó.
 */
object ReproduccionReloj {
  fun posicionSeg(
    inicio: OffsetDateTime?,
    ahora: OffsetDateTime,
    segundosEnPausa: Long,
    pausadaDesde: OffsetDateTime?,
    segundosReportados: Long,
    duracionSeg: Long,
  ): Long {
    val porReloj = inicio?.let {
      val corrido = Duration.between(it.toInstant(), ahora.toInstant()).seconds
      val pausaEnCurso = pausadaDesde?.let { p -> Duration.between(p.toInstant(), ahora.toInstant()).seconds.coerceAtLeast(0L) } ?: 0L
      (corrido - segundosEnPausa.coerceAtLeast(0L) - pausaEnCurso).coerceAtLeast(0L)
    } ?: 0L
    return maxOf(porReloj, segundosReportados).coerceIn(0L, duracionSeg.coerceAtLeast(0L))
  }
}
