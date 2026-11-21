package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

/**
 * El personal del bar (RF-023 a RF-026) y el cambio de la clave propia: solo el Administrador
 * gestiona usuarios, y solo los de su bar (el id del bar sale de la sesión). Las reglas son las de
 * la simulación de la web (`frontend/web/simulacion/gestion.ts`): el correo no se repite en el
 * bar, el bar nunca se queda sin un Administrador activo y nadie se desactiva a sí mismo.
 *
 * La clave temporal se muestra una sola vez y en la base solo queda su hash bcrypt; obliga a
 * cambiarla al entrar (`cambiar_password`). Hecho por el equipo de frontend (2026-11-21, D14).
 */
@Service
class ContratoUsuariosService(
  private val jdbc: JdbcTemplate,
  private val sesiones: ContratoPanelSesiones,
  private val bitacora: ContratoBitacora,
  private val panel: CambioPanelPublisher,
) {
  private data class Fila(val id: Long, val nombre: String, val correo: String, val rol: String, val activo: Boolean, val debeCambiar: Boolean) {
    fun aContrato(): Map<String, Any?> = mapOf(
      "id" to id.toString(), "nombre" to nombre, "correo" to correo, "rol" to rol,
      "estado" to if (activo) "activo" else "inactivo", "debe_cambiar_clave" to debeCambiar,
      "ultimo_acceso" to null, // La tabla usuario no guarda el último acceso.
    )
  }

  private val bcrypt = BCryptPasswordEncoder()
  private val azar = SecureRandom()

  fun listar(p: ContratoPanelAcceso.Personal, q: String?, rol: String?, estado: String?): Map<String, Any?> {
    if (rol != null && rol !in ROLES) ContratoCuerpo.invalido("rol: operador o administrador")
    if (estado != null && estado !in setOf("activo", "inactivo")) ContratoCuerpo.invalido("estado: activo o inactivo")
    val texto = q?.trim()?.lowercase()?.take(120)?.takeIf { it.isNotEmpty() }
    val usuarios = filas(p.barId)
      .filter { u -> texto == null || "${u.nombre} ${u.correo}".lowercase().contains(texto) }
      .filter { u -> rol == null || u.rol == rol }
      .filter { u -> estado == null || u.activo == (estado == "activo") }
      .sortedBy { it.nombre.lowercase() }
    return mapOf("usuarios" to usuarios.map(Fila::aContrato))
  }

  @Transactional
  fun crear(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("nombre", "correo", "rol"))
    val nombre = ContratoCuerpo.texto(datos, "nombre", max = 120, obligatorio = true)!!
    val correo = correo(datos, obligatorio = true)!!
    val rol = rol(datos, obligatorio = true)!!
    bloquearBar(p.barId)
    if (correoUsado(p.barId, correo, excepto = null)) ContratoCuerpo.conflicto("Ya hay un usuario con ese correo")
    val clave = claveTemporal()
    val id = jdbc.queryForObject(
      """INSERT INTO public.usuario (id_establecimiento, id_rol, nombre, correo, password_hash, activo, cambiar_password)
         VALUES (?, ${idRol()}, ?, ?, ?, TRUE, TRUE) RETURNING id_usuario""",
      Long::class.javaObjectType, p.barId, rol, nombre, correo, bcrypt.encode(clave),
    )!!
    bitacora.anotar(p, "crear_usuario", "usuario", id, nombre)
    panel.publicar(p.barSlug, setOf("usuarios", "bitacora"))
    return mapOf("usuario" to fila(p.barId, id)!!.aContrato(), "clave_temporal" to clave)
  }

  @Transactional
  fun editar(p: ContratoPanelAcceso.Personal, idTexto: String, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("nombre", "correo", "rol"))
    val nombre = ContratoCuerpo.texto(datos, "nombre", max = 120)
    val correo = correo(datos, obligatorio = false)
    val rol = rol(datos, obligatorio = false)
    bloquearBar(p.barId)
    val u = existente(p, idTexto)
    if (correo != null && correoUsado(p.barId, correo, excepto = u.id)) ContratoCuerpo.conflicto("Ya hay un usuario con ese correo")
    // RF-025: el bar no se queda sin Administrador.
    if (rol == "operador" && u.rol == "administrador" && u.activo && administradoresActivos(p.barId) <= 1) {
      ContratoCuerpo.conflicto("Es el último Administrador")
    }
    if (nombre != null || correo != null || rol != null) {
      jdbc.update(
        """UPDATE public.usuario SET nombre = COALESCE(?, nombre), correo = COALESCE(?, correo),
                  id_rol = COALESCE(${idRol()}, id_rol)
           WHERE id_usuario = ? AND id_establecimiento = ?""",
        nombre, correo, rol, u.id, p.barId,
      )
      bitacora.anotar(p, "editar_usuario", "usuario", u.id, nombre ?: u.nombre)
      panel.publicar(p.barSlug, setOf("usuarios", "bitacora") + if (u.id == p.id) setOf("yo") else emptySet())
    }
    return fila(p.barId, u.id)!!.aContrato()
  }

  @Transactional
  fun restablecerClave(p: ContratoPanelAcceso.Personal, idTexto: String): Map<String, Any?> {
    val u = existente(p, idTexto)
    val clave = claveTemporal()
    jdbc.update(
      """UPDATE public.usuario SET password_hash = ?, cambiar_password = TRUE, intentos_fallidos = 0, bloqueado_hasta = NULL
         WHERE id_usuario = ? AND id_establecimiento = ?""",
      bcrypt.encode(clave), u.id, p.barId,
    )
    // Sus sesiones abiertas se cierran: tiene que entrar con la temporal.
    sesiones.cerrarDeUsuario(u.id)
    bitacora.anotar(p, "restablecer_clave", "usuario", u.id, u.nombre)
    panel.publicar(p.barSlug, setOf("usuarios", "bitacora"))
    return mapOf("clave_temporal" to clave)
  }

  @Transactional
  fun cambiarEstado(p: ContratoPanelAcceso.Personal, idTexto: String, activo: Boolean): Map<String, Any?> {
    bloquearBar(p.barId)
    val u = existente(p, idTexto)
    if (!activo && u.id == p.id) ContratoCuerpo.conflicto("No puedes desactivarte a ti mismo")
    if (!activo && u.activo && u.rol == "administrador" && administradoresActivos(p.barId) <= 1) {
      ContratoCuerpo.conflicto("Es el último Administrador")
    }
    if (u.activo != activo) {
      jdbc.update("UPDATE public.usuario SET activo = ? WHERE id_usuario = ? AND id_establecimiento = ?", activo, u.id, p.barId)
      if (!activo) sesiones.cerrarDeUsuario(u.id)
      bitacora.anotar(p, if (activo) "reactivar_usuario" else "desactivar_usuario", "usuario", u.id, u.nombre)
      panel.publicar(p.barSlug, setOf("usuarios", "bitacora"))
    }
    return fila(p.barId, u.id)!!.aContrato()
  }

  /** `PUT /panel/yo/clave` (RF-023, RF-025): la clave propia; quita la obligación de cambiarla. */
  @Transactional
  fun cambiarClavePropia(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("actual", "nueva"))
    val actual = datos["actual"] as? String ?: ContratoCuerpo.invalido("Falta la clave actual")
    val nueva = datos["nueva"] as? String ?: ContratoCuerpo.invalido("Falta la clave nueva")
    if (actual.isEmpty() || actual.length > 200) ContratoCuerpo.invalido("Clave actual inválida")
    // bcrypt solo usa 72 bytes (y Spring rechaza más): con tildes o emojis, un carácter ocupa varios.
    if (nueva.length < LARGO_MINIMO || nueva.toByteArray(Charsets.UTF_8).size > LARGO_MAXIMO) {
      ContratoCuerpo.invalido("La clave nueva debe tener al menos $LARGO_MINIMO caracteres y no ser tan larga")
    }
    val hash = jdbc.query(
      "SELECT password_hash FROM public.usuario WHERE id_usuario = ? FOR UPDATE", { rs, _ -> rs.getString(1) }, p.id,
    ).firstOrNull() ?: throw ContratoApiException(HttpStatus.UNAUTHORIZED, "no_autenticado", "Inicia sesión en el panel")
    val coincide = try { bcrypt.matches(actual, hash) } catch (_: IllegalArgumentException) { false }
    if (!coincide) throw ContratoApiException(HttpStatus.UNAUTHORIZED, "credenciales_invalidas", "La clave actual no coincide")
    if (nueva == actual) ContratoCuerpo.invalido("La clave nueva tiene que ser distinta")
    jdbc.update(
      "UPDATE public.usuario SET password_hash = ?, cambiar_password = FALSE WHERE id_usuario = ?",
      bcrypt.encode(nueva), p.id,
    )
    panel.publicar(p.barSlug, setOf("yo", "usuarios"))
  }

  private fun correo(datos: Map<String, Any?>, obligatorio: Boolean): String? =
    ContratoCuerpo.texto(datos, "correo", max = 254, obligatorio = obligatorio)?.lowercase()?.also {
      if (!FORMATO_CORREO.matches(it)) ContratoCuerpo.invalido("Correo inválido")
    }

  private fun rol(datos: Map<String, Any?>, obligatorio: Boolean): String? {
    if (!datos.containsKey("rol") || datos["rol"] == null) {
      if (obligatorio) ContratoCuerpo.invalido("Falta rol")
      return null
    }
    return ContratoCuerpo.opcion(datos["rol"] as? String, "rol", ROLES) { it }
  }

  /** El id del rol a partir de su nombre del contrato (la base los nombra con mayúscula). */
  private fun idRol() = "(SELECT id_rol FROM public.rol WHERE lower(nombre) = ?)"

  /** Serializa los cambios de personal del bar: la regla del último Administrador no se esquiva con dos peticiones. */
  private fun bloquearBar(idBar: Long) {
    jdbc.query("SELECT 1 FROM public.establecimiento WHERE id_establecimiento = ? FOR UPDATE", { _, _ -> }, idBar)
  }

  private fun correoUsado(idBar: Long, correo: String, excepto: Long?): Boolean = jdbc.queryForObject(
    "SELECT EXISTS(SELECT 1 FROM public.usuario WHERE id_establecimiento = ? AND lower(correo) = ? AND id_usuario <> ?)",
    Boolean::class.javaObjectType, idBar, correo, excepto ?: -1L,
  ) ?: false

  private fun administradoresActivos(idBar: Long): Int = jdbc.queryForObject(
    """SELECT COUNT(*) FROM public.usuario u JOIN public.rol r ON r.id_rol = u.id_rol
       WHERE u.id_establecimiento = ? AND u.activo = TRUE AND lower(r.nombre) = 'administrador'""",
    Int::class.javaObjectType, idBar,
  ) ?: 0

  private fun existente(p: ContratoPanelAcceso.Personal, idTexto: String): Fila =
    fila(p.barId, ContratoCuerpo.id(idTexto, "Ese usuario no existe")) ?: ContratoCuerpo.noEncontrado("Ese usuario no existe")

  private val selectFila = """
    SELECT u.id_usuario, u.nombre, u.correo, lower(r.nombre) AS rol, u.activo, u.cambiar_password
    FROM public.usuario u JOIN public.rol r ON r.id_rol = u.id_rol
    WHERE u.id_establecimiento = ? AND lower(r.nombre) IN ('operador', 'administrador')
  """.trimIndent()

  private fun filas(idBar: Long): List<Fila> = jdbc.query(selectFila, { rs, _ -> leerFila(rs) }, idBar)

  private fun fila(idBar: Long, id: Long): Fila? =
    jdbc.query("$selectFila AND u.id_usuario = ?", { rs, _ -> leerFila(rs) }, idBar, id).firstOrNull()

  private fun leerFila(rs: java.sql.ResultSet) = Fila(
    rs.getLong("id_usuario"), rs.getString("nombre"), rs.getString("correo"), rs.getString("rol"),
    rs.getBoolean("activo"), rs.getBoolean("cambiar_password"),
  )

  /** 12 caracteres sin los que se confunden al dictarlos (0/O, 1/I/l), en tres grupos. */
  private fun claveTemporal(): String = generarClaveTemporal(azar)

  companion object {
    val ROLES = setOf("operador", "administrador")
    /** El contrato pide 10 como mínimo *(propuesto)*; el máximo protege a bcrypt (72 bytes útiles). */
    const val LARGO_MINIMO = 10
    const val LARGO_MAXIMO = 72
    private const val ALFABETO = "abcdefghjkmnpqrstuvwxyz23456789"
    private val FORMATO_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun generarClaveTemporal(azar: SecureRandom): String =
      (1..3).joinToString("-") { (1..4).map { ALFABETO[azar.nextInt(ALFABETO.length)] }.joinToString("") }
  }
}
