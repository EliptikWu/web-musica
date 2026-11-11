package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Endpoints públicos de la TV; el código se concede únicamente por el panel autenticado. */
@RestController
@RequestMapping("/api/bares/{slug}/pantalla/vinculo")
class ContratoVinculoPantallaController(private val pantallas: ContratoPantallasService) {
    @GetMapping
    fun consultar(
        @PathVariable slug: String,
        @CookieValue(ContratoPantallasService.COOKIE, required = false) token: String?,
    ): ResponseEntity<Map<String, Any?>> = ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(pantallas.consultar(slug, token))

    @PostMapping
    fun pedirCodigo(
        @PathVariable slug: String,
        @CookieValue(ContratoPantallasService.COOKIE, required = false) token: String?,
    ): ResponseEntity<Map<String, Any?>> {
        val (respuesta, cookie) = pantallas.pedirCodigo(slug, token)
        return ResponseEntity.status(201)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(respuesta)
    }
}
