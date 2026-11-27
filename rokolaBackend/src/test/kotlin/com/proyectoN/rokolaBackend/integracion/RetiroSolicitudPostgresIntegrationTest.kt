package com.proyectoN.rokolaBackend.integracion

import com.proyectoN.rokolaBackend.compat.*
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.util.UUID
import kotlin.test.*

/** CP-022/035/037/040: transición real de estado y votos; repetición de Idempotency-Key.
 * No requiere reproductor ni navegador. Nunca se habilita sobre Railway.
 */
@EnabledIfEnvironmentVariable(named = "ROKOLA_TEST_DB_URL", matches = ".+")
class RetiroSolicitudPostgresIntegrationTest {
    private fun sqlNumero(d: PostgresLocalFixture.Datos, sql: String, vararg args: Any): Int =
        d.conexion.prepareStatement(sql).use { stmt ->
            args.forEachIndexed { i, v -> stmt.setObject(i + 1, v) }
            stmt.executeQuery().use { rs -> assertTrue(rs.next()); rs.getInt(1) }
        }

    private fun administrador(bar: Long) = ContratoPanelAcceso.Personal(
        19, "Admin QA", "admin@example.test", "administrador", true, false,
        bar, "qa-bar", "Bar QA", "America/Bogota")

    @Test fun `retiro cambia estado desactiva voto crea auditoria y acepta reintento sin repetir efectos`() {
        PostgresLocalFixture.conDatos { d ->
            PostgresLocalFixture.ejecutar(d.conexion,
                "INSERT INTO public.voto(id_solicitud,id_dispositivo,activo) VALUES (?,?,TRUE)",
                d.solicitud, d.dispositivo1)
            val bitacora = mock(ContratoBitacora::class.java)
            val cola = mock(CambioColaPublisher::class.java)
            val panel = mock(CambioPanelPublisher::class.java)
            val servicio = ContratoRetiroService(JdbcTemplate(SingleConnectionDataSource(d.conexion, true)), bitacora, cola, panel)
            val personal = administrador(d.bar)
            val key = UUID.randomUUID().toString()
            val cuerpo = mapOf<String, Any?>("motivo" to "duplicada")
            servicio.retirar(personal, d.solicitud.toString(), cuerpo, key)
            assertEquals(1, sqlNumero(d, "SELECT COUNT(*) FROM public.solicitud WHERE id_solicitud=? AND estado='retirada'", d.solicitud))
            assertEquals(0, sqlNumero(d, "SELECT COUNT(*) FROM public.voto WHERE id_solicitud=? AND activo=TRUE", d.solicitud))
            assertEquals(1, sqlNumero(d, "SELECT COUNT(*) FROM public.api_idempotencia WHERE id_establecimiento=?", d.bar))
            servicio.retirar(personal, d.solicitud.toString(), cuerpo, key)
            assertEquals(1, sqlNumero(d, "SELECT COUNT(*) FROM public.api_idempotencia WHERE id_establecimiento=?", d.bar))
            assertEquals(1, mockingDetails(bitacora).invocations.count { it.method.name == "anotar" })
            assertEquals(1, mockingDetails(cola).invocations.count { it.method.name == "publicar" })
        }
    }

    @Test fun `mismo idempotency key con otro motivo da conflicto`() {
        PostgresLocalFixture.conDatos { d ->
            val servicio = ContratoRetiroService(JdbcTemplate(SingleConnectionDataSource(d.conexion, true)),
                mock(ContratoBitacora::class.java), mock(CambioColaPublisher::class.java), mock(CambioPanelPublisher::class.java))
            val admin = administrador(d.bar)
            val key = UUID.randomUUID().toString()
            servicio.retirar(admin, d.solicitud.toString(), mapOf("motivo" to "duplicada"), key)
            val e = assertFailsWith<ContratoApiException> {
                servicio.retirar(admin, d.solicitud.toString(), mapOf("motivo" to "inapropiada"), key)
            }
            assertEquals(HttpStatus.CONFLICT, e.status)
            assertEquals(1, sqlNumero(d, "SELECT COUNT(*) FROM public.api_idempotencia WHERE id_establecimiento=?", d.bar))
        }
    }
}
