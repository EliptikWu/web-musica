package com.proyectoN.rokolaBackend.compat

import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-023/046: la bitácora debe validar filtros y reconocer acciones administrativas. */
class BitacoraServiceTest {
    private val bitacora = ContratoBitacora(mock(JdbcTemplate::class.java))
    @Test fun `rechaza fechas usuario tipo y cursor invalidos`() {
        assertFailsWith<ContratoApiException> { bitacora.pagina(1, "ayer", null, null, null, null) }
        assertFailsWith<ContratoApiException> { bitacora.pagina(1, null, "mal", null, null, null) }
        assertFailsWith<ContratoApiException> { bitacora.pagina(1, null, null, "abc", null, null) }
        assertFailsWith<ContratoApiException> { bitacora.pagina(1, null, null, null, "accion_desconocida", null) }
        assertFailsWith<ContratoApiException> { bitacora.pagina(1, null, null, null, null, "-1") }
    }
    @Test fun `incluye operaciones sensibles en catalogo de auditoria`() {
        assertTrue(setOf("retirar", "editar_categoria", "desactivar_usuario", "mantenimiento", "editar_horario", "regenerar_qr")
            .all { it in ContratoBitacora.TIPOS_ACCION })
    }
}
