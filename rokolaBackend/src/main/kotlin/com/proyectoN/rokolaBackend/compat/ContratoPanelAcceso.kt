package com.proyectoN.rokolaBackend.compat

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Entrar al panel y saber quién llama (RF-021, RF-022). La matriz rol × acción es la de
 * `frontend/web/lib/permisos.ts` y el doc 04: aquí se decide, la interfaz solo oculta.
 */
@Service
class ContratoPanelAcceso(
  private val jdbc: JdbcTemplate,
  private val sesiones: ContratoPanelSesiones,
) {
  /** Quien llama al panel, ya autenticado. */
  data class Personal(
    val id: Long,
    val nombre: String,
    val correo: String,
    val rol: String, // operador | administrador
    val activo: Boolean,
    val debeCambiarClave: Boolean,
    val barId: Long,
    val barSlug: String,
    val barNombre: String,
    val barZona: String,
  )

  private data class Credencial(val personal: Personal, val hash: String, val bloqueadoHasta: OffsetDateTime?)

  private val bcrypt = BCryptPasswordEncoder()
  private val formatoCorreo = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

  /** Comparar contra algo aunque el correo no exista: el tiempo no delata qué correos hay. */
  private val hashDeRelleno: String = bcrypt.encode("relleno-para-igualar-tiempos")!!

  @Transactional(noRollbackFor = [ContratoApiException::class])
  fun entrar(correo: String?, clave: String?): Pair<Personal, String> {
    if (correo == null || clave == null || clave.isEmpty() || clave.length > 200 || !formatoCorreo.matches(correo)) {
      throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requieren correo y clave")
    }
    // El mismo correo puede existir en dos bares (único por establecimiento): vale el que coincida.
    val candidatos = credenciales(correo.trim().lowercase())
    if (candidatos.isEmpty()) {
      bcrypt.matches(clave, hashDeRelleno)
      throw credencialesInvalidas()
    }
    // Primero bcrypt y después el bloqueo: así una cuenta bloqueada tarda lo mismo que cualquier otra.
    val aciertos = candidatos.filter { coincide(clave, it.hash) }
    val ahora = OffsetDateTime.now(ZoneOffset.UTC)
    val bloqueada = { c: Credencial -> c.bloqueadoHasta?.isAfter(ahora) == true }
    val acierto = aciertos.firstOrNull { it.personal.activo && !bloqueada(it) }
    if (acierto == null) {
      aciertos.firstOrNull { it.personal.activo && bloqueada(it) }?.let { throw cuentaBloqueada(it.bloqueadoHasta!!) }
      // Sin acierto no se sabe de qué bar era el intento: cuenta para las cuentas con ese correo.
      candidatos.filter { !bloqueada(it) }.forEach { registrarFallo(it.personal.id) }
      candidatos.firstNotNullOfOrNull { bloqueadoHasta(it.personal.id) }?.let { throw cuentaBloqueada(it) }
      throw credencialesInvalidas()
    }
    jdbc.update(
      "UPDATE public.usuario SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id_usuario = ?",
      acierto.personal.id,
    )
    return acierto.personal to sesiones.crear(acierto.personal.id)
  }

  /** El personal de la cookie, o 401. Con `exigirClaveNueva`, 403 si aún debe cambiar la clave. */
  @Transactional(readOnly = true)
  fun exigir(token: String?, rolMinimo: String = "operador", exigirClaveNueva: Boolean = true): Personal {
    val sesion = sesiones.vigente(token) ?: throw noAutenticado()
    val personal = porId(sesion.usuarioId)
    if (personal == null || !personal.activo) {
      sesiones.cerrar(token)
      throw noAutenticado()
    }
    if (exigirClaveNueva && personal.debeCambiarClave) {
      throw ContratoApiException(HttpStatus.FORBIDDEN, "debe_cambiar_clave", "Debes cambiar tu clave antes de seguir")
    }
    if (rolMinimo == "administrador" && personal.rol != "administrador") {
      throw ContratoApiException(HttpStatus.FORBIDDEN, "sin_permiso", "Tu rol no permite esta acción")
    }
    return personal
  }

  fun yo(p: Personal): Map<String, Any?> = mapOf(
    "usuario" to mapOf(
      "id" to p.id.toString(),
      "nombre" to p.nombre,
      "correo" to p.correo,
      "rol" to p.rol,
      "estado" to if (p.activo) "activo" else "inactivo",
      "debe_cambiar_clave" to p.debeCambiarClave,
      "ultimo_acceso" to null, // La tabla usuario no guarda el último acceso.
    ),
    "establecimiento" to mapOf("slug" to p.barSlug, "nombre" to p.barNombre, "zona_horaria" to p.barZona),
  )

  private fun coincide(clave: String, hash: String): Boolean = try {
    bcrypt.matches(clave, hash)
  } catch (_: IllegalArgumentException) {
    false // Hash con formato que no es bcrypt: nunca coincide.
  }

  private fun registrarFallo(idUsuario: Long) {
    // Si un bloqueo ya venció, la cuenta empieza de cero: vuelve a tener sus intentos completos.
    jdbc.update(
      """UPDATE public.usuario u
         SET intentos_fallidos = n.intentos,
             bloqueado_hasta = CASE WHEN n.intentos >= ? THEN CURRENT_TIMESTAMP + (? * INTERVAL '1 minute') ELSE NULL END
         FROM (SELECT id_usuario,
                      CASE WHEN bloqueado_hasta IS NOT NULL AND bloqueado_hasta <= CURRENT_TIMESTAMP THEN 1
                           ELSE LEAST(intentos_fallidos + 1, 32000) END AS intentos
               FROM public.usuario WHERE id_usuario = ?) n
         WHERE u.id_usuario = n.id_usuario""",
      INTENTOS_MAXIMOS, MINUTOS_BLOQUEO, idUsuario,
    )
  }

  private fun bloqueadoHasta(idUsuario: Long): OffsetDateTime? = jdbc.query(
    "SELECT bloqueado_hasta FROM public.usuario WHERE id_usuario = ? AND bloqueado_hasta > CURRENT_TIMESTAMP",
    { rs, _ -> rs.getObject(1, OffsetDateTime::class.java) }, idUsuario,
  ).firstOrNull()

  private val selectPersonal = """
    SELECT u.id_usuario, u.nombre, u.correo, lower(r.nombre) AS rol, u.activo, u.cambiar_password,
           u.password_hash, u.bloqueado_hasta,
           e.id_establecimiento, e.slug, e.nombre AS bar_nombre, e.zona_horaria
    FROM public.usuario u
    JOIN public.rol r ON r.id_rol = u.id_rol
    JOIN public.establecimiento e ON e.id_establecimiento = u.id_establecimiento
    WHERE e.activo = TRUE AND lower(r.nombre) IN ('operador', 'administrador')
  """.trimIndent()

  private fun credenciales(correo: String): List<Credencial> = jdbc.query(
    "$selectPersonal AND lower(u.correo) = ?",
    { rs, _ ->
      Credencial(
        personal = filaPersonal(rs),
        hash = rs.getString("password_hash"),
        bloqueadoHasta = rs.getObject("bloqueado_hasta", OffsetDateTime::class.java),
      )
    },
    correo,
  )

  private fun porId(id: Long): Personal? =
    jdbc.query("$selectPersonal AND u.id_usuario = ?", { rs, _ -> filaPersonal(rs) }, id).firstOrNull()

  private fun filaPersonal(rs: java.sql.ResultSet) = Personal(
    id = rs.getLong("id_usuario"),
    nombre = rs.getString("nombre"),
    correo = rs.getString("correo"),
    rol = rs.getString("rol"),
    activo = rs.getBoolean("activo"),
    debeCambiarClave = rs.getBoolean("cambiar_password"),
    barId = rs.getLong("id_establecimiento"),
    barSlug = rs.getString("slug"),
    barNombre = rs.getString("bar_nombre"),
    barZona = rs.getString("zona_horaria"),
  )

  private fun credencialesInvalidas() =
    ContratoApiException(HttpStatus.UNAUTHORIZED, "credenciales_invalidas", "Correo o clave incorrectos")

  private fun cuentaBloqueada(hasta: OffsetDateTime) = ContratoApiException(
    HttpStatus.LOCKED, "cuenta_bloqueada", "Demasiados intentos fallidos",
    mapOf("reintentar_en" to hasta.withOffsetSameInstant(ZoneOffset.UTC).toString()),
  )

  private fun noAutenticado() =
    ContratoApiException(HttpStatus.UNAUTHORIZED, "no_autenticado", "Inicia sesión en el panel")

  companion object {
    /** *(Propuesto)*: el doc 04 tiene estos valores como pendiente del backend (punto 6). */
    const val INTENTOS_MAXIMOS = 5
    const val MINUTOS_BLOQUEO = 15
  }
}
