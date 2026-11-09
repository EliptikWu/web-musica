package com.proyectoN.rokolaBackend.sesion

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/**
 * Se lanza cuando el establecimiento existe pero no tiene ninguna sesión abierta,
 * por ejemplo antes de que el personal abra la noche. Responde 404 con formato Problem Details.
 */
class SinSesionAbiertaException(idEstablecimiento: Long) : ErrorResponseException(
	HttpStatus.NOT_FOUND,
	ProblemDetail.forStatusAndDetail(
		HttpStatus.NOT_FOUND,
		"El establecimiento con id $idEstablecimiento no tiene una sesión abierta.",
	).apply {
		title = "No hay sesión abierta"
	},
	null,
)
