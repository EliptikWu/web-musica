package com.example.rokola.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.rokola.dominio.TemaBar

/*
 * Rockola y Vidrio son oscuros; Afiche, claro (color-scheme en tokens.css). Sin color dinámico:
 * cada ranura de Material apunta a un token, así ningún componente muestra el morado por defecto.
 */
private val EsquemaRockola = darkColorScheme(
    primary = CerezaBase,
    onPrimary = CremaBase,
    primaryContainer = CerezaHondo,
    onPrimaryContainer = CremaBase,
    inversePrimary = CerezaBase,
    secondary = AmbarBase,
    onSecondary = TintaBase,
    secondaryContainer = AmbarClaro,
    onSecondaryContainer = TintaBase,
    tertiary = PapelBase,
    onTertiary = TintaBase,
    tertiaryContainer = PapelSombra,
    onTertiaryContainer = TintaBase,
    background = Nogal,
    onBackground = CremaBase,
    surface = Madera,
    onSurface = CremaBase,
    surfaceVariant = MaderaClaraBase,
    onSurfaceVariant = Crema2Base,
    surfaceTint = Madera,
    inverseSurface = PapelBase,
    inverseOnSurface = TintaBase,
    error = CerezaBase,
    onError = CremaBase,
    errorContainer = CerezaHondo,
    onErrorContainer = CremaBase,
    outline = Eje,
    outlineVariant = BordeBase,
    scrim = NogalHondo,
    surfaceBright = MaderaClaraBase,
    surfaceDim = NogalHondo,
    surfaceContainerLowest = NogalHondo,
    surfaceContainerLow = Nogal,
    surfaceContainer = Madera,
    surfaceContainerHigh = MaderaClaraBase,
    surfaceContainerHighest = MaderaClaraBase,
)

private val EsquemaVidrio = darkColorScheme(
    primary = TonosVidrio.Rosa,
    onPrimary = TonosVidrio.TintaVidrio,
    secondary = TonosVidrio.Durazno,
    onSecondary = TonosVidrio.TintaVidrio,
    background = TonosVidrio.Carbon,
    onBackground = TonosVidrio.Blanco,
    surface = TonosVidrio.Carbon,
    onSurface = TonosVidrio.Blanco,
    surfaceVariant = TonosVidrio.Carbon,
    onSurfaceVariant = TonosVidrio.Texto2,
    surfaceTint = TonosVidrio.Carbon,
    error = TonosVidrio.Rosa,
    onError = TonosVidrio.TintaVidrio,
    outline = TonosVidrio.TeclaBorde,
    outlineVariant = TonosVidrio.BordeVidrio,
    scrim = TonosVidrio.Carbon,
)

private val EsquemaAfiche = lightColorScheme(
    primary = TonosAfiche.TintaAfiche,
    onPrimary = TonosAfiche.Amarillo,
    secondary = TonosAfiche.Amarillo,
    onSecondary = TonosAfiche.TintaAfiche,
    background = TonosAfiche.PapelAfiche,
    onBackground = TonosAfiche.TintaAfiche,
    surface = TonosAfiche.PapelAfiche,
    onSurface = TonosAfiche.TintaAfiche,
    surfaceVariant = TonosAfiche.PapelHondo,
    onSurfaceVariant = TonosAfiche.Tinta2Afiche,
    surfaceTint = TonosAfiche.PapelAfiche,
    error = TonosAfiche.TintaAfiche,
    onError = TonosAfiche.PapelAfiche,
    outline = TonosAfiche.TintaAfiche,
    outlineVariant = TonosAfiche.Punteado,
    scrim = TonosAfiche.TintaAfiche,
)

@Composable
fun RokolaTheme(tema: TemaBar = TemaBar.ROCKOLA, content: @Composable () -> Unit) {
    val diseno = disenoDe(tema)
    val formas = remember(tema) { formasDe(diseno.radios) }
    val tipografia = remember(tema) { tipografiaDe(diseno.tipos) }
    val vista = LocalView.current
    DisposableEffect(tema, vista) {
        // En el papel de Afiche, los iconos de las barras del sistema van oscuros.
        val barras = (vista.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, vista) }
        barras?.isAppearanceLightStatusBars = diseno.esAfiche
        barras?.isAppearanceLightNavigationBars = diseno.esAfiche
        onDispose {
            barras?.isAppearanceLightStatusBars = false
            barras?.isAppearanceLightNavigationBars = false
        }
    }
    CompositionLocalProvider(LocalDiseno provides diseno) {
        MaterialTheme(
            colorScheme = when (tema) {
                TemaBar.ROCKOLA -> EsquemaRockola
                TemaBar.VIDRIO -> EsquemaVidrio
                TemaBar.AFICHE -> EsquemaAfiche
            },
            typography = tipografia,
            shapes = formas,
            content = content,
        )
    }
}
