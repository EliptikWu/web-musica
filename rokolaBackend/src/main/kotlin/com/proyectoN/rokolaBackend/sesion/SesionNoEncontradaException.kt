package com.proyectoN.rokolaBackend.sesion

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/**
 * Se lanza cuando se pide una sesión que no existe.
 * Spring la convierte en una respuesta 404 con formato Problem Details (RFC 9457).
 */
class SesionNoEncontradaException(id: Long) : ErrorResponseException(
	HttpStatus.NOT_FOUND,
	ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No existe una sesión con id $id.").apply {
		title = "Sesión no encontrada"
	},
	null,
)
