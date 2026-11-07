package com.proyectoN.rokolaBackend.establecimiento

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/**
 * Se lanza cuando se pide un establecimiento que no existe.
 * Spring la convierte en una respuesta 404 con formato Problem Details (RFC 9457).
 */
class EstablecimientoNoEncontradoException(id: Long) : ErrorResponseException(
	HttpStatus.NOT_FOUND,
	ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No existe un establecimiento con id $id.").apply {
		title = "Establecimiento no encontrado"
	},
	null,
)
