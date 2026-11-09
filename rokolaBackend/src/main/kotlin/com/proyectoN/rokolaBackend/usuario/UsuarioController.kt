package com.proyectoN.rokolaBackend.usuario

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints REST de consulta de usuarios y roles.
 *
 * Por ahora son públicos: en la Etapa 7 (seguridad) se restringirán al personal autorizado.
 * Las listas no se paginan porque son pocas filas (2 roles y unos cuantos usuarios por establecimiento).
 */
@RestController
@RequestMapping("/api")
class UsuarioController(
	private val servicio: UsuarioService,
) {

	@GetMapping("/roles")
	fun listarRoles(): List<RolResponse> = servicio.listarRoles()

	@GetMapping("/establecimientos/{idEstablecimiento}/usuarios")
	fun listarPorEstablecimiento(@PathVariable idEstablecimiento: Long): List<UsuarioResponse> =
		servicio.listarPorEstablecimiento(idEstablecimiento)

	@GetMapping("/usuarios/{id}")
	fun obtener(@PathVariable id: Long): UsuarioResponse = servicio.obtener(id)
}
