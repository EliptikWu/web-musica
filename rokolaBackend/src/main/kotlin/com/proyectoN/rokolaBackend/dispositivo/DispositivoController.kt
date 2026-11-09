package com.proyectoN.rokolaBackend.dispositivo

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints REST de consulta de dispositivos.
 *
 * Por ahora son públicos: en la Etapa 7 (seguridad) se restringirán al personal del establecimiento.
 * El registro de un dispositivo al escanear el QR se hará en la Etapa 5.
 */
@RestController
@RequestMapping("/api")
class DispositivoController(
	private val servicio: DispositivoService,
) {

	/**
	 * Dispositivos del establecimiento, del usado más recientemente al más antiguo.
	 * Se pagina porque crece cada noche. Parámetros: `page` (desde 0) y `size` (por defecto 20, máximo 100).
	 */
	@GetMapping("/establecimientos/{idEstablecimiento}/dispositivos")
	fun listarPorEstablecimiento(
		@PathVariable idEstablecimiento: Long,
		@PageableDefault(size = 20) pagina: Pageable,
	): Page<DispositivoResponse> {
		// Orden fijo (igual que en los demás listados): un `sort` con un campo inexistente daría error 500.
		val orden = Sort.by(Sort.Order.desc("ultimoAcceso"), Sort.Order.desc("id"))
		val paginaOrdenada = PageRequest.of(pagina.pageNumber, pagina.pageSize, orden)
		return servicio.listarPorEstablecimiento(idEstablecimiento, paginaOrdenada)
	}

	@GetMapping("/dispositivos/{id}")
	fun obtener(@PathVariable id: Long): DispositivoResponse = servicio.obtener(id)
}
