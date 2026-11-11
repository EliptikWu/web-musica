package com.example.rokola.dominio

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/*
 * Entidades que ve el teléfono, de C:\web-musica\docs\04-modelo-de-datos-y-contrato.md.
 * Mismos nombres que el contrato, en camelCase. Fechas en Instant; duraciones en segundos.
 */

data class Cancion(
    val videoId: String,
    val titulo: String,
    val artista: String,
    val duracionSeg: Int,
    val miniaturaUrl: String?,
)

/** Una franja del horario. Si cruza medianoche, pertenece al día en que empieza. */
data class Franja(
    /** 1 = lunes … 7 = domingo. */
    val diaSemana: Int,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val activo: Boolean,
)

/** Forma de un slug en el contrato (comunes.ts): minúsculas, números y guiones sueltos. */
val FORMATO_SLUG = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

/** Datos públicos del bar (GET /bares/{slug}). */
data class Bar(
    val slug: String,
    val nombre: String,
    val zonaHoraria: ZoneId,
    val pedidosMaximos: Int,
    val duracionMaxSeg: Int,
    val ventanaNoRepeticionMin: Int,
    val horario: List<Franja>,
)

enum class EstadoServicio { ABIERTO, FUERA_DE_HORARIO, EN_PAUSA, MANTENIMIENTO }

enum class ModoQr { AUTOMATICO, FORZADO_ACTIVO, PAUSADO }

data class Servicio(
    val estado: EstadoServicio,
    val modoQr: ModoQr,
    val desde: Instant?,
    val hasta: Instant?,
    val proximaApertura: Instant?,
)

enum class OrigenReproduccion { SOLICITUD, LISTA_CASA }

enum class EstadoReproduccion { SONANDO, PAUSADA }

/** Lo que suena. El avance se calcula con [posicionSeg] y el reloj del servidor. */
data class Reproduccion(
    val id: String,
    val cancion: Cancion,
    val origen: OrigenReproduccion,
    val solicitudId: String?,
    /** null cuando suena la lista de la casa. */
    val votos: Int?,
    val inicio: Instant,
    val posicionSeg: Int,
    val estado: EstadoReproduccion,
)

/** Una canción de la cola, ya ordenada por el servidor (RN-05). */
data class ItemCola(
    val solicitudId: String,
    val cancion: Cancion,
    val votosTotal: Int,
    val fechaSolicitud: Instant,
    val puesto: Int,
    /** Alimenta «Subió N puestos»; null si no subió hace poco. */
    val puestoAnterior: Int?,
    /** Espera estimada en servidorAhora, calculada por el backend (RF-018, ADR-20 de la web). */
    val esperaSeg: Int,
)

data class ListaCasaResumen(val nombre: String, val totalCanciones: Int)

/** La foto que se envía por tiempo real. */
data class Ahora(
    val version: Int,
    val servidorAhora: Instant,
    val servicio: Servicio,
    val reproduccion: Reproduccion?,
    val cola: List<ItemCola>,
    val listaCasa: ListaCasaResumen?,
)

enum class EstadoSolicitud {
    PENDIENTE, REPRODUCIENDO, REPRODUCIDA, SALTADA, NO_DISPONIBLE, BLOQUEADA, RETIRADA, EXPIRADA,
}

/** Para «Tus pedidos» (RF-013). */
data class MiSolicitud(
    val solicitudId: String,
    val cancion: Cancion,
    val estado: EstadoSolicitud,
    val fechaSolicitud: Instant,
    val votosTotal: Int,
    /** Solo si sigue pendiente. */
    val puesto: Int?,
    val motivo: String?,
)

/** El teléfono: cupo, sus solicitudes y sus votos. */
data class Yo(
    val pedidosMaximos: Int,
    val pedidosRestantes: Int,
    val solicitudes: List<MiSolicitud>,
    /** IDs de las solicitudes por las que votó. */
    val votos: List<String>,
    val avisoVistoEn: Instant?,
)

enum class ContextoDestacada { SONANDO, SONO, EN_COLA }

/** «Lo más votado de la noche». */
data class Destacada(
    val cancion: Cancion,
    val votos: Int,
    val contexto: ContextoDestacada,
    /** Hora en que sonó, si ya sonó. */
    val hora: Instant?,
)

/** Motivos de RN-06 y RN-07 por los que una canción no se puede pedir. */
enum class MotivoNoPedible { BLOQUEADA, NO_DISPONIBLE, NO_EMBEBIBLE, MUY_LARGA, REPRODUCIDA_HACE_POCO }

data class ResultadoBusqueda(
    val cancion: Cancion,
    val pedible: Boolean,
    val motivo: MotivoNoPedible?,
    /** Para «reproducida hace poco»: desde cuándo se puede pedir otra vez. */
    val disponibleDesde: Instant?,
    /** null = sin clasificar. */
    val genero: String?,
    /** Veces pedida en el bar en las últimas cuatro semanas. */
    val pedidasEnElBar: Int,
)

data class Busqueda(
    val consulta: String,
    /** Coincidencias que ya están en la cola: se votan en lugar de pedirse. */
    val enCola: List<ItemCola>,
    val resultados: List<ResultadoBusqueda>,
    /** YouTube agotó la cuota y los resultados vienen de la caché del servidor. */
    val desdeCache: Boolean,
)

enum class TipoRespuestaSolicitud {
    CREADA,
    /** Ya estaba en la cola: pedirla contó como voto (RN-02). */
    VOTADA,
}

data class RespuestaSolicitud(
    val resultado: TipoRespuestaSolicitud,
    val solicitudId: String,
    val puesto: Int,
    val esperaSeg: Int,
    val pedidosRestantes: Int,
)

data class RespuestaVoto(val solicitudId: String, val votosTotal: Int)
