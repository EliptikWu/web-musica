package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus

/**
 * Lectura de los cuerpos JSON del panel con los límites del contrato (`packages/contrato`). Un
 * campo de más, de otro tipo o fuera de rango es `400 datos_invalidos`, nunca un 500.
 */
internal object ContratoCuerpo {
  fun exigirCampos(cuerpo: Map<String, Any?>, permitidos: Set<String>) {
    val desconocidos = cuerpo.keys - permitidos
    if (desconocidos.isNotEmpty()) invalido("Campos que no se pueden enviar: ${desconocidos.sorted().joinToString()}")
  }

  /** Texto sin los espacios de los lados; null si el campo no viene y no es obligatorio. */
  fun texto(cuerpo: Map<String, Any?>, campo: String, max: Int, obligatorio: Boolean = false, vacioPermitido: Boolean = false): String? {
    if (!cuerpo.containsKey(campo) || cuerpo[campo] == null) {
      if (obligatorio) invalido("Falta $campo")
      return null
    }
    val v = (cuerpo[campo] as? String ?: invalido("$campo debe ser texto")).trim()
    if (v.isEmpty() && !vacioPermitido) invalido("$campo no puede quedar vacío")
    if (v.length > max) invalido("$campo: máximo $max caracteres")
    return v
  }

  /** Entero dentro de [min, max]; null si no viene (o viene null y se acepta). */
  fun entero(cuerpo: Map<String, Any?>, campo: String, min: Int, max: Int, obligatorio: Boolean = false): Int? {
    if (!cuerpo.containsKey(campo) || cuerpo[campo] == null) {
      if (obligatorio) invalido("Falta $campo")
      return null
    }
    val n = cuerpo[campo] as? Number ?: invalido("$campo debe ser un número entero")
    val d = n.toDouble()
    if (d != Math.floor(d) || d < min || d > max) invalido("$campo: de $min a $max")
    return d.toInt()
  }

  fun booleano(cuerpo: Map<String, Any?>, campo: String): Boolean =
    cuerpo[campo] as? Boolean ?: invalido("$campo debe ser verdadero o falso")

  fun <T> opcion(valor: String?, campo: String, opciones: Set<String>, convertir: (String) -> T): T {
    if (valor == null || valor !in opciones) invalido("$campo: ${opciones.sorted().joinToString(" o ")}")
    return convertir(valor)
  }

  /** Un id del contrato (texto con un entero positivo). Lo que no lo es no existe: 404. */
  fun id(texto: String, queNoExiste: String): Long =
    texto.toLongOrNull()?.takeIf { it > 0 } ?: throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", queNoExiste)

  fun invalido(mensaje: String): Nothing = throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", mensaje)

  fun conflicto(mensaje: String): Nothing = throw ContratoApiException(HttpStatus.CONFLICT, "conflicto", mensaje)

  fun noEncontrado(mensaje: String): Nothing = throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", mensaje)
}
