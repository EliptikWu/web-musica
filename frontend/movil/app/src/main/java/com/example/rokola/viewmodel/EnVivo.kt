package com.example.rokola.viewmodel

import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.FalloRockola
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.Resultado
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/** Lo que llega del tiempo real, o de preguntar mientras está cortado. */
sealed interface EventoEnVivo {
    /** [enVivo] = false: la foto llegó preguntando, no por el tiempo real («Sin conexión»). */
    data class Foto(val ahora: Ahora, val enVivo: Boolean) : EventoEnVivo

    /** Sin tiempo real y sin respuesta al preguntar. */
    data class Fallo(val error: ErrorRockola) : EventoEnVivo
}

/** Reintentos del tiempo real (apps/web/lib/tiempo-real/fuente.ts), con ±20 % de variación. */
private val ESPERAS_MS = listOf(1000L, 2000L, 4000L, 8000L, 15000L)

/** Sin tiempo real, se pregunta cada 3 s (hooks-cliente.ts, RNF-009). */
private const val SONDEO_MS = 3000L

/**
 * La foto de la noche siempre al día: escucha el tiempo real y, si se corta, pregunta cada
 * 3 s mientras reintenta conectarse.
 */
fun RepositorioRockola.ahoraEnVivo(azar: () -> Double = Math::random): Flow<EventoEnVivo> = channelFlow {
    var intento = 0
    while (true) {
        try {
            observarAhora().collect {
                intento = 0
                send(EventoEnVivo.Foto(it, enVivo = true))
            }
        } catch (_: FalloRockola) {
            // Se cortó: abajo se pregunta y se reintenta.
        }
        val sondeo = launch {
            while (true) {
                when (val r = ahora()) {
                    is Resultado.Exito -> send(EventoEnVivo.Foto(r.valor, enVivo = false))
                    is Resultado.Fallo -> send(EventoEnVivo.Fallo(r.error))
                }
                delay(SONDEO_MS)
            }
        }
        val base = ESPERAS_MS[minOf(intento, ESPERAS_MS.lastIndex)]
        delay((base * (0.8 + azar() * 0.4)).toLong())
        sondeo.cancel()
        intento++
    }
}
