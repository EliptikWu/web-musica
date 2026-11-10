package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaEvent
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Version del snapshot para un PMV con una sola instancia de Spring Boot.
 * Cambia después de confirmar solicitudes, votos y reproducciones existentes.
 * No representa cambios escritos manualmente en SQL ni operaciones admin que no emitan CambioColaEvent.
 */
@Component
class ContratoVersionService {
  private val versiones = ConcurrentHashMap<Long, AtomicLong>()

  fun actual(idSesion: Long?): Long =
    if (idSesion == null) 0L else versiones.computeIfAbsent(idSesion) {
      AtomicLong(System.currentTimeMillis())
    }.get()

  @Order(-100)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun alCambiar(evento: CambioColaEvent) {
    versiones.computeIfAbsent(evento.sesionId) {
      AtomicLong(System.currentTimeMillis())
    }.updateAndGet { anterior -> maxOf(anterior + 1, System.currentTimeMillis()) }
  }
}
