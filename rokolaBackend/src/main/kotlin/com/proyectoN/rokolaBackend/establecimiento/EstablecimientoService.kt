package com.proyectoN.rokolaBackend.establecimiento

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Casos de uso de consulta de establecimientos y de sus datos asociados. */
@Service
@Transactional(readOnly = true)
class EstablecimientoService(
	private val repositorio: EstablecimientoRepository,
	private val horarioRepositorio: HorarioOperacionRepository,
	private val configuracionRepositorio: ConfiguracionServicioRepository,
	private val codigoQrRepositorio: CodigoQrRepository,
) {

	fun listar(pagina: Pageable): Page<EstablecimientoResponse> =
		repositorio.findAll(pagina).map { it.toResponse() }

	/** @throws EstablecimientoNoEncontradoException si no existe un establecimiento con ese id. */
	fun obtener(id: Long): EstablecimientoResponse =
		repositorio.findByIdOrNull(id)?.toResponse() ?: throw EstablecimientoNoEncontradoException(id)

	/** Horarios ordenados por día de la semana y hora de inicio. */
	fun listarHorarios(idEstablecimiento: Long): List<HorarioOperacionResponse> {
		verificarQueExiste(idEstablecimiento)
		return horarioRepositorio.findByEstablecimientoIdOrderByDiaSemanaAscHoraInicioAsc(idEstablecimiento)
			.map { it.toResponse() }
	}

	/** @throws ConfiguracionServicioNoEncontradaException si el establecimiento existe pero no tiene configuración. */
	fun obtenerConfiguracion(idEstablecimiento: Long): ConfiguracionServicioResponse {
		verificarQueExiste(idEstablecimiento)
		return configuracionRepositorio.findByEstablecimientoId(idEstablecimiento)?.toResponse()
			?: throw ConfiguracionServicioNoEncontradaException(idEstablecimiento)
	}

	/** Códigos QR del más reciente al más antiguo, incluidos los revocados. */
	fun listarCodigosQr(idEstablecimiento: Long): List<CodigoQrResponse> {
		verificarQueExiste(idEstablecimiento)
		return codigoQrRepositorio.findByEstablecimientoIdOrderByFechaCreacionDesc(idEstablecimiento)
			.map { it.toResponse() }
	}

	// Distingue "el establecimiento no existe" (404) de "existe pero no tiene datos" (lista vacía).
	private fun verificarQueExiste(idEstablecimiento: Long) {
		if (!repositorio.existsById(idEstablecimiento)) {
			throw EstablecimientoNoEncontradoException(idEstablecimiento)
		}
	}
}
