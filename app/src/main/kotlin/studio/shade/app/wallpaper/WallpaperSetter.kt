package studio.shade.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.shade.core.model.GlassStyle
import studio.shade.core.wallpaper.WallPalette
import studio.shade.core.wallpaper.WallSpec

enum class WallTarget(val title: String) { HOME("Home screen"), LOCK("Lock screen"), BOTH("Both") }

object WallpaperSetter {
    /** Physical screen size in portrait orientation (width < height). */
    fun screenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(WindowManager::class.java)
        val (a, b) = if (Build.VERSION.SDK_INT >= 30) {
            wm.maximumWindowMetrics.bounds.let { it.width() to it.height() }
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
        return minOf(a, b) to maxOf(a, b)
    }

    suspend fun apply(context: Context, spec: WallSpec, target: WallTarget): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (w, h) = screenSize(context)
            val bmp = WallpaperRenderer.render(spec, w, h)
            try {
                val flags = when (target) {
                    WallTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                WallpaperManager.getInstance(context).setBitmap(bmp, null, true, flags)
            } finally {
                bmp.recycle()
            }
            Unit
        }
    }

    /** A palette derived from the active profile, so wallpapers match the editor's glass design. */
    fun profilePalette(style: GlassStyle): WallPalette {
        fun shift(argb: Long, dh: Float, s: Float = 1f, v: Float = 1f): Long {
            val hsv = FloatArray(3)
            Color.colorToHSV(argb.toInt(), hsv)
            hsv[0] = (hsv[0] + dh + 360f) % 360f
            hsv[1] = (hsv[1] * s).coerceIn(0f, 1f)
            hsv[2] = (hsv[2] * v).coerceIn(0f, 1f)
            return Color.HSVToColor(hsv).toLong() and 0xFFFFFFFFL
        }
        val a = style.activeColor
        val bg = style.panelColor
        return WallPalette(
            id = "profile",
            title = "Profile",
            background = listOf(shift(bg, 0f, v = 0.6f), shift(bg, 12f, v = 1.15f)),
            accents = listOf(a, shift(a, 40f), shift(a, -50f), shift(a, 160f, s = 0.7f)),
        )
    }
}
