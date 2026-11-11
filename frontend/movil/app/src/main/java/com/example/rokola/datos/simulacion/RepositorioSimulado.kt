package com.example.rokola.datos.simulacion

import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.Bar
import com.example.rokola.dominio.Busqueda
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.Destacada
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.FalloRockola
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.RespuestaSolicitud
import com.example.rokola.dominio.RespuestaVoto
import com.example.rokola.dominio.Resultado
import com.example.rokola.dominio.Yo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** ID del teléfono en la simulación. Con backend será un UUID guardado en el teléfono. */
const val DISPOSITIVO_SIMULADO = "este-telefono"

/**
 * El backend simulado: el mismo motor y los mismos datos de ejemplo que la web, en memoria.
 * Sin red. La hora avanza con el reloj real desde la hora del escenario (10:45 p. m. en el
 * momento B), así que las canciones terminan y la cola avanza sola.
 */
class RepositorioSimulado(
    escenario: Escenario = Escenario.MOMENTO_B,
    private val dispositivo: String = DISPOSITIVO_SIMULADO,
    private val relojReal: () -> Long = System::currentTimeMillis,
    private val velocidad: Double = 1.0,
    /** El bar que pide la app; la simulación solo tiene uno («demo»). null = ese mismo. */
    private val slugPedido: () -> String? = { null },
) : RepositorioRockola {

    private val candado = Mutex()
    private var estado = crearEstado(escenario, dispositivo, relojReal(), velocidad)
    private val versiones = MutableStateFlow(estado.version)

    /** Vuelve a empezar con otro escenario (para probar estados desde la app). */
    suspend fun cambiarEscenario(escenario: Escenario) = candado.withLock {
        estado = crearEstado(escenario, dispositivo, relojReal(), velocidad)
        versiones.value = -1
        versiones.value = estado.version
    }

    /** Lleva la simulación hasta ahora y opera sobre ella sin que otra llamada se cruce. */
    private suspend fun <T> conEstado(operacion: (EstadoSim, Long) -> T): T = candado.withLock {
        val ahora = ahoraSim(estado, relojReal())
        avanzar(estado, ahora)
        val salida = operacion(estado, ahora)
        versiones.value = estado.version
        salida
    }

    /** Como apps/web/simulacion/handlers/cliente.ts: el QR vigente lleva al bar; otro, a ninguno. */
    override suspend fun resolverQr(token: String): Resultado<String> = conEstado { e, _ ->
        if (token == QR_VIGENTE) Resultado.Exito(e.bar.slug)
        else Resultado.Fallo(ErrorRockola(CodigoError.NO_ENCONTRADO, "QR desconocido"))
    }

    override suspend fun bar(slug: String?): Resultado<Bar> = conEstado { e, _ ->
        val pedido = slug ?: slugPedido()
        if (pedido != null && pedido != e.bar.slug) return@conEstado Resultado.Fallo(ErrorRockola(CodigoError.NO_ENCONTRADO, "Bar desconocido"))
        Resultado.Exito(
            Bar(
                slug = e.bar.slug,
                nombre = e.bar.nombre,
                zonaHoraria = e.bar.zonaHoraria,
                pedidosMaximos = e.bar.pedidosMaximos,
                duracionMaxSeg = e.bar.duracionMaxSeg,
                ventanaNoRepeticionMin = e.bar.ventanaNoRepeticionMin,
                horario = e.horario,
            ),
        )
    }

    override suspend fun ahora(): Resultado<Ahora> = conEstado { e, a -> Resultado.Exito(construirAhora(e, a)) }

    /**
     * Como el SSE de la web: una foto al conectar y otra cada vez que cambia la versión. Revisa
     * cada segundo si terminó la canción. Con la conexión cortada, falla sin mandar foto.
     */
    override fun observarAhora(): Flow<Ahora> = flow {
        var ultima = Int.MIN_VALUE
        while (true) {
            val (cortada, foto) = conEstado { e, a -> (e.falloConexion == FalloConexion.CORTADA) to construirAhora(e, a) }
            if (cortada) throw FalloRockola(ErrorRockola(CodigoError.SIN_CONEXION, "Sin conexión en vivo"))
            if (foto.version != ultima) {
                ultima = foto.version
                emit(foto)
            }
            withTimeoutOrNull(REVISION_MS) { versiones.first { it != ultima } }
        }
    }

    override suspend fun destacadas(): Resultado<List<Destacada>> = conEstado { e, _ -> Resultado.Exito(construirDestacadas(e)) }

    override suspend fun registrarDispositivo(): Resultado<Yo> =
        conEstado { e, a -> Resultado.Exito(registrarDispositivo(e, dispositivo, "movil", a)) }

    override suspend fun yo(): Resultado<Yo> = conEstado { e, _ -> Resultado.Exito(construirYo(e, dispositivo)) }

    override suspend fun anotarAvisoVisto(): Resultado<Unit> = conEstado { e, a ->
        anotarAviso(e, dispositivo, a)
        Resultado.Exito(Unit)
    }

    override suspend fun buscar(consulta: String): Resultado<Busqueda> = conEstado { e, a -> buscar(e, consulta, a) }

    override suspend fun pedir(videoId: String): Resultado<RespuestaSolicitud> =
        conEstado { e, a -> pedirCancion(e, dispositivo, videoId, a) }

    override suspend fun votar(solicitudId: String): Resultado<RespuestaVoto> =
        conEstado { e, a -> votar(e, dispositivo, solicitudId, a) }

    override suspend fun moverVoto(desde: String, hacia: String): Resultado<RespuestaVoto> =
        conEstado { e, a -> moverVoto(e, dispositivo, desde, hacia, a) }

    companion object {
        private const val REVISION_MS = 1000L

        /** El token del QR de la semilla de la web (semilla.ts). */
        const val QR_VIGENTE = "demo-qr-1"
    }
}
