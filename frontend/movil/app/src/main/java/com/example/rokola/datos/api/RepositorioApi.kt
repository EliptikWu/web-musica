package com.example.rokola.datos.api

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
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient

/**
 * El backend de verdad, con las rutas del contrato (packages/contrato/generado/endpoints.md,
 * «Cliente»). [slugActual] es el bar del último QR; [tipoDispositivo] sale del ancho de la pantalla
 * (movil, tableta u otro), como en la web.
 */
class RepositorioApi(
    private val api: ClienteApi,
    private val http: OkHttpClient,
    private val slugActual: () -> String?,
    private val tipoDispositivo: () -> String,
) : RepositorioRockola {

    /** Sin bar no hay a quién preguntar: la entrada ya lo impide, esto es la red de seguridad. */
    private inline fun <T> conBar(operacion: (String) -> T): T? = slugActual()?.let(operacion)

    private val sinBar = Resultado.Fallo(ErrorRockola(CodigoError.NO_ENCONTRADO, "No hay bar elegido"))

    override suspend fun resolverQr(token: String): Resultado<String> =
        api.obtener(api.url("qr", token), ResolucionQrJson.serializer()) { it.slug }

    override suspend fun bar(slug: String?): Resultado<Bar> =
        (slug ?: slugActual())?.let { api.obtener(api.url("bares", it), BarJson.serializer(), BarJson::aDominio) } ?: sinBar

    override suspend fun ahora(): Resultado<Ahora> =
        conBar { api.obtener(api.url("bares", it, "ahora"), AhoraJson.serializer(), AhoraJson::aDominio) } ?: sinBar

    override fun observarAhora(): Flow<Ahora> {
        val bar = slugActual() ?: return flow { throw FalloRockola(sinBar.error) }
        return tiempoReal.de(bar).flujo
    }

    /** Una sola conexión SSE por bar para todas las pantallas que escuchan (ver [TiempoRealCompartido]). */
    private val tiempoReal = TiempoRealCompartido { bar -> conectarSse(http, api.url("bares", bar, "eventos")) }

    override suspend fun destacadas(): Resultado<List<Destacada>> = conBar {
        api.obtener(api.url("bares", it, "destacadas"), RespuestaDestacadasJson.serializer()) { r -> r.destacadas.map(DestacadaJson::aDominio) }
    } ?: sinBar

    override suspend fun registrarDispositivo(): Resultado<Yo> = conBar {
        val cuerpo = JsonContrato.encodeToString(CuerpoDispositivoJson.serializer(), CuerpoDispositivoJson(tipoDispositivo()))
        api.enviar("POST", api.url("bares", it, "dispositivo"), cuerpo, idempotente = false, YoJson.serializer(), YoJson::aDominio)
    } ?: sinBar

    override suspend fun yo(): Resultado<Yo> =
        conBar { api.obtener(api.url("bares", it, "dispositivo"), YoJson.serializer(), YoJson::aDominio) } ?: sinBar

    override suspend fun anotarAvisoVisto(): Resultado<Unit> = conBar {
        api.enviarSinRespuesta("POST", api.url("bares", it, "dispositivo", "aviso"))
    } ?: sinBar

    override suspend fun buscar(consulta: String): Resultado<Busqueda> = conBar {
        api.obtener(api.url("bares", it, "busqueda", consulta = mapOf("q" to consulta)), BusquedaJson.serializer(), BusquedaJson::aDominio)
    } ?: sinBar

    override suspend fun pedir(videoId: String): Resultado<RespuestaSolicitud> = conBar {
        val cuerpo = JsonContrato.encodeToString(CuerpoSolicitudJson.serializer(), CuerpoSolicitudJson(videoId))
        api.enviar("POST", api.url("bares", it, "solicitudes"), cuerpo, idempotente = true, RespuestaSolicitudJson.serializer(), RespuestaSolicitudJson::aDominio)
    } ?: sinBar

    override suspend fun votar(solicitudId: String): Resultado<RespuestaVoto> = conBar {
        api.enviar("POST", api.url("bares", it, "solicitudes", solicitudId, "votos"), cuerpo = null, idempotente = true, RespuestaVotoJson.serializer(), RespuestaVotoJson::aDominio)
    } ?: sinBar

    override suspend fun moverVoto(desde: String, hacia: String): Resultado<RespuestaVoto> = conBar {
        val cuerpo = JsonContrato.encodeToString(CuerpoCambioVotoJson.serializer(), CuerpoCambioVotoJson(desde, hacia))
        api.enviar("PUT", api.url("bares", it, "dispositivo", "voto"), cuerpo, idempotente = true, RespuestaVotoJson.serializer(), RespuestaVotoJson::aDominio)
    } ?: sinBar
}
