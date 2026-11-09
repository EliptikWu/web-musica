package com.proyectoN.rokolaBackend.dispositivo

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Tipo de dispositivo del cliente.
 * Los valores son los que permite la restricción `ck_dispositivo_tipo` de la base de datos.
 */
enum class TipoDispositivo(
	/** Texto guardado en la columna `tipo` y usado en el JSON de la API. */
	@get:JsonValue val valor: String,
) {
	MOVIL("movil"),
	TABLETA("tableta"),
	OTRO("otro"),
	;

	companion object {
		fun desdeValor(valor: String): TipoDispositivo =
			entries.firstOrNull { it.valor == valor }
				?: throw IllegalArgumentException("Tipo de dispositivo desconocido en la base de datos: '$valor'")
	}
}

/** Traduce entre [TipoDispositivo] y el texto en minúsculas que guarda PostgreSQL. */
@Converter(autoApply = true)
class TipoDispositivoConverter : AttributeConverter<TipoDispositivo, String> {
	override fun convertToDatabaseColumn(tipo: TipoDispositivo?): String? = tipo?.valor

	override fun convertToEntityAttribute(valor: String?): TipoDispositivo? = valor?.let(TipoDispositivo::desdeValor)
}
