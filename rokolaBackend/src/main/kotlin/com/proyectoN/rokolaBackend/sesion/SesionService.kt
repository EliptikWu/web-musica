package com.proyectoN.rokolaBackend.sesion

import com.proyectoN.rokolaBackend.establecimiento.EstablecimientoNoEncontradoException
import com.proyectoN.rokolaBackend.establecimiento.EstablecimientoRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Casos de uso de consulta de sesiones musicales. */
@Service
@Transactional(readOnly = true)
class SesionService(
	private val repositorio: SesionRepository,
	private val establecimientoRepositorio: EstablecimientoRepository,
) {

	/** @throws EstablecimientoNoEncontradoException si el establecimiento no existe. */
	fun listarPorEstablecimiento(idEstablecimiento: Long, pagina: Pageable): Page<SesionResponse> {
		verificarQueExisteEstablecimiento(idEstablecimiento)
		return repositorio.findByEstablecimientoId(idEstablecimiento, pagina).map { it.toResponse() }
	}

	/**
	 * La sesión abierta del establecimiento (si hubiera varias, la abierta más recientemente).
	 * Es la que usarán los clientes para pedir y votar canciones.
	 *
	 * @throws EstablecimientoNoEncontradoException si el establecimiento no existe.
	 * @throws SinSesionAbiertaException si existe pero no tiene una sesión abierta.
	 */
	fun obtenerSesionAbierta(idEstablecimiento: Long): SesionResponse {
		verificarQueExisteEstablecimiento(idEstablecimiento)
		return repositorio.findFirstByEstablecimientoIdAndEstadoOrderByFechaAperturaDesc(
			idEstablecimiento,
			EstadoSesion.ABIERTA,
		)?.toResponse() ?: throw SinSesionAbiertaException(idEstablecimiento)
	}

	/** @throws SesionNoEncontradaException si no existe una sesión con ese id. */
	fun obtener(id: Long): SesionResponse =
		repositorio.findByIdOrNull(id)?.toResponse() ?: throw SesionNoEncontradaException(id)

	private fun verificarQueExisteEstablecimiento(idEstablecimiento: Long) {
		if (!establecimientoRepositorio.existsById(idEstablecimiento)) {
			throw EstablecimientoNoEncontradoException(idEstablecimiento)
		}
	}
}
