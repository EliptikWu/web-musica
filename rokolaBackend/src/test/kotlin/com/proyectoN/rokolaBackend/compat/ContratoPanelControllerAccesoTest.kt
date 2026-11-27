package com.proyectoN.rokolaBackend.compat

import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-015/016/039: las rutas del panel no acceden a recursos sin cookie válida. */
class ContratoPanelControllerAccesoTest {
    private val sesiones = ContratoPanelSesiones(cookieSegura = true)
    private val acceso = ContratoPanelAcceso(mock(JdbcTemplate::class.java), sesiones)
    private val controller = ContratoPanelController(
        acceso, sesiones, mock(ContratoNocheService::class.java), mock(ContratoSaltoService::class.java),
        mock(ContratoRetiroService::class.java), mock(ContratoEstablecimientoService::class.java),
        mock(ContratoPausaService::class.java), mock(ContratoVolumenService::class.java))

    @Test fun `las rutas de panel protegidas responden sin autenticacion`() {
        val llamadas: List<() -> Any> = listOf(
            { controller.yo(null) },
            { controller.noche(null) },
            { controller.establecimiento(null) },
            { controller.editarEstablecimiento(null, mapOf("nombre" to "Invasor")) },
            { controller.retirar("1", null, null, mapOf("motivo" to "duplicada")) },
            { controller.pendiente(null) },
        )
        for (llamada in llamadas) {
            val e = assertFailsWith<ContratoApiException> { llamada() }
            assertEquals(HttpStatus.UNAUTHORIZED, e.status)
        }
    }

    @Test fun `cerrar sesion sin cookie es idempotente y borra la cookie`() {
        val r = controller.salir(null)
        assertEquals(HttpStatus.NO_CONTENT, r.statusCode)
        val header = r.headers.getFirst("Set-Cookie") ?: error("No hay cookie de salida")
        assertTrue("rockola_panel=" in header)
        assertTrue("Max-Age=0" in header)
        assertTrue("HttpOnly" in header)
    }
}
