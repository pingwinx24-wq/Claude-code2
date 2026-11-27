package studio.shade.app.ui.glass

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import studio.shade.core.model.BackdropPreset

private class Blob(val x: Float, val y: Float, val r: Float, val color: Color)
private class Scene(val bg: List<Color>, val blobs: List<Blob>)

private fun scene(p: BackdropPreset): Scene = when (p) {
    BackdropPreset.AURORA -> Scene(
        listOf(Color(0xFF0A1128), Color(0xFF111B3F)),
        listOf(Blob(0.15f, 0.20f, 0.55f, Color(0xFF5B6CFF)), Blob(0.85f, 0.45f, 0.50f, Color(0xFF29D9B0)), Blob(0.35f, 0.95f, 0.60f, Color(0xFF9B5CFF))),
    )
    BackdropPreset.SUNSET -> Scene(
        listOf(Color(0xFF1B0B24), Color(0xFF2B1030)),
        listOf(Blob(0.85f, 0.15f, 0.55f, Color(0xFFFF5F6D)), Blob(0.10f, 0.55f, 0.50f, Color(0xFFFFB347)), Blob(0.60f, 1.00f, 0.60f, Color(0xFFD13FD1))),
    )
    BackdropPreset.OCEAN -> Scene(
        listOf(Color(0xFF031B2B), Color(0xFF04283D)),
        listOf(Blob(0.20f, 0.15f, 0.55f, Color(0xFF00B4D8)), Blob(0.90f, 0.60f, 0.55f, Color(0xFF0077B6)), Blob(0.30f, 0.95f, 0.50f, Color(0xFF48CAE4))),
    )
    BackdropPreset.GRAPHITE -> Scene(
        listOf(Color(0xFF0E0F13), Color(0xFF181A20)),
        listOf(Blob(0.20f, 0.25f, 0.50f, Color(0xFF3A3F55)), Blob(0.85f, 0.80f, 0.50f, Color(0xFF4A4660))),
    )
}

/** Draws the wallpaper stand-in: a gradient plus soft radial blobs. Pure drawing, no composition cost. */
private fun DrawScope.drawScene(preset: BackdropPreset) {
    val s = scene(preset)
    drawRect(Brush.linearGradient(s.bg, Offset.Zero, Offset(size.width, size.height)))
    val dim = maxOf(size.width, size.height)
    for (b in s.blobs) {
        val c = Offset(b.x * size.width, b.y * size.height)
        drawCircle(
            brush = Brush.radialGradient(listOf(b.color.copy(alpha = 0.85f), Color.Transparent), c, b.r * dim),
            radius = b.r * dim,
            center = c,
        )
    }
}

/**
 * Shared state for glass surfaces. The wallpaper is rendered **once per blur level** into a blurred
 * [GraphicsLayer]; every glass surface then just draws a translated, clipped slice of that layer.
 * This keeps cost independent of tile count (one blur pass per level instead of one per tile).
 */
class BackdropState(val tileLayer: GraphicsLayer, val panelLayer: GraphicsLayer) {
    var hostOrigin by mutableStateOf(Offset.Zero)
    internal var appliedTileBlur = -1f
    internal var appliedPanelBlur = -1f
}

val LocalBackdrop = staticCompositionLocalOf<BackdropState?> { null }

@Composable
fun BackdropHost(
    preset: BackdropPreset,
    tileBlurDp: Float,
    panelBlurDp: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tileLayer = rememberGraphicsLayer()
    val panelLayer = rememberGraphicsLayer()
    val state = remember { BackdropState(tileLayer, panelLayer) }
    CompositionLocalProvider(LocalBackdrop provides state) {
        Box(
            modifier
                .onGloballyPositioned { state.hostOrigin = it.positionInWindow() }
                .drawBehind {
                    drawScene(preset)
                    val recSize = IntSize(size.width.toInt(), size.height.toInt())
                    for ((layer, blurDp) in listOf(tileLayer to tileBlurDp, panelLayer to panelBlurDp)) {
                        layer.record(this, layoutDirection, recSize) { drawScene(preset) }
                        val px = blurDp * density
                        val applied = if (layer === tileLayer) state.appliedTileBlur else state.appliedPanelBlur
                        if (px != applied) {
                            // Real-time blur needs RenderEffect (API 31+); below that glass falls back to tint only.
                            layer.renderEffect =
                                if (Build.VERSION.SDK_INT >= 31 && px > 0.5f) BlurEffect(px, px, TileMode.Clamp) else null
                            if (layer === tileLayer) state.appliedTileBlur = px else state.appliedPanelBlur = px
                        }
                    }
                },
            content = content,
        )
    }
}
