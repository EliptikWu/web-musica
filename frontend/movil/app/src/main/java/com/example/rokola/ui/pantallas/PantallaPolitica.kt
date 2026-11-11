package com.example.rokola.ui.pantallas

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.ui.componentes.MarcoCliente
import com.example.rokola.ui.componentes.TamanoTecla
import com.example.rokola.ui.componentes.Tecla
import com.example.rokola.ui.componentes.TituloDeSeccion
import com.example.rokola.ui.componentes.Ventana
import com.example.rokola.ui.textos.T
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.Tinta

private val SECCIONES = listOf(
    "Qué se registra" to "Un código anónimo que se crea en este teléfono para reconocerlo, las canciones que pides y los votos que das, con su fecha y hora.",
    "Qué no" to "No pedimos tu nombre, tu correo ni tu número de teléfono. No necesitas cuenta.",
    "Para qué" to "Para respetar tu cupo de pedidos, contar un solo voto por canción y saber qué música funciona en el bar.",
    "Si borras los datos de la app" to "El código se pierde y tu teléfono cuenta como nuevo.",
)

/**
 * Política de datos (RF-004, PB-06; Politica.tsx de la web): qué se registra, para qué y qué
 * no. El texto legal completo del bar (Ley 1581 de 2012) está pendiente y la pantalla lo dice.
 */
@Composable
fun PantallaPolitica(nombreBar: String?, onVolver: () -> Unit) {
    MarcoCliente(
        titulo = T.POLITICA,
        abajo = { Tecla("Volver", onVolver, tamano = TamanoTecla.GRANDE) },
    ) {
        if (nombreBar != null) Text(nombreBar, style = Cuerpo.copy(fontSize = 14.sp), color = Crema2)
        TituloPlaca(T.POLITICA, Modifier.padding(top = 4.dp))
        Ventana(
            Modifier.padding(top = 20.dp),
            relleno = PaddingValues(16.dp),
            alineacion = Alignment.Start,
        ) {
            SECCIONES.forEachIndexed { i, (titulo, texto) ->
                TituloDeSeccion(titulo, Modifier.padding(top = if (i == 0) 0.dp else 16.dp), color = Tinta)
                Text(texto, Modifier.padding(top = 4.dp), style = Cuerpo.copy(fontSize = 15.sp, lineHeight = 1.5.em), color = Tinta)
            }
        }
        Text(
            "Tratamiento conforme a la Ley 1581 de 2012. El texto legal completo del bar está pendiente de publicarse.",
            Modifier.padding(top = 16.dp),
            style = Cuerpo.copy(fontSize = 14.sp, lineHeight = 1.45.em),
            color = Crema2,
        )
    }
}
