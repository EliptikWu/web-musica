package com.proyectoN.rokolaBackend.compat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContratoPanelSesionesTest {
    private val sesiones = ContratoPanelSesiones(cookieSegura = true)

    @Test
    fun `un token nuevo identifica a su usuario`() {
        val token = sesiones.crear(7L)
        assertEquals(7L, assertNotNull(sesiones.vigente(token)).usuarioId)
    }

    @Test
    fun `cada sesion tiene un token distinto`() {
        assertNotEquals(sesiones.crear(1L), sesiones.crear(1L))
    }

    @Test
    fun `un token inventado, vacio o enorme no vale`() {
        assertNull(sesiones.vigente("inventado"))
        assertNull(sesiones.vigente(""))
        assertNull(sesiones.vigente(null))
        assertNull(sesiones.vigente("x".repeat(500)))
    }

    @Test
    fun `salir invalida el token`() {
        val token = sesiones.crear(3L)
        sesiones.cerrar(token)
        assertNull(sesiones.vigente(token))
    }

    @Test
    fun `la cookie es httpOnly, segura, Lax y de todo el sitio`() {
        val cookie = sesiones.cookie("abc").toString()
        assertTrue(cookie.startsWith("rockola_panel=abc"))
        listOf("HttpOnly", "Secure", "SameSite=Lax", "Path=/", "Max-Age=43200").forEach { assertTrue(it in cookie, it) }
        assertTrue("Max-Age=0" in sesiones.cookieBorrada().toString())
    }

    @Test
    fun `el codigo del telefono son 4 letras sin caracteres confusos y estable`() {
        val huella = "a3f9c2e1b0" + "0".repeat(54)
        val codigo = ContratoNocheService.codigoTelefono(huella)
        assertEquals(4, codigo.length)
        assertEquals(codigo, ContratoNocheService.codigoTelefono(huella))
        assertFalse(codigo.any { it in "01IO" })
        assertNotEquals(codigo, ContratoNocheService.codigoTelefono("b" + huella.drop(1)))
    }
}
