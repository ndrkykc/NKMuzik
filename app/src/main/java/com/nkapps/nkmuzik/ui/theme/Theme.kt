package com.nkapps.nkmuzik.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.Black,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberPrimary,
    secondary = NeonMint,
    onSecondary = Color.Black,
    background = DeepNightBlue,
    onBackground = TextPrimary,
    surface = DeepNightBlue,
    surfaceVariant = DeepNightBlue,
    onSurface = TextPrimary,
    surfaceContainer = DarkSlateSurface,
    surfaceContainerHigh = CardSurface,
    surfaceContainerLow = CardSurface,
    secondaryContainer = CardSurface,
    onSecondaryContainer = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightAmberPrimary,
    onPrimary = Color.White,
    primaryContainer = LightAmberContainer,
    onPrimaryContainer = LightAmberPrimary,
    secondary = LightNeonMint,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    surfaceVariant = LightSurface,
    onSurface = LightTextPrimary,
    surfaceContainer = LightCardSurface,
    surfaceContainerHigh = LightCardSurface,
    surfaceContainerLow = LightSurface,
    secondaryContainer = LightCardSurface,
    onSecondaryContainer = LightTextPrimary
)

@Composable
fun NKMüzikTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}
