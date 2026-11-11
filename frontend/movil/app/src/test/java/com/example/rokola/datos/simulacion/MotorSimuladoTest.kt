package com.example.rokola.datos.simulacion

import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.ContextoDestacada
import com.example.rokola.dominio.EstadoServicio
import com.example.rokola.dominio.EstadoSolicitud
import com.example.rokola.dominio.MotivoNoPedible
import com.example.rokola.dominio.OrigenReproduccion
import com.example.rokola.dominio.Resultado
import com.example.rokola.dominio.TipoRespuestaSolicitud
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

/** Las mismas pruebas que apps/web/simulacion/motor.test.ts: la simulación da los mismos números. */
class MotorSimuladoTest {

    private val persona = "persona-0000"
    private val otro = "otro-telefono"
    private val real = 1_000_000L

    private fun escena(escenario: Escenario = Escenario.MOMENTO_B): Pair<EstadoSim, Long> {
        val estado = crearEstado(escenario, persona, real)
        return estado to ahoraSim(estado, real)
    }

    private fun <T> Resultado<T>.exito(): T = (this as? Resultado.Exito)?.valor ?: error("Se esperaba éxito y llegó $this")
    private fun Resultado<*>.codigo(): CodigoError? = (this as? Resultado.Fallo)?.error?.codigo

    @Test
    fun `momento B - cola de 9, Monaco y La Bicicleta empatadas, Monaco antes`() {
        val (estado, ahora) = escena()
        val foto = construirAhora(estado, ahora)
        assertEquals("Rebelión", foto.reproduccion?.cancion?.titulo)
        assertEquals(161, foto.reproduccion?.posicionSeg)
        assertEquals(24, foto.reproduccion?.votos)
        assertEquals(
            listOf(
                "Cali Pachanguero 21", "DtMF 18", "Monaco 15", "La Bicicleta 15", "NUEVAYoL 14", "TQG 9", "Luna 7",
                "Una aventura 4", "Tití Me Preguntó 2",
            ),
            foto.cola.map { "${it.cancion.titulo} ${it.votosTotal}" },
        )
        assertEquals(5, foto.cola.first { it.cancion.titulo == "Monaco" }.puestoAnterior)
        assertEquals(3, foto.cola.first { it.cancion.titulo == "DtMF" }.puestoAnterior)
        // RF-018: la primera espera lo que le falta a Rebelión; cada una, eso más lo que duran las de delante
        val rep = foto.reproduccion!!
        assertEquals(rep.cancion.duracionSeg - rep.posicionSeg, foto.cola[0].esperaSeg)
        foto.cola.zipWithNext { antes, despues -> assertEquals(antes.esperaSeg + antes.cancion.duracionSeg, despues.esperaSeg) }
        assertEquals(EstadoServicio.ABIERTO, foto.servicio.estado)
        assertEquals("Salsa y tropical de la casa", foto.listaCasa?.nombre)
        assertEquals(42, foto.listaCasa?.totalCanciones)
    }

    @Test
    fun `momento B - Monaco es suya, votó DtMF, le queda 1 de 3`() {
        val (estado, _) = escena()
        val yo = construirYo(estado, persona)
        assertEquals(1, yo.pedidosRestantes)
        assertEquals(
            listOf(Triple("Monaco", EstadoSolicitud.PENDIENTE, 3), Triple("Rebelión", EstadoSolicitud.REPRODUCIENDO, null)),
            yo.solicitudes.map { Triple(it.cancion.titulo, it.estado, it.puesto) },
        )
        assertTrue("s-dtmf" in yo.votos)
    }

