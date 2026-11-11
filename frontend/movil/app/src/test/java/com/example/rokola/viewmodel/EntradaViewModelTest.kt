package com.example.rokola.viewmodel

import com.example.rokola.datos.simulacion.RepositorioSimulado
import com.example.rokola.dominio.Bar
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.Resultado
import com.example.rokola.tipoDispositivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntradaViewModelTest {

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private var guardado: String? = null

    /** La simulación, con el QR y los bares que ella conoce, más fallos a pedido. */
    private class Repo(
        private val base: RepositorioSimulado,
        var qr: Resultado<String>? = null,
        var barFalla: CodigoError? = null,
        var pedidos: MutableList<String?> = mutableListOf(),
    ) : RepositorioRockola by base {
        override suspend fun resolverQr(token: String) = qr ?: base.resolverQr(token)
        override suspend fun bar(slug: String?): Resultado<Bar> {
            pedidos += slug
            return barFalla?.let { Resultado.Fallo(ErrorRockola(it, "")) } ?: base.bar(slug)
        }
    }

    private fun crear(porDefecto: String? = null, repo: RepositorioRockola? = null) = EntradaViewModel(
        repositorio = repo ?: Repo(RepositorioSimulado(slugPedido = { guardado })),
        leerSlug = { guardado },
        guardarSlug = { guardado = it },
        barPorDefecto = porDefecto,
    )

    @Test
    fun `lee los enlaces del QR y del bar como la web`() {
        assertEquals(Enlace.Qr("demo-qr-1"), leerEnlace(listOf("q", "demo-qr-1")))
        assertEquals(Enlace.Bar("demo"), leerEnlace(listOf("b", "demo")))
        assertEquals(Enlace.Bar("demo"), leerEnlace(listOf("b", "demo", "cola")))
        assertNull(leerEnlace(listOf("q")))
        assertNull(leerEnlace(listOf("q", "a", "b")))
        assertNull(leerEnlace(listOf("panel")))
        assertNull(leerEnlace(emptyList()))
        // Fuera de forma: ni `..` (ya decodificado de %2E%2E) ni mayúsculas ni símbolos.
        assertNull(leerEnlace(listOf("b", "..")))
        assertNull(leerEnlace(listOf("q", "..")))
        assertNull(leerEnlace(listOf("b", "Demo")))
        assertNull(leerEnlace(listOf("q", "a b")))
    }

    @Test
    fun `comprueba el bar del enlace sin tocar el recordado hasta que existe`() {
        guardado = "demo"
        val repo = Repo(RepositorioSimulado(slugPedido = { guardado }))
        val vm = crear(repo = repo)
        vm.iniciar(Enlace.Bar("otro-bar"))
        assertEquals(listOf<String?>("otro-bar"), repo.pedidos)
        assertEquals("demo", guardado)
    }

    @Test
    fun `un QR que lleva a un slug fuera de forma no entra`() {
        val repo = Repo(RepositorioSimulado(slugPedido = { guardado }), qr = Resultado.Exito("../solicitudes"))
        val vm = crear(repo = repo)
        vm.iniciar(Enlace.Qr("cualquiera"))
        assertEquals(Entrada.BarNoEncontrado(puedeVolver = false), vm.estado.value)
        assertEquals(emptyList<String?>(), repo.pedidos)
    }

    @Test
    fun `volver a escanear el QR del bar actual no saca de sus pantallas`() {
        guardado = "demo"
        val vm = crear()
        vm.iniciar(null)
        vm.abrir(Enlace.Qr("demo-qr-1"))
        assertEquals(Entrada.Lista("demo"), vm.estado.value)
    }

    @Test
    fun `el QR lleva al bar y lo recuerda`() {
        val vm = crear()
        vm.iniciar(Enlace.Qr("demo-qr-1"))
        assertEquals(Entrada.Lista("demo"), vm.estado.value)
        assertEquals("demo", guardado)
    }

    @Test
    fun `sin enlace abre el bar recordado, o pide el QR`() {
        guardado = "demo"
        crear().also { it.iniciar(null) }.let { assertEquals(Entrada.Lista("demo"), it.estado.value) }
        guardado = null
        crear().also { it.iniciar(null) }.let { assertEquals(Entrada.SinBar, it.estado.value) }
        // En la simulación no hace falta QR.
        crear(porDefecto = "demo").also { it.iniciar(null) }.let { assertEquals(Entrada.Lista("demo"), it.estado.value) }
    }

    @Test
    fun `un QR reemplazado lo dice y deja volver al bar que había`() {
        guardado = "demo"
        val repo = Repo(RepositorioSimulado(slugPedido = { guardado }), qr = Resultado.Fallo(ErrorRockola(CodigoError.QR_REEMPLAZADO, "")))
        val vm = crear(repo = repo)
        vm.iniciar(Enlace.Qr("viejo"))
        assertEquals(Entrada.QrReemplazado(puedeVolver = true), vm.estado.value)
        vm.volver()
        assertEquals(Entrada.Lista("demo"), vm.estado.value)
    }

    @Test
    fun `un bar que no existe no reemplaza al recordado`() {
        guardado = "demo"
        val vm = crear()
        vm.iniciar(Enlace.Bar("otro-bar"))
        assertEquals(Entrada.BarNoEncontrado(puedeVolver = true), vm.estado.value)
        assertEquals("demo", guardado)
    }

    @Test
    fun `un QR desconocido sin bar recordado no ofrece volver`() {
        val vm = crear()
        vm.iniciar(Enlace.Qr("no-existe"))
        assertEquals(Entrada.BarNoEncontrado(puedeVolver = false), vm.estado.value)
    }

    @Test
    fun `sin conexión se puede reintentar el mismo enlace`() {
        val repo = Repo(RepositorioSimulado(slugPedido = { guardado }), barFalla = CodigoError.SIN_CONEXION)
        val vm = crear(repo = repo)
        vm.iniciar(Enlace.Bar("demo"))
        assertEquals(Entrada.Fallo(Enlace.Bar("demo")), vm.estado.value)
        assertNull("No se queda con un bar sin comprobar", guardado)
        repo.barFalla = null
        vm.reintentar()
        assertEquals(Entrada.Lista("demo"), vm.estado.value)
    }

    @Test
    fun `iniciar dos veces (girar el teléfono) no vuelve a resolver`() {
        val vm = crear()
        vm.iniciar(Enlace.Qr("demo-qr-1"))
        guardado = null
        vm.iniciar(Enlace.Qr("demo-qr-1"))
        assertEquals(Entrada.Lista("demo"), vm.estado.value)
        assertNull(guardado)
    }

    @Test
    fun `el tipo de teléfono sale del ancho, como en la web`() {
        assertEquals("movil", tipoDispositivo(360))
        assertEquals("tableta", tipoDispositivo(600))
        assertEquals("tableta", tipoDispositivo(1023))
        assertEquals("otro", tipoDispositivo(1280))
    }
}
