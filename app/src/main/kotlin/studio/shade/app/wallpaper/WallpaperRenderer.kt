package studio.shade.app.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import studio.shade.core.wallpaper.WallPalette
import studio.shade.core.wallpaper.WallSpec
import studio.shade.core.wallpaper.WallStyle
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural wallpaper renderer. All geometry is relative to the canvas size and every random value is
 * drawn from the spec's seed in a fixed order, so a small preview and the full-resolution wallpaper
 * have the same composition.
 */
object WallpaperRenderer {
    fun render(spec: WallSpec, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = Random(spec.seed)
        val w = width.toFloat()
        val h = height.toFloat()
        val u = min(w, h)
        val pal = spec.palette
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

        paint.shader = LinearGradient(0f, 0f, w * 0.3f, h, pal.background[0].toInt(), pal.background[1].toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)

        when (spec.style) {
            WallStyle.AURORA -> blobs(c, paint, r, pal, w, h, u, count = 5, alpha = 0.85f)
            WallStyle.GLASS -> {
                blobs(c, paint, r, pal, w, h, u, count = 4, alpha = 0.9f)
                panes(c, r, w, h, u)
            }
            WallStyle.WAVES -> {
                blobs(c, paint, r, pal, w, h, u, count = 2, alpha = 0.5f)
                waves(c, paint, r, pal, w, h, u)
            }
            WallStyle.ORBS -> {
                blobs(c, paint, r, pal, w, h, u, count = 3, alpha = 0.55f)
                orbs(c, paint, r, pal, w, h, u)
            }
        }
        grain(c, r, w, h, u)
        return bmp
    }

    private fun Long.alpha(a: Float): Int = (toInt() and 0x00FFFFFF) or ((a * 255).toInt().coerceIn(0, 255) shl 24)
    private fun white(a: Float): Int = 0xFFFFFFFFL.alpha(a)

    private fun darken(color: Long, f: Float): Int {
        val c = color.toInt()
        return Color.rgb((Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt())
    }

    private fun blobs(c: Canvas, p: Paint, r: Random, pal: WallPalette, w: Float, h: Float, u: Float, count: Int, alpha: Float) {
        repeat(count) {
            val cx = r.nextFloat() * w
            val cy = r.nextFloat() * h
            val rad = u * (0.45f + r.nextFloat() * 0.6f)
            val col = pal.accents[r.nextInt(pal.accents.size)]
            p.shader = RadialGradient(
                cx, cy, rad,
                intArrayOf(col.alpha(alpha), col.alpha(alpha * 0.35f), col.alpha(0f)),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawCircle(cx, cy, rad, p)
        }
    }

    /** Frosted panes: translucent white gradient fill plus a light-catching hairline edge. */
    private fun panes(c: Canvas, r: Random, w: Float, h: Float, u: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1f, u * 0.004f) }
        repeat(3 + r.nextInt(3)) {
            val pw = u * (0.45f + r.nextFloat() * 0.4f)
            val ph = pw * (0.6f + r.nextFloat() * 0.8f)
            val cx = w * (0.15f + r.nextFloat() * 0.7f)
            val cy = h * (0.12f + r.nextFloat() * 0.76f)
            val rot = -18f + r.nextFloat() * 36f
            val rect = RectF(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2)
            val corner = u * 0.06f
            c.save()
            c.rotate(rot, cx, cy)
            fill.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, white(0.20f), white(0.03f), Shader.TileMode.CLAMP)
            c.drawRoundRect(rect, corner, corner, fill)
            edge.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, white(0.65f), white(0.04f), Shader.TileMode.CLAMP)
            c.drawRoundRect(rect, corner, corner, edge)
            c.restore()
        }
    }

    private fun waves(c: Canvas, p: Paint, r: Random, pal: WallPalette, w: Float, h: Float, u: Float) {
        val crest = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = max(1f, u * 0.003f); color = white(0.28f)
        }
        val bands = 4 + r.nextInt(3)
        val steps = 64
        for (i in 0 until bands) {
            val baseY = h * (0.3f + 0.62f * i / bands)
            val amp = h * (0.025f + r.nextFloat() * 0.05f)
            val freq = 0.8f + r.nextFloat() * 1.8f
            val phase = r.nextFloat() * 2f * PI.toFloat()
            val col = pal.accents[i % pal.accents.size]
            val top = Path()
            for (s in 0..steps) {
                val x = w * s / steps
                val y = baseY + amp * sin(phase + s.toFloat() / steps * freq * 2f * PI.toFloat())
                if (s == 0) top.moveTo(x, y) else top.lineTo(x, y)
            }
            val body = Path(top).apply { lineTo(w, h); lineTo(0f, h); close() }
            p.shader = LinearGradient(0f, baseY - amp, 0f, h, col.alpha(0.7f), col.alpha(0.08f), Shader.TileMode.CLAMP)
            c.drawPath(body, p)
            c.drawPath(top, crest)
        }
    }

    /** Glossy spheres with an off-centre specular highlight and a soft drop shadow. */
    private fun orbs(c: Canvas, p: Paint, r: Random, pal: WallPalette, w: Float, h: Float, u: Float) {
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1f, u * 0.003f); color = white(0.22f) }
        repeat(3 + r.nextInt(4)) {
            val cx = r.nextFloat() * w
            val cy = h * (0.1f + r.nextFloat() * 0.8f)
            val rad = u * (0.08f + r.nextFloat() * 0.22f)
            val col = pal.accents[r.nextInt(pal.accents.size)]
            p.shader = RadialGradient(cx + rad * 0.15f, cy + rad * 0.3f, rad * 1.4f, intArrayOf(0x59000000, 0), null, Shader.TileMode.CLAMP)
            c.drawCircle(cx + rad * 0.15f, cy + rad * 0.3f, rad * 1.4f, p)
            p.shader = RadialGradient(
                cx - rad * 0.35f, cy - rad * 0.4f, rad * 1.6f,
                intArrayOf(white(0.9f), col.toInt(), darken(col, 0.35f)),
                floatArrayOf(0f, 0.32f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawCircle(cx, cy, rad, p)
            c.drawCircle(cx, cy, rad, rim)
        }
    }

    /** Fine film grain; hides gradient banding on 8-bit panels. */
    private fun grain(c: Canvas, r: Random, w: Float, h: Float, u: Float) {
        val n = (w * h / 200f).toInt()
        val pts = FloatArray(n * 2)
        for (i in 0 until n) {
            pts[2 * i] = r.nextFloat() * w
            pts[2 * i + 1] = r.nextFloat() * h
        }
        val p = Paint().apply { color = white(0.05f); strokeWidth = max(1f, u / 700f) }
        c.drawPoints(pts, p)
    }
}
