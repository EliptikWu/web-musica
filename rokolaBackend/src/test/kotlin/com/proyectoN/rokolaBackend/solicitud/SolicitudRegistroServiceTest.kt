package com.proyectoN.rokolaBackend.solicitud

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import com.proyectoN.rokolaBackend.cancion.CancionRepository
import com.proyectoN.rokolaBackend.cancion.ClasificadorGeneroService
import com.proyectoN.rokolaBackend.establecimiento.ConfiguracionServicioRepository
import com.proyectoN.rokolaBackend.establecimiento.Establecimiento
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.Sesion
import com.proyectoN.rokolaBackend.sesion.SesionNoEncontradaException
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.voto.VotoRepository
import com.proyectoN.rokolaBackend.youtube.YoutubeVideoResponse
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import kotlin.test.*

/** CP-009/036/049: validaciones ANTES de guardar solicitudes; no usa datos de Railway. */
class SolicitudRegistroServiceTest {
    private val em = mock(EntityManager::class.java)
    private val config = mock(ConfiguracionServicioRepository::class.java)
    private val cambios = mock(CambioColaPublisher::class.java)
    private val servicio = SolicitudRegistroService(em, mock(TokenSeguro::class.java), config,
        mock(HorarioActivoService::class.java), mock(CancionRepository::class.java),
        mock(ClasificadorGeneroService::class.java), mock(SolicitudRepository::class.java),
        mock(VotoRepository::class.java), mock(JdbcTemplate::class.java), cambios)
    private val video = YoutubeVideoResponse("dQw4w9WgXcQ", "Canción de prueba", null, false, "Canal", 180, null, true,
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    private fun sesion(estado: EstadoSesion, activo: Boolean) = Sesion(id = 9L,
        establecimiento = Establecimiento(id = 3L, nombre = "Prueba", slug = "test", zonaHoraria = "America/Bogota",
            activo = activo, fechaCreacion = OffsetDateTime.now()),
        fechaApertura = OffsetDateTime.now(), estado = estado, indiceListaDefecto = 1)

    @Test fun `sesion inexistente rechaza solicitud antes de mutar repositorios`() {
        assertFailsWith<SesionNoEncontradaException> { servicio.registrar(999L, "token", video) }
        verifyNoInteractions(config, cambios)
    }
    @Test fun `sesion cerrada no acepta nuevas solicitudes`() {
        `when`(em.find(Sesion::class.java, 9L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(sesion(EstadoSesion.CERRADA, true))
        val e = assertFailsWith<ResponseStatusException> { servicio.registrar(9L, "token", video) }
        assertEquals(HttpStatus.CONFLICT, e.statusCode)
        verifyNoInteractions(config, cambios)
    }
    @Test fun `establecimiento inactivo no acepta solicitudes`() {
        `when`(em.find(Sesion::class.java, 9L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(sesion(EstadoSesion.ABIERTA, false))
        assertEquals(HttpStatus.CONFLICT,
            assertFailsWith<ResponseStatusException> { servicio.registrar(9L, "token", video) }.statusCode)
        verifyNoInteractions(config, cambios)
    }
    @Test fun `sesion activa sin configuracion rechaza operacion`() {
        `when`(em.find(Sesion::class.java, 9L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(sesion(EstadoSesion.ABIERTA, true))
        val e = assertFailsWith<ResponseStatusException> { servicio.registrar(9L, "token", video) }
        assertEquals(HttpStatus.CONFLICT, e.statusCode)
        verifyNoInteractions(cambios)
    }
}
