package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.reproduccion.FinalizarReproduccionRequest
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionGestionService
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * El servicio del QR desde el panel: `PUT /panel/servicio` (RF-036, RF-039; Operador y
 * Administrador) y `POST /panel/servicio/mantenimiento` (RF-038; solo el Administrador). El modo
 * vive en `configuracion_servicio.estado_manual` (`disponible`, `pausado`, `mantenimiento`), que
 * ya leen `ahora`, pedir y la reproducción automática.
 *
 * Mantenimiento, como la simulación (`activarMantenimiento`): corta lo que suena, expira los
 * pedidos pendientes y cierra la noche. El historial queda. Se sale con «Reanudar solicitudes»
 * (`modo_qr = automatico`): como el backend no abre noches solo, reanudar abre una si no hay.
 *
 * `forzado_activo` todavía no abre fuera del horario: `estado_manual` no tiene ese valor y se
 * trata como `automatico` (doc 04, «Pendiente del backend»).
 *
 * Hecho por el equipo de frontend (2026-11-21, D14).
 */
@Service
class ContratoServicioService(
  private val jdbc: JdbcTemplate,
  private val gestion: ReproduccionGestionService,
  private val bitacora: ContratoBitacora,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
) {
  @Transactional
  fun cambiar(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("modo_qr"))
    val modo = ContratoCuerpo.opcion(datos["modo_qr"] as? String, "modo_qr", MODOS) { it }
    val anterior = bloquearConfiguracion(p.barId)
    val nuevo = if (modo == "pausado") "pausado" else "disponible"
    var sesion = sesionAbierta(p.barId)
    // Lo mismo que ya está, con noche abierta, no deja otra línea en la bitácora.
    if (anterior == nuevo && (nuevo == "pausado" || sesion != null)) return
    if (anterior != nuevo) {
      jdbc.update(
        "UPDATE public.configuracion_servicio SET estado_manual = ?, fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?",
        nuevo, p.barId,
      )
    }
    if (nuevo == "disponible" && sesion == null) {
      sesion = jdbc.queryForObject(
        "INSERT INTO public.sesion (id_establecimiento, estado) VALUES (?, 'abierta') RETURNING id_sesion",
        Long::class.javaObjectType, p.barId,
      )!!
      bitacora.anotar(p, "abrir_sesion", "sesion", sesion, "Noche", "al reanudar")
    }
    val tipo = when {
      nuevo == "pausado" -> "pausar_solicitudes"
      modo == "forzado_activo" -> "forzar_apertura"
      else -> "reanudar_solicitudes"
    }
    bitacora.anotar(p, tipo, "servicio", null, "Solicitudes", if (anterior == "mantenimiento") "sale de mantenimiento" else null)
    avisar(p, sesion)
  }

  @Transactional
  fun mantenimiento(p: ContratoPanelAcceso.Personal) {
    val anterior = bloquearConfiguracion(p.barId)
    val sesion = sesionAbierta(p.barId)
    if (anterior == "mantenimiento" && sesion == null) return
    jdbc.update(
      "UPDATE public.configuracion_servicio SET estado_manual = 'mantenimiento', fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?",
      p.barId,
    )
    val expirados = sesion?.let { cerrarNoche(it, "Mantenimiento") } ?: 0
    bitacora.anotar(p, "mantenimiento", "sesion", sesion, "Noche", textoExpirados(expirados))
    avisar(p, sesion)
  }

  /**
   * `POST /panel/sesiones` (RF-037, Administrador): abrir la noche a mano. Con una abierta, 409.
   * Si el bar estaba en mantenimiento, sale de él (como la simulación).
   */
  @Transactional
  fun abrirNoche(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
    val datos = cuerpo ?: emptyMap()
    ContratoCuerpo.exigirCampos(datos, setOf("tipo_evento"))
    val tipoEvento = ContratoCuerpo.texto(datos, "tipo_evento", max = 60, obligatorio = true)!!
    val anterior = bloquearConfiguracion(p.barId)
    if (sesionAbierta(p.barId) != null) ContratoCuerpo.conflicto("Ya hay una noche abierta")
    if (anterior == "mantenimiento") {
      jdbc.update(
        "UPDATE public.configuracion_servicio SET estado_manual = 'disponible', fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?",
        p.barId,
      )
    }
    val sesion = jdbc.queryForObject(
      "INSERT INTO public.sesion (id_establecimiento, estado, tipo_evento) VALUES (?, 'abierta', ?) RETURNING id_sesion",
      Long::class.javaObjectType, p.barId, tipoEvento,
    )!!
    bitacora.anotar(p, "abrir_sesion", "sesion", sesion, "Noche", "manual")
    avisar(p, sesion)
  }

  /**
   * `POST /panel/sesiones/actual/cierre` (RF-037, Administrador): cerrar la noche a mano. Sin una
   * abierta, 409. Corta lo que suena y expira lo pendiente; el modo del QR no cambia.
   */
  @Transactional
  fun cerrarNoche(p: ContratoPanelAcceso.Personal) {
    bloquearConfiguracion(p.barId)
    val sesion = sesionAbierta(p.barId) ?: ContratoCuerpo.conflicto("No hay una noche abierta")
    val expirados = cerrarNoche(sesion, "Noche cerrada")
    bitacora.anotar(p, "cerrar_sesion", "sesion", sesion, "Noche", textoExpirados(expirados))
    avisar(p, sesion)
  }

  /** Corta lo que suena, expira los pedidos pendientes y cierra la noche. Devuelve cuántos expiraron. */
  private fun cerrarNoche(sesion: Long, motivo: String): Int {
    jdbc.query("SELECT 1 FROM public.sesion WHERE id_sesion = ? FOR UPDATE", { _, _ -> }, sesion)
    jdbc.query(
      "SELECT id_reproduccion FROM public.reproduccion WHERE id_sesion = ? AND resultado = 'iniciada'",
      { rs, _ -> rs.getLong(1) }, sesion,
    ).firstOrNull()?.let { gestion.finalizar(sesion, it, FinalizarReproduccionRequest("saltada")) }
    val expirados = jdbc.update(
      "UPDATE public.solicitud SET estado = 'expirada', motivo_estado = ? WHERE id_sesion = ? AND estado = 'pendiente'",
      motivo, sesion,
    )
    jdbc.update(
      "UPDATE public.sesion SET estado = 'cerrada', fecha_cierre = GREATEST(fecha_apertura, CURRENT_TIMESTAMP) WHERE id_sesion = ?",
      sesion,
    )
    return expirados
  }

  private fun textoExpirados(n: Int) = if (n == 1) "1 pedido pendiente expiró" else "$n pedidos pendientes expiraron"

  /** El modo actual, con la fila bloqueada: dos cambios a la vez no se cruzan. */
  private fun bloquearConfiguracion(idBar: Long): String = jdbc.query(
    "SELECT estado_manual FROM public.configuracion_servicio WHERE id_establecimiento = ? FOR UPDATE",
    { rs, _ -> rs.getString(1) }, idBar,
  ).firstOrNull() ?: throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio")

  private fun sesionAbierta(idBar: Long): Long? = jdbc.query(
    "SELECT id_sesion FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta' ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1",
    { rs, _ -> rs.getLong(1) }, idBar,
  ).firstOrNull()

  /** El teléfono y la TV ven el estado nuevo por el canal de la noche; el panel, por el suyo. */
  private fun avisar(p: ContratoPanelAcceso.Personal, sesion: Long?) {
    sesion?.let { cola.publicar(it, "servicio") }
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  private companion object {
    val MODOS = setOf("automatico", "forzado_activo", "pausado")
  }
}
