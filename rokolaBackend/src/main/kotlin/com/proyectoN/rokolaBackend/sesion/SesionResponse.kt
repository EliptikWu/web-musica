package com.proyectoN.rokolaBackend.sesion

import java.time.OffsetDateTime

/** Datos de una sesión musical que entrega la API. */
data class SesionResponse(
	val id: Long,
	val idEstablecimiento: Long,
	val idEventoCalendario: Long?,
	val fechaApertura: OffsetDateTime,
	val fechaCierre: OffsetDateTime?,
	val tipoEvento: String?,
	val estado: EstadoSesion,
	val indiceListaDefecto: Int,
)

fun Sesion.toResponse() = SesionResponse(
	id = requireNotNull(id) { "Una sesión leída de la base de datos siempre tiene id" },
	// Leer solo el id del establecimiento no dispara una consulta extra: Hibernate ya lo conoce.
	idEstablecimiento = requireNotNull(establecimiento.id) { "El establecimiento de una sesión siempre tiene id" },
	idEventoCalendario = idEventoCalendario,
	fechaApertura = fechaApertura,
	fechaCierre = fechaCierre,
	tipoEvento = tipoEvento,
	estado = estado,
	indiceListaDefecto = indiceListaDefecto,
)
