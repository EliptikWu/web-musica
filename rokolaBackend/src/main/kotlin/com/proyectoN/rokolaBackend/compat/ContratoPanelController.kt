package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Panel mínimo del contrato: entrar, salir, `yo`, la noche, saltar y el establecimiento
 * (RF-021, RF-034, RF-037, RF-040, RF-059).
 * La sesión es la cookie httpOnly `rockola_panel`; el resto del panel aún no existe.
 */
@RestController
@RequestMapping("/api/panel")
class ContratoPanelController(
  private val acceso: ContratoPanelAcceso,
  private val sesiones: ContratoPanelSesiones,
  private val noches: ContratoNocheService,
  private val saltos: ContratoSaltoService,
  private val establecimientos: ContratoEstablecimientoService,
  private val pausas: ContratoPausaService,
) {
  @PostMapping("/sesion")
  fun entrar(@RequestBody(required = false) cuerpo: Map<String, Any?>?): ResponseEntity<Map<String, Any?>> {
    val (personal, token) = acceso.entrar(cuerpo?.get("correo") as? String, cuerpo?.get("clave") as? String)
    return ResponseEntity.ok()
      .header(HttpHeaders.SET_COOKIE, sesiones.cookie(token).toString())
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .body(acceso.yo(personal))
  }

  /** Siempre 204: salir sin sesión no es un error. */
  @DeleteMapping("/sesion")
  fun salir(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?): ResponseEntity<Void> {
    sesiones.cerrar(token)
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, sesiones.cookieBorrada().toString()).build()
  }

  /** Sin exigir la clave nueva: la interfaz necesita `yo` para llevar a cambiarla. */
  @GetMapping("/yo")
  fun yo(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?): ResponseEntity<Map<String, Any?>> =
    sinCache(acceso.yo(acceso.exigir(token, exigirClaveNueva = false)))

  @GetMapping("/noche")
  fun noche(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?): ResponseEntity<Map<String, Any?>> =
    sinCache(noches.noche(acceso.exigir(token)))

  @PostMapping("/reproduccion/{id}/salto")
  fun saltar(
    @PathVariable id: String,
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestHeader("Idempotency-Key", required = false) clave: String?,
  ): ResponseEntity<Void> {
    saltos.saltar(acceso.exigir(token), id, clave)
    return ResponseEntity.noContent().build()
  }

  /** RF-040: el Operador también puede (matriz D9, doc 04). */
  @PostMapping("/reproduccion/{id}/pausa")
  fun pausar(
    @PathVariable id: String,
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
  ): ResponseEntity<Void> {
    pausas.pausar(acceso.exigir(token), id)
    return ResponseEntity.noContent().build()
  }

  @PostMapping("/reproduccion/{id}/reanudacion")
  fun reanudar(
    @PathVariable id: String,
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
  ): ResponseEntity<Void> {
    pausas.reanudar(acceso.exigir(token), id)
    return ResponseEntity.noContent().build()
  }

  /** Solo el Administrador (matriz de permisos, doc 04): el Operador recibe 403. */
  @GetMapping("/establecimiento")
  fun establecimiento(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?): ResponseEntity<Map<String, Any?>> =
    sinCache(establecimientos.leer(acceso.exigir(token, rolMinimo = "administrador")))

  @PatchMapping("/establecimiento")
  fun editarEstablecimiento(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ): ResponseEntity<Map<String, Any?>> =
    sinCache(establecimientos.editar(acceso.exigir(token, rolMinimo = "administrador"), cuerpo))

  /** Lo que el panel del backend todavía no tiene: 404 con el formato del contrato. */
  @RequestMapping("/**")
  fun pendiente(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?): Nothing {
    acceso.exigir(token, exigirClaveNueva = false) // Sin sesión, 401 como cualquier ruta del panel.
    throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Esta parte del panel aún no existe en el backend")
  }

  private fun <T : Any> sinCache(cuerpo: T): ResponseEntity<T> =
    ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(cuerpo)
}
