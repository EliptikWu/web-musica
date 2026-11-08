package com.proyectoN.rokolaBackend.sesion

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Estado de una sesión musical.
 * Los valores son los que permite la restricción `ck_sesion_estado` de la base de datos.
 */
enum class EstadoSesion(
	/** Texto guardado en la columna `estado` y usado en el JSON de la API. */
	@get:JsonValue val valor: String,
) {
	ABIERTA("abierta"),
	CERRADA("cerrada"),
	;

	companion object {
		fun desdeValor(valor: String): EstadoSesion =
			entries.firstOrNull { it.valor == valor }
				?: throw IllegalArgumentException("Estado de sesión desconocido en la base de datos: '$valor'")
	}
}

/** Traduce entre [EstadoSesion] y el texto en minúsculas que guarda PostgreSQL. */
@Converter(autoApply = true)
class EstadoSesionConverter : AttributeConverter<EstadoSesion, String> {
	override fun convertToDatabaseColumn(estado: EstadoSesion?): String? = estado?.valor

	override fun convertToEntityAttribute(valor: String?): EstadoSesion? = valor?.let(EstadoSesion::desdeValor)
}
