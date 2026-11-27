package com.proyectoN.rokolaBackend.compat

import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import kotlin.test.*

/** CP-016/025: acceso al tablero exige rol administrador antes de consultar estadísticas. */
class AnaliticaAccesoTest {
    private val acceso = mock(ContratoPanelAcceso::class.java)
    private val historico = mock(ContratoAnaliticaService::class.java)
    private val vivo = mock(ContratoCuboVivoService::class.java)
    private val controlador = ContratoAnaliticaController(acceso, historico, vivo)
    @Test fun `tablero solicita permiso administrador en cada consulta`() {
        // Mockito retorna null por defecto: la solicitud de permiso se verifica ANTES de leer estadísticas.
        doThrow(ContratoApiException(HttpStatus.UNAUTHORIZED, "no_autenticado", "Debes ingresar"))
            .`when`(acceso).exigir(null, "administrador", true)
        assertFailsWith<ContratoApiException> { controlador.noches(null) }
        verify(acceso).exigir(null, "administrador", true)
        verifyNoInteractions(historico, vivo)
    }
}
