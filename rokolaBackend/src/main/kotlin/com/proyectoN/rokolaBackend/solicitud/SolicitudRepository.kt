package com.proyectoN.rokolaBackend.solicitud

import org.springframework.data.jpa.repository.JpaRepository

/** Operaciones JPA básicas de solicitud, para reutilizar en los siguientes pasos de escritura. */
interface SolicitudRepository : JpaRepository<Solicitud, Long> {
    fun findBySesionIdAndEstado(idSesion: Long, estado: EstadoSolicitud): List<Solicitud>
}
