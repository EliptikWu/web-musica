package com.example.rokola.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.rokola.dominio.TemaBar

/*
 * Los tres diseños del bar (RF-059, ADR-25), 1:1 con los bloques `[data-tema]` de
 * packages/ui/src/tokens.css. Como en la web, los nombres de Rockola funcionan como papeles:
 * `crema` es el texto principal, `papel` la superficie de contenido, `cereza` la acción y `ambar`
 * «lo tuyo». Cada tema les da sus valores y los componentes los leen de LocalDiseno.
 */

/** Los valores que cambian con el tema. Sin fuentes, para poder medir el contraste en una prueba. */
@Immutable
data class Paleta(
    val crema: Color,
    val crema2: Color,
    val papel: Color,
    val papelApagado: Color,
    val tinta: Color,
    val tinta2: Color,
    val grisApagado: Color,
    val cereza: Color,
    val ambar: Color,
    val hueco: Color,
    val borde: Color,
    val maderaClara: Color,
    /** El fondo liso de la pantalla, debajo de la veta o las manchas. */
    val fondo: Color,
    val pista: Color,
    val errorFondo: Color,
    val sobreError: Color,
    val bannerFondo: Color,
    val bannerTexto: Color,
    /** El material de la luz: tubos en Rockola, degradado en Vidrio, tinta partida en Afiche. */
    val luz: Brush,
)

val PaletaRockola = Paleta(
    crema = CremaBase,
    crema2 = Crema2Base,
    papel = PapelBase,
    papelApagado = PapelApagadoBase,
    tinta = TintaBase,
    tinta2 = Tinta2Base,
    grisApagado = GrisApagadoBase,
    cereza = CerezaBase,
    ambar = AmbarBase,
    hueco = HuecoBase,
    borde = BordeBase,
    maderaClara = MaderaClaraBase,
    fondo = Nogal,
    pista = TintaBase.copy(alpha = 0.25f),
    errorFondo = CerezaBase,
    sobreError = CremaBase,
    bannerFondo = CerezaBase,
    bannerTexto = CremaBase,
    luz = Luz,
)

