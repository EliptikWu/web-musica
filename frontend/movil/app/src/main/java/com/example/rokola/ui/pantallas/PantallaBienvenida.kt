package com.example.rokola.ui.pantallas

import com.example.rokola.ui.theme.estiloCartel
import com.example.rokola.ui.theme.estiloEtiqueta
import com.example.rokola.ui.theme.comoCartel
import com.example.rokola.ui.theme.TituloSonando
import com.example.rokola.ui.theme.LocalDiseno
import com.example.rokola.ui.componentes.alineacionArco
import com.example.rokola.ui.componentes.Filas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.dominio.ContextoDestacada
import com.example.rokola.dominio.reglas.posicionActual
import com.example.rokola.ui.componentes.Arco
import com.example.rokola.ui.componentes.Avance
import com.example.rokola.ui.componentes.Insignia
import com.example.rokola.ui.componentes.MarcoCliente
import com.example.rokola.ui.componentes.TamanoTecla
import com.example.rokola.ui.componentes.TarjetaTitulo
import com.example.rokola.ui.componentes.Tecla
import com.example.rokola.ui.componentes.TituloDeSeccion
import com.example.rokola.ui.componentes.VarianteTecla
import com.example.rokola.ui.componentes.Ventana
import com.example.rokola.ui.componentes.Vacio
import com.example.rokola.ui.componentes.rememberAhoraServidor
import com.example.rokola.ui.textos.Conexion
import com.example.rokola.ui.textos.T
import com.example.rokola.ui.textos.formatoDuracion
import com.example.rokola.ui.textos.formatoHora
import com.example.rokola.ui.theme.Condensada
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.Tinta2
import com.example.rokola.viewmodel.Bienvenida
import com.example.rokola.viewmodel.EstadoPantalla

/**
 * Bienvenida (prototipo 1, PB-03, PB-04, PB-06): el nombre del bar, lo que suena dentro del
 * arco, lo más votado de la noche y el aviso de datos, que se registra como visto (RF-004).
 * Con los pedidos cerrados, la pantalla es la del prototipo 4.
 */
