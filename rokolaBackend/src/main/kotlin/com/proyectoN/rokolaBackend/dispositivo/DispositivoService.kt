package com.proyectoN.rokolaBackend.dispositivo

import com.proyectoN.rokolaBackend.establecimiento.EstablecimientoNoEncontradoException
import com.proyectoN.rokolaBackend.establecimiento.EstablecimientoRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Casos de uso de consulta de dispositivos de los clientes. */
@Service
@Transactional(readOnly = true)
class DispositivoService(
	private val repositorio: DispositivoRepository,
	private val establecimientoRepositorio: EstablecimientoRepository,
) {

	/** @throws EstablecimientoNoEncontradoException si el establecimiento no existe (en lugar de devolver una lista vacía). */
	fun listarPorEstablecimiento(idEstablecimiento: Long, pagina: Pageable): Page<DispositivoResponse> {
		if (!establecimientoRepositorio.existsById(idEstablecimiento)) {
			throw EstablecimientoNoEncontradoException(idEstablecimiento)
		}
		return repositorio.findByEstablecimientoId(idEstablecimiento, pagina).map { it.toResponse() }
	}

	/** @throws DispositivoNoEncontradoException si no existe un dispositivo con ese id. */
	fun obtener(id: Long): DispositivoResponse =
		repositorio.findByIdOrNull(id)?.toResponse() ?: throw DispositivoNoEncontradoException(id)
}