    @Test
    fun `momento A - le quedan 2 de 3 y lo más votado es Rebelión, Hips Don't Lie y Cali Pachanguero`() {
        val (estado, _) = escena(Escenario.MOMENTO_A)
        assertEquals(2, construirYo(estado, persona).pedidosRestantes)
        assertEquals(
            listOf(
                Triple("Rebelión", 24, ContextoDestacada.SONANDO),
                Triple("Hips Don't Lie", 19, ContextoDestacada.SONO),
                Triple("Cali Pachanguero", 15, ContextoDestacada.EN_COLA),
            ),
            construirDestacadas(estado).map { Triple(it.cancion.titulo, it.votos, it.contexto) },
        )
    }

    @Test
    fun `momento A - bad bunny separa lo que ya está en la cola de los resultados`() {
        val (estado, ahora) = escena(Escenario.MOMENTO_A)
        val r = buscar(estado, "bad bunny", ahora).exito()
        assertEquals(listOf("NUEVAYoL", "DtMF"), r.enCola.map { it.cancion.titulo })
        val mix = r.resultados.first { it.cancion.titulo == "Mix Bad Bunny 2025" }
        assertEquals(false, mix.pedible)
        assertEquals(MotivoNoPedible.MUY_LARGA, mix.motivo)
        assertTrue(r.resultados.any { it.cancion.titulo == "Monaco" && it.pedible })
    }

    @Test
    fun `momento A - Monaco entra de cuarta, suena en unos 14 min y queda 1 de 3`() {
        val (estado, ahora) = escena(Escenario.MOMENTO_A)
        val r = pedirCancion(estado, persona, "sim-monaco", ahora).exito()
        assertEquals(TipoRespuestaSolicitud.CREADA, r.resultado)
        assertEquals(4, r.puesto)
        assertEquals(1, r.pedidosRestantes)
        assertEquals(14, r.esperaSeg / 60)
    }

    @Test
    fun `pedir una que ya está en la cola cuenta como voto y no gasta cupo`() {
        val (estado, ahora) = escena()
        assertEquals(TipoRespuestaSolicitud.VOTADA, pedirCancion(estado, otro, "sim-tqg", ahora).exito().resultado)
        assertEquals(3, construirYo(estado, otro).pedidosRestantes)
        assertEquals(CodigoError.YA_VOTASTE, pedirCancion(estado, otro, "sim-tqg", ahora).codigo())
    }

    @Test
    fun `con 3 pedidos en uso no deja pedir otro`() {
        val (estado, ahora) = escena()
        pedirCancion(estado, persona, "sim-provenza", ahora).exito()
        assertEquals(CodigoError.LIMITE_PEDIDOS, pedirCancion(estado, persona, "sim-classy-101", ahora).codigo())
    }

    @Test
    fun `rechaza la bloqueada, la larga y la que sonó hace poco, con su motivo`() {
        val (estado, ahora) = escena()
        assertEquals(CodigoError.BLOQUEADA, pedirCancion(estado, otro, "sim-gasolina", ahora).codigo())
        assertEquals(CodigoError.MUY_LARGA, pedirCancion(estado, otro, "sim-mix-bad-bunny-2025", ahora).codigo())
        val r = pedirCancion(estado, otro, "sim-rebelion", ahora) as Resultado.Fallo
        assertEquals(CodigoError.REPRODUCIDA_HACE_POCO, r.error.codigo)
        assertNotNull(r.error.disponibleDesde)
    }

    @Test
    fun `un voto que empata no adelanta, uno que desempata sube y queda anotado`() {
        val (estado, ahora) = escena()
        assertEquals(16, votar(estado, otro, "s-bici", ahora).exito().votosTotal)
        val bici = construirAhora(estado, ahora).cola.first { it.solicitudId == "s-bici" }
        assertEquals(3, bici.puesto)
        assertEquals(4, bici.puestoAnterior)
        assertEquals(CodigoError.YA_VOTASTE, votar(estado, otro, "s-bici", ahora).codigo())
    }

