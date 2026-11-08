package com.proyectoN.rokolaBackend.usuario

import com.proyectoN.rokolaBackend.establecimiento.EstablecimientoNoEncontradoException
import com.proyectoN.rokolaBackend.establecimiento.EstablecimientoRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Casos de uso de consulta de usuarios y roles. */
@Service
@Transactional(readOnly = true)
class UsuarioService(
	private val usuarioRepositorio: UsuarioRepository,
	private val rolRepositorio: RolRepository,
	private val establecimientoRepositorio: EstablecimientoRepository,
) {

	/** Todos los roles, ordenados por id. */
	fun listarRoles(): List<RolResponse> =
		rolRepositorio.findAll(Sort.by("id")).map { it.toResponse() }

	/**
	 * Usuarios de un establecimiento, ordenados por id.
	 * @throws EstablecimientoNoEncontradoException si el establecimiento no existe (en lugar de devolver una lista vacía).
	 */
	fun listarPorEstablecimiento(idEstablecimiento: Long): List<UsuarioResponse> {
		if (!establecimientoRepositorio.existsById(idEstablecimiento)) {
			throw EstablecimientoNoEncontradoException(idEstablecimiento)
		}
		return usuarioRepositorio.findByEstablecimientoIdOrderByIdAsc(idEstablecimiento).map { it.toResponse() }
	}

	/** @throws UsuarioNoEncontradoException si no existe un usuario con ese id. */
	fun obtener(id: Long): UsuarioResponse =
		usuarioRepositorio.findConRolById(id)?.toResponse() ?: throw UsuarioNoEncontradoException(id)
}
