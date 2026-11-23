package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Personal con cookie del panel: solo el administrador puede corregir géneros. */
@RestController
@RequestMapping("/api/panel")
class ContratoGenerosController(
    private val acceso: ContratoPanelAcceso,
    private val generos: ContratoGenerosService,
) {
    @GetMapping("/generos")
    fun catalogo(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?): ResponseEntity<List<String>> {
        acceso.exigir(token, rolMinimo = "administrador")
        return sinCache(generos.catalogo())
    }

    @GetMapping("/canciones")
    fun canciones(
        @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
        @RequestParam(defaultValue = "true") soloSinCategoria: Boolean,
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "50") limite: Int,
    ): ResponseEntity<List<Map<String, Any?>>> =
        sinCache(generos.canciones(acceso.exigir(token, rolMinimo = "administrador"), soloSinCategoria, q, limite))

    @PatchMapping("/canciones/{id}/categoria")
    fun corregir(
        @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
        @PathVariable id: Long,
        @RequestBody(required = false) cuerpo: Map<String, Any?>?,
    ): ResponseEntity<Map<String, Any?>> = sinCache(
        generos.corregir(acceso.exigir(token, rolMinimo = "administrador"), id, cuerpo?.get("categoria") as? String)
    )

    @PostMapping("/canciones/reclasificar")
    fun reclasificar(
        @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
        @RequestParam(defaultValue = "10") limite: Int,
        @RequestParam(defaultValue = "0") despuesDe: Long,
    ): ResponseEntity<Map<String, Any?>> =
        sinCache(generos.reclasificar(acceso.exigir(token, rolMinimo = "administrador"), limite, despuesDe))

    @GetMapping("/generos/estado-analitica")
    fun estadoAnalitica(
        @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    ): ResponseEntity<Map<String, Any?>> =
        sinCache(generos.estadoAnalitica(acceso.exigir(token, rolMinimo = "administrador")))

    @GetMapping("/generos/resumen")
    fun resumen(
        @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
        @RequestParam(defaultValue = "30") dias: Int,
    ): ResponseEntity<List<Map<String, Any?>>> =
        sinCache(generos.resumen(acceso.exigir(token, rolMinimo = "administrador"), dias))

    private fun <T : Any> sinCache(valor: T): ResponseEntity<T> = ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(valor)
}
