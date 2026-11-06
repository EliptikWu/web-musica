package com.proyectoN.rokolaBackend.establecimiento

import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla `configuracion_servicio`. */
interface ConfiguracionServicioRepository : JpaRepository<ConfiguracionServicio, Long> {
	/** Devuelve `null` si el establecimiento no tiene configuración. */
	fun findByEstablecimientoId(idEstablecimiento: Long): ConfiguracionServicio?
}
