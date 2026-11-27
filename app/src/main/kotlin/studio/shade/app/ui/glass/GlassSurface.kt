package studio.shade.app.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp

/**
 * A frosted-glass surface: a slice of the blurred wallpaper from [LocalBackdrop], a colour tint with
 * [tintAlpha] opacity, and a light-catching gradient hairline border. Without a backdrop it degrades
 * to a plain translucent surface, which is what the app chrome uses.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerDp: Float = 20f,
    tint: Color = Color.White,
    tintAlpha: Float = 0.25f,
    borderAlpha: Float = 0.35f,
    panel: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val backdrop = LocalBackdrop.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(cornerDp.dp)
    Box(
        modifier
            .clip(shape)
            .onGloballyPositioned { origin = it.positionInWindow() }
            .drawBehind {
                if (backdrop != null) {
                    val d = backdrop.hostOrigin - origin
                    translate(d.x, d.y) { drawLayer(if (panel) backdrop.panelLayer else backdrop.tileLayer) }
                }
            }
            .background(tint.copy(alpha = tintAlpha.coerceIn(0f, 1f)))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = borderAlpha), Color.White.copy(alpha = borderAlpha * 0.12f))),
                shape,
            ),
        content = content,
    )
}
