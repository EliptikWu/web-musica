package com.example.rokola.datos.api

import com.example.rokola.datos.simulacion.RepositorioSimulado
import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.FalloRockola
import com.example.rokola.dominio.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class TiempoRealCompartidoTest {
    private val foto: Ahora = runBlocking { (RepositorioSimulado().ahora() as Resultado.Exito).valor }

    @Test
    fun `varias pantallas comparten una conexión y la siguiente tras un corte abre otra`() = runBlocking {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val conexiones = AtomicInteger()
        val eventos = Channel<Result<Ahora>>(Channel.UNLIMITED)
        val tr = TiempoRealCompartido(alcance) {
            // La primera conexión lee del canal; las siguientes quedan abiertas sin emitir.
            if (conexiones.incrementAndGet() == 1) flow { eventos.consumeAsFlow().collect { emit(it.getOrThrow()) } }
            else flow { awaitCancellation() }
        }
        try {
            val a = async { tr.de("bar").flujo.take(2).toList() }
            val b = async { tr.de("bar").flujo.take(2).toList() }
            withTimeout(5_000) { while (conexiones.get() == 0) delay(10) }
            delay(200) // que las dos estén suscritas antes de emitir
            eventos.send(Result.success(foto))
            eventos.send(Result.success(foto.copy(version = foto.version + 1)))
            assertEquals(2, withTimeout(5_000) { a.await() }.size)
            assertEquals(2, withTimeout(5_000) { b.await() }.size)
            assertEquals("Una sola conexión para las dos", 1, conexiones.get())

            // Se corta: quien escuchaba recibe el fallo y la siguiente suscripción conecta de nuevo.
            var fallo: Throwable? = null
            val c = async { tr.de("bar").flujo.catch { fallo = it }.toList() }
            eventos.send(Result.failure(FalloRockola(ErrorRockola(CodigoError.SIN_CONEXION, "corte"))))
            withTimeout(5_000) { c.await() }
            assertTrue(fallo is FalloRockola)
            val d = async { tr.de("bar").flujo.first() }
            withTimeout(5_000) { while (conexiones.get() < 2) delay(10) }
            assertEquals(2, conexiones.get())
            d.cancel()
        } finally {
            alcance.cancel()
        }
    }
}
