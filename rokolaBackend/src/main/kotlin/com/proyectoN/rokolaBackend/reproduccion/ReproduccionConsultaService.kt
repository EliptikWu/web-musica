package com.proyectoN.rokolaBackend.reproduccion

import com.proyectoN.rokolaBackend.sesion.SesionNoEncontradaException
import com.proyectoN.rokolaBackend.sesion.SesionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReproduccionConsultaService(
    private val sesiones: SesionRepository,
    private val reproducciones: ReproduccionConsultaRepository,
) {
    @Transactional(readOnly = true)
    fun obtener(idSesion: Long): EstadoReproduccionResponse {
        if (!sesiones.existsById(idSesion)) throw SesionNoEncontradaException(idSesion)
        return EstadoReproduccionResponse(idSesion, reproducciones.actual(idSesion))
    }
}
