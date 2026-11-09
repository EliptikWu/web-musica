package com.proyectoN.rokolaBackend.sesion

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla `sesion`. */
interface SesionRepository : JpaRepository<Sesion, Long> {

	/** Sesiones de un establecimiento, en el orden que indique [pagina]. */
	fun findByEstablecimientoId(idEstablecimiento: Long, pagina: Pageable): Page<Sesion>

	/**
	 * La sesión con ese estado abierta más recientemente, o `null` si no hay ninguna.
	 * Se pide "la primera" porque la base de datos no impide que haya dos sesiones abiertas a la vez.
	 */
	fun findFirstByEstablecimientoIdAndEstadoOrderByFechaAperturaDesc(
		idEstablecimiento: Long,
		estado: EstadoSesion,
	): Sesion?
}
