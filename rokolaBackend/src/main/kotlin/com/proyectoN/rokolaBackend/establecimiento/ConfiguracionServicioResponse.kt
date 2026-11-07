package com.proyectoN.rokolaBackend.establecimiento

import java.time.OffsetDateTime

/** Reglas de participación musical de un establecimiento. */
data class ConfiguracionServicioResponse(
	val estadoManual: EstadoServicio,
	val maxSolicitudesDispositivo: Int,
	val duracionMaxSeg: Int,
	val minutosBloqueoRepeticion: Int,
	val avisoPrivacidadVersion: String?,
	val fechaModificacion: OffsetDateTime,
)

fun ConfiguracionServicio.toResponse() = ConfiguracionServicioResponse(
	estadoManual = estadoManual,
	maxSolicitudesDispositivo = maxSolicitudesDispositivo,
	duracionMaxSeg = duracionMaxSeg,
	minutosBloqueoRepeticion = minutosBloqueoRepeticion,
	avisoPrivacidadVersion = avisoPrivacidadVersion,
	fechaModificacion = fechaModificacion,
)
