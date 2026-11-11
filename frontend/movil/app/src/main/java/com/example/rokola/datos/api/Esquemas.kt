package com.example.rokola.datos.api

import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.Bar
import com.example.rokola.dominio.Busqueda
import com.example.rokola.dominio.Cancion
import com.example.rokola.dominio.ContextoDestacada
import com.example.rokola.dominio.Destacada
import com.example.rokola.dominio.EstadoReproduccion
import com.example.rokola.dominio.EstadoServicio
import com.example.rokola.dominio.EstadoSolicitud
import com.example.rokola.dominio.FORMATO_SLUG
import com.example.rokola.dominio.Franja
import com.example.rokola.dominio.ItemCola
import com.example.rokola.dominio.ListaCasaResumen
import com.example.rokola.dominio.MiSolicitud
import com.example.rokola.dominio.ModoQr
import com.example.rokola.dominio.MotivoNoPedible
import com.example.rokola.dominio.OrigenReproduccion
import com.example.rokola.dominio.Reproduccion
import com.example.rokola.dominio.RespuestaSolicitud
import com.example.rokola.dominio.RespuestaVoto
import com.example.rokola.dominio.ResultadoBusqueda
import com.example.rokola.dominio.Servicio
import com.example.rokola.dominio.TipoRespuestaSolicitud
import com.example.rokola.dominio.Yo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/*
 * Los cuerpos del contrato (C:\web-musica\packages\contrato\src, cliente), campo por campo
 * en snake_case. Un campo nullable sin valor por defecto es obligatorio: si falta, la
 * respuesta no cumple el contrato y es un error, no un dato a medias (doc 11, «Validación»).
 * Al pasar al dominio se comprueban los rangos que el esquema zod exige.
 */

/** Fecha ISO 8601 con zona; sin zona no cumple el contrato. */
private fun fecha(texto: String): Instant = OffsetDateTime.parse(texto).toInstant()

/** "HH:MM" en 24 h. */
private val HORA_DEL_DIA = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
private fun hora(texto: String): LocalTime {
    require(HORA_DEL_DIA.matches(texto)) { "Hora fuera de formato: $texto" }
    return LocalTime.parse(texto)
}

private fun noNegativo(n: Int, campo: String): Int = n.also { require(it >= 0) { "$campo negativo" } }
private fun positivo(n: Int, campo: String): Int = n.also { require(it > 0) { "$campo debe ser positivo" } }
private fun id(texto: String, campo: String): String = texto.also { require(it.isNotEmpty()) { "$campo vacío" } }

@Serializable
data class ErrorApiJson(val codigo: String, val mensaje: String, val detalle: JsonObject? = null)

@Serializable
data class CancionJson(
    @SerialName("video_id") val videoId: String,
    val titulo: String,
    val artista: String,
    @SerialName("duracion_seg") val duracionSeg: Int,
    @SerialName("miniatura_url") val miniaturaUrl: String?,
) {
    fun aDominio(): Cancion {
        id(videoId, "video_id")
        id(titulo, "titulo")
        // z.url(): una miniatura que no es una dirección no cumple el contrato.
        miniaturaUrl?.let { require(it.startsWith("https://") || it.startsWith("http://")) { "miniatura_url no es una URL" } }
        return Cancion(videoId, titulo, artista, noNegativo(duracionSeg, "duracion_seg"), miniaturaUrl)
    }
}

@Serializable
data class FranjaJson(
    @SerialName("dia_semana") val diaSemana: Int,
    @SerialName("hora_inicio") val horaInicio: String,
    @SerialName("hora_fin") val horaFin: String,
    val activo: Boolean,
) {
    fun aDominio(): Franja {
        require(diaSemana in 1..7) { "dia_semana fuera de 1..7" }
        return Franja(diaSemana, hora(horaInicio), hora(horaFin), activo)
    }
}

