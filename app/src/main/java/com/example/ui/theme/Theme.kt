package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.viewmodel.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = VictorianGoldPrimary,
    onPrimary = VictorianMidnight,
    primaryContainer = VictorianMidnightSurfaceVar,
    onPrimaryContainer = VictorianGoldPrimary,
    secondary = VictorianRubySecondary,
    onSecondary = Color.White,
    secondaryContainer = VictorianMidnightSurface,
    onSecondaryContainer = VictorianCreamText,
    tertiary = VictorianSageTertiary,
    onTertiary = VictorianMidnight,
    background = VictorianMidnight,
    onBackground = VictorianCreamText,
    surface = VictorianMidnightSurface,
    onSurface = VictorianCreamText,
    surfaceVariant = VictorianMidnightSurfaceVar,
    onSurfaceVariant = VictorianCreamMuted,
    outline = VictorianDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = VictorianInkPrimary,
    onPrimary = VictorianPaper,
    primaryContainer = VictorianPaperSurfaceVar,
    onPrimaryContainer = VictorianInkPrimary,
    secondary = VictorianGoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = VictorianPaperSurface,
    onSecondaryContainer = VictorianInkText,
    tertiary = VictorianForestTertiary,
    onTertiary = Color.White,
    background = VictorianPaper,
    onBackground = VictorianInkText,
    surface = VictorianPaperSurface,
    onSurface = VictorianInkText,
    surfaceVariant = VictorianPaperSurfaceVar,
    onSurfaceVariant = VictorianInkMuted,
    outline = VictorianBorder
)

@Composable
fun CharlesDickensTheme(
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appThemeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
