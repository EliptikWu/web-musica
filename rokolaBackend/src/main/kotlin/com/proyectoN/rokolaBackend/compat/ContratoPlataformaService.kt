package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.QrCreacionService
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Base64
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap

/**
 * Sesiones de la plataforma: otra cookie y otro almacén que las del panel, para que una sesión de
 * bar nunca valga aquí. SameSite=Strict porque estas rutas crean cuentas. Duran menos que las del
 * panel *(propuesto)*. Reiniciar el backend las cierra.
 */
@Component
class SesionesPlataforma(
  @Value("\${ROKOLA_PANEL_COOKIE_SEGURA:true}") private val cookieSegura: Boolean,
) {
  private data class Sesion(val superadminId: Long, val creada: Instant, @Volatile var ultimoUso: Instant)

  private val sesiones = ConcurrentHashMap<String, Sesion>()
  private val azar = SecureRandom()

  fun crear(superadminId: Long): String {
    val ahora = Instant.now()
    sesiones.entries.removeIf { vencida(it.value, ahora) }
    val token = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(azar::nextBytes))
    sesiones[huella(token)] = Sesion(superadminId, ahora, ahora)
    return token
  }

  fun vigente(token: String?): Long? {
    if (token.isNullOrBlank() || token.length > 100) return null
    val clave = huella(token)
    val sesion = sesiones[clave] ?: return null
    val ahora = Instant.now()
    if (vencida(sesion, ahora)) {
      sesiones.remove(clave)
      return null
    }
    sesion.ultimoUso = ahora
    return sesion.superadminId
  }

  fun cerrar(token: String?) {
    if (!token.isNullOrBlank() && token.length <= 100) sesiones.remove(huella(token))
  }

  /** Al cambiar la clave se cierran las demás sesiones de esa cuenta. */
  fun cerrarOtras(superadminId: Long, token: String?) {
    val propia = token?.takeIf { it.length <= 100 }?.let(::huella)
    sesiones.entries.removeIf { it.value.superadminId == superadminId && it.key != propia }
  }

  fun cookie(token: String): ResponseCookie = base(token).maxAge(DURACION_MAXIMA).build()
  fun cookieBorrada(): ResponseCookie = base("").maxAge(Duration.ZERO).build()

  private fun base(valor: String) =
    ResponseCookie.from(COOKIE, valor).httpOnly(true).secure(cookieSegura).sameSite("Strict").path("/")

  private fun vencida(s: Sesion, ahora: Instant) =
    s.creada.plus(DURACION_MAXIMA).isBefore(ahora) || s.ultimoUso.plus(INACTIVIDAD_MAXIMA).isBefore(ahora)

  private fun huella(token: String) =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)))

  companion object {
    const val COOKIE = "rockola_plataforma"
    val DURACION_MAXIMA: Duration = Duration.ofHours(8)
    val INACTIVIDAD_MAXIMA: Duration = Duration.ofHours(1)
  }
}

/**
 * La plataforma (RF-064, ADR-29): crear bares con su primer Administrador y crear cuentas en un
 * bar. No hay registro público: la primera cuenta sale de `ROCKOLA_SUPERADMIN_CORREO` y
 * `ROCKOLA_SUPERADMIN_CLAVE`, solo si la tabla está vacía, y debe cambiar la clave al entrar.
 */
