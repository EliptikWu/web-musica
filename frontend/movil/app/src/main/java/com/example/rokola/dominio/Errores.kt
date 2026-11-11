package com.example.rokola.dominio

import java.time.Instant

/**
 * Códigos de error del contrato (doc 04, «Códigos de error»). La interfaz traduce el código
 * a su texto; el mensaje del servidor no se muestra.
 */
enum class CodigoError {
    DATOS_INVALIDOS,
    NO_AUTENTICADO,
    CREDENCIALES_INVALIDAS,
    SIN_PERMISO,
    DEBE_CAMBIAR_CLAVE,
    NO_ENCONTRADO,
    LIMITE_PEDIDOS,
    YA_VOTASTE,
    CONFLICTO,
    QR_REEMPLAZADO,
    NO_PENDIENTE,
    BLOQUEADA,
    NO_DISPONIBLE,
    NO_EMBEBIBLE,
    MUY_LARGA,
    REPRODUCIDA_HACE_POCO,
    SERVICIO_NO_DISPONIBLE,
    CUENTA_BLOQUEADA,
    YOUTUBE_NO_DISPONIBLE,
    CUOTA_AGOTADA,
    ERROR_INTERNO,

    /** Del teléfono, no del servidor: no llegó respuesta o se cortó el tiempo real. */
    SIN_CONEXION,
}

data class ErrorRockola(
    val codigo: CodigoError,
    val mensaje: String,
    /** detalle.disponible_desde de «reproducida_hace_poco». */
    val disponibleDesde: Instant? = null,
)

/** Respuesta de una operación: el valor o el error del contrato. */
sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ErrorRockola) : Resultado<Nothing>
}

/** Excepción con la que un Flow de tiempo real avisa que se cortó. */
class FalloRockola(val error: ErrorRockola) : Exception(error.mensaje)
