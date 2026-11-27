package studio.shade.app.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import studio.shade.core.model.TileKind

/** Small line icons drawn on a 24x24 grid, so the app needs no icon-font/extended-icons dependency. */
@Composable
fun TileIcon(kind: TileKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawKind(kind, color, u, stroke)
    }
}

private fun DrawScope.p(u: Float, x: Float, y: Float) = Offset(x * u, y * u)

private fun DrawScope.arc(u: Float, cx: Float, cy: Float, r: Float, start: Float, sweep: Float, color: Color, s: Stroke) =
    drawArc(color, start, sweep, false, Offset((cx - r) * u, (cy - r) * u), Size(2 * r * u, 2 * r * u), style = s)

private fun DrawScope.poly(u: Float, color: Color, s: Stroke, closed: Boolean, vararg pts: Float) {
    val path = Path()
    for (i in pts.indices step 2) {
        if (i == 0) path.moveTo(pts[0] * u, pts[1] * u) else path.lineTo(pts[i] * u, pts[i + 1] * u)
    }
    if (closed) path.close()
    drawPath(path, color, style = s)
}

private fun DrawScope.drawKind(kind: TileKind, c: Color, u: Float, s: Stroke) {
    when (kind) {
        TileKind.WIFI -> {
            arc(u, 12f, 19f, 15f, -135f, 90f, c, s)
            arc(u, 12f, 19f, 10f, -135f, 90f, c, s)
            arc(u, 12f, 19f, 5f, -135f, 90f, c, s)
            drawCircle(c, 1.6f * u, p(u, 12f, 19f))
        }
        TileKind.BLUETOOTH -> poly(u, c, s, false, 6f, 8f, 18f, 17f, 12f, 22f, 12f, 2f, 18f, 7f, 6f, 16f)
        TileKind.MOBILE_DATA -> {
            poly(u, c, s, false, 8f, 20f, 8f, 5f, 5f, 8f)
            poly(u, c, s, false, 8f, 5f, 11f, 8f)
            poly(u, c, s, false, 16f, 4f, 16f, 19f, 13f, 16f)
            poly(u, c, s, false, 16f, 19f, 19f, 16f)
        }
        TileKind.FLASHLIGHT -> {
            poly(u, c, s, true, 8f, 2f, 16f, 2f, 16f, 7f, 14f, 10f, 14f, 22f, 10f, 22f, 10f, 10f, 8f, 7f)
            poly(u, c, s, false, 8f, 7f, 16f, 7f)
        }
        TileKind.AIRPLANE -> poly(u, c, s, true, 12f, 2f, 14f, 9f, 22f, 14f, 22f, 16f, 14f, 13.5f, 13f, 19f, 16f, 21f, 16f, 22f, 12f, 21f, 8f, 22f, 8f, 21f, 11f, 19f, 10f, 13.5f, 2f, 16f, 2f, 14f, 10f, 9f)
        TileKind.DND -> {
            drawCircle(c, 9f * u, p(u, 12f, 12f), style = s)
            poly(u, c, s, false, 7.5f, 12f, 16.5f, 12f)
        }
        TileKind.AUTO_ROTATE -> {
            arc(u, 12f, 12f, 8f, -40f, 270f, c, s)
            poly(u, c, s, false, 18f, 3f, 18.5f, 8f, 13.5f, 8.5f)
        }
        TileKind.LOCATION -> {
            val pin = Path().apply {
                moveTo(12f * u, 22f * u)
                cubicTo(5f * u, 15f * u, 4f * u, 11f * u, 4f * u, 9.5f * u)
                arcTo(Rect(Offset(4f * u, 1.5f * u), Size(16f * u, 16f * u)), 180f, 180f, false)
                cubicTo(20f * u, 11f * u, 19f * u, 15f * u, 12f * u, 22f * u)
            }
            drawPath(pin, c, style = s)
            drawCircle(c, 2.6f * u, p(u, 12f, 9.5f), style = s)
        }
        TileKind.BATTERY_SAVER -> {
            drawRoundRect(c, p(u, 5f, 7f), Size(14f * u, 13f * u), androidx.compose.ui.geometry.CornerRadius(2.5f * u), style = s)
            poly(u, c, s, false, 9f, 7f, 9f, 4f, 15f, 4f, 15f, 7f)
            poly(u, c, s, false, 9.5f, 13.5f, 14.5f, 13.5f)
            poly(u, c, s, false, 12f, 11f, 12f, 16f)
        }
        TileKind.HOTSPOT -> {
            drawCircle(c, 2f * u, p(u, 12f, 12f))
            arc(u, 12f, 12f, 5.5f, -50f, 100f, c, s)
            arc(u, 12f, 12f, 5.5f, 130f, 100f, c, s)
            arc(u, 12f, 12f, 10f, -50f, 100f, c, s)
            arc(u, 12f, 12f, 10f, 130f, 100f, c, s)
        }
        TileKind.NFC -> {
            drawRoundRect(c, p(u, 3f, 3f), Size(18f * u, 18f * u), androidx.compose.ui.geometry.CornerRadius(4f * u), style = s)
            arc(u, 9f, 12f, 3.5f, -60f, 120f, c, s)
            arc(u, 9f, 12f, 7f, -60f, 120f, c, s)
        }
        TileKind.DARK_MODE -> {
            drawCircle(c, 9f * u, p(u, 12f, 12f), style = s)
            drawArc(c, 90f, 180f, true, p(u, 3f, 3f), Size(18f * u, 18f * u))
        }
        TileKind.NIGHT_LIGHT -> {
            drawCircle(c, 4f * u, p(u, 12f, 12f), style = s)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val cx = kotlin.math.cos(a).toFloat()
                val sy = kotlin.math.sin(a).toFloat()
                drawLine(c, p(u, 12f + 7f * cx, 12f + 7f * sy), p(u, 12f + 10f * cx, 12f + 10f * sy), 2f * u, StrokeCap.Round)
            }
        }
        TileKind.SCREEN_CAST -> {
            poly(u, c, s, false, 3f, 7f, 3f, 5f, 21f, 5f, 21f, 19f, 14f, 19f)
            arc(u, 3f, 20f, 5f, -90f, 90f, c, s)
            arc(u, 3f, 20f, 10f, -90f, 90f, c, s)
        }
    }
}
