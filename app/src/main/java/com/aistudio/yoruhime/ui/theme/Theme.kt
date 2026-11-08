package com.aistudio.yoruhime.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = MidnightBlack,
    primaryContainer = VioletDeep,
    onPrimaryContainer = TextWhite,
    secondary = VioletAccent,
    onSecondary = TextWhite,
    secondaryContainer = SlateSurface,
    onSecondaryContainer = TextSilver,
    tertiary = BlushPink,
    onTertiary = MidnightBlack,
    background = MidnightBlack,
    onBackground = TextSilver,
    surface = SlateDark,
    onSurface = TextWhite,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = TextMuted,
    outline = SlateBorder
)

@Composable
fun YoruHimeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = MidnightBlack.toArgb()
                window.navigationBarColor = MidnightBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
