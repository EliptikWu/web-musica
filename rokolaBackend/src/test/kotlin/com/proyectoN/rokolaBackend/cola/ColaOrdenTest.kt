package com.proyectoN.rokolaBackend.cola

import com.proyectoN.rokolaBackend.establecimiento.Establecimiento
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionConsultaRepository
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.Sesion
import com.proyectoN.rokolaBackend.sesion.SesionRepository
import org.mockito.Mockito.*
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.test.*

/** CP-012/013: servicio de cola y espera; el orden SQL se valida además en PostgreSQL local. */
class ColaOrdenTest {
    private val sesiones = mock(SesionRepository::class.java)
    private val repo = mock(ColaRepository::class.java)
    private val reproducciones = mock(ReproduccionConsultaRepository::class.java)
    private val cola = ColaService(sesiones, repo, reproducciones)
    private val ahora = OffsetDateTime.now()
    private fun sesion(activo: Boolean, estado: EstadoSesion) = Sesion(id = 2,
        establecimiento = Establecimiento(id = 3, nombre = "Bar", slug = "bar", zonaHoraria = "America/Bogota",
            activo = activo, fechaCreacion = ahora), fechaApertura = ahora,
        estado = estado, indiceListaDefecto = 1)
    private fun fila(id: Long, votos: Long, segundos: Int) = ColaRepository.FilaCola(id, id,
        "dQw4w9WgXcQ", "Canción $id", null, segundos, null, votos, ahora)

    @Test fun `calcula posiciones y suma duraciones desde el orden entregado por repositorio`() {
        `when`(sesiones.findById(2L)).thenReturn(Optional.of(sesion(true, EstadoSesion.ABIERTA)))
        `when`(repo.listarPendientes(2L)).thenReturn(listOf(fila(7, 4, 180), fila(9, 2, 120)))
        val resultado = cola.obtener(2L)
        assertEquals(2, resultado.totalSolicitudes)
        assertEquals(listOf(7L, 9L), resultado.canciones.map { it.solicitudId })
        assertEquals(listOf(1, 2), resultado.canciones.map { it.posicion })
        assertEquals(listOf(0L, 180L), resultado.canciones.map { it.esperaEstimadaSeg })
    }
    @Test fun `no expone cola de sesion cerrada`() {
        `when`(sesiones.findById(2L)).thenReturn(Optional.of(sesion(true, EstadoSesion.CERRADA)))
        assertFailsWith<org.springframework.web.server.ResponseStatusException> { cola.obtener(2L) }
        verifyNoInteractions(repo)
    }
}
