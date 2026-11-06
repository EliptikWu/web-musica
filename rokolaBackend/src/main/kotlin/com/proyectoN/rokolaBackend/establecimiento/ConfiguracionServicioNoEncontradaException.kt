package com.proyectoN.rokolaBackend.establecimiento

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/** Se lanza cuando un establecimiento existe pero todavía no tiene configuración del servicio musical. */
class ConfiguracionServicioNoEncontradaException(idEstablecimiento: Long) : ErrorResponseException(
	HttpStatus.NOT_FOUND,
	ProblemDetail.forStatusAndDetail(
		HttpStatus.NOT_FOUND,
		"El establecimiento $idEstablecimiento no tiene configuración del servicio musical.",
	).apply {
		title = "Configuración no encontrada"
	},
	null,
)
