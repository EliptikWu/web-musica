package com.proyectoN.rokolaBackend.usuario

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Acceso a la tabla `usuario`.
 *
 * `@EntityGraph(attributePaths = ["rol"])` trae el rol en la misma consulta (con un JOIN).
 * Sin eso, Hibernate haría una consulta extra por cada usuario para leer su rol.
 */
interface UsuarioRepository : JpaRepository<Usuario, Long> {

	@EntityGraph(attributePaths = ["rol"])
	fun findByEstablecimientoIdOrderByIdAsc(idEstablecimiento: Long): List<Usuario>

	/** Igual que `findById`, pero cargando el rol. Devuelve `null` si no existe. */
	@EntityGraph(attributePaths = ["rol"])
	fun findConRolById(id: Long): Usuario?
}
