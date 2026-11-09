package com.proyectoN.rokolaBackend.cancion

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Si el video de YouTube se puede reproducir, según la última validación.
 * Los valores son los que permite la restricción `ck_cancion_disponibilidad` de la base de datos.
 */
enum class Disponibilidad(
	/** Texto guardado en la columna `disponibilidad` y usado en el JSON de la API. */
	@get:JsonValue val valor: String,
) {
	DISPONIBLE("disponible"),
	NO_DISPONIBLE("no_disponible"),
	RESTRINGIDA("restringida"),

	/** Todavía no se ha validado contra YouTube. */
	DESCONOCIDA("desconocida"),
	;

	companion object {
		fun desdeValor(valor: String): Disponibilidad =
			entries.firstOrNull { it.valor == valor }
				?: throw IllegalArgumentException("Disponibilidad desconocida en la base de datos: '$valor'")
	}
}

/** Traduce entre [Disponibilidad] y el texto que guarda PostgreSQL. */
@Converter(autoApply = true)
class DisponibilidadConverter : AttributeConverter<Disponibilidad, String> {
	override fun convertToDatabaseColumn(disponibilidad: Disponibilidad?): String? = disponibilidad?.valor

	override fun convertToEntityAttribute(valor: String?): Disponibilidad? = valor?.let(Disponibilidad::desdeValor)
}
