package com.proyectoN.rokolaBackend.dispositivo

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/**
 * Se lanza cuando se pide un dispositivo que no existe.
 * Spring la convierte en una respuesta 404 con formato Problem Details (RFC 9457).
 */
class DispositivoNoEncontradoException(id: Long) : ErrorResponseException(
	HttpStatus.NOT_FOUND,
	ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No existe un dispositivo con id $id.").apply {
		title = "Dispositivo no encontrado"
	},
	null,
)
