package cz.kovmak.pomocnik.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFA726),
    secondary = Color(0xFFFFB74D),
    tertiary = Color(0xFF8D6E63),
    surface = Color(0xFF1A1F2E),
    background = Color(0xFF121212),
    onPrimary = Color(0xFF1A1F2E),
    onSurface = Color(0xFFE0E0E0),
    onBackground = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9800),
    secondary = Color(0xFFFFB74D),
    tertiary = Color(0xFF795548),
    surface = Color(0xFFFFF8E1),
    background = Color(0xFFFAFAFA),
    onPrimary = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    onBackground = Color(0xFF212121)
)

@Composable
fun PomocnikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val Typography = Typography()
