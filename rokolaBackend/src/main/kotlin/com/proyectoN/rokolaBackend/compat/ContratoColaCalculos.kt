package com.proyectoN.rokolaBackend.compat

import org.springframework.jdbc.core.JdbcTemplate

/** Reglas RN-01 y RF-018 compartidas entre las APIs del cliente.
 * RN-01: una solicitud propia PENDIENTE o REPRODUCIENDO ocupa un cupo.
 * RF-018: espera hasta el INICIO estimado de cada elemento de la cola.
 */
internal object ContratoColaCalculos {
    fun cuposOcupados(jdbc: JdbcTemplate, idSesion: Long, idDispositivo: Long): Long =
        jdbc.queryForObject(
            """SELECT COUNT(*) FROM public.solicitud
               WHERE id_sesion = ? AND id_dispositivo_solicitante = ?
                 AND estado IN ('pendiente', 'reproduciendo')""",
            Long::class.javaObjectType, idSesion, idDispositivo,
        ) ?: 0L

    fun cuposRestantes(maximo: Int, ocupados: Long): Int =
        (maximo.toLong() - ocupados).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    /** Calculado con el tiempo restante de la reproducción a servidor_ahora.
     * Se usan Long para acumular; el contrato exige segundos enteros no negativos.
     */
    fun esperasSeg(duracionesPendientes: List<Int>, restanteReproduccion: Long): List<Int> {
        var acumulada = restanteReproduccion.coerceAtLeast(0L)
        return duracionesPendientes.map { duracion ->
            val espera = acumulada.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            acumulada = (acumulada + duracion.coerceAtLeast(0).toLong())
                .coerceAtMost(Int.MAX_VALUE.toLong())
            espera
        }
    }
}
