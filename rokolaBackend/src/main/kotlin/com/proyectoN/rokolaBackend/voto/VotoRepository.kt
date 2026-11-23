package com.proyectoN.rokolaBackend.voto

import org.springframework.data.jpa.repository.JpaRepository

interface VotoRepository : JpaRepository<Voto, Long> {
    /** Cuenta exclusivamente los votos activos de una solicitud. */
    fun countBySolicitudIdAndActivoTrue(idSolicitud: Long): Long

    /** Útil para la futura validación de un voto por dispositivo y solicitud. */
    fun existsBySolicitudIdAndDispositivoIdAndActivoTrue(
        idSolicitud: Long,
        idDispositivo: Long,
    ): Boolean
}
