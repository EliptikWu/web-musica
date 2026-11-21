package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** La plataforma (RF-064, ADR-29). Solo vale su propia cookie; la del panel de un bar no entra. */
@RestController
@RequestMapping("/api/plataforma")
class ContratoPlataformaController(
  private val plataforma: ContratoPlataformaService,
  private val sesiones: SesionesPlataforma,
) {
  @PostMapping("/sesion")
  fun entrar(@RequestBody(required = false) cuerpo: Map<String, Any?>?): ResponseEntity<Map<String, Any?>> {
    val (admin, token) = plataforma.entrar(cuerpo)
    return ResponseEntity.ok()
      .header(HttpHeaders.SET_COOKIE, sesiones.cookie(token).toString())
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .body(admin.aContrato())
  }

  @DeleteMapping("/sesion")
  fun salir(@CookieValue(SesionesPlataforma.COOKIE, required = false) token: String?): ResponseEntity<Void> {
    sesiones.cerrar(token)
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, sesiones.cookieBorrada().toString()).build()
  }

  @GetMapping("/yo")
  fun yo(@CookieValue(SesionesPlataforma.COOKIE, required = false) token: String?) =
    sinCache(HttpStatus.OK, plataforma.exigir(token, exigirClaveNueva = false).aContrato())

  @PutMapping("/yo/clave")
  fun cambiarClave(
    @CookieValue(SesionesPlataforma.COOKIE, required = false) token: String?,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ): ResponseEntity<Void> {
    plataforma.cambiarClave(plataforma.exigir(token, exigirClaveNueva = false), token, cuerpo)
    return ResponseEntity.noContent().build()
  }

  @GetMapping("/bares")
  fun bares(@CookieValue(SesionesPlataforma.COOKIE, required = false) token: String?): ResponseEntity<Map<String, Any?>> {
    plataforma.exigir(token)
    return sinCache(HttpStatus.OK, plataforma.bares())
  }

  @PostMapping("/bares")
  fun crearBar(
    @CookieValue(SesionesPlataforma.COOKIE, required = false) token: String?,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ): ResponseEntity<Map<String, Any?>> {
    plataforma.exigir(token)
    return sinCache(HttpStatus.CREATED, plataforma.crearBar(cuerpo))
  }

  @PostMapping("/bares/{id}/cuentas")
  fun crearCuenta(
    @CookieValue(SesionesPlataforma.COOKIE, required = false) token: String?,
    @PathVariable id: String,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ): ResponseEntity<Map<String, Any?>> {
    plataforma.exigir(token)
    return sinCache(HttpStatus.CREATED, plataforma.crearCuenta(id, cuerpo))
  }

  @PostMapping("/cuentas/{id}/clave-temporal")
  fun nuevaClave(
    @CookieValue(SesionesPlataforma.COOKIE, required = false) token: String?,
    @PathVariable id: String,
  ): ResponseEntity<Map<String, Any?>> {
    plataforma.exigir(token)
    return sinCache(HttpStatus.OK, plataforma.nuevaClave(id))
  }

  private fun sinCache(estado: HttpStatus, cuerpo: Map<String, Any?>): ResponseEntity<Map<String, Any?>> =
    ResponseEntity.status(estado).header(HttpHeaders.CACHE_CONTROL, "no-store").body(cuerpo)
}
