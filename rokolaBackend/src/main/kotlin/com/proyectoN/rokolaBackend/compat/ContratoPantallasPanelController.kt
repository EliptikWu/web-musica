package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Solo personal Administrador del bar (la autorización depende de la cookie del panel). */
@RestController
@RequestMapping("/api/panel/pantallas")
class ContratoPantallasPanelController(
    private val acceso: ContratoPanelAcceso,
    private val pantallas: ContratoPantallasService,
) {
    @GetMapping
    fun listar(
        @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    ): ResponseEntity<Map<String, Any>> = ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(pantallas.listar(acceso.exigir(token, "administrador")))

    @PostMapping
    fun vincular(
        @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
        @RequestBody(required = false) cuerpo: Map<String, Any?>?,
    ): ResponseEntity<Map<String, Any?>> {
        val personal = acceso.exigir(token, "administrador")
        val vinculada = pantallas.vincular(personal, cuerpo)
        return ResponseEntity.status(201).header(HttpHeaders.CACHE_CONTROL, "no-store").body(vinculada)
    }

    @DeleteMapping("/{id}")
    fun desvincular(
        @PathVariable id: String,
        @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    ): ResponseEntity<Void> {
        pantallas.desvincular(acceso.exigir(token, "administrador"), id)
        return ResponseEntity.noContent().build()
    }
}
