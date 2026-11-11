package com.proyectoN.rokolaBackend.compat

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap

/**
 * Sesiones del panel (doc 04, «Panel (cookie de sesión)»).
 * El navegador recibe un token aleatorio en la cookie httpOnly `rockola_panel`; aquí solo se
 * guarda su SHA-256. PMV de una instancia: reiniciar el backend cierra todas las sesiones.
 * Duraciones *(propuestas)*: el doc 04 las tiene como pendiente del backend (punto 6).
 */
@Component
class ContratoPanelSesiones(
  @Value("\${ROKOLA_PANEL_COOKIE_SEGURA:true}") private val cookieSegura: Boolean,
) {
  data class SesionPanel(val usuarioId: Long, val creada: Instant, @Volatile var ultimoUso: Instant)

  private val sesiones = ConcurrentHashMap<String, SesionPanel>()
  private val azar = SecureRandom()

  fun crear(usuarioId: Long): String {
    limpiar()
    val bytes = ByteArray(32).also(azar::nextBytes)
    val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    val ahora = Instant.now()
    sesiones[huella(token)] = SesionPanel(usuarioId, ahora, ahora)
    return token
  }

  /** La sesión vigente del token, o null si no existe o venció. Renueva la inactividad. */
  fun vigente(token: String?): SesionPanel? {
    if (token.isNullOrBlank() || token.length > 100) return null
    val clave = huella(token)
    val sesion = sesiones[clave] ?: return null
    val ahora = Instant.now()
    if (vencida(sesion, ahora)) {
      sesiones.remove(clave)
      return null
    }
    sesion.ultimoUso = ahora
    return sesion
  }

  fun cerrar(token: String?) {
    if (!token.isNullOrBlank() && token.length <= 100) sesiones.remove(huella(token))
  }

  fun cookie(token: String): ResponseCookie = base(token).maxAge(DURACION_MAXIMA).build()

  fun cookieBorrada(): ResponseCookie = base("").maxAge(Duration.ZERO).build()

  private fun base(valor: String) = ResponseCookie.from(COOKIE, valor)
    .httpOnly(true)
    .secure(cookieSegura)
    .sameSite("Lax") // Lax y no Strict: abrir /panel desde un enlace debe llevar la sesión.
    .path("/")

  private fun vencida(s: SesionPanel, ahora: Instant) =
    s.creada.plus(DURACION_MAXIMA).isBefore(ahora) || s.ultimoUso.plus(INACTIVIDAD_MAXIMA).isBefore(ahora)

  private fun limpiar() {
    val ahora = Instant.now()
    sesiones.entries.removeIf { vencida(it.value, ahora) }
  }

  private fun huella(token: String): String = HexFormat.of().formatHex(
    MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)),
  )

  companion object {
    const val COOKIE = "rockola_panel"
    val DURACION_MAXIMA: Duration = Duration.ofHours(12)
    val INACTIVIDAD_MAXIMA: Duration = Duration.ofHours(4)
  }
}
