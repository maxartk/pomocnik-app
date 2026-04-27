package cz.kovmak.pomocnik.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Modern dark color palette
val NeonOrange = Color(0xFFFF6B35)
val NeonBlue = Color(0xFF00B4D8)
val DarkBackground = Color(0xFF0A0E21)
val DarkSurface = Color(0xFF141832)
val DarkCard = Color(0xFF1A1F35)

private val DarkColorScheme = darkColorScheme(
    primary = NeonOrange,
    secondary = NeonBlue,
    tertiary = Color(0xFF7209B7),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE8E8E8),
    onSurface = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF8892B0),
    outline = Color(0xFF2A2F45)
)

@Composable
fun PomocnikTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
