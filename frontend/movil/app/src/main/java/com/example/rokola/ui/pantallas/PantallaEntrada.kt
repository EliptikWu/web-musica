package com.example.rokola.ui.pantallas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.ui.componentes.Arco
import com.example.rokola.ui.componentes.MarcoCliente
import com.example.rokola.ui.componentes.TamanoTecla
import com.example.rokola.ui.componentes.Tecla
import com.example.rokola.ui.componentes.VarianteTecla
import com.example.rokola.ui.componentes.Ventana
import com.example.rokola.ui.componentes.madera
import com.example.rokola.ui.textos.T
import com.example.rokola.ui.textos.TextosEntrada
import com.example.rokola.ui.textos.TituloMeta
import com.example.rokola.ui.theme.Condensada
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.Tinta2
import com.example.rokola.viewmodel.Entrada

/**
 * Lo que se ve antes de entrar a un bar (apps/web/components/comun/AvisoPantalla.tsx): el arco
 * apagado con el mensaje y, abajo, la única salida que tenga sentido.
 */
@Composable
fun PantallaEntrada(estado: Entrada, onReintentar: () -> Unit, onVolver: () -> Unit) {
    when (estado) {
        is Entrada.Lista -> Unit
        Entrada.Abriendo -> Abriendo()
        Entrada.SinBar -> PantallaAviso(TextosEntrada.sinBar)
        is Entrada.QrReemplazado -> PantallaAviso(TextosEntrada.reemplazado, volverSi(estado.puedeVolver, onVolver))
        is Entrada.BarNoEncontrado -> PantallaAviso(TextosEntrada.noEncontrado, volverSi(estado.puedeVolver, onVolver))
        is Entrada.Fallo -> PantallaAviso(TextosEntrada.fallo, T.REINTENTAR to onReintentar)
    }
}

private fun volverSi(puede: Boolean, onVolver: () -> Unit): Pair<String, () -> Unit>? =
    if (puede) TextosEntrada.VOLVER to onVolver else null

/** Mientras se resuelve el QR: una línea, sin esqueleto (dura menos de un segundo). */
@Composable
private fun Abriendo() {
    Box(Modifier.fillMaxSize().madera(apagada = true).padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            TextosEntrada.ABRIENDO,
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = Cuerpo.copy(fontSize = 15.sp),
            color = Crema2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PantallaAviso(textos: TituloMeta, accion: Pair<String, () -> Unit>? = null) {
    MarcoCliente(
        apagada = true,
        titulo = textos.titulo,
        abajo = accion?.let { (texto, alPulsar) -> { Tecla(texto, alPulsar, variante = VarianteTecla.CEREZA, tamano = TamanoTecla.GRANDE) } },
    ) {
        Arco(
            Modifier.padding(top = 26.dp),
            apagado = true,
            relleno = PaddingValues(start = 18.dp, end = 18.dp, top = 104.dp, bottom = 18.dp),
            descripcion = "Aviso",
        ) {
            Ventana(relleno = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp)) {
                Text(
                    textos.titulo,
                    Modifier.fillMaxWidth().semantics { heading() },
                    style = Condensada.copy(fontSize = 34.sp, lineHeight = 0.98.em),
                    color = Tinta,
                )
                Text(
                    textos.meta,
                    Modifier.padding(top = 10.dp),
                    style = Cuerpo.copy(fontSize = 15.sp, lineHeight = 1.4.em),
                    color = Tinta2,
                )
            }
        }
    }
}
