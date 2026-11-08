package com.proyectoN.rokolaBackend.cancion

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla `cancion`. */
interface CancionRepository : JpaRepository<Cancion, Long> {

	/**
	 * Canciones cuyo título o artista contienen el texto, sin distinguir mayúsculas.
	 * Spring Data lo traduce a `LOWER(titulo) LIKE %texto% OR LOWER(artista) LIKE %texto%`
	 * y escapa los caracteres especiales `%` y `_` del texto.
	 */
	fun findByTituloContainingIgnoreCaseOrArtistaContainingIgnoreCase(
		titulo: String,
		artista: String,
		pagina: Pageable,
	): Page<Cancion>

	fun findByYoutubeVideoId(youtubeVideoId: String): Cancion?
}