@Composable
fun PantallaBienvenida(
    estado: EstadoPantalla<Bienvenida>,
    onReintentar: () -> Unit,
    onAvisoMostrado: () -> Unit,
    onEntrar: () -> Unit,
    onVerQueSuena: () -> Unit,
    onPolitica: () -> Unit,
) {
    val datos = when (estado) {
        is EstadoPantalla.Contenido -> estado.datos
        is EstadoPantalla.Vacio -> estado.datos
        EstadoPantalla.Cargando -> return MarcoCliente(titulo = T.CARGANDO) { CargandoCliente() }
        is EstadoPantalla.Error -> return MarcoCliente(titulo = "Rockola") { FalloCarga(onReintentar) }
    }
    LaunchedEffect(datos.debeVerAviso) { if (datos.debeVerAviso) onAvisoMostrado() }

    if (!datos.aceptaPedidos) {
        MarcoCliente(
            apagada = true,
            sinConexion = !datos.enVivo,
            titulo = datos.bar.nombre,
            abajo = { EstadoCerradoAbajo(onVerQueSuena, onPolitica) },
        ) { EstadoCerradoArriba(datos.bar, datos.ahora) }
        return
    }

    val rep = datos.ahora.reproduccion
    val zona = datos.bar.zonaHoraria
    MarcoCliente(
        sinConexion = !datos.enVivo,
        titulo = datos.bar.nombre,
        abajo = {
            Text(
                buildAnnotatedString {
                    append(T.PRIVACIDAD_BREVE)
                    append(" ")
                    withLink(
                        LinkAnnotation.Clickable(
                            "politica",
                            TextLinkStyles(SpanStyle(color = Crema, fontWeight = FontWeight.W600, textDecoration = TextDecoration.Underline)),
                        ) { onPolitica() },
                    ) { append(T.POLITICA) }
                },
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                style = Cuerpo.copy(fontSize = 14.sp, lineHeight = 1.45.em),
                color = Crema2,
                textAlign = TextAlign.Center,
            )
            Tecla(T.ENTRAR, onEntrar, variante = VarianteTecla.CEREZA, tamano = TamanoTecla.GRANDE)
        },
    ) {
        CabeceraBar(datos.bar.nombre, if (datos.enVivo) T.EN_VIVO else Conexion.LUZ, encendida = datos.enVivo)

        // En Afiche, la cabecera y el arco son un solo bloque de tinta.
        Arco(Modifier.padding(top = if (LocalDiseno.current.esAfiche) 0.dp else 20.dp), descripcion = T.SUENA_AHORA) {
            Ventana(relleno = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                if (rep != null) {
                    val ahora = rememberAhoraServidor(datos.ahora.servidorAhora)
                    val posicion = posicionActual(rep, datos.ahora.servidorAhora, ahora)
                    Text(T.SUENA_AHORA.comoCartel(), style = estiloEtiqueta(Cuerpo.copy(fontSize = 13.sp, fontWeight = FontWeight.W600)), color = Tinta2)
                    Text(
                        rep.cancion.titulo.comoCartel(),
                        Modifier.padding(top = 4.dp).semantics {
                            heading()
                            contentDescription = rep.cancion.titulo
                        },
                        style = estiloCartel(TituloSonando, rep.cancion.titulo),
                        color = Tinta,
                        textAlign = alineacionArco(),
                    )
                    val diseno = LocalDiseno.current
                    Text(
                        rep.cancion.artista,
                        Modifier.padding(top = 4.dp),
                        style = Cuerpo.copy(fontSize = if (diseno.esAfiche) 18.sp else 16.sp, fontWeight = if (diseno.esAfiche) FontWeight.W700 else FontWeight.W400),
                        color = if (diseno.esVidrio) Tinta2 else Tinta,
                        textAlign = alineacionArco(),
                    )
                    if (rep.votos != null) {
                        Insignia(if (rep.votos == 1) "1 voto" else "${rep.votos} votos", Modifier.padding(top = 8.dp))
                    } else {
                        Text("De la lista de la casa", Modifier.padding(top = 8.dp), style = Cuerpo.copy(fontSize = 13.sp, fontWeight = FontWeight.W600), color = Tinta2)
                    }
                    Avance(
                        fraccion = (posicion / maxOf(1, rep.cancion.duracionSeg)).toFloat(),
                        textoInicio = formatoDuracion(posicion),
                        textoFin = formatoDuracion(rep.cancion.duracionSeg),
                        modifier = Modifier.padding(top = 10.dp),
                    )
                } else {
                    Text(T.NADA_SUENA, style = Condensada.copy(fontSize = 32.sp, lineHeight = 1.em), color = Tinta, textAlign = alineacionArco())
                    Text(T.NADA_SUENA_META, Modifier.padding(top = 8.dp), style = Cuerpo.copy(fontSize = 15.sp), color = Tinta2, textAlign = alineacionArco())
                }
            }
        }

        TituloDeSeccion(T.LO_MAS_VOTADO, Modifier.padding(top = 24.dp, bottom = 12.dp).fillMaxWidth(), alineacion = TextAlign.Center)
        if (datos.destacadas.isEmpty()) {
            Vacio(T.SIN_VOTOS_AUN)
        } else {
            Filas(datos.destacadas.withIndex().toList()) { (i, d) ->
                val contexto = when {
                    d.contexto == ContextoDestacada.SONANDO -> "suena ahora"
                    d.contexto == ContextoDestacada.SONO && d.hora != null -> "sonó a las ${formatoHora(d.hora, zona)}"
                    else -> "en la cola"
                }
                TarjetaTitulo(
                    titulo = d.cancion.titulo,
                    artista = "${d.cancion.artista}, $contexto",
                    modifier = Modifier.fillMaxWidth(),
                    puesto = i + 1,
                    votos = d.votos,
                    miniatura = d.cancion.miniaturaUrl,
                )
            }
        }
    }
}
