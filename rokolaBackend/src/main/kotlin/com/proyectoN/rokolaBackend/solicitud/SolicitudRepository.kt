package com.proyectoN.rokolaBackend.solicitud

import org.springframework.data.jpa.repository.JpaRepository

interface SolicitudRepository : JpaRepository<Solicitud, Long> {
    fun findBySesionIdAndEstado(idSesion: Long, estado: EstadoSolicitud): List<Solicitud>

    /** Buscar solicitudes aún activas de la misma canción dentro de una sesión. */
    fun findFirstBySesionIdAndCancionIdAndEstadoIn(
        idSesion: Long,
        idCancion: Long,
        estados: Collection<EstadoSolicitud>,
    ): Solicitud?
}
