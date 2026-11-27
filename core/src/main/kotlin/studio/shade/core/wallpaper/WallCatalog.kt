package studio.shade.core.wallpaper

import kotlin.random.Random

enum class WallStyle(val title: String) {
    AURORA("Aurora"),
    GLASS("Glass"),
    WAVES("Waves"),
    ORBS("Orbs"),
}

/** Colours are ARGB longs. [background] is a 2-stop vertical gradient; [accents] feed the shapes. */
data class WallPalette(val id: String, val title: String, val background: List<Long>, val accents: List<Long>) {
    init {
        require(background.size == 2) { "background needs exactly 2 colours" }
        require(accents.isNotEmpty()) { "at least one accent is required" }
    }
}

/** Everything needed to render one wallpaper deterministically at any resolution. */
data class WallSpec(val style: WallStyle, val palette: WallPalette, val seed: Long)

object WallCatalog {
    val palettes = listOf(
        WallPalette("aurora", "Aurora", listOf(0xFF070B1E, 0xFF121A3D), listOf(0xFF5B6CFF, 0xFF29D9B0, 0xFF9B5CFF, 0xFF3FA9FF)),
        WallPalette("sunset", "Sunset", listOf(0xFF1A0820, 0xFF2D0F2A), listOf(0xFFFF5F6D, 0xFFFFB347, 0xFFD13FD1, 0xFFFF8A5B)),
        WallPalette("ocean", "Ocean", listOf(0xFF02141F, 0xFF052A40), listOf(0xFF00B4D8, 0xFF0077B6, 0xFF48CAE4, 0xFF90E0EF)),
        WallPalette("neon", "Neon", listOf(0xFF07060F, 0xFF130A24), listOf(0xFFFF2BD6, 0xFF00F0FF, 0xFF7A2BFF, 0xFFB6FF3B)),
        WallPalette("mint", "Mint", listOf(0xFF04140F, 0xFF0A2620), listOf(0xFF59E3B0, 0xFF2EC4B6, 0xFFB8F2E6, 0xFF3A86FF)),
        WallPalette("rose", "Rosé", listOf(0xFF1A0B12, 0xFF2A1220), listOf(0xFFFF8FAB, 0xFFFFC2D1, 0xFFB5179E, 0xFFFFD6A5)),
        WallPalette("graphite", "Graphite", listOf(0xFF0B0C10, 0xFF17191F), listOf(0xFF4A4F66, 0xFF6B6F85, 0xFF2F3446, 0xFF8E94AD)),
    )

    /**
     * Builds [count] specs from [seedBase]. Each entry picks a style and palette from the filtered lists,
     * so the same inputs always produce the same carousel.
     */
    fun build(
        seedBase: Long,
        styles: List<WallStyle> = WallStyle.entries,
        palettes: List<WallPalette> = this.palettes,
        count: Int = 24,
    ): List<WallSpec> {
        require(styles.isNotEmpty() && palettes.isNotEmpty() && count >= 0)
        val rnd = Random(seedBase)
        return List(count) { i ->
            // Round-robin over styles keeps the carousel varied; palette and seed are random.
            WallSpec(styles[i % styles.size], palettes[rnd.nextInt(palettes.size)], rnd.nextLong())
        }
    }
}
