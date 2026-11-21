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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Gestión del panel (doc 04, «Panel»): servicio y mantenimiento, bitácora, horario, usuarios y
 * clave propia, bloqueos, listas de la casa y la búsqueda para armarlas. El rol mínimo de cada ruta es el del contrato
 * (`packages/contrato/src/endpoints.ts`) y lo decide el servidor: la interfaz solo oculta.
 * Lo que aún no existe sigue en `ContratoPanelController.pendiente` (404 del contrato).
 */
@RestController
@RequestMapping("/api/panel")
class ContratoGestionController(
  private val acceso: ContratoPanelAcceso,
  private val horario: ContratoHorarioService,
  private val usuarios: ContratoUsuariosService,
  private val bloqueos: ContratoBloqueosService,
  private val listas: ContratoListasService,
  private val servicio: ContratoServicioService,
  private val bitacora: ContratoBitacora,
  private val eventos: ContratoEventosService,
) {
  // ── La noche (RF-037) ──────────────────────────────────────────────────────

  @PostMapping("/sesiones")
  fun abrirNoche(@CookieValue(COOKIE, required = false) token: String?, @RequestBody(required = false) cuerpo: Map<String, Any?>?): ResponseEntity<Void> {
    servicio.abrirNoche(admin(token), cuerpo)
    return sinContenido()
  }

  @PostMapping("/sesiones/actual/cierre")
  fun cerrarNoche(@CookieValue(COOKIE, required = false) token: String?): ResponseEntity<Void> {
    servicio.cerrarNoche(admin(token))
    return sinContenido()
  }

  // ── Calendario de eventos (RF-045) ─────────────────────────────────────────

  @GetMapping("/eventos")
  fun eventos(@CookieValue(COOKIE, required = false) token: String?) = ok(eventos.listar(admin(token)))

  @PostMapping("/eventos")
  fun crearEvento(@CookieValue(COOKIE, required = false) token: String?, @RequestBody(required = false) cuerpo: Map<String, Any?>?) =
    creado(eventos.crear(admin(token), cuerpo))

  @PatchMapping("/eventos/{id}")
  fun editarEvento(
    @CookieValue(COOKIE, required = false) token: String?,
    @PathVariable id: String,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = ok(eventos.editar(admin(token), id, cuerpo))

  @DeleteMapping("/eventos/{id}")
  fun eliminarEvento(@CookieValue(COOKIE, required = false) token: String?, @PathVariable id: String): ResponseEntity<Void> {
    eventos.eliminar(admin(token), id)
    return sinContenido()
  }

  // ── Servicio y mantenimiento (RF-036, RF-038, RF-039) ──────────────────────

  @PutMapping("/servicio")
  fun cambiarServicio(@CookieValue(COOKIE, required = false) token: String?, @RequestBody(required = false) cuerpo: Map<String, Any?>?): ResponseEntity<Void> {
    servicio.cambiar(acceso.exigir(token), cuerpo)
    return sinContenido()
  }

  @PostMapping("/servicio/mantenimiento")
  fun mantenimiento(@CookieValue(COOKIE, required = false) token: String?): ResponseEntity<Void> {
    servicio.mantenimiento(admin(token))
    return sinContenido()
  }

  // ── Bitácora (RF-044) ──────────────────────────────────────────────────────

  @GetMapping("/bitacora")
  fun bitacora(
    @CookieValue(COOKIE, required = false) token: String?,
    @RequestParam(required = false) desde: String?,
    @RequestParam(required = false) hasta: String?,
    @RequestParam(required = false) usuario: String?,
    @RequestParam(required = false) tipo: String?,
    @RequestParam(required = false) cursor: String?,
  ) = ok(bitacora.pagina(admin(token).barId, desde, hasta, usuario, tipo, cursor))

  // ── Horario (RF-036) ───────────────────────────────────────────────────────

  @GetMapping("/horario")
  fun horario(@CookieValue(COOKIE, required = false) token: String?) = ok(horario.leer(admin(token)))

  @PutMapping("/horario")
  fun guardarHorario(@CookieValue(COOKIE, required = false) token: String?, @RequestBody(required = false) cuerpo: Map<String, Any?>?) =
    ok(horario.guardar(admin(token), cuerpo))

  // ── Usuarios (RF-023 a RF-026) y clave propia ──────────────────────────────

  @GetMapping("/usuarios")
  fun usuarios(
    @CookieValue(COOKIE, required = false) token: String?,
    @RequestParam(required = false) q: String?,
    @RequestParam(required = false) rol: String?,
    @RequestParam(required = false) estado: String?,
  ) = ok(usuarios.listar(admin(token), q, rol, estado))

  @PostMapping("/usuarios")
  fun crearUsuario(@CookieValue(COOKIE, required = false) token: String?, @RequestBody(required = false) cuerpo: Map<String, Any?>?) =
    creado(usuarios.crear(admin(token), cuerpo))

  @PatchMapping("/usuarios/{id}")
  fun editarUsuario(
    @CookieValue(COOKIE, required = false) token: String?,
    @PathVariable id: String,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = ok(usuarios.editar(admin(token), id, cuerpo))

  @PostMapping("/usuarios/{id}/clave-temporal")
  fun claveTemporal(@CookieValue(COOKIE, required = false) token: String?, @PathVariable id: String) =
    ok(usuarios.restablecerClave(admin(token), id))

  @PostMapping("/usuarios/{id}/desactivacion")
  fun desactivar(@CookieValue(COOKIE, required = false) token: String?, @PathVariable id: String) =
    ok(usuarios.cambiarEstado(admin(token), id, activo = false))

  @PostMapping("/usuarios/{id}/reactivacion")
  fun reactivar(@CookieValue(COOKIE, required = false) token: String?, @PathVariable id: String) =
    ok(usuarios.cambiarEstado(admin(token), id, activo = true))

  /** Sin exigir la clave nueva: es justamente donde se cambia. */
  @PutMapping("/yo/clave")
  fun cambiarClave(@CookieValue(COOKIE, required = false) token: String?, @RequestBody(required = false) cuerpo: Map<String, Any?>?): ResponseEntity<Void> {
    usuarios.cambiarClavePropia(acceso.exigir(token, exigirClaveNueva = false), cuerpo)
    return sinContenido()
  }

  // ── Bloqueos (RF-042, RF-043) ──────────────────────────────────────────────

  @GetMapping("/bloqueos")
  fun bloqueos(@CookieValue(COOKIE, required = false) token: String?, @RequestParam(required = false) vigencia: String?) =
    ok(bloqueos.listar(acceso.exigir(token), vigencia))

  @PostMapping("/bloqueos")
  fun bloquear(@CookieValue(COOKIE, required = false) token: String?, @RequestBody(required = false) cuerpo: Map<String, Any?>?) =
    creado(bloqueos.bloquear(acceso.exigir(token), cuerpo))

  @PatchMapping("/bloqueos/{id}")
  fun cambiarBloqueo(
    @CookieValue(COOKIE, required = false) token: String?,
    @PathVariable id: String,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = ok(bloqueos.cambiarDuracion(admin(token), id, cuerpo))

  @DeleteMapping("/bloqueos/{id}")
  fun desbloquear(@CookieValue(COOKIE, required = false) token: String?, @PathVariable id: String): ResponseEntity<Void> {
    bloqueos.desbloquear(admin(token), id)
    return sinContenido()
  }

  // ── Listas de la casa (RF-027 a RF-033) y búsqueda (RF-031) ────────────────

  @GetMapping("/listas")
  fun listas(@CookieValue(COOKIE, required = false) token: String?, @RequestParam(required = false) q: String?) =
    ok(listas.listar(admin(token), q))

  @PostMapping("/listas")
  fun crearLista(@CookieValue(COOKIE, required = false) token: String?, @RequestBody(required = false) cuerpo: Map<String, Any?>?) =
    creado(listas.crear(admin(token), cuerpo))

  @GetMapping("/listas/{id}")
  fun lista(@CookieValue(COOKIE, required = false) token: String?, @PathVariable id: String) = ok(listas.detalle(admin(token), id))

  @PatchMapping("/listas/{id}")
  fun renombrarLista(
    @CookieValue(COOKIE, required = false) token: String?,
    @PathVariable id: String,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = ok(listas.renombrar(admin(token), id, cuerpo))

  @DeleteMapping("/listas/{id}")
  fun eliminarLista(@CookieValue(COOKIE, required = false) token: String?, @PathVariable id: String): ResponseEntity<Void> {
    listas.eliminar(admin(token), id)
    return sinContenido()
  }

  @PostMapping("/listas/{id}/canciones")
  fun agregarALista(
    @CookieValue(COOKIE, required = false) token: String?,
    @PathVariable id: String,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = creado(listas.agregar(admin(token), id, cuerpo))

  @DeleteMapping("/listas/{id}/canciones/{elemento}")
  fun quitarDeLista(@CookieValue(COOKIE, required = false) token: String?, @PathVariable id: String, @PathVariable elemento: String) =
    ok(listas.quitar(admin(token), id, elemento))

  @PutMapping("/listas/{id}/orden")
  fun ordenarLista(
    @CookieValue(COOKIE, required = false) token: String?,
    @PathVariable id: String,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = ok(listas.ordenar(admin(token), id, cuerpo))

  @PostMapping("/listas/{id}/activacion")
  fun activarLista(
    @CookieValue(COOKIE, required = false) token: String?,
    @PathVariable id: String,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = ok(listas.activar(admin(token), id, cuerpo))

  @GetMapping("/busqueda")
  fun buscar(@CookieValue(COOKIE, required = false) token: String?, @RequestParam(required = false) q: String?) =
    ok(listas.buscar(q.also { admin(token) }))

  private fun admin(token: String?) = acceso.exigir(token, rolMinimo = "administrador")

  private fun <T : Any> ok(cuerpo: T): ResponseEntity<T> =
    ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(cuerpo)

  private fun <T : Any> creado(cuerpo: T): ResponseEntity<T> =
    ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.CACHE_CONTROL, "no-store").body(cuerpo)

  private fun sinContenido(): ResponseEntity<Void> = ResponseEntity.noContent().build()

  private companion object {
    const val COOKIE = ContratoPanelSesiones.COOKIE
  }
}