/** Valores propios de Vidrio (prototipos/vidrio/estilos.css). */
object TonosVidrio {
    val Carbon = Color(0xFF121016)
    val Durazno = Color(0xFFF6B29A)
    val Rosa = Color(0xFFE07FAE)
    val Blanco = Color(0xFFFFFFFF)
    val Superficie = Blanco.copy(alpha = 0.08f)
    val SuperficieAlta = Blanco.copy(alpha = 0.12f)
    val TeclaFondo = Blanco.copy(alpha = 0.14f)
    val TeclaBorde = Blanco.copy(alpha = 0.2f)
    val BordeVidrio = Blanco.copy(alpha = 0.13f)
    val Separador = Blanco.copy(alpha = 0.09f)
    val Pista = Blanco.copy(alpha = 0.16f)
    val TuyaBorde = Blanco.copy(alpha = 0.38f)
    val Hoy = Blanco.copy(alpha = 0.7f)
    val Apagado = Blanco.copy(alpha = 0.45f)
    val Subio = Blanco.copy(alpha = 0.42f)
    val Texto2 = Blanco.copy(alpha = 0.74f)
    val Votado = Color(red = 168, green = 148, blue = 238).copy(alpha = 0.22f)
    val VotadoBorde = Color(red = 196, green = 182, blue = 255).copy(alpha = 0.45f)
    val Velo = Carbon.copy(alpha = 0.86f)
    val Halo = Durazno.copy(alpha = 0.22f)
    val SombraAcento = Color(red = 229, green = 139, blue = 176).copy(alpha = 0.7f)
    val TintaVidrio = Color(0xFF2A1230)
    val Tinta2Vidrio = TintaVidrio.copy(alpha = 0.84f)
    val ParadasAcento = listOf(Color(0xFFF6B29A), Color(0xFFE58BB0), Color(0xFFA894EE))
    val Acento: Brush = Brush.linearGradient(
        0f to ParadasAcento[0],
        0.52f to ParadasAcento[1],
        1f to ParadasAcento[2],
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}

val PaletaVidrio = Paleta(
    crema = TonosVidrio.Blanco,
    crema2 = TonosVidrio.Texto2,
    papel = TonosVidrio.Superficie,
    papelApagado = TonosVidrio.Blanco.copy(alpha = 0.04f),
    tinta = TonosVidrio.Blanco,
    tinta2 = TonosVidrio.Texto2,
    grisApagado = TonosVidrio.Apagado,
    cereza = TonosVidrio.Rosa,
    ambar = TonosVidrio.Durazno,
    hueco = TonosVidrio.Blanco.copy(alpha = 0.06f),
    borde = TonosVidrio.BordeVidrio,
    maderaClara = TonosVidrio.SuperficieAlta,
    fondo = TonosVidrio.Carbon,
    pista = TonosVidrio.Pista,
    errorFondo = TonosVidrio.SuperficieAlta,
    sobreError = TonosVidrio.Blanco,
    bannerFondo = TonosVidrio.Velo,
    bannerTexto = TonosVidrio.Blanco,
    luz = TonosVidrio.Acento,
)

/** Valores propios de Afiche (prototipos/afiche/estilos.css). */
object TonosAfiche {
    val PapelAfiche = Color(0xFFECE9E1)
    val PapelHondo = Color(0xFFE2DED4)
    val PapelClaro = Color(0xFFF7F5F0)
    val TintaAfiche = Color(0xFF17120F)
    val Tinta2Afiche = Color(0xFF4E4741)
    val Punteado = TintaAfiche.copy(alpha = 0.35f)
    val Amarillo = Color(0xFFFFE600)
    val Rosa = Color(0xFFFF2E93)
    val Sello = Color(0xFFB0105D)
    val PapelSobreTinta = PapelAfiche.copy(alpha = 0.78f)
    val ParadasPartida = listOf(Color(0xFFFFE600), Color(0xFFFFB02E), Color(0xFFFF7A2F), Color(0xFFFF2E93))
    val Partida: Brush = Brush.horizontalGradient(
        0f to ParadasPartida[0],
        0.30f to ParadasPartida[1],
        0.52f to ParadasPartida[2],
        1f to ParadasPartida[3],
    )
    val ResaltadorFila = Amarillo.copy(alpha = 0.32f)
    val SombraRosa = Rosa.copy(alpha = 0.55f)
}

val PaletaAfiche = Paleta(
    crema = TonosAfiche.TintaAfiche,
    crema2 = TonosAfiche.Tinta2Afiche,
    papel = TonosAfiche.PapelAfiche,
    papelApagado = TonosAfiche.PapelHondo,
    tinta = TonosAfiche.TintaAfiche,
    tinta2 = TonosAfiche.Tinta2Afiche,
    grisApagado = TonosAfiche.Tinta2Afiche,
    cereza = TonosAfiche.TintaAfiche,
    ambar = TonosAfiche.Amarillo,
    hueco = TonosAfiche.TintaAfiche.copy(alpha = 0.07f),
    borde = TonosAfiche.Punteado,
    maderaClara = TonosAfiche.PapelHondo,
    fondo = TonosAfiche.PapelAfiche,
    pista = TonosAfiche.TintaAfiche.copy(alpha = 0.22f),
    errorFondo = TonosAfiche.TintaAfiche,
    sobreError = TonosAfiche.PapelAfiche,
    bannerFondo = TonosAfiche.TintaAfiche,
    bannerTexto = TonosAfiche.PapelAfiche,
    luz = TonosAfiche.Partida,
)

/** Lo que una pantalla necesita saber de su tema. */
@Immutable
data class Diseno(val tema: TemaBar, val paleta: Paleta, val tipos: Tipos, val radios: Radios) {
    val esRockola: Boolean get() = tema == TemaBar.ROCKOLA
    val esVidrio: Boolean get() = tema == TemaBar.VIDRIO
    val esAfiche: Boolean get() = tema == TemaBar.AFICHE

    /** Sobre la luz (degradado o tinta partida) el texto es oscuro: el claro no llega a 4,5:1. */
    fun sobreLuz(): Diseno = when (tema) {
        TemaBar.ROCKOLA -> this
        TemaBar.VIDRIO -> copy(
            paleta = paleta.copy(
                crema = TonosVidrio.TintaVidrio,
                crema2 = TonosVidrio.Tinta2Vidrio,
                tinta = TonosVidrio.TintaVidrio,
                tinta2 = TonosVidrio.Tinta2Vidrio,
                pista = TonosVidrio.TintaVidrio.copy(alpha = 0.22f),
            ),
        )
        TemaBar.AFICHE -> copy(paleta = paleta.copy(crema2 = TonosAfiche.TintaAfiche, tinta2 = TonosAfiche.TintaAfiche))
    }

    /** Sobre la franja de tinta de Afiche (el mini reproductor) el texto es papel. */
    fun sobreTinta(): Diseno = if (!esAfiche) this else copy(
        paleta = paleta.copy(
            crema = TonosAfiche.PapelAfiche,
            crema2 = TonosAfiche.PapelSobreTinta,
            tinta = TonosAfiche.PapelAfiche,
            tinta2 = TonosAfiche.PapelSobreTinta,
        ),
    )
}

val DisenoRockola = Diseno(TemaBar.ROCKOLA, PaletaRockola, TiposRockola, RadiosRockola)
val DisenoVidrio = Diseno(TemaBar.VIDRIO, PaletaVidrio, TiposVidrio, RadiosVidrio)
val DisenoAfiche = Diseno(TemaBar.AFICHE, PaletaAfiche, TiposAfiche, RadiosAfiche)

fun disenoDe(tema: TemaBar): Diseno = when (tema) {
    TemaBar.ROCKOLA -> DisenoRockola
    TemaBar.VIDRIO -> DisenoVidrio
    TemaBar.AFICHE -> DisenoAfiche
}

val LocalDiseno = compositionLocalOf { DisenoRockola }
