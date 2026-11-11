package com.example.rokola.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rokola.ui.componentes.PieCliente
import com.example.rokola.ui.componentes.Pestana
import com.example.rokola.viewmodel.BienvenidaViewModel
import com.example.rokola.viewmodel.BuscarViewModel
import com.example.rokola.viewmodel.ColaViewModel
import com.example.rokola.viewmodel.PieViewModel

enum class Pantalla { INICIO, BUSCAR, COLA, POLITICA }

/**
 * Las pantallas del cliente y cómo se pasa de una a otra: Inicio, Buscar y Cola por las
 * pestañas del pie; la política se abre encima y «atrás» vuelve a donde estaba.
 */
@Composable
fun AppRockola(inicial: Pantalla = Pantalla.INICIO) {
    val bienvenidaVm: BienvenidaViewModel = viewModel(factory = BienvenidaViewModel.Fabrica)
    val buscarVm: BuscarViewModel = viewModel(factory = BuscarViewModel.Fabrica)
    val colaVm: ColaViewModel = viewModel(factory = ColaViewModel.Fabrica)
    val pieVm: PieViewModel = viewModel(factory = PieViewModel.Fabrica)

    var pantalla by rememberSaveable { mutableStateOf(inicial) }
    var antesDePolitica by rememberSaveable { mutableStateOf(Pantalla.INICIO) }

    BackHandler(enabled = pantalla != Pantalla.INICIO) {
        pantalla = if (pantalla == Pantalla.POLITICA) antesDePolitica else Pantalla.INICIO
    }

    val bienvenida by bienvenidaVm.estado.collectAsStateWithLifecycle()
    val pie by pieVm.estado.collectAsStateWithLifecycle()

    val abrirPolitica = { antesDePolitica = pantalla; pantalla = Pantalla.POLITICA }
    val ir = { p: Pestana ->
        pantalla = when (p) {
            Pestana.INICIO -> Pantalla.INICIO
            Pestana.BUSCAR -> Pantalla.BUSCAR
            Pestana.COLA -> Pantalla.COLA
        }
    }
    val piePara = { p: Pestana -> @Composable { PieCliente(pie, p, ir) } }

    when (pantalla) {
        Pantalla.INICIO -> PantallaBienvenida(
            estado = bienvenida,
            onReintentar = bienvenidaVm::reintentar,
            onAvisoMostrado = bienvenidaVm::avisoMostrado,
            onEntrar = { pantalla = Pantalla.BUSCAR },
            onVerQueSuena = { pantalla = Pantalla.COLA },
            onPolitica = abrirPolitica,
        )
        Pantalla.BUSCAR -> {
            val buscar by buscarVm.estado.collectAsStateWithLifecycle()
            PantallaBuscar(
                estado = buscar,
                bienvenida = bienvenida,
                avisos = buscarVm.avisos,
                onTexto = buscarVm::cambiarTexto,
                onReintentar = buscarVm::reintentar,
                onFiltros = buscarVm::cambiarFiltros,
                onPedir = buscarVm::pedir,
                onVotar = buscarVm::votar,
                onVerCola = { pantalla = Pantalla.COLA },
                onPolitica = abrirPolitica,
                pie = piePara(Pestana.BUSCAR),
            )
        }
        Pantalla.COLA -> {
            val cola by colaVm.estado.collectAsStateWithLifecycle()
            PantallaCola(
                estado = cola,
                avisos = colaVm.avisos,
                onReintentar = colaVm::reintentar,
                onVotar = colaVm::votar,
                onMover = colaVm::moverVoto,
                destinos = colaVm::destinosParaMoverVoto,
                onPedirUna = { pantalla = Pantalla.BUSCAR },
                onPolitica = abrirPolitica,
                pie = piePara(Pestana.COLA),
            )
        }
        Pantalla.POLITICA -> PantallaPolitica(
            nombreBar = (bienvenida as? com.example.rokola.viewmodel.EstadoPantalla.Contenido)?.datos?.bar?.nombre
                ?: (bienvenida as? com.example.rokola.viewmodel.EstadoPantalla.Vacio)?.datos?.bar?.nombre,
            onVolver = { pantalla = antesDePolitica },
        )
    }
}
