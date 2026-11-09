package com.proyectoN.rokolaBackend.solicitud

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/** Estados permitidos por ck_solicitud_estado en PostgreSQL. */
enum class EstadoSolicitud(val valor: String) {
    PENDIENTE("pendiente"),
    REPRODUCIENDO("reproduciendo"),
    REPRODUCIDA("reproducida"),
    SALTADA("saltada"),
    NO_DISPONIBLE("no_disponible"),
    BLOQUEADA("bloqueada"),
    EXPIRADA("expirada"),
    RETIRADA("retirada");

    companion object {
        fun desdeValor(valor: String): EstadoSolicitud =
            entries.firstOrNull { it.valor == valor }
                ?: throw IllegalArgumentException("Estado de solicitud desconocido: '$valor'")
    }
}

@Converter(autoApply = true)
class EstadoSolicitudConverter : AttributeConverter<EstadoSolicitud, String> {
    override fun convertToDatabaseColumn(attribute: EstadoSolicitud?): String? = attribute?.valor

    override fun convertToEntityAttribute(dbData: String?): EstadoSolicitud? =
        dbData?.let(EstadoSolicitud::desdeValor)
}
