package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioEfectivoService
import com.proyectoN.rokolaBackend.reproduccion.FinalizarReproduccionRequest
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionGestionService
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Bloqueos de canciones (RF-042, RF-043): el Operador y el Administrador bloquean; solo el
 * Administrador cambia la duración o levanta un bloqueo. Solo los del propio bar.
 *
 * Como en la simulación de la web (`motor.ts`, `bloquear`): bloquear lo que ya está bloqueado
 * devuelve el bloqueo vigente; si la canción suena, se corta, y sus pedidos pendientes de la
 * noche quedan `bloqueada` con el motivo. «Hasta el fin de la noche» termina cuando cierra la
 * franja (o el evento) vigente; sin ninguna (bar abierto a mano), a las 12 h *(propuesto)*.
 *
 * Hecho por el equipo de frontend (2026-11-21, D14).
 */
@Service
class ContratoBloqueosService(
  private val jdbc: JdbcTemplate,
  private val canciones: ContratoCanciones,
  private val horarioEfectivo: HorarioEfectivoService,
  private val gestion: ReproduccionGestionService,
  private val bitacora: ContratoBitacora,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
) {
  fun listar(p: ContratoPanelAcceso.Personal, vigencia: String?): Map<String, Any?> {
    val filtro = when (vigencia ?: "vigentes") {
      "vigentes" -> "AND $VIGENTE"
      "vencidos" -> "AND NOT ($VIGENTE)"
      "todos" -> ""
      else -> ContratoCuerpo.invalido("vigencia: vigentes, vencidos o todos")
    }
    return mapOf("bloqueos" to jdbc.query("$SELECT WHERE b.id_establecimiento = ? $filtro ORDER BY b.fecha_inicio DESC LIMIT 200", { rs, _ -> fila(rs) }, p.barId))
  }

  @Transactional
  fun bloquear(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("video_id", "motivo", "duracion", "horas"))
    val videoId = ContratoCuerpo.texto(datos, "video_id", max = 30, obligatorio = true)!!
    val motivo = ContratoCuerpo.texto(datos, "motivo", max = 300, obligatorio = true)!!
    val (duracion, horas) = duracion(datos)
    // Una canción que ya está en la base se bloquea aunque YouTube la tenga como no disponible:
    // solo se consulta a YouTube por un video que la base no conoce.
    val cancion = canciones.enBase(videoId.trim()) ?: canciones.obtenerOValidar(videoId)
    // Bajo el bloqueo del bar: dos bloqueos de la misma canción a la vez no se duplican.
    jdbc.query("SELECT 1 FROM public.establecimiento WHERE id_establecimiento = ? FOR UPDATE", { _, _ -> }, p.barId)
    vigenteDe(p.barId, cancion.id)?.let { return it }
    val fin = fin(p, duracion, horas)
    val id = jdbc.queryForObject(
      """INSERT INTO public.bloqueo_contenido (id_establecimiento, id_cancion, id_usuario, motivo, fecha_fin, activo, duracion)
         VALUES (?, ?, ?, ?, ?, TRUE, ?) RETURNING id_bloqueo""",
      Long::class.javaObjectType, p.barId, cancion.id, p.id, motivo, fin, duracion,
    )!!
    bitacora.anotar(p, "bloquear", "cancion", cancion.id, texto = textoDuracion(duracion, horas))
    aplicarALaNoche(p, cancion.id, motivo)
    panel.publicar(p.barSlug, setOf("bloqueos", "noche", "bitacora"))
    return porId(p.barId, id)!!
  }

  @Transactional
  fun cambiarDuracion(p: ContratoPanelAcceso.Personal, idTexto: String, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("duracion", "horas"))
    val (duracion, horas) = duracion(datos)
    val id = existente(p, idTexto)
    jdbc.query("SELECT 1 FROM public.establecimiento WHERE id_establecimiento = ? FOR UPDATE", { _, _ -> }, p.barId)
    val (cancionId, motivo) = jdbc.queryForObject(
      "SELECT id_cancion, motivo FROM public.bloqueo_contenido WHERE id_bloqueo = ?",
      { rs, _ -> rs.getLong(1) to rs.getString(2) }, id,
    )!!
    // Si la canción ya tiene otro bloqueo vigente, no se crea un segundo: se responde con ese.
    vigenteDe(p.barId, cancionId)?.takeIf { it["id"] != id.toString() }?.let { return it }
    // Como la simulación: el periodo nuevo cuenta desde ahora (así `horas` es la que se eligió) y
    // vuelve a estar vigente aunque se hubiera levantado.
    jdbc.update(
      """UPDATE public.bloqueo_contenido SET fecha_inicio = CURRENT_TIMESTAMP, fecha_fin = ?, activo = TRUE, duracion = ?
         WHERE id_bloqueo = ? AND id_establecimiento = ?""",
      fin(p, duracion, horas), duracion, id, p.barId,
    )
    bitacora.anotar(p, "extender_bloqueo", "cancion", cancionId, texto = textoDuracion(duracion, horas))
    aplicarALaNoche(p, cancionId, motivo)
    panel.publicar(p.barSlug, setOf("bloqueos", "noche", "bitacora"))
    return porId(p.barId, id)!!
  }

  /** Siempre 204: levantar uno ya levantado no es un error (como la simulación). */
  @Transactional
  fun desbloquear(p: ContratoPanelAcceso.Personal, idTexto: String) {
    val id = existente(p, idTexto)
    val cancionId = jdbc.query(
      """UPDATE public.bloqueo_contenido SET activo = FALSE,
                fecha_fin = GREATEST(fecha_inicio, LEAST(COALESCE(fecha_fin, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP))
         WHERE id_bloqueo = ? AND id_establecimiento = ? AND activo = TRUE RETURNING id_cancion""",
      { rs, _ -> rs.getLong(1) }, id, p.barId,
    ).firstOrNull() ?: return
    bitacora.anotar(p, "desbloquear", "cancion", cancionId)
    panel.publicar(p.barSlug, setOf("bloqueos", "noche", "bitacora"))
  }

  /** Corta la canción si suena y saca de la cola sus pedidos pendientes. */
  private fun aplicarALaNoche(p: ContratoPanelAcceso.Personal, cancionId: Long, motivo: String) {
    val sesion = jdbc.query(
      "SELECT id_sesion FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta' ORDER BY id_sesion DESC LIMIT 1 FOR UPDATE",
      { rs, _ -> rs.getLong(1) }, p.barId,
    ).firstOrNull() ?: return
    val retiradas = jdbc.update(
      "UPDATE public.solicitud SET estado = 'bloqueada', motivo_estado = ? WHERE id_sesion = ? AND id_cancion = ? AND estado = 'pendiente'",
      motivo.take(250), sesion, cancionId,
    )
    val sonando = jdbc.query(
      "SELECT id_reproduccion FROM public.reproduccion WHERE id_sesion = ? AND id_cancion = ? AND resultado = 'iniciada'",
      { rs, _ -> rs.getLong(1) }, sesion, cancionId,
    ).firstOrNull()
    if (sonando != null) {
      gestion.finalizar(sesion, sonando, FinalizarReproduccionRequest("saltada"))
      bitacora.anotar(p, "saltar", "cancion", cancionId)
    }
    if (retiradas > 0 || sonando != null) cola.publicar(sesion, "bloqueo")
  }

  /** `duracion` y `horas` del contrato: `horas` (1 a 168) solo con la duración «horas». */
  private fun duracion(datos: Map<String, Any?>): Pair<String, Int?> {
    val duracion = ContratoCuerpo.opcion(datos["duracion"] as? String, "duracion", DURACIONES) { it }
    val horas = ContratoCuerpo.entero(datos, "horas", 1, 168)
    if (duracion == "horas" && horas == null) ContratoCuerpo.invalido("Con duración «horas» hay que decir cuántas (1 a 168)")
    return duracion to if (duracion == "horas") horas else null
  }

  private fun fin(p: ContratoPanelAcceso.Personal, duracion: String, horas: Int?): OffsetDateTime? {
    val ahora = ZonedDateTime.now(ZoneId.of(p.barZona))
    return when (duracion) {
      "horas" -> ahora.plusHours(horas!!.toLong()).toOffsetDateTime()
      "fin_sesion" -> horarioEfectivo.cierre(p.barId, ahora.zone) ?: ahora.plusHours(HORAS_SIN_FRANJA).toOffsetDateTime()
      else -> null
    }
  }

  private fun existente(p: ContratoPanelAcceso.Personal, idTexto: String): Long {
    val id = ContratoCuerpo.id(idTexto, "Ese bloqueo no existe")
    val existe = jdbc.queryForObject(
      "SELECT EXISTS(SELECT 1 FROM public.bloqueo_contenido WHERE id_bloqueo = ? AND id_establecimiento = ?)",
      Boolean::class.javaObjectType, id, p.barId,
    ) == true
    if (!existe) ContratoCuerpo.noEncontrado("Ese bloqueo no existe")
    return id
  }

  private fun vigenteDe(idBar: Long, cancionId: Long): Map<String, Any?>? = jdbc.query(
    "$SELECT WHERE b.id_establecimiento = ? AND b.id_cancion = ? AND $VIGENTE ORDER BY b.fecha_inicio DESC LIMIT 1",
    { rs, _ -> fila(rs) }, idBar, cancionId,
  ).firstOrNull()

  private fun porId(idBar: Long, id: Long): Map<String, Any?>? =
    jdbc.query("$SELECT WHERE b.id_establecimiento = ? AND b.id_bloqueo = ?", { rs, _ -> fila(rs) }, idBar, id).firstOrNull()

  companion object {
    private val DURACIONES = setOf("fin_sesion", "horas", "indefinido")
    private const val HORAS_SIN_FRANJA = 12L

    /** Vigente: activo, ya empezó y no ha terminado. La misma condición que usan pedir y la rotación. */
    const val VIGENTE = "b.activo = TRUE AND b.fecha_inicio <= CURRENT_TIMESTAMP AND (b.fecha_fin IS NULL OR b.fecha_fin > CURRENT_TIMESTAMP)"

    internal val SELECT = """
      SELECT b.id_bloqueo, b.motivo, b.fecha_inicio, b.fecha_fin, b.duracion,
             ($VIGENTE) AS vigente, u.nombre AS creador, ${ContratoCanciones.COLUMNAS}
      FROM public.bloqueo_contenido b
      JOIN public.cancion c ON c.id_cancion = b.id_cancion
      LEFT JOIN public.usuario u ON u.id_usuario = b.id_usuario
    """.trimIndent()

    fun textoDuracion(duracion: String, horas: Int?): String = when (duracion) {
      "fin_sesion" -> "Hasta el fin de la noche"
      "indefinido" -> "Hasta que alguien lo levante"
      else -> if (horas == 1) "Por 1 hora" else "Por $horas horas"
    }

    /** `Bloqueo` del contrato. Sin la marca de duración (bloqueos anteriores a la V5), sale como horas o indefinido. */
    fun fila(rs: java.sql.ResultSet): Map<String, Any?> {
      val inicio = rs.getObject("fecha_inicio", OffsetDateTime::class.java)
      val fin = rs.getObject("fecha_fin", OffsetDateTime::class.java)
      val duracion = rs.getString("duracion") ?: if (fin == null) "indefinido" else "horas"
      val horas = if (duracion == "horas" && fin != null) {
        Duration.between(inicio, fin).toMinutes().let { m -> ((m + 59) / 60).toInt().coerceIn(1, 168) }
      } else null
      return mapOf(
        "id" to rs.getLong("id_bloqueo").toString(),
        "cancion" to ContratoCanciones.cancion(rs),
        "motivo" to rs.getString("motivo").orEmpty(),
        "duracion" to duracion,
        "horas" to horas,
        "inicio" to inicio,
        "fin" to fin,
        "creado_por" to (rs.getString("creador") ?: "Sistema"),
        "vigente" to rs.getBoolean("vigente"),
      )
    }
  }
}
