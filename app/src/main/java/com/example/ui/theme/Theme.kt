package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    tertiary = GreenTertiary,
    background = GreenBackground,
    surface = GreenSurface,
    onPrimary = GreenOnPrimary,
    onBackground = GreenOnBackground,
    onSurface = GreenOnBackground
)

private val OceanColorScheme = lightColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanBackground,
    surface = OceanSurface,
    onPrimary = OceanOnPrimary,
    onBackground = OceanOnBackground,
    onSurface = OceanOnBackground
)

private val NightColorScheme = lightColorScheme(
    primary = NightPrimary,
    secondary = NightSecondary,
    tertiary = NightTertiary,
    background = NightBackground,
    surface = NightSurface,
    onPrimary = NightOnPrimary,
    onBackground = NightOnBackground,
    onSurface = NightOnBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightOnPrimary,
    onBackground = LightOnBackground,
    onSurface = LightOnBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground
)

@Composable
fun IstaTheme(
    themeName: String = "Green Nature",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Light" -> LightColorScheme
        "Dark" -> DarkColorScheme
        "System Default" -> if (darkTheme) DarkColorScheme else LightColorScheme
        "Green Nature" -> GreenColorScheme
        "Blue Ocean" -> OceanColorScheme
        "Purple Night" -> NightColorScheme
        else -> GreenColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
