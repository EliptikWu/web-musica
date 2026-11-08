package com.proyectoN.rokolaBackend.dispositivo

import java.time.OffsetDateTime

/**
 * Datos de un dispositivo que entrega la API.
 * A propósito NO incluye `deviceHash` (ver [Dispositivo]).
 */
data class DispositivoResponse(
	val id: Long,
	val idEstablecimiento: Long,
	val tipo: TipoDispositivo,
	val primerAcceso: OffsetDateTime,
	val ultimoAcceso: OffsetDateTime,
	val avisoPrivacidadFecha: OffsetDateTime?,
)

fun Dispositivo.toResponse() = DispositivoResponse(
	id = requireNotNull(id) { "Un dispositivo leído de la base de datos siempre tiene id" },
	// Leer solo el id del establecimiento no dispara una consulta extra: Hibernate ya lo conoce.
	idEstablecimiento = requireNotNull(establecimiento.id) { "El establecimiento de un dispositivo siempre tiene id" },
	tipo = tipo,
	primerAcceso = primerAcceso,
	ultimoAcceso = ultimoAcceso,
	avisoPrivacidadFecha = avisoPrivacidadFecha,
)
