package com.proyectoN.rokolaBackend.compat

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

/** Error contractual: el formato de errores de los controladores antiguos se conserva. */
class ContratoApiException(
  val status: HttpStatus,
  val codigo: String,
  override val message: String,
) : RuntimeException(message)

/**
 * Maneja únicamente controladores de compatibilidad, no los del backend anterior.
 * Nunca devuelve trazas, tokens ni detalles internos de base de datos al cliente.
 */
@RestControllerAdvice(assignableTypes = [
  ContratoQrBarController::class,
  ContratoDispositivoController::class,
  ContratoBusquedaController::class,
  ContratoRutaNoEncontradaController::class,
  ContratoParticipacionController::class,
  ContratoCambioVotoController::class,
  ContratoEstadoController::class,
  ContratoPantallaController::class,
  ContratoEventosController::class,
])
class ContratoErrorAdvice {
  private val logger = LoggerFactory.getLogger(ContratoErrorAdvice::class.java)

  @ExceptionHandler(ContratoApiException::class)
  fun errorContrato(error: ContratoApiException): ResponseEntity<Map<String, String>> =
    respuesta(error.status, error.codigo, error.message)

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun jsonInvalido(error: HttpMessageNotReadableException): ResponseEntity<Map<String, String>> =
    respuesta(HttpStatus.BAD_REQUEST, "datos_invalidos", "El cuerpo JSON no es válido")

  @ExceptionHandler(ServletRequestBindingException::class, MethodArgumentTypeMismatchException::class)
  fun parametroInvalido(error: Exception): ResponseEntity<Map<String, String>> =
    respuesta(HttpStatus.BAD_REQUEST, "datos_invalidos", "Falta un parámetro obligatorio o tiene un formato inválido")

  /** Evita que los errores heredados de servicios YouTube expongan ProblemDetail. */
  @ExceptionHandler(ResponseStatusException::class)
  fun estadoHttp(error: ResponseStatusException): ResponseEntity<Map<String, String>> {
    val (status, codigo) = when (error.statusCode.value()) {
      400 -> HttpStatus.BAD_REQUEST to "datos_invalidos"
      401 -> HttpStatus.UNAUTHORIZED to "no_autenticado"
      403 -> HttpStatus.FORBIDDEN to "sin_permiso"
      404 -> HttpStatus.NOT_FOUND to "no_encontrado"
      409 -> HttpStatus.CONFLICT to "conflicto"
      410 -> HttpStatus.GONE to "no_pendiente"
      422 -> HttpStatus.UNPROCESSABLE_ENTITY to "no_disponible"
      423 -> HttpStatus.LOCKED to "servicio_no_disponible"
      429 -> HttpStatus.SERVICE_UNAVAILABLE to "cuota_agotada"
      503 -> HttpStatus.SERVICE_UNAVAILABLE to "youtube_no_disponible"
      else -> HttpStatus.INTERNAL_SERVER_ERROR to "error_interno"
    }
    // En errores de infraestructura no revelamos el texto de la excepción.
    val mensaje = if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
      "No fue posible procesar la petición"
    } else {
      error.reason ?: "No fue posible completar la operación"
    }
    return respuesta(status, codigo, mensaje)
  }

  @ExceptionHandler(Exception::class)
  fun imprevisto(error: Exception): ResponseEntity<Map<String, String>> {
    // No registrar mensajes de excepciones ni request bodies: podrían contener datos de dispositivo.
    logger.error("Error imprevisto en una API contractual, tipo={}", error.javaClass.simpleName)
    return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "Ocurrió un error interno")
  }

  private fun respuesta(status: HttpStatus, codigo: String, mensaje: String): ResponseEntity<Map<String, String>> =
    ResponseEntity.status(status).body(mapOf("codigo" to codigo, "mensaje" to mensaje))
}
