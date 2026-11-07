package com.proyectoN.rokolaBackend.establecimiento

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Estado del servicio musical que el personal fija manualmente.
 * Los valores son los que permite la restricción `ck_config_estado` de la base de datos.
 */
enum class EstadoServicio(
	/** Texto guardado en la columna `estado_manual` y usado en el JSON de la API. */
	@get:JsonValue val valor: String,
) {
	DISPONIBLE("disponible"),
	PAUSADO("pausado"),
	MANTENIMIENTO("mantenimiento"),
	;

	companion object {
		fun desdeValor(valor: String): EstadoServicio =
			entries.firstOrNull { it.valor == valor }
				?: throw IllegalArgumentException("Estado de servicio desconocido en la base de datos: '$valor'")
	}
}

/** Traduce entre [EstadoServicio] y el texto en minúsculas que guarda PostgreSQL. */
@Converter(autoApply = true)
class EstadoServicioConverter : AttributeConverter<EstadoServicio, String> {
	override fun convertToDatabaseColumn(estado: EstadoServicio?): String? = estado?.valor

	override fun convertToEntityAttribute(valor: String?): EstadoServicio? = valor?.let(EstadoServicio::desdeValor)
}
