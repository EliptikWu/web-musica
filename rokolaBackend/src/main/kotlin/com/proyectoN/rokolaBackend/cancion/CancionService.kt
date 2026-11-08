package com.proyectoN.rokolaBackend.cancion

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Casos de uso de consulta del catálogo local de canciones. */
@Service
@Transactional(readOnly = true)
class CancionService(
	private val repositorio: CancionRepository,
) {

	/**
	 * Busca por título o artista. Si [texto] es `null` o está en blanco, devuelve todo el catálogo.
	 * Solo busca en las canciones ya guardadas; la búsqueda en YouTube llegará en la Etapa 8.
	 */
	fun buscar(texto: String?, pagina: Pageable): Page<CancionResponse> {
		val filtro = texto?.trim().orEmpty()
		val resultado = if (filtro.isEmpty()) {
			repositorio.findAll(pagina)
		} else {
			repositorio.findByTituloContainingIgnoreCaseOrArtistaContainingIgnoreCase(filtro, filtro, pagina)
		}
		return resultado.map { it.toResponse() }
	}

	/** @throws CancionNoEncontradaException si no existe una canción con ese id. */
	fun obtener(id: Long): CancionResponse =
		repositorio.findByIdOrNull(id)?.toResponse() ?: throw CancionNoEncontradaException.porId(id)

	/** @throws CancionNoEncontradaException si ese video todavía no está en el catálogo. */
	fun obtenerPorYoutubeVideoId(youtubeVideoId: String): CancionResponse =
		repositorio.findByYoutubeVideoId(youtubeVideoId)?.toResponse()
			?: throw CancionNoEncontradaException.porYoutubeVideoId(youtubeVideoId)
}
