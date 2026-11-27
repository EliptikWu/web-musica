package com.proyectoN.rokolaBackend.compat

import kotlin.test.*

/** CP-045: recuperación de sesión de panel (no sustituye un ensayo de reinicio E2E). */
class RecuperacionServicioTest {
    @Test fun `cerrar todas las sesiones de un usuario no afecta a otros`() {
        val sesiones = ContratoPanelSesiones(cookieSegura = true)
        val a = sesiones.crear(1)
        val b = sesiones.crear(1)
        val tercero = sesiones.crear(2)
        sesiones.cerrarDeUsuario(1)
        assertNull(sesiones.vigente(a))
        assertNull(sesiones.vigente(b))
        assertNotNull(sesiones.vigente(tercero))
    }
}
