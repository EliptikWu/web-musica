package com.proyectoN.rokolaBackend.compat

import org.mockito.Mockito.*
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-023/046: escribe acciones con bar y usuario; no declara éxito si falla SQL. */
class BitacoraEscrituraTest {
    private val jdbc = mock(JdbcTemplate::class.java)
    private val bitacora = ContratoBitacora(jdbc)
    private val admin = ContratoPanelAcceso.Personal(18, "Ana", "ana@example.test", "administrador", true,
        false, 11, "bar-11", "Bar", "America/Bogota")

    @Test fun `escritura lleva el bar usuario tipo y objeto correctamente`() {
        bitacora.anotar(admin, "retirar", "cancion", 123L, texto = "Duplicada")
        val llamada = mockingDetails(jdbc).invocations.single { it.method.name == "update" }
        assertEquals(11L, llamada.arguments[1])
        assertEquals(18L, llamada.arguments[2])
        assertEquals("retirar", llamada.arguments[3])
        assertEquals("cancion", llamada.arguments[4])
        assertEquals(123L, llamada.arguments[5])
        assertEquals("Duplicada", llamada.arguments[6])
    }

    @Test fun `errores de persistencia de la bitacora no se silencian`() {
        val fallido = mock(JdbcTemplate::class.java, org.mockito.stubbing.Answer<Any?> {
            throw DataAccessResourceFailureException("simulacion-bd-caida")
        })
        assertFailsWith<DataAccessResourceFailureException> {
            ContratoBitacora(fallido).anotar(admin, "editar_lista", "lista", 15L, texto = "Cambio de prueba")
        }
    }
}
