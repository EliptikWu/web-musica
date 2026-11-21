package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Tablero analítico (RF-049 a RF-054, RF-063): solo el Administrador, solo su bar. */
@RestController
@RequestMapping("/api/panel/analitica")
class ContratoAnaliticaController(
  private val acceso: ContratoPanelAcceso,
  private val analitica: ContratoAnaliticaService,
) {
  private fun admin(token: String?) = acceso.exigir(token, rolMinimo = "administrador")

  @GetMapping("/noches")
  fun noches(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?) =
    sinCache(analitica.noches(admin(token)))

  @GetMapping("/resumen")
  fun resumen(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestParam(required = false) noche: String?,
    @RequestParam(required = false) desde: String?,
    @RequestParam(required = false) hasta: String?,
    @RequestParam("dia_semana", required = false) dia: String?,
  ) = sinCache(analitica.resumen(admin(token), ContratoAnaliticaService.Filtro(noche, desde, hasta, dia)))

  @GetMapping("/participacion")
  fun participacion(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestParam(required = false) noche: String?,
    @RequestParam(required = false) desde: String?,
    @RequestParam(required = false) hasta: String?,
    @RequestParam("dia_semana", required = false) dia: String?,
  ) = sinCache(analitica.participacion(admin(token), ContratoAnaliticaService.Filtro(noche, desde, hasta, dia)))

  @GetMapping("/diagnostico")
  fun diagnostico(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestParam(required = false) noche: String?,
    @RequestParam(required = false) desde: String?,
    @RequestParam(required = false) hasta: String?,
    @RequestParam("dia_semana", required = false) dia: String?,
  ) = sinCache(analitica.diagnostico(admin(token), ContratoAnaliticaService.Filtro(noche, desde, hasta, dia)))

  @GetMapping("/umbrales")
  fun umbrales(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?) =
    sinCache(analitica.umbrales(admin(token).barId))

  @PutMapping("/umbrales")
  fun guardarUmbrales(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = sinCache(analitica.guardarUmbrales(admin(token), cuerpo))

  @GetMapping("/comparacion")
  fun comparacion(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestParam(required = false) agrupar: String?,
    @RequestParam(required = false) noches: String?,
    @RequestParam(required = false) desde: String?,
    @RequestParam(required = false) hasta: String?,
  ) = sinCache(analitica.comparar(admin(token), agrupar, noches, desde, hasta))

  @GetMapping("/recomendaciones")
  fun recomendaciones(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestParam(required = false) noche: String?,
    @RequestParam(required = false) desde: String?,
    @RequestParam(required = false) hasta: String?,
    @RequestParam("dia_semana", required = false) dia: String?,
  ) = sinCache(analitica.recomendaciones(admin(token), ContratoAnaliticaService.Filtro(noche, desde, hasta, dia)))

  @GetMapping("/cubo")
  fun cubo(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestParam(required = false) eje: String?,
  ) = sinCache(analitica.cubo(admin(token), eje))

  private fun <T : Any> sinCache(cuerpo: T): ResponseEntity<T> =
    ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(cuerpo)
}
