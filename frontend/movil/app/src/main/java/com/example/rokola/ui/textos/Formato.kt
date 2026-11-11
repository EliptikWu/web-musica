package com.example.rokola.ui.textos

import com.example.rokola.dominio.Franja
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/*
 * Formatos de la interfaz (C:\web-musica\apps\web\lib\formato.ts, doc 05 «Textos»): horas de
 * 12 h como en Colombia, duraciones m:ss y esperas redondeadas hacia abajo, en la zona del bar.
 */

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private const val NBSP = ' '

/** «8:00 p. m.». Ni el espacio antes de «p. m.» ni el de dentro se parten al final de una línea. */
fun formatoHoraDelDia(hora: LocalTime): String {
    val h12 = if (hora.hour % 12 == 0) 12 else hora.hour % 12
    val periodo = if (hora.hour < 12) "a.${NBSP}m." else "p.${NBSP}m."
    return "$h12:${hora.minute.toString().padStart(2, '0')}$NBSP$periodo"
}

fun formatoHora(instante: Instant, zona: ZoneId): String = formatoHoraDelDia(instante.atZone(zona).toLocalTime())

/** «4:27», «1:12:40». */
fun formatoDuracion(segundos: Double): String {
    val total = maxOf(0, segundos.toInt())
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = (total % 60).toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s"
}

fun formatoDuracion(segundos: Int): String = formatoDuracion(segundos.toDouble())

/** Minutos enteros hacia abajo: 14:30 de espera es «unos 14 min», no 15 (doc 05). */
fun minutosEnteros(segundos: Double): Int = (maxOf(0.0, segundos) / 60).toInt()

/** «en unos 13 min», «en menos de 1 min», «en más de 2 h». */
fun formatoEspera(segundos: Double): String {
    val minutos = minutosEnteros(segundos)
    return when {
        minutos < 1 -> "en menos de 1 min"
        minutos >= 120 -> "en más de 2 h"
        else -> "en unos $minutos min"
    }
}

/** Forma corta para las listas: «en 3 min» (que es estimada se dice una vez, aparte). */
fun formatoEsperaCorta(segundos: Double): String {
    val minutos = minutosEnteros(segundos)
    return when {
        minutos < 1 -> "en menos de 1 min"
        minutos >= 120 -> "en más de 2 h"
        else -> "en $minutos min"
    }
}

/** «9 canciones, unos 36 min». */
fun formatoResumenCola(canciones: Int, segundos: Double): String {
    if (canciones == 0) return "Sin canciones en la cola"
    val minutos = minutosEnteros(segundos)
    val nombre = if (canciones == 1) "canción" else "canciones"
    return if (minutos < 1) "$canciones $nombre, menos de 1 min" else "$canciones $nombre, unos $minutos min"
}

data class NombreDia(val corto: String, val largo: String)

val DIAS = listOf(
    NombreDia("Lun", "lunes"),
    NombreDia("Mar", "martes"),
    NombreDia("Mié", "miércoles"),
    NombreDia("Jue", "jueves"),
    NombreDia("Vie", "viernes"),
    NombreDia("Sáb", "sábado"),
    NombreDia("Dom", "domingo"),
)

/** 1 = lunes … 7 = domingo. */
fun nombreDia(dia: Int): String = DIAS.getOrNull(dia - 1)?.largo ?: ""

fun capitalizar(texto: String): String = texto.replaceFirstChar { it.titlecase(ES_CO) }

/** Días seguidos como rango («jueves a sábado»); sueltos, en lista («lunes y jueves»). */
private fun nombrarDias(dias: List<Int>): String {
    val tramos = mutableListOf<MutableList<Int>>()
    for (d in dias.sorted()) {
        val ultimo = tramos.lastOrNull()
        if (ultimo != null && ultimo.last() == d - 1) ultimo += d else tramos += mutableListOf(d)
    }
    val partes = tramos.map { t -> if (t.size >= 3) "${nombreDia(t.first())} a ${nombreDia(t.last())}" else t.joinToString(" y ") { nombreDia(it) } }
    return if (partes.size > 1) "${partes.dropLast(1).joinToString(", ")} y ${partes.last()}" else partes.firstOrNull().orEmpty()
}

data class LineaHorario(val dias: String, val horas: String)

/** El horario en una línea, como en el prototipo: «Jueves a sábado» + «8:00 p. m. a 2:00 a. m.». */
fun resumenHorario(franjas: List<Franja>): List<LineaHorario> =
    franjas.filter { it.activo && it.horaInicio != it.horaFin }
        .groupBy { it.horaInicio to it.horaFin }
        .entries
        .sortedBy { (_, fs) -> fs.minOf { it.diaSemana } }
        .map { (horas, fs) ->
            LineaHorario(
                dias = capitalizar(nombrarDias(fs.map { it.diaSemana })),
                horas = "${formatoHoraDelDia(horas.first)} a ${formatoHoraDelDia(horas.second)}",
            )
        }

/** «hoy», «mañana» o «el jueves», en la zona del bar. */
fun diaRelativo(fecha: Instant, ahora: Instant, zona: ZoneId): String {
    val objetivo = fecha.atZone(zona).toLocalDate()
    val hoy = ahora.atZone(zona).toLocalDate()
    return when (objetivo) {
        hoy -> "hoy"
        hoy.plusDays(1) -> "mañana"
        else -> "el ${objetivo.dayOfWeek.getDisplayName(TextStyle.FULL, ES_CO)}"
    }
}

/** Cierra una frase con punto, sin duplicarlo cuando ya termina en «p. m.» o «a. m.». */
fun conPunto(frase: String): String = if (frase.endsWith(".")) frase else "$frase."

/** Día de la semana (1 = lunes) de un instante en la zona del bar. */
fun diaDeLaSemana(instante: Instant, zona: ZoneId): Int = instante.atZone(zona).dayOfWeek.value