@Service
class ContratoPlataformaService(
  private val jdbc: JdbcTemplate,
  private val sesiones: SesionesPlataforma,
  private val qr: QrCreacionService,
  private val bitacora: ContratoBitacora,
  private val sesionesPanel: ContratoPanelSesiones,
  private val panel: CambioPanelPublisher,
  @Value("\${ROCKOLA_SUPERADMIN_CORREO:}") private val correoInicial: String,
  @Value("\${ROCKOLA_SUPERADMIN_CLAVE:}") private val claveInicial: String,
) {
  data class Superadmin(val id: Long, val nombre: String, val correo: String, val debeCambiar: Boolean) {
    fun aContrato(): Map<String, Any?> =
      mapOf("id" to id.toString(), "nombre" to nombre, "correo" to correo, "debe_cambiar_clave" to debeCambiar)
  }

  private val log = LoggerFactory.getLogger(javaClass)
  private val bcrypt = BCryptPasswordEncoder()
  private val azar = SecureRandom()
  private val hashDeRelleno: String = bcrypt.encode("relleno-para-igualar-tiempos")!!

  @EventListener(ApplicationReadyEvent::class)
  fun crearPrimeraCuenta() {
    val correo = correoInicial.trim().lowercase()
    if (correo.isEmpty() || claveInicial.isEmpty()) return
    if (!FORMATO_CORREO.matches(correo) || claveInicial.length < LARGO_MINIMO_INICIAL || claveInicial.toByteArray().size > LARGO_MAXIMO) {
      log.warn("La cuenta inicial de la plataforma no se creó: revisa el correo y que la clave tenga de {} a {} caracteres", LARGO_MINIMO_INICIAL, LARGO_MAXIMO)
      return
    }
    // INSERT condicionado: dos instancias que arranquen a la vez no crean dos cuentas.
    val creadas = jdbc.update(
      """INSERT INTO public.superadmin (nombre, correo, password_hash, cambiar_password)
         SELECT 'Superadmin', ?, ?, TRUE WHERE NOT EXISTS (SELECT 1 FROM public.superadmin)
         ON CONFLICT DO NOTHING""",
      correo, bcrypt.encode(claveInicial),
    )
    if (creadas > 0) log.info("Se creó la cuenta inicial de la plataforma")
  }

  @Transactional(noRollbackFor = [ContratoApiException::class])
  fun entrar(cuerpo: Map<String, Any?>?): Pair<Superadmin, String> {
    val correo = (cuerpo?.get("correo") as? String)?.trim()?.lowercase()
    val clave = cuerpo?.get("clave") as? String
    if (correo == null || clave.isNullOrEmpty() || clave.length > 200 || !FORMATO_CORREO.matches(correo)) {
      ContratoCuerpo.invalido("Se requieren correo y clave")
    }
    val fila = jdbc.query(
      """SELECT id_superadmin, nombre, correo, cambiar_password, password_hash, bloqueado_hasta, activo
         FROM public.superadmin WHERE lower(correo) = ? FOR UPDATE""",
      { rs, _ ->
        Triple(
          Superadmin(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)),
          rs.getString(5),
          rs.getObject(6, OffsetDateTime::class.java) to rs.getBoolean(7),
        )
      },
      correo,
    ).firstOrNull()
    // Siempre se compara: el tiempo no delata si el correo existe.
    val coincide = coincide(clave, fila?.second ?: hashDeRelleno)
    if (fila == null || !fila.third.second) throw credencialesInvalidas()
    val (admin, _, estado) = fila
    val bloqueadaHasta = estado.first?.takeIf { it.isAfter(OffsetDateTime.now(ZoneOffset.UTC)) }
    // Durante el bloqueo todo intento responde igual, con clave buena o mala: no sirve para adivinarla
    // ni para saber qué correos existen.
    if (bloqueadaHasta != null) throw credencialesInvalidas()
    if (!coincide) {
      registrarFallo(admin.id)
      throw credencialesInvalidas()
    }
    jdbc.update("UPDATE public.superadmin SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id_superadmin = ?", admin.id)
    return admin to sesiones.crear(admin.id)
  }

  /** Quien llama, o 401. Con `exigirClaveNueva`, 403 mientras no cambie la clave inicial. */
  @Transactional(readOnly = true)
  fun exigir(token: String?, exigirClaveNueva: Boolean = true): Superadmin {
    val id = sesiones.vigente(token) ?: throw noAutenticado()
    val admin = jdbc.query(
      "SELECT id_superadmin, nombre, correo, cambiar_password FROM public.superadmin WHERE id_superadmin = ? AND activo = TRUE",
      { rs, _ -> Superadmin(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)) }, id,
    ).firstOrNull()
    if (admin == null) {
      sesiones.cerrar(token)
      throw noAutenticado()
    }
    if (exigirClaveNueva && admin.debeCambiar) {
      throw ContratoApiException(HttpStatus.FORBIDDEN, "debe_cambiar_clave", "Debes cambiar tu clave antes de seguir")
    }
    return admin
  }

  @Transactional
  fun cambiarClave(admin: Superadmin, token: String?, cuerpo: Map<String, Any?>?) {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("actual", "nueva"))
    val actual = datos["actual"] as? String ?: ContratoCuerpo.invalido("Falta la clave actual")
    val nueva = datos["nueva"] as? String ?: ContratoCuerpo.invalido("Falta la clave nueva")
    if (actual.isEmpty() || actual.length > 200) ContratoCuerpo.invalido("Clave actual inválida")
    if (nueva.length < LARGO_MINIMO_INICIAL || nueva.toByteArray(Charsets.UTF_8).size > LARGO_MAXIMO) {
      ContratoCuerpo.invalido("La clave nueva debe tener al menos $LARGO_MINIMO_INICIAL caracteres y no ser tan larga")
    }
    val hash = jdbc.query(
      "SELECT password_hash FROM public.superadmin WHERE id_superadmin = ? FOR UPDATE", { rs, _ -> rs.getString(1) }, admin.id,
    ).firstOrNull() ?: throw noAutenticado()
    if (!coincide(actual, hash)) throw ContratoApiException(HttpStatus.UNAUTHORIZED, "credenciales_invalidas", "La clave actual no coincide")
    if (nueva == actual) ContratoCuerpo.invalido("La clave nueva tiene que ser distinta")
    jdbc.update("UPDATE public.superadmin SET password_hash = ?, cambiar_password = FALSE WHERE id_superadmin = ?", bcrypt.encode(nueva), admin.id)
    sesiones.cerrarOtras(admin.id, token)
  }

  @Transactional(readOnly = true)
  fun bares(): Map<String, Any?> {
    val cuentas = jdbc.query(
      """SELECT u.id_establecimiento, u.id_usuario, u.nombre, u.correo, lower(r.nombre), u.activo, u.cambiar_password
         FROM public.usuario u JOIN public.rol r ON r.id_rol = u.id_rol
         WHERE lower(r.nombre) IN ('operador', 'administrador')
         ORDER BY lower(r.nombre), lower(u.nombre)""",
      { rs, _ -> rs.getLong(1) to cuenta(rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6), rs.getBoolean(7)) },
    ).groupBy({ it.first }, { it.second })
    val bares = jdbc.query(
      "SELECT id_establecimiento, slug, nombre, zona_horaria, activo FROM public.establecimiento ORDER BY lower(nombre)",
      { rs, _ -> bar(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5), cuentas[rs.getLong(1)].orEmpty()) },
    )
    return mapOf("bares" to bares)
  }

  /** Todo o nada: el bar, su configuración, su QR y su primer Administrador. */
  @Transactional
  fun crearBar(cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("nombre", "slug", "zona_horaria", "administrador"))
    val nombre = ContratoCuerpo.texto(datos, "nombre", max = 150, obligatorio = true)!!
    val slug = ContratoCuerpo.texto(datos, "slug", max = 100, obligatorio = true)!!
    if (!FORMATO_SLUG.matches(slug)) ContratoCuerpo.invalido("slug en minúsculas con guiones")
    val zona = ContratoCuerpo.texto(datos, "zona_horaria", max = 60, obligatorio = true)!!
    if (zona !in ZoneId.getAvailableZoneIds()) ContratoCuerpo.invalido("Zona horaria desconocida")
    @Suppress("UNCHECKED_CAST")
    val admin = datos["administrador"] as? Map<String, Any?> ?: ContratoCuerpo.invalido("Falta administrador")
    ContratoCuerpo.exigirCampos(admin, setOf("nombre", "correo"))
    val nombreAdmin = ContratoCuerpo.texto(admin, "nombre", max = 120, obligatorio = true)!!
    val correoAdmin = correo(admin)

    val idBar = try {
      jdbc.queryForObject(
        "INSERT INTO public.establecimiento (nombre, slug, zona_horaria) VALUES (?, ?, ?) RETURNING id_establecimiento",
        Long::class.javaObjectType, nombre, slug, zona,
      )!!
    } catch (_: DuplicateKeyException) {
      ContratoCuerpo.conflicto("Ya hay un bar con esa dirección")
    }
    jdbc.update(
      """INSERT INTO public.configuracion_servicio
           (id_establecimiento, estado_manual, max_solicitudes_dispositivo, duracion_max_seg, minutos_bloqueo_repeticion)
         VALUES (?, 'disponible', 3, 600, 60)""",
      idBar,
    )
    try {
      qr.crearParaEstablecimiento(idBar)
    } catch (e: ResponseStatusException) {
      if (e.statusCode != HttpStatus.SERVICE_UNAVAILABLE) throw e
      throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "Falta configurar la dirección pública del QR")
    }
    val (fila, clave) = insertarCuenta(idBar, nombreAdmin, correoAdmin, "administrador")
    return mapOf("bar" to bar(idBar, slug, nombre, zona, true, listOf(fila)), "clave_temporal" to clave)
  }

  @Transactional
  fun crearCuenta(idTexto: String, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val idBar = ContratoCuerpo.id(idTexto, "Ese bar no existe")
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("nombre", "correo", "rol"))
    val nombre = ContratoCuerpo.texto(datos, "nombre", max = 120, obligatorio = true)!!
    val correo = correo(datos)
    val rol = ContratoCuerpo.opcion(datos["rol"] as? String, "rol", ContratoUsuariosService.ROLES) { it }
    // Serializa con otras altas del mismo bar: el correo no se repite dentro de él.
    val (slug, activo) = jdbc.query(
      "SELECT slug, activo FROM public.establecimiento WHERE id_establecimiento = ? FOR UPDATE",
      { rs, _ -> rs.getString(1) to rs.getBoolean(2) }, idBar,
    ).firstOrNull() ?: ContratoCuerpo.noEncontrado("Ese bar no existe")
    // El panel no deja entrar a las cuentas de un bar inactivo: la clave no serviría.
    if (!activo) ContratoCuerpo.conflicto("Ese bar está inactivo")
    val usado = jdbc.queryForObject(
      "SELECT EXISTS(SELECT 1 FROM public.usuario WHERE id_establecimiento = ? AND lower(correo) = ?)",
      Boolean::class.javaObjectType, idBar, correo,
    )
    if (usado == true) ContratoCuerpo.conflicto("Ya hay una cuenta con ese correo en este bar")
    val (fila, clave) = insertarCuenta(idBar, nombre, correo, rol)
    panel.publicar(slug, setOf("usuarios", "bitacora"))
    return mapOf("cuenta" to fila, "clave_temporal" to clave)
  }

  /** Para cuando la clave se perdió: cierra las sesiones de esa cuenta y obliga a cambiarla. */
  @Transactional
  fun nuevaClave(idTexto: String): Map<String, Any?> {
    val id = ContratoCuerpo.id(idTexto, "Esa cuenta no existe")
    val (idBar, slug, nombre) = jdbc.query(
      """SELECT u.id_establecimiento, e.slug, u.nombre FROM public.usuario u
         JOIN public.establecimiento e ON e.id_establecimiento = u.id_establecimiento
         JOIN public.rol r ON r.id_rol = u.id_rol
         WHERE u.id_usuario = ? AND lower(r.nombre) IN ('operador', 'administrador') FOR UPDATE OF u""",
      { rs, _ -> Triple(rs.getLong(1), rs.getString(2), rs.getString(3)) }, id,
    ).firstOrNull() ?: ContratoCuerpo.noEncontrado("Esa cuenta no existe")
    val clave = ContratoUsuariosService.generarClaveTemporal(azar)
    jdbc.update(
      "UPDATE public.usuario SET password_hash = ?, cambiar_password = TRUE, intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id_usuario = ?",
      bcrypt.encode(clave), id,
    )
    sesionesPanel.cerrarDeUsuario(id)
    bitacora.anotar(null, "restablecer_clave", "usuario", id, nombre, "Desde la plataforma", idBar)
    panel.publicar(slug, setOf("usuarios", "bitacora"))
    return mapOf("clave_temporal" to clave)
  }

  private fun insertarCuenta(idBar: Long, nombre: String, correo: String, rol: String): Pair<Map<String, Any?>, String> {
    val clave = ContratoUsuariosService.generarClaveTemporal(azar)
    val id = jdbc.queryForObject(
      """INSERT INTO public.usuario (id_establecimiento, id_rol, nombre, correo, password_hash, activo, cambiar_password)
         VALUES (?, (SELECT id_rol FROM public.rol WHERE lower(nombre) = ?), ?, ?, ?, TRUE, TRUE) RETURNING id_usuario""",
      Long::class.javaObjectType, idBar, rol, nombre, correo, bcrypt.encode(clave),
    )!!
    bitacora.anotar(null, "crear_usuario", "usuario", id, nombre, "Creada desde la plataforma", idBar)
    return cuenta(id, nombre, correo, rol, activo = true, debeCambiar = true) to clave
  }

  private fun correo(datos: Map<String, Any?>): String =
    ContratoCuerpo.texto(datos, "correo", max = 254, obligatorio = true)!!.lowercase().also {
      if (!FORMATO_CORREO.matches(it)) ContratoCuerpo.invalido("Correo inválido")
    }

  private fun cuenta(id: Long, nombre: String, correo: String, rol: String, activo: Boolean, debeCambiar: Boolean): Map<String, Any?> = mapOf(
    "id" to id.toString(), "nombre" to nombre, "correo" to correo, "rol" to rol,
    "estado" to if (activo) "activo" else "inactivo", "debe_cambiar_clave" to debeCambiar,
  )

  private fun bar(id: Long, slug: String, nombre: String, zona: String, activo: Boolean, cuentas: List<Map<String, Any?>>): Map<String, Any?> = mapOf(
    "id" to id.toString(), "slug" to slug, "nombre" to nombre, "zona_horaria" to zona, "activo" to activo, "cuentas" to cuentas,
  )

  private fun coincide(clave: String, hash: String) = try {
    bcrypt.matches(clave, hash)
  } catch (_: IllegalArgumentException) {
    false
  }

  /** Cuenta el fallo; al llegar al tope bloquea la cuenta unos minutos. */
  private fun registrarFallo(id: Long) {
    jdbc.update(
      """UPDATE public.superadmin SET
           intentos_fallidos = CASE WHEN intentos_fallidos + 1 >= ? THEN 0 ELSE intentos_fallidos + 1 END,
           bloqueado_hasta = CASE WHEN intentos_fallidos + 1 >= ? THEN CURRENT_TIMESTAMP + (? * INTERVAL '1 minute') ELSE NULL END
         WHERE id_superadmin = ?""",
      ContratoPanelAcceso.INTENTOS_MAXIMOS, ContratoPanelAcceso.INTENTOS_MAXIMOS, ContratoPanelAcceso.MINUTOS_BLOQUEO, id,
    )
  }

  private fun credencialesInvalidas() = ContratoApiException(HttpStatus.UNAUTHORIZED, "credenciales_invalidas", "Correo o clave incorrectos")
  private fun noAutenticado() = ContratoApiException(HttpStatus.UNAUTHORIZED, "no_autenticado", "Inicia sesión en la plataforma")

  private companion object {
    /** Más larga que la del panel *(propuesto)*: esta cuenta crea bares y accesos. */
    const val LARGO_MINIMO_INICIAL = 12
    const val LARGO_MAXIMO = 72
    /** La misma regla de `z.email()` del contrato: lo que aquí se acepta, la interfaz lo puede leer. */
    val FORMATO_CORREO = Regex("^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9_'+\\-.]*[A-Za-z0-9_+-]@(?:[A-Za-z0-9][A-Za-z0-9\\-]*\\.)+[A-Za-z]{2,}$")
    val FORMATO_SLUG = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
  }
}
