package com.proyectoN.rokolaBackend.tiemporeal

import com.proyectoN.rokolaBackend.cola.ColaService
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionConsultaService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Una transaccion de lectura NUEVA para el listener AFTER_COMMIT. */
@Service
class EstadoMusicalSnapshotService(
    private val cola: ColaService,
    private val reproduccion: ReproduccionConsultaService,
) {
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun crear(idSesion: Long, tipo: String): Map<String, Any?> = mapOf(
        "tipo" to tipo,
        "sesionId" to idSesion,
        "emitidoEn" to OffsetDateTime.now(ZoneOffset.UTC),
        "cola" to cola.obtener(idSesion),
        "reproduccionActual" to reproduccion.obtener(idSesion).actual,
    )
}
