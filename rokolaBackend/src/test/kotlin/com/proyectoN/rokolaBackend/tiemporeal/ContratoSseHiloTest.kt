package com.proyectoN.rokolaBackend.tiemporeal

import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.ArgumentMatchers.anyLong
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Prueba de carga del 2026-11-25 (doc 07): el oyente AFTER_COMMIT pedía una conexión nueva
 * (`REQUIRES_NEW`) en el hilo de la petición, que aún tenía la suya; con 10 votos a la vez el pool
 * se trababa 30 s. El aviso tiene que armarse en otro hilo y la petición no debe esperarlo.
 */
class ContratoSseHiloTest {
  @Test
  fun `el aviso de un voto no se arma en el hilo de la petición`() {
    val instantaneas = mock(ContratoSseInstantaneas::class.java)
    val llamado = CountDownLatch(1)
    val soltar = CountDownLatch(1)
    var hilo = ""
    doAnswer {
      hilo = Thread.currentThread().name
      llamado.countDown()
      soltar.await(5, TimeUnit.SECONDS) // Un aviso lento no frena a quien votó.
      "demo"
    }.`when`(instantaneas).slugDeSesion(anyLong())
    val sse = ContratoSseService(instantaneas)

    val inicio = System.nanoTime()
    sse.cambioCola(CambioColaEvent(1L, "voto"))
    val esperoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio)

    assertTrue(esperoMs < 1000, "la petición esperó al aviso: $esperoMs ms")
    assertTrue(llamado.await(5, TimeUnit.SECONDS), "el aviso no se programó")
    assertEquals("rokola-sse", hilo)
    soltar.countDown()
  }
}
