package com.example.rokola.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/*
 * Tema Rockola: solo oscuro (color-scheme: dark en tokens.css), sin color dinámico.
 * Cada ranura de Material apunta a un token, así ningún componente muestra el morado
 * por defecto.
 */
private val EsquemaRockola = darkColorScheme(
    primary = Cereza,
    onPrimary = Crema,
    primaryContainer = CerezaHondo,
    onPrimaryContainer = Crema,
    inversePrimary = Cereza,
    secondary = Ambar,
    onSecondary = Tinta,
    secondaryContainer = AmbarClaro,
    onSecondaryContainer = Tinta,
    tertiary = Papel,
    onTertiary = Tinta,
    tertiaryContainer = PapelSombra,
    onTertiaryContainer = Tinta,
    background = Nogal,
    onBackground = Crema,
    surface = Madera,
    onSurface = Crema,
    surfaceVariant = MaderaClara,
    onSurfaceVariant = Crema2,
    surfaceTint = Madera,
    inverseSurface = Papel,
    inverseOnSurface = Tinta,
    error = Cereza,
    onError = Crema,
    errorContainer = CerezaHondo,
    onErrorContainer = Crema,
    outline = Eje,
    outlineVariant = Borde,
    scrim = NogalHondo,
    surfaceBright = MaderaClara,
    surfaceDim = NogalHondo,
    surfaceContainerLowest = NogalHondo,
    surfaceContainerLow = Nogal,
    surfaceContainer = Madera,
    surfaceContainerHigh = MaderaClara,
    surfaceContainerHighest = MaderaClara,
)

@Composable
fun RokolaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaRockola,
        typography = TipografiaRockola,
        shapes = FormasRockola,
        content = content,
    )
}
