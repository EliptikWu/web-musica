package com.proyectoN.rokolaBackend.compat

import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-025/032/033: límites del contrato OLAP; conteos SQL requieren Postgres aislado. */
class CuboVivoIntegrationTest {
    private val cubo = ContratoCuboVivoService(mock(JdbcTemplate::class.java))
    @Test fun `solo admite eje dia o evento para evitar sql arbitrario`() {
        for (eje in listOf("otro", "dia; DROP TABLE cancion", "franja", "evento'")) {
            val e = assertFailsWith<ContratoApiException> { cubo.cubo(1, eje) }
            assertEquals(HttpStatus.BAD_REQUEST, e.status)
            assertEquals("datos_invalidos", e.codigo)
        }
    }
}
