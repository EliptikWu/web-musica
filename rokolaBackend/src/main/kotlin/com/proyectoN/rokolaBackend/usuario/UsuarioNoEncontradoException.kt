package com.proyectoN.rokolaBackend.usuario

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/**
 * Se lanza cuando se pide un usuario que no existe.
 * Spring la convierte en una respuesta 404 con formato Problem Details (RFC 9457).
 */
class UsuarioNoEncontradoException(id: Long) : ErrorResponseException(
	HttpStatus.NOT_FOUND,
	ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No existe un usuario con id $id.").apply {
		title = "Usuario no encontrado"
	},
	null,
)
