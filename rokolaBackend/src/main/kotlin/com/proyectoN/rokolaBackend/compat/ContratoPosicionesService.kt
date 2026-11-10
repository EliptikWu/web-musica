package com.proyectoN.rokolaBackend.compat

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/** Conserva durante cinco segundos el puesto previo para las pantallas del PMV.
 * Historial efímero por instancia: si reinicia el backend, vuelve a null.
 */
@Component
class ContratoPosicionesService {
    private data class Cambio(val anterior: Int, val expiraEnMs: Long)
    private class Estado {
        var posiciones = emptyMap<Long, Int>()
        val cambios = mutableMapOf<Long, Cambio>()
    }

    private val estados = ConcurrentHashMap<Long, Estado>()

    fun anteriores(idSesion: Long, actuales: Map<Long, Int>): Map<Long, Int?> {
        val estado = estados.computeIfAbsent(idSesion) { Estado() }
        val ahora = System.currentTimeMillis()
        synchronized(estado) {
            estado.cambios.entries.removeIf { (id, cambio) -> id !in actuales || cambio.expiraEnMs <= ahora }
            for ((id, puesto) in actuales) {
                val viejo = estado.posiciones[id]
                if (viejo != null && viejo != puesto) {
                    estado.cambios[id] = Cambio(viejo, ahora + 5_000L)
                }
            }
            estado.posiciones = actuales.toMap()
            return actuales.keys.associateWith { id -> estado.cambios[id]?.anterior }
        }
    }
}
