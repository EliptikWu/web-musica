package com.proyectoN.rokolaBackend.establecimiento

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Endpoints REST de consulta de establecimientos. */
@RestController
@RequestMapping("/api/establecimientos")
class EstablecimientoController(
	private val servicio: EstablecimientoService,
) {

	/** Lista paginada. Parámetros: `page` (desde 0) y `size` (por defecto 20, máximo 100). */
	@GetMapping
	fun listar(@PageableDefault(size = 20) pagina: Pageable): Page<EstablecimientoResponse> {
		// El orden es fijo: si el cliente pudiera enviar `sort`, un campo inexistente provocaría un error 500.
		val paginaOrdenada = PageRequest.of(pagina.pageNumber, pagina.pageSize, Sort.by("id"))
		return servicio.listar(paginaOrdenada)
	}

	@GetMapping("/{id}")
	fun obtener(@PathVariable id: Long): EstablecimientoResponse = servicio.obtener(id)

	// Las tres listas siguientes no se paginan: son pocas filas por establecimiento
	@GetMapping("/{id}/horarios")
	fun listarHorarios(@PathVariable id: Long): List<HorarioOperacionResponse> = servicio.listarHorarios(id)

	@GetMapping("/{id}/configuracion")
	fun obtenerConfiguracion(@PathVariable id: Long): ConfiguracionServicioResponse = servicio.obtenerConfiguracion(id)

	@GetMapping("/{id}/codigos-qr")
	fun listarCodigosQr(@PathVariable id: Long): List<CodigoQrResponse> = servicio.listarCodigosQr(id)
}
