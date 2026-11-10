package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Fallback de solo lectura para rutas públicas contractuales inexistentes.
 * Los mappings concretos tienen prioridad por ser más específicos.
 * No se abren permisos POST/PUT para evitar exponer controles de reproducción.
 */
@RestController
class ContratoRutaNoEncontradaController {
    @GetMapping("/api/bares/**")
    fun barDesconocido(): Nothing = noEncontrada()

    @GetMapping("/api/qr/**")
    fun qrDesconocido(): Nothing = noEncontrada()

    private fun noEncontrada(): Nothing = throw ContratoApiException(
        HttpStatus.NOT_FOUND, "no_encontrado", "Ruta pública no encontrada",
    )
}
