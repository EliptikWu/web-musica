package com.proyectoN.rokolaBackend.sesion

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Endpoints REST de consulta de sesiones musicales. */
@RestController
@RequestMapping("/api")
class SesionController(
	private val servicio: SesionService,
) {

	/**
	 * Historial de sesiones, de la más reciente a la más antigua. Se pagina porque crece cada noche.
	 * Parámetros: `page` (desde 0) y `size` (por defecto 20, máximo 100).
	 */
	@GetMapping("/establecimientos/{idEstablecimiento}/sesiones")
	fun listarPorEstablecimiento(
		@PathVariable idEstablecimiento: Long,
		@PageableDefault(size = 20) pagina: Pageable,
	): Page<SesionResponse> {
		// Orden fijo (igual que en establecimientos): un `sort` con un campo inexistente daría error 500.
		// El id desempata dos sesiones abiertas en el mismo instante.
		val orden = Sort.by(Sort.Order.desc("fechaApertura"), Sort.Order.desc("id"))
		val paginaOrdenada = PageRequest.of(pagina.pageNumber, pagina.pageSize, orden)
		return servicio.listarPorEstablecimiento(idEstablecimiento, paginaOrdenada)
	}

	/** La sesión abierta en este momento; es la que necesita el cliente después de escanear el QR. */
	@GetMapping("/establecimientos/{idEstablecimiento}/sesion-abierta")
	fun obtenerSesionAbierta(@PathVariable idEstablecimiento: Long): SesionResponse =
		servicio.obtenerSesionAbierta(idEstablecimiento)

	@GetMapping("/sesiones/{id}")
	fun obtener(@PathVariable id: Long): SesionResponse = servicio.obtener(id)
}
