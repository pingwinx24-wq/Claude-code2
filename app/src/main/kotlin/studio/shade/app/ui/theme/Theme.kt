package studio.shade.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ShadeColors {
    val Bg = Color(0xFF070A14)
    val Surface = Color(0xFF111628)
    val Accent = Color(0xFF8EA8FF)
    val OnSurface = Color(0xFFE8EBF7)
    val Muted = Color(0xFF9AA3BF)
    val Danger = Color(0xFFFF7A8A)
}

private val scheme = darkColorScheme(
    primary = ShadeColors.Accent,
    onPrimary = Color(0xFF0A1030),
    secondary = Color(0xFF7BE0C3),
    background = ShadeColors.Bg,
    surface = ShadeColors.Surface,
    onSurface = ShadeColors.OnSurface,
    onBackground = ShadeColors.OnSurface,
    surfaceVariant = Color(0xFF1B2140),
    onSurfaceVariant = ShadeColors.Muted,
    error = ShadeColors.Danger,
    outline = Color(0x33FFFFFF),
)

@Composable
fun ShadeTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)
