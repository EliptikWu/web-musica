package com.proyectoN.rokolaBackend.cancion

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/**
 * Se lanza cuando se pide una canción que no existe, por id o por id de video de YouTube.
 * Spring la convierte en una respuesta 404 con formato Problem Details (RFC 9457).
 *
 * El constructor es privado: se crea con [porId] o [porYoutubeVideoId], que arman el mensaje adecuado.
 */
class CancionNoEncontradaException private constructor(detalle: String) : ErrorResponseException(
	HttpStatus.NOT_FOUND,
	ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detalle).apply {
		title = "Canción no encontrada"
	},
	null,
) {
	companion object {
		fun porId(id: Long) = CancionNoEncontradaException("No existe una canción con id $id.")

		fun porYoutubeVideoId(youtubeVideoId: String) =
			CancionNoEncontradaException("No existe una canción con el video de YouTube '$youtubeVideoId'.")
	}
}