@Serializable
data class BarJson(
    val slug: String,
    val nombre: String,
    @SerialName("zona_horaria") val zonaHoraria: String,
    @SerialName("pedidos_maximos") val pedidosMaximos: Int,
    @SerialName("duracion_max_seg") val duracionMaxSeg: Int,
    @SerialName("ventana_no_repeticion_min") val ventanaNoRepeticionMin: Int,
    val horario: List<FranjaJson>,
) {
    fun aDominio(): Bar {
        require(FORMATO_SLUG.matches(slug)) { "slug fuera de formato" }
        id(nombre, "nombre")
        return Bar(
            slug = slug,
            nombre = nombre,
            zonaHoraria = ZoneId.of(zonaHoraria),
            pedidosMaximos = positivo(pedidosMaximos, "pedidos_maximos"),
            duracionMaxSeg = positivo(duracionMaxSeg, "duracion_max_seg"),
            ventanaNoRepeticionMin = noNegativo(ventanaNoRepeticionMin, "ventana_no_repeticion_min"),
            horario = horario.map { it.aDominio() },
        )
    }
}

@Serializable
enum class EstadoServicioJson(val dominio: EstadoServicio) {
    @SerialName("abierto") ABIERTO(EstadoServicio.ABIERTO),
    @SerialName("fuera_de_horario") FUERA_DE_HORARIO(EstadoServicio.FUERA_DE_HORARIO),
    @SerialName("en_pausa") EN_PAUSA(EstadoServicio.EN_PAUSA),
    @SerialName("mantenimiento") MANTENIMIENTO(EstadoServicio.MANTENIMIENTO),
}

@Serializable
enum class ModoQrJson(val dominio: ModoQr) {
    @SerialName("automatico") AUTOMATICO(ModoQr.AUTOMATICO),
    @SerialName("forzado_activo") FORZADO_ACTIVO(ModoQr.FORZADO_ACTIVO),
    @SerialName("pausado") PAUSADO(ModoQr.PAUSADO),
}

@Serializable
data class ServicioJson(
    val estado: EstadoServicioJson,
    @SerialName("modo_qr") val modoQr: ModoQrJson,
    val desde: String?,
    val hasta: String?,
    @SerialName("proxima_apertura") val proximaApertura: String?,
) {
    fun aDominio() = Servicio(estado.dominio, modoQr.dominio, desde?.let(::fecha), hasta?.let(::fecha), proximaApertura?.let(::fecha))
}

@Serializable
enum class OrigenJson(val dominio: OrigenReproduccion) {
    @SerialName("solicitud") SOLICITUD(OrigenReproduccion.SOLICITUD),
    @SerialName("lista_casa") LISTA_CASA(OrigenReproduccion.LISTA_CASA),
}

@Serializable
enum class EstadoReproduccionJson(val dominio: EstadoReproduccion) {
    @SerialName("sonando") SONANDO(EstadoReproduccion.SONANDO),
    @SerialName("pausada") PAUSADA(EstadoReproduccion.PAUSADA),
}

@Serializable
data class ReproduccionJson(
    val id: String,
    val cancion: CancionJson,
    val origen: OrigenJson,
    @SerialName("solicitud_id") val solicitudId: String?,
    val votos: Int?,
    val inicio: String,
    @SerialName("posicion_seg") val posicionSeg: Int,
    val estado: EstadoReproduccionJson,
) {
    fun aDominio() = Reproduccion(
        id = id(id, "id"),
        cancion = cancion.aDominio(),
        origen = origen.dominio,
        solicitudId = solicitudId?.let { id(it, "solicitud_id") },
        votos = votos?.let { noNegativo(it, "votos") },
        inicio = fecha(inicio),
        posicionSeg = noNegativo(posicionSeg, "posicion_seg"),
        estado = estado.dominio,
    )
}

@Serializable
data class ItemColaJson(
    @SerialName("solicitud_id") val solicitudId: String,
    val cancion: CancionJson,
    @SerialName("votos_total") val votosTotal: Int,
    @SerialName("fecha_solicitud") val fechaSolicitud: String,
    val puesto: Int,
    @SerialName("puesto_anterior") val puestoAnterior: Int?,
    @SerialName("espera_seg") val esperaSeg: Int,
) {
    fun aDominio() = ItemCola(
        solicitudId = id(solicitudId, "solicitud_id"),
        cancion = cancion.aDominio(),
        votosTotal = noNegativo(votosTotal, "votos_total"),
        fechaSolicitud = fecha(fechaSolicitud),
        puesto = positivo(puesto, "puesto"),
        puestoAnterior = puestoAnterior?.let { positivo(it, "puesto_anterior") },
        esperaSeg = noNegativo(esperaSeg, "espera_seg"),
    )
}

