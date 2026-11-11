package com.example.rokola.datos.api

import android.util.Log
import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.FalloRockola
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformWhile
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

/**
 * Silencio máximo: dos pings perdidos (cada 15 s) y uno de gracia (apps/web/lib/tiempo-real/fuente.ts).
 * Va como tiempo de lectura: si en ese lapso no llega ni un byte, la conexión se da por caída.
 */
private const val SIN_PING_S = 35L

/**
 * Una conexión SSE a `GET /bares/{slug}/eventos` (ADR-07): emite cada foto `ahora` y termina
 * con [FalloRockola] (SIN_CONEXION) cuando se corta. Los reintentos y el sondeo de respaldo
 * los pone `ahoraEnVivo` (viewmodel/EnVivo.kt), igual para la simulación que para la API.
 */
internal fun conectarSse(http: OkHttpClient, url: HttpUrl): Flow<Ahora> = callbackFlow {
    val clienteSse = http.newBuilder()
        .callTimeout(0, TimeUnit.SECONDS)
        .readTimeout(SIN_PING_S, TimeUnit.SECONDS)
        .build()
    val peticion = Request.Builder().url(url).header("Accept", "text/event-stream").build()
    val cortada = { FalloRockola(ErrorRockola(CodigoError.SIN_CONEXION, "Se cortó el tiempo real")) }

    val fuente = EventSources.createFactory(clienteSse).newEventSource(peticion, object : EventSourceListener() {
        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            // `panel` y `ping` no cambian nada en el teléfono; el ping ya renovó el tiempo de lectura.
            if (type != "ahora") return
            val foto = try {
                JsonContrato.decodeFromString(EventoAhoraJson.serializer(), data).ahora.aDominio()
            } catch (e: IllegalArgumentException) {
                Log.e(ClienteApi.ETIQUETA, "Evento fuera del contrato: ${e.message}")
                return
            } catch (e: java.time.DateTimeException) {
                Log.e(ClienteApi.ETIQUETA, "Evento con fecha fuera del contrato: ${e.message}")
                return
            }
            trySend(foto)
        }

        override fun onClosed(eventSource: EventSource) {
            close(cortada())
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            close(cortada())
        }
    })
    awaitClose { fuente.cancel() }
}

/**
 * Comparte una conexión de tiempo real entre todas las pantallas del bar: Inicio, Buscar, Cola
 * y el pie escuchan a la vez, y sin esto cada una abriría su propia conexión al servidor.
 *
 * Cuando la conexión se corta, todas las que escuchaban reciben el fallo y la siguiente que
 * vuelva a conectarse abre una nueva. La última foto se reparte a quien llega tarde; si nadie
 * escucha por 5 s, la conexión se cierra y esa foto se descarta.
 */
internal class TiempoRealCompartido(
    private val alcance: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val conectar: (String) -> Flow<Ahora>,
) {
    private sealed interface Suceso {
        data class Foto(val ahora: Ahora) : Suceso
        data class Corte(val causa: Throwable) : Suceso
    }

    inner class Conexion(val bar: String) {
        private val compartida: SharedFlow<Suceso> = conectar(bar)
            .map<Ahora, Suceso> { Suceso.Foto(it) }
            .catch { emit(Suceso.Corte(it)) }
            .shareIn(alcance, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000, replayExpirationMillis = 0), replay = 1)

        val flujo: Flow<Ahora> = compartida.transformWhile { suceso ->
            when (suceso) {
                is Suceso.Foto -> { emit(suceso.ahora); true }
                is Suceso.Corte -> { olvidar(this@Conexion); throw suceso.causa }
            }
        }
    }

    private var actual: Conexion? = null

    @Synchronized
    fun de(bar: String): Conexion = actual?.takeIf { it.bar == bar } ?: Conexion(bar).also { actual = it }

    @Synchronized
    private fun olvidar(conexion: Conexion) {
        if (actual === conexion) actual = null
    }
}
