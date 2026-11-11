package com.example.rokola.dominio.reglas

import com.example.rokola.dominio.EstadoServicio
import com.example.rokola.dominio.EstadoSolicitud
import com.example.rokola.dominio.Yo

/*
 * Lo que el teléfono puede hacer con cada canción (apps/web/lib/reglas/pedidos.ts; RF-009 a
 * RF-012, RN-01 a RN-04). La regla manda en el servidor; aquí se decide qué tecla se pinta.
 */

enum class EstadoTecla {
    /** «+1» */
    VOTAR,
    /** Hundida; se pulsa para mover el voto. */
    VOTASTE,
    /** La pidió este teléfono; no es interactiva. */
    TUYA,
}

fun estadoTecla(solicitudId: String, yo: Yo?): EstadoTecla = when {
    yo == null -> EstadoTecla.VOTAR
    yo.solicitudes.any { it.solicitudId == solicitudId } -> EstadoTecla.TUYA
    solicitudId in yo.votos -> EstadoTecla.VOTASTE
    else -> EstadoTecla.VOTAR
}

/** IDs de las solicitudes propias que siguen en la cola. */
fun solicitudesPropiasPendientes(yo: Yo?): Set<String> =
    yo?.solicitudes.orEmpty()
        .filter { it.estado == EstadoSolicitud.PENDIENTE }
        .map { it.solicitudId }
        .toSet()

/** El servicio deja pedir y votar solo si está abierto (RF-003). */
fun aceptaPedidos(estado: EstadoServicio): Boolean = estado == EstadoServicio.ABIERTO
