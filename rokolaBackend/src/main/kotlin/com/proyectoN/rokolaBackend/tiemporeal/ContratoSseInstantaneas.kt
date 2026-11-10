package com.proyectoN.rokolaBackend.tiemporeal

import com.proyectoN.rokolaBackend.compat.ContratoEstadoService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** Transacción nueva: AFTER_COMMIT puede ejecutarse antes de liberar la transacción anterior. */
@Service
class ContratoSseInstantaneas(
    private val estados: ContratoEstadoService,
    private val jdbc: JdbcTemplate,
) {
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun ahora(slug: String): Map<String, Any?> = estados.ahora(slug)

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun slugDeSesion(sesionId: Long): String? = jdbc.query(
        """SELECT e.slug FROM public.sesion s
           JOIN public.establecimiento e ON e.id_establecimiento = s.id_establecimiento
           WHERE s.id_sesion = ? AND e.activo = TRUE""".trimIndent(),
        { rs, _ -> rs.getString("slug") },
        sesionId,
    ).firstOrNull()
}
