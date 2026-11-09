package com.proyectoN.rokolaBackend.cancion

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Endpoints REST de consulta del catálogo de canciones. */
@RestController
@RequestMapping("/api/canciones")
class CancionController(
	private val servicio: CancionService,
) {

	/**
	 * Catálogo ordenado por título. Parámetros:
	 * - `q` (opcional): texto a buscar en el título o el artista, sin distinguir mayúsculas.
	 * - `page` (desde 0) y `size` (por defecto 20, máximo 100).
	 */
	@GetMapping
	fun buscar(
		@RequestParam(name = "q", required = false) texto: String?,
		@PageableDefault(size = 20) pagina: Pageable,
	): Page<CancionResponse> {
		// Orden fijo (igual que en los demás listados): un `sort` con un campo inexistente daría error 500.
		val orden = Sort.by(Sort.Order.asc("titulo"), Sort.Order.asc("id"))
		val paginaOrdenada = PageRequest.of(pagina.pageNumber, pagina.pageSize, orden)
		return servicio.buscar(texto, paginaOrdenada)
	}

	@GetMapping("/{id}")
	fun obtener(@PathVariable id: Long): CancionResponse = servicio.obtener(id)

	/** Permite saber si un video de YouTube ya está en el catálogo, por ejemplo `/api/canciones/youtube/dQw4w9WgXcQ`. */
	@GetMapping("/youtube/{youtubeVideoId}")
	fun obtenerPorYoutubeVideoId(@PathVariable youtubeVideoId: String): CancionResponse =
		servicio.obtenerPorYoutubeVideoId(youtubeVideoId)
}
