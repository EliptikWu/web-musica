package com.proyectoN.rokolaBackend.dispositivo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla `dispositivo`. */
interface DispositivoRepository : JpaRepository<Dispositivo, Long> {

	/** Dispositivos de un establecimiento, en el orden que indique [pagina]. */
	fun findByEstablecimientoId(idEstablecimiento: Long, pagina: Pageable): Page<Dispositivo>
}
