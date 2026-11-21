package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaEvent
import com.proyectoN.rokolaBackend.tiemporeal.CambioServicioEvent
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Version del snapshot para un PMV con una sola instancia de Spring Boot.
 * Cambia después de confirmar solicitudes, votos y reproducciones existentes.
 * No representa cambios escritos manualmente en SQL ni operaciones admin que no emitan CambioColaEvent.
 * Sin noche abierta vale la última emitida: el cliente descarta las fotos con versión menor, y
 * con 0 nunca se enteraría de que la noche cerró.
 */
@Component
class ContratoVersionService {
  private val versiones = ConcurrentHashMap<Long, AtomicLong>()
  private val ultima = AtomicLong(System.currentTimeMillis())

  fun actual(idSesion: Long?): Long =
    if (idSesion == null) ultima.get() else versiones.computeIfAbsent(idSesion) { AtomicLong(siguiente()) }.get()

  @Order(-100)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun alCambiar(evento: CambioColaEvent) {
    val nueva = versiones.computeIfAbsent(evento.sesionId) { AtomicLong(siguiente()) }
      .updateAndGet { anterior -> maxOf(anterior + 1, System.currentTimeMillis()) }
    ultima.accumulateAndGet(nueva, ::maxOf)
  }

  /** Cambió el servicio de un bar sin noche abierta: su foto también es nueva. */
  @Order(-100)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun alCambiarServicio(@Suppress("UNUSED_PARAMETER") evento: CambioServicioEvent) {
    siguiente()
  }

  private fun siguiente(): Long = ultima.updateAndGet { maxOf(it + 1, System.currentTimeMillis()) }
}
