package com.example.rokola.datos.simulacion

import com.example.rokola.dominio.EstadoSolicitud
import com.example.rokola.dominio.Franja
import com.example.rokola.dominio.ModoQr
import com.example.rokola.dominio.OrigenReproduccion
import java.time.ZoneId

/*
 * Estado interno de la simulación (apps/web/simulacion/tipos.ts): se parece más a la base de
 * datos que al contrato. Las respuestas se arman en MotorSimulado. Fechas en milisegundos de
 * la hora simulada. Solo lo que usa el teléfono: el panel y el tablero quedan en la web.
 */

internal data class CancionSim(
    val videoId: String,
    val titulo: String,
    val artista: String,
    val duracionSeg: Int,
    val embebible: Boolean,
    val disponible: Boolean,
    /** "sin_clasificar" cuando YouTube no trae género. */
    val genero: String,
)

/** Lo que subió en el último reordenamiento por votos, y cuándo. */
internal data class Subida(val puestos: Int, val en: Long)

internal class SolicitudSim(
    val id: String,
    val sesionId: String,
    val videoId: String,
    var dispositivo: String,
    val fecha: Long,
    val votos: MutableList<String>,
    var estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
    var inicio: Long? = null,
    var fin: Long? = null,
    var motivo: String? = null,
    var subida: Subida? = null,
)

internal class ReproduccionSim(
    val id: String,
    val videoId: String,
    val origen: OrigenReproduccion,
    val solicitudId: String?,
    var inicio: Long,
    /** Segundos ya reproducidos antes de la última reanudación. */
    var acumuladoSeg: Double,
    /** Desde cuándo suena sin pausa; null = en pausa. */
    var reanudadaEn: Long?,
)

internal enum class ModoRotacion { SECUENCIAL, ALEATORIA }

internal class ElementoLista(val elementoId: String, val videoId: String, var veces: Int = 0, var ultima: Long? = null)

internal class ListaSim(
    val id: String,
    val nombre: String,
    val modoRotacion: ModoRotacion,
    var activa: Boolean,
    val elementos: List<ElementoLista>,
    /** Próximo índice (secuencial). */
    var indice: Int = 0,
    /** Lo que queda por sonar en esta vuelta (aleatoria). */
    var pendientesAleatorios: MutableList<String> = mutableListOf(),
)

internal enum class DuracionBloqueo { FIN_SESION, HORAS, INDEFINIDO }

internal class BloqueoSim(
    val id: String,
    val videoId: String,
    val motivo: String,
    val duracion: DuracionBloqueo,
    val inicio: Long,
    var fin: Long?,
    val creadoPor: String,
    var levantado: Boolean = false,
)

internal class SesionSim(val id: String, val apertura: Long, val tipoEvento: String, val origen: String)

/** Solo el tipo y la hora: el servicio los usa para saber «desde cuándo». */
internal class AccionSim(val tipo: String, val fecha: Long)

internal class DispositivoSim(val codigo: String, val tipo: String, val primerAcceso: Long, var aviso: Long?)

internal class BarSim(
    val id: String,
    val slug: String,
    val nombre: String,
    val zonaHoraria: ZoneId,
    val pedidosMaximos: Int,
    val duracionMaxSeg: Int,
    val ventanaNoRepeticionMin: Int,
)

/** Reloj virtual: hora simulada = anclaSim + (ahora real − anclaReal) × velocidad. */
internal data class RelojSim(val anclaSim: Long, val anclaReal: Long, val velocidad: Double)

internal enum class FalloYoutube { BIEN, CAIDO, CUOTA }

internal enum class FalloConexion { BIEN, CORTADA }

internal class EstadoSim(
    var escenario: Escenario,
    var version: Int,
    var reloj: RelojSim,
    val bar: BarSim,
    var modoQr: ModoQr,
    var mantenimiento: Boolean,
    val horario: List<Franja>,
    var sesion: SesionSim?,
    /** Si el personal cerró la noche a mano dentro del horario, no se reabre sola hasta esta hora. */
    var sinAperturaAutomaticaHasta: Long?,
    val catalogo: LinkedHashMap<String, CancionSim>,
    val solicitudes: MutableList<SolicitudSim>,
    var reproduccion: ReproduccionSim?,
    val listas: List<ListaSim>,
    val bloqueos: MutableList<BloqueoSim>,
    val acciones: MutableList<AccionSim>,
    val dispositivos: MutableMap<String, DispositivoSim>,
    var falloYoutube: FalloYoutube = FalloYoutube.BIEN,
    var falloConexion: FalloConexion = FalloConexion.BIEN,
    var secuencia: Int,
)
