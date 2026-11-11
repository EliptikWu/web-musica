package com.example.rokola.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rokola.datos.simulacion.Escenario
import com.example.rokola.datos.simulacion.RepositorioSimulado
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.EstadoServicio
import com.example.rokola.dominio.reglas.EstadoTecla
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Los ViewModel sobre la simulación, con el tiempo de prueba: el reloj de la simulación
 * queda quieto (siempre 10:45 p. m.) para que los números sean los del prototipo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() = Dispatchers.setMain(despachador)

    @After
    fun despues() {
        almacen.clear()
        Dispatchers.resetMain()
    }

    private val almacen = ViewModelStore()

    private var creados = 0

    /** Cada ViewModel, nuevo y en un almacén, para poder cerrarlo (y su tiempo real) al final. */
    private inline fun <reified VM : ViewModel> crear(noinline fabrica: () -> VM): VM =
        ViewModelProvider.create(almacen, viewModelFactory { initializer { fabrica() } }).get("vm-${creados++}", VM::class)

    /**
     * Cierra los ViewModel aunque la prueba falle: si no, su tiempo real sigue programando
     * esperas y runTest no termina nunca en lugar de reportar el fallo.
     */
    private fun prueba(cuerpo: suspend TestScope.() -> Unit): TestResult = runTest(despachador) {
        try {
            cuerpo()
        } finally {
            almacen.clear()
        }
    }

    private fun repositorio(escenario: Escenario = Escenario.MOMENTO_B) =
        RepositorioSimulado(escenario, dispositivo = "persona", relojReal = { 0L })

    /** Deja correr lo pendiente sin entrar en el bucle del tiempo real. */
    private fun TestScope.asentar() {
        runCurrent()
        advanceTimeBy(10)
        runCurrent()
    }

    @Test
    fun `la cola empieza cargando y llega con el orden y las teclas del prototipo`() = prueba {
        val vm = crear { ColaViewModel(repositorio()) }
        assertEquals(EstadoPantalla.Cargando, vm.estado.value)
        asentar()
        val cola = (vm.estado.value as EstadoPantalla.Contenido).datos
        assertEquals("Cali Pachanguero", cola.filas.first().item.cancion.titulo)
        assertEquals(9, cola.filas.size)
        // La persona pidió Monaco y votó DtMF (la semilla de la web).
        assertEquals(EstadoTecla.TUYA, cola.filas.first { it.item.solicitudId == "s-monaco" }.tecla)
        assertEquals(EstadoTecla.VOTASTE, cola.filas.first { it.item.solicitudId == "s-dtmf" }.tecla)
        assertEquals("s-monaco", cola.tuCancion?.item?.solicitudId)
        assertEquals(2, cola.tuCancion?.subio)
        assertEquals("Monaco", cola.desempate?.antes?.cancion?.titulo)
        assertEquals(1, cola.pedidosRestantes)
    }

    @Test
    fun `votar hunde la tecla y reordena al instante`() = prueba {
        val vm = crear { ColaViewModel(repositorio()) }
        asentar()
        vm.votar("s-bici")
        // Antes de que responda el servidor: ya subió de 4 a 3.
        val optimista = (vm.estado.value as EstadoPantalla.Contenido).datos
        val bici = optimista.filas.first { it.item.solicitudId == "s-bici" }
        assertEquals(EstadoTecla.VOTASTE, bici.tecla)
        assertEquals(3, bici.item.puesto)
        assertTrue(bici.ocupada)
        asentar()
        val final = (vm.estado.value as EstadoPantalla.Contenido).datos.filas.first { it.item.solicitudId == "s-bici" }
        assertEquals(16, final.item.votosTotal)
        assertEquals(false, final.ocupada)
    }

    @Test
    fun `cola vacía es estado vacío y sigue mostrando la lista de la casa`() = prueba {
        val vm = crear { ColaViewModel(repositorio(Escenario.COLA_VACIA)) }
        asentar()
        val vacia = vm.estado.value as EstadoPantalla.Vacio
        assertEquals("Salsa y tropical de la casa", vacia.datos.listaCasa?.nombre)
    }

    @Test
    fun `sin conexión en vivo la cola se ve igual pero dice que no está en vivo`() = prueba {
        val vm = crear { ColaViewModel(repositorio(Escenario.DESCONEXION)) }
        asentar()
        val cola = (vm.estado.value as EstadoPantalla.Contenido).datos
        assertEquals(false, cola.enVivo)
        assertEquals(9, cola.filas.size)
    }

    @Test
    fun `en pausa no se puede votar`() = prueba {
        val vm = crear { ColaViewModel(repositorio(Escenario.EN_PAUSA)) }
        asentar()
        val cola = (vm.estado.value as EstadoPantalla.Contenido).datos
        assertEquals(EstadoServicio.EN_PAUSA, cola.servicio.estado)
        assertEquals(false, cola.puedeVotar)
        vm.votar("s-bici")
        asentar()
        assertEquals(15, (vm.estado.value as EstadoPantalla.Contenido).datos.filas.first { it.item.solicitudId == "s-bici" }.item.votosTotal)
    }

    @Test
    fun `buscar espera 2 letras y 300 ms`() = prueba {
        val vm = crear { BuscarViewModel(repositorio(Escenario.MOMENTO_A)) }
        asentar()
        vm.cambiarTexto("b")
        asentar()
        assertNull(vm.estado.value.resultados)
        vm.cambiarTexto("bad bunny")
        advanceTimeBy(BuscarViewModel.ESPERA_TECLAS_MS - 50)
        runCurrent()
        assertNull(vm.estado.value.resultados)
        advanceTimeBy(100)
        runCurrent()
        val r = (vm.estado.value.resultados as EstadoPantalla.Contenido).datos
        assertEquals(listOf("NUEVAYoL", "DtMF"), r.enCola.map { it.item.cancion.titulo })
    }

    @Test
    fun `sin resultados es vacío`() = prueba {
        val vacio = crear { BuscarViewModel(repositorio()) }
        asentar()
        vacio.cambiarTexto("zzzz qqqq")
        advanceTimeBy(400)
        runCurrent()
        assertTrue(vacio.estado.value.resultados is EstadoPantalla.Vacio)
    }

    @Test
    fun `YouTube caído es error`() = prueba {
        val caido = crear { BuscarViewModel(repositorio(Escenario.YOUTUBE_CAIDO)) }
        asentar()
        caido.cambiarTexto("monaco")
        advanceTimeBy(400)
        runCurrent()
        assertEquals(CodigoError.YOUTUBE_NO_DISPONIBLE, (caido.estado.value.resultados as EstadoPantalla.Error).error.codigo)
    }

    @Test
    fun `la bienvenida llega con el bar, lo que suena y lo más votado`() = prueba {
        val vm = crear { BienvenidaViewModel(repositorio(Escenario.MOMENTO_A)) }
        asentar()
        val b = (vm.estado.value as EstadoPantalla.Contenido).datos
        assertEquals("Bar de demostración", b.bar.nombre)
        assertEquals("Rebelión", b.ahora.reproduccion?.cancion?.titulo)
        assertEquals(3, b.destacadas.size)
        assertEquals(2, b.yo.pedidosRestantes)
        assertEquals(true, b.aceptaPedidos)
    }
}
