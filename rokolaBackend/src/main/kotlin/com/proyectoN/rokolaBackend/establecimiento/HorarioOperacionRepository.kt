package com.proyectoN.rokolaBackend.establecimiento

import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla `horario_operacion`. */
interface HorarioOperacionRepository : JpaRepository<HorarioOperacion, Long> {
	// Spring Data genera el SQL desde el nombre: filtra por id_establecimiento y ordena por día y hora de inicio.
	fun findByEstablecimientoIdOrderByDiaSemanaAscHoraInicioAsc(idEstablecimiento: Long): List<HorarioOperacion>
}
