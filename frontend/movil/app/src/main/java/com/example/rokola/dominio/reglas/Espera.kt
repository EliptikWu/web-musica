package com.example.rokola.dominio.reglas

import com.example.rokola.dominio.EstadoReproduccion
import com.example.rokola.dominio.Reproduccion
import java.time.Duration
import java.time.Instant

/*
 * Avance de lo que suena (apps/web/lib/reglas/espera.ts; RF-016). El servidor manda
 * posicionSeg en el instante servidorAhora; con el reloj del servidor corregido se calcula
 * dónde va sin preguntar cada segundo. La espera de cada canción (RF-018) no se calcula aquí:
 * la manda el backend en ItemCola.esperaSeg.
 */

/** Segundos reproducidos en [ahoraServidor]. */
fun posicionActual(reproduccion: Reproduccion, servidorAhora: Instant, ahoraServidor: Instant): Double {
    val base = reproduccion.posicionSeg.toDouble()
    val duracion = reproduccion.cancion.duracionSeg.toDouble()
    if (reproduccion.estado == EstadoReproduccion.PAUSADA) return minOf(base, duracion)
    val transcurrido = maxOf(0.0, Duration.between(servidorAhora, ahoraServidor).toMillis() / 1000.0)
    return minOf(base + transcurrido, duracion)
}