@Serializable
data class ListaCasaJson(val nombre: String, @SerialName("total_canciones") val totalCanciones: Int) {
    fun aDominio() = ListaCasaResumen(nombre, noNegativo(totalCanciones, "total_canciones"))
}

@Serializable
data class AhoraJson(
    val version: Int,
    @SerialName("servidor_ahora") val servidorAhora: String,
    val servicio: ServicioJson,
    val reproduccion: ReproduccionJson?,
    val cola: List<ItemColaJson>,
    @SerialName("lista_casa") val listaCasa: ListaCasaJson?,
) {
    fun aDominio() = Ahora(
        version = noNegativo(version, "version"),
        servidorAhora = fecha(servidorAhora),
        servicio = servicio.aDominio(),
        reproduccion = reproduccion?.aDominio(),
        cola = cola.map { it.aDominio() },
        listaCasa = listaCasa?.aDominio(),
    )
}

@Serializable
enum class ContextoJson(val dominio: ContextoDestacada) {
    @SerialName("sonando") SONANDO(ContextoDestacada.SONANDO),
    @SerialName("sono") SONO(ContextoDestacada.SONO),
    @SerialName("en_cola") EN_COLA(ContextoDestacada.EN_COLA),
}

@Serializable
data class DestacadaJson(val cancion: CancionJson, val votos: Int, val contexto: ContextoJson, val hora: String?) {
    fun aDominio() = Destacada(cancion.aDominio(), noNegativo(votos, "votos"), contexto.dominio, hora?.let(::fecha))
}

/** Respuesta de GET /bares/{slug}/destacadas: `{ destacadas: [...] }`. */
@Serializable
data class RespuestaDestacadasJson(val destacadas: List<DestacadaJson>)

@Serializable
enum class EstadoSolicitudJson(val dominio: EstadoSolicitud) {
    @SerialName("pendiente") PENDIENTE(EstadoSolicitud.PENDIENTE),
    @SerialName("reproduciendo") REPRODUCIENDO(EstadoSolicitud.REPRODUCIENDO),
    @SerialName("reproducida") REPRODUCIDA(EstadoSolicitud.REPRODUCIDA),
    @SerialName("saltada") SALTADA(EstadoSolicitud.SALTADA),
    @SerialName("no_disponible") NO_DISPONIBLE(EstadoSolicitud.NO_DISPONIBLE),
    @SerialName("bloqueada") BLOQUEADA(EstadoSolicitud.BLOQUEADA),
    @SerialName("retirada") RETIRADA(EstadoSolicitud.RETIRADA),
    @SerialName("expirada") EXPIRADA(EstadoSolicitud.EXPIRADA),
}

@Serializable
data class MiSolicitudJson(
    @SerialName("solicitud_id") val solicitudId: String,
    val cancion: CancionJson,
    val estado: EstadoSolicitudJson,
    @SerialName("fecha_solicitud") val fechaSolicitud: String,
    @SerialName("votos_total") val votosTotal: Int,
    val puesto: Int?,
    val motivo: String?,
) {
    fun aDominio() = MiSolicitud(
        solicitudId = id(solicitudId, "solicitud_id"),
        cancion = cancion.aDominio(),
        estado = estado.dominio,
        fechaSolicitud = fecha(fechaSolicitud),
        votosTotal = noNegativo(votosTotal, "votos_total"),
        puesto = puesto?.let { positivo(it, "puesto") },
        motivo = motivo,
    )
}

