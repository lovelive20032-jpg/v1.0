package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CosmicColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = LightText,
    secondary = SecondaryNeon,
    onSecondary = DeepBackground,
    tertiary = GoldAccent,
    background = DeepBackground,
    surface = CardSurface,
    surfaceVariant = VariantSurface,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = DimText,
    error = ErrorRose
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for a premium cohesive experience
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our premium cosmic design system
    content: @Composable () -> Unit
) {
    val colorScheme = CosmicColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepBackground.toArgb()
            window.navigationBarColor = DeepBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
