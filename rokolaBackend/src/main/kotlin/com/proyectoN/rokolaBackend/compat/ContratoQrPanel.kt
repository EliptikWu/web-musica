package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.QrCreacionService
import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.time.OffsetDateTime

/**
 * Lo que hace falta para ofrecer la app (RF-065): `ROKOLA_APK_URL` (https) y `ROKOLA_WEB_URL`, la
 * web que decide qué hacer al escanear. Un QR impreso con la dirección del backend se manda a ella.
 * Sin las dos, ningún bar puede elegir la descarga y todos siguen en la web.
 */
@Component
class DescargaApp(
  @Value("\${ROKOLA_APK_URL:}") apkCrudo: String,
  @Value("\${ROKOLA_WEB_URL:}") webCruda: String,
) {
  private val log = LoggerFactory.getLogger(DescargaApp::class.java)

  val webUrl: String? = webCruda.trim().trimEnd('/').takeIf { it.isNotEmpty() }?.let { valor ->
    val uri = runCatching { URI(valor) }.getOrNull()
    val valida = uri?.scheme in listOf("http", "https") && !uri?.host.isNullOrBlank() &&
      uri?.rawPath.isNullOrEmpty() && uri?.rawQuery == null && uri?.rawFragment == null
    if (valida) valor else null.also { log.warn("ROKOLA_WEB_URL debe ser http(s)://host[:puerto] sin ruta: se ignora") }
  }

  val url: String? = apkCrudo.trim().takeIf { it.isNotEmpty() && webUrl != null }?.let { valor ->
    val uri = runCatching { URI(valor) }.getOrNull()
    if (uri?.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawFragment == null) {
      valor
    } else {
      null.also { log.warn("ROKOLA_APK_URL no es una URL https válida: se ignora") }
    }
  }

  /** Lo que de verdad pasa al escanear: sin instalador publicado, siempre la web. */
  fun destinoEfectivo(guardado: String?): String = if (guardado == "app" && url != null) "app" else "web"
}

/** El QR del bar en el panel (RF-035, RF-065): solo el Administrador, solo su bar. */
@Service
class ContratoQrPanelService(
  private val jdbc: JdbcTemplate,
  private val tokens: TokenSeguro,
  private val qr: QrCreacionService,
  private val app: DescargaApp,
  private val bitacora: ContratoBitacora,
  private val panel: CambioPanelPublisher,
) {
  private val log = LoggerFactory.getLogger(javaClass)

  private data class Fila(val url: String?, val hash: String, val generado: OffsetDateTime)

  /**
   * Si se retira el instalador, los bares vuelven a la web de verdad: así no reaparece la descarga
   * sola el día que se publique otro, sin que su Administrador la haya elegido.
   */
  @EventListener(ApplicationReadyEvent::class)
  fun volverALaWebSinInstalador() {
    if (app.url != null) return
    runCatching { jdbc.update("UPDATE public.establecimiento SET destino_qr = 'web' WHERE destino_qr = 'app'") }
      .onSuccess { if (it > 0) log.info("Sin instalador publicado: {} bares vuelven a la web", it) }
      .onFailure { log.warn("No se pudo revisar el destino del QR: {}", it.javaClass.simpleName) }
  }

  /** Sin QR, o con uno cuya URL no se guardó, `url` es null: se puede elegir destino, no dibujarlo. */
  @Transactional(readOnly = true)
  fun leer(p: ContratoPanelAcceso.Personal): Map<String, Any?> {
    val destino = jdbc.queryForObject(
      "SELECT destino_qr FROM public.establecimiento WHERE id_establecimiento = ?", String::class.java, p.barId,
    )
    val fila = jdbc.query(
      """SELECT url_publica, token_hash, fecha_creacion FROM public.codigo_qr
         WHERE id_establecimiento = ? AND activo = TRUE AND fecha_revocacion IS NULL
         ORDER BY fecha_creacion DESC, id_qr DESC LIMIT 1""",
      { rs, _ -> Fila(rs.getString(1), rs.getString(2), rs.getObject(3, OffsetDateTime::class.java)) },
      p.barId,
    ).firstOrNull()
    // Solo se guarda la huella del token: un QR sin su URL no se puede volver a dibujar.
    val token = tokenDe(fila?.url)?.takeIf { tokens.esValido(it) && tokens.huellaQr(it) == fila?.hash }
    return mapOf(
      "url" to fila?.url?.takeIf { token != null },
      "token" to token,
      "generado_en" to fila?.generado?.takeIf { token != null },
      "destino" to app.destinoEfectivo(destino),
      "apk_disponible" to (app.url != null),
    )
  }

  @Transactional
  fun regenerar(p: ContratoPanelAcceso.Personal): Map<String, Any?> {
    try {
      qr.crearParaEstablecimiento(p.barId)
    } catch (e: ResponseStatusException) {
      if (e.statusCode == HttpStatus.SERVICE_UNAVAILABLE) ContratoCuerpo.conflicto("Falta configurar la dirección pública del QR")
      throw e
    }
    bitacora.anotar(p, "regenerar_qr", "establecimiento", p.barId, p.barNombre)
    panel.publicar(p.barSlug, setOf("establecimiento", "bitacora"))
    return leer(p)
  }

  @Transactional
  fun cambiarDestino(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("destino"))
    val destino = (datos["destino"] as? String)?.takeIf { it in DESTINOS } ?: ContratoCuerpo.invalido("destino: web o app")
    if (destino == "app" && app.url == null) ContratoCuerpo.conflicto("Falta publicar el instalador de la app")
    val cambiadas = jdbc.update(
      "UPDATE public.establecimiento SET destino_qr = ? WHERE id_establecimiento = ? AND destino_qr <> ?",
      destino, p.barId, destino,
    )
    if (cambiadas > 0) {
      val texto = if (destino == "app") "El QR ofrece descargar la app" else "El QR abre la web"
      bitacora.anotar(p, "editar_establecimiento", "establecimiento", p.barId, p.barNombre, texto)
      panel.publicar(p.barSlug, setOf("establecimiento", "bitacora"))
    }
    return leer(p)
  }

  private fun tokenDe(url: String?): String? {
    val uri = url?.let { runCatching { URI(it) }.getOrNull() } ?: return null
    if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank() || uri.rawQuery != null || uri.rawFragment != null) return null
    return uri.path?.takeIf { it.startsWith("/q/") }?.removePrefix("/q/")
  }

  private companion object {
    val DESTINOS = setOf("web", "app")
  }
}

@RestController
@RequestMapping("/api/panel/establecimiento/qr")
class ContratoQrPanelController(
  private val acceso: ContratoPanelAcceso,
  private val servicio: ContratoQrPanelService,
) {
  private fun admin(token: String?) = acceso.exigir(token, rolMinimo = "administrador")

  @GetMapping
  fun leer(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?) =
    sinCache(servicio.leer(admin(token)))

  @PostMapping("/regeneracion")
  fun regenerar(@CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?) =
    sinCache(servicio.regenerar(admin(token)))

  @PutMapping("/destino")
  fun cambiarDestino(
    @CookieValue(ContratoPanelSesiones.COOKIE, required = false) token: String?,
    @RequestBody(required = false) cuerpo: Map<String, Any?>?,
  ) = sinCache(servicio.cambiarDestino(admin(token), cuerpo))

  private fun sinCache(cuerpo: Map<String, Any?>): ResponseEntity<Map<String, Any?>> =
    ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(cuerpo)
}