@Serializable
data class YoJson(
    @SerialName("pedidos_maximos") val pedidosMaximos: Int,
    @SerialName("pedidos_restantes") val pedidosRestantes: Int,
    val solicitudes: List<MiSolicitudJson>,
    val votos: List<String>,
    @SerialName("aviso_visto_en") val avisoVistoEn: String?,
) {
    fun aDominio() = Yo(
        pedidosMaximos = positivo(pedidosMaximos, "pedidos_maximos"),
        pedidosRestantes = noNegativo(pedidosRestantes, "pedidos_restantes"),
        solicitudes = solicitudes.map { it.aDominio() },
        votos = votos.map { id(it, "votos[]") },
        avisoVistoEn = avisoVistoEn?.let(::fecha),
    )
}

@Serializable
data class CuerpoDispositivoJson(val tipo: String)

@Serializable
enum class MotivoJson(val dominio: MotivoNoPedible) {
    @SerialName("bloqueada") BLOQUEADA(MotivoNoPedible.BLOQUEADA),
    @SerialName("no_disponible") NO_DISPONIBLE(MotivoNoPedible.NO_DISPONIBLE),
    @SerialName("no_embebible") NO_EMBEBIBLE(MotivoNoPedible.NO_EMBEBIBLE),
    @SerialName("muy_larga") MUY_LARGA(MotivoNoPedible.MUY_LARGA),
    @SerialName("reproducida_hace_poco") REPRODUCIDA_HACE_POCO(MotivoNoPedible.REPRODUCIDA_HACE_POCO),
}

@Serializable
data class ResultadoBusquedaJson(
    val cancion: CancionJson,
    val pedible: Boolean,
    val motivo: MotivoJson?,
    @SerialName("disponible_desde") val disponibleDesde: String?,
    val genero: String?,
    @SerialName("pedidas_en_el_bar") val pedidasEnElBar: Int,
) {
    fun aDominio() = ResultadoBusqueda(
        cancion = cancion.aDominio(),
        pedible = pedible,
        motivo = motivo?.dominio,
        disponibleDesde = disponibleDesde?.let(::fecha),
        genero = genero,
        pedidasEnElBar = noNegativo(pedidasEnElBar, "pedidas_en_el_bar"),
    )
}

@Serializable
data class BusquedaJson(
    val consulta: String,
    @SerialName("en_cola") val enCola: List<ItemColaJson>,
    val resultados: List<ResultadoBusquedaJson>,
    @SerialName("desde_cache") val desdeCache: Boolean,
) {
    fun aDominio() = Busqueda(consulta, enCola.map { it.aDominio() }, resultados.map { it.aDominio() }, desdeCache)
}

@Serializable
data class CuerpoSolicitudJson(@SerialName("video_id") val videoId: String)

@Serializable
enum class TipoRespuestaJson(val dominio: TipoRespuestaSolicitud) {
    @SerialName("creada") CREADA(TipoRespuestaSolicitud.CREADA),
    @SerialName("votada") VOTADA(TipoRespuestaSolicitud.VOTADA),
}

@Serializable
data class RespuestaSolicitudJson(
    val resultado: TipoRespuestaJson,
    @SerialName("solicitud_id") val solicitudId: String,
    val puesto: Int,
    @SerialName("espera_seg") val esperaSeg: Int,
    @SerialName("pedidos_restantes") val pedidosRestantes: Int,
) {
    fun aDominio() = RespuestaSolicitud(
        resultado = resultado.dominio,
        solicitudId = id(solicitudId, "solicitud_id"),
        puesto = positivo(puesto, "puesto"),
        esperaSeg = noNegativo(esperaSeg, "espera_seg"),
        pedidosRestantes = noNegativo(pedidosRestantes, "pedidos_restantes"),
    )
}

@Serializable
data class RespuestaVotoJson(
    @SerialName("solicitud_id") val solicitudId: String,
    @SerialName("votos_total") val votosTotal: Int,
) {
    fun aDominio() = RespuestaVoto(id(solicitudId, "solicitud_id"), noNegativo(votosTotal, "votos_total"))
}

@Serializable
data class CuerpoCambioVotoJson(val desde: String, val hacia: String)

@Serializable
data class ResolucionQrJson(val slug: String)

/** Un evento del tiempo real; el teléfono solo usa `ahora` (los demás se ignoran). */
@Serializable
data class EventoAhoraJson(val tipo: String, val ahora: AhoraJson)
