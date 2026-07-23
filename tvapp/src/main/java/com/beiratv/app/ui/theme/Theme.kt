package com.beiratv.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.beiratv.app.data.preferences.ThemeMode

private val DarkColors = darkColorScheme(
    primary = TekasOrange,
    secondary = TekasOrangeLight,
    tertiary = Color(0xFFFFC266),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextOnDark,
    onSurface = TextOnDark,
    error = TekasRed
)

private val AmoledColors = darkColorScheme(
    primary = TekasOrange,
    secondary = TekasOrangeLight,
    tertiary = Color(0xFFFFC266),
    background = AmoledBackground,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextOnDark,
    onSurface = TextOnDark,
    error = TekasRed
)

private val LightColors = lightColorScheme(
    primary = TekasOrange,
    secondary = Color(0xFFD66F00),
    tertiary = Color(0xFF9C5B00),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextOnLight,
    onSurface = TextOnLight,
    error = TekasRed
)

@Composable
fun BeiraTVTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = when (themeMode) {
            ThemeMode.LIGHT -> LightColors
            ThemeMode.DARK -> DarkColors
            ThemeMode.AMOLED -> AmoledColors
        },
        typography = Typography,
        content = content
    )
}