    @Test
    fun `en pausa no se pide ni se vota, pero la cola se ve`() {
        val (estado, ahora) = escena(Escenario.EN_PAUSA)
        assertEquals(EstadoServicio.EN_PAUSA, construirAhora(estado, ahora).servicio.estado)
        assertEquals(9, construirAhora(estado, ahora).cola.size)
        assertEquals(CodigoError.SERVICIO_NO_DISPONIBLE, votar(estado, otro, "s-bici", ahora).codigo())
    }

    @Test
    fun `al terminar Rebelión suena Cali Pachanguero, justo a su hora`() {
        val (estado, ahora) = escena()
        avanzar(estado, ahora + 197_000)
        val foto = construirAhora(estado, ahora + 197_000)
        assertEquals("Cali Pachanguero", foto.reproduccion?.cancion?.titulo)
        assertEquals(0, foto.reproduccion?.posicionSeg)
        assertEquals(8, foto.cola.size)
        assertEquals(EstadoSolicitud.REPRODUCIDA, construirYo(estado, persona).solicitudes.first { it.solicitudId == "s-rebelion" }.estado)
    }

    @Test
    fun `sin pedidos suena la lista de la casa y vuelve a los pedidos sin cortar la canción`() {
        val (estado, ahora) = escena(Escenario.COLA_VACIA)
        avanzar(estado, ahora + 197_000)
        construirAhora(estado, ahora + 197_000).reproduccion!!.let {
            assertEquals(OrigenReproduccion.LISTA_CASA, it.origen)
            assertEquals("Rebelión", it.cancion.titulo)
        }
        val t = ahora + 200_000
        pedirCancion(estado, otro, "sim-provenza", t).exito()
        assertEquals(OrigenReproduccion.LISTA_CASA, construirAhora(estado, t).reproduccion?.origen)
        avanzar(estado, ahora + 197_000 + 358_000)
        assertEquals("PROVENZA", construirAhora(estado, ahora + 197_000 + 358_000).reproduccion?.cancion?.titulo)
    }

    @Test
    fun `sin lista activa y sin pedidos no suena nada`() {
        val (estado, ahora) = escena(Escenario.SIN_LISTA_ACTIVA)
        avanzar(estado, ahora + 200_000)
        assertNull(construirAhora(estado, ahora + 200_000).reproduccion)
    }

    @Test
    fun `a las 2 a m se cierra la noche y lo pendiente expira`() {
        val (estado, ahora) = escena()
        val dos = OffsetDateTime.parse("2026-10-23T02:00:30-05:00").toInstant().toEpochMilli()
        assertTrue(ahora < dos)
        avanzar(estado, dos)
        assertNull(estado.sesion)
        assertEquals(EstadoServicio.FUERA_DE_HORARIO, construirAhora(estado, dos).servicio.estado)
        assertEquals(0, estado.solicitudes.count { it.estado == EstadoSolicitud.PENDIENTE })
    }

    @Test
    fun `fuera de horario abre sola a las 8 p m`() {
        val (estado, ahora) = escena(Escenario.FUERA_DE_HORARIO)
        val ocho = OffsetDateTime.parse("2026-10-22T20:00:10-05:00").toInstant().toEpochMilli()
        assertEquals(EstadoServicio.FUERA_DE_HORARIO, construirAhora(estado, ahora).servicio.estado)
        avanzar(estado, ocho)
        assertEquals("horario", estado.sesion?.origen)
        assertEquals(EstadoServicio.ABIERTO, construirAhora(estado, ocho).servicio.estado)
    }

    @Test
    fun `el catálogo y las listas son los de la web`() {
        val catalogo = crearCatalogo()
        assertEquals(59, catalogo.size)
        assertEquals(42, LISTA_SALSA_Y_TROPICAL.size)
        assertEquals(8, LISTA_VIERNES_CROSSOVER.size)
        assertTrue((LISTA_SALSA_Y_TROPICAL + LISTA_VIERNES_CROSSOVER).all { it in catalogo })
        assertEquals(4360, catalogo.getValue("sim-mix-bad-bunny-2025").duracionSeg)
    }
}
