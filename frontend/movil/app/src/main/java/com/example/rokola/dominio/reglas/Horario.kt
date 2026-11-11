package com.example.rokola.dominio.reglas

import com.example.rokola.dominio.Franja
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * El horario semanal (apps/web/lib/reglas/horario.ts; RF-036, RN-09). Una franja que cruza
 * medianoche (20:00–02:00) pertenece al día en que empieza. Todo en la zona del bar.
 */

private const val MIN_DIA = 1440
private const val MIN_SEMANA = 7 * MIN_DIA

private fun minutosDe(f: java.time.LocalTime) = f.hour * 60 + f.minute

/** Duración en minutos; 0 si empieza y termina a la misma hora (no válida). */
fun duracionFranja(f: Franja): Int =
    Math.floorMod(minutosDe(f.horaFin) - minutosDe(f.horaInicio), MIN_DIA)

private fun inicioEnSemana(f: Franja) = (f.diaSemana - 1) * MIN_DIA + minutosDe(f.horaInicio)

private fun minutoDeSemana(instante: Instant, zona: ZoneId): Int {
    val local = instante.atZone(zona)
    return (local.dayOfWeek.value - 1) * MIN_DIA + local.hour * 60 + local.minute
}

/** La franja activa que contiene el instante, si hay una. */
fun franjaVigente(franjas: List<Franja>, instante: Instant, zona: ZoneId): Franja? {
    val m = minutoDeSemana(instante, zona)
    return franjas.firstOrNull { f ->
        f.activo && duracionFranja(f) > 0 && Math.floorMod(m - inicioEnSemana(f), MIN_SEMANA) < duracionFranja(f)
    }
}

fun abiertoPorHorario(franjas: List<Franja>, instante: Instant, zona: ZoneId): Boolean =
    franjaVigente(franjas, instante, zona) != null

/** Cuándo termina la franja vigente. */
fun cierreDeFranja(franjas: List<Franja>, instante: Instant, zona: ZoneId): Instant? {
    val f = franjaVigente(franjas, instante, zona) ?: return null
    val dentro = Math.floorMod(minutoDeSemana(instante, zona) - inicioEnSemana(f), MIN_SEMANA)
    return instante.plus((duracionFranja(f) - dentro).toLong(), ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)
}

/** Cuándo abre la próxima franja (si ahora está cerrado). */
fun proximaApertura(franjas: List<Franja>, instante: Instant, zona: ZoneId): Instant? {
    val activas = franjas.filter { it.activo && duracionFranja(it) > 0 }
    if (activas.isEmpty()) return null
    val m = minutoDeSemana(instante, zona)
    val faltan = activas.minOf { f -> Math.floorMod(inicioEnSemana(f) - m, MIN_SEMANA).takeIf { it != 0 } ?: MIN_SEMANA }
    return instante.plus(faltan.toLong(), ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)
}
