package studio.shade.core.model

import java.util.UUID

/** Tile types the editor can place. [aospSpec] is the AOSP SystemUI tile spec, if one exists. */
enum class TileKind(val id: String, val defaultLabel: String, val aospSpec: String?) {
    WIFI("wifi", "Wi-Fi", "internet"),
    BLUETOOTH("bluetooth", "Bluetooth", "bt"),
    MOBILE_DATA("data", "Mobile data", "cell"),
    FLASHLIGHT("flashlight", "Flashlight", "flashlight"),
    AIRPLANE("airplane", "Airplane", "airplane"),
    DND("dnd", "Do not disturb", "dnd"),
    AUTO_ROTATE("rotate", "Auto-rotate", "rotation"),
    LOCATION("location", "Location", "location"),
    BATTERY_SAVER("battery", "Battery saver", "battery"),
    HOTSPOT("hotspot", "Hotspot", "hotspot"),
    NFC("nfc", "NFC", "nfc"),
    DARK_MODE("dark", "Dark mode", "dark"),
    NIGHT_LIGHT("night", "Night light", "night"),
    SCREEN_CAST("cast", "Screen cast", "cast");

    companion object {
        fun fromId(id: String): TileKind? = entries.firstOrNull { it.id == id }
    }
}

enum class TileSize(val span: Int) { SMALL(1), WIDE(2) }

enum class BackdropPreset(val id: String, val title: String) {
    AURORA("aurora", "Aurora"),
    SUNSET("sunset", "Sunset"),
    OCEAN("ocean", "Ocean"),
    GRAPHITE("graphite", "Graphite");

    companion object {
        fun fromId(id: String?): BackdropPreset = entries.firstOrNull { it.id == id } ?: AURORA
    }
}

/** A single tile. [accent] is an optional ARGB override of the style's active colour. */
data class Tile(
    val id: String,
    val kind: TileKind,
    val size: TileSize = TileSize.SMALL,
    val label: String? = null,
    val accent: Long? = null,
) {
    val displayLabel: String get() = label?.takeIf { it.isNotBlank() } ?: kind.defaultLabel

    companion object {
        fun create(kind: TileKind, size: TileSize = TileSize.SMALL) =
            Tile(id = UUID.randomUUID().toString(), kind = kind, size = size)
    }
}

/** Visual parameters of the panel. Colours are ARGB longs; alpha/blur ranges are in [Limits]. */
data class GlassStyle(
    val columns: Int = 4,
    val tileHeightDp: Float = 76f,
    val spacingDp: Float = 10f,
    val cornerDp: Float = 22f,
    val tileBlurDp: Float = 24f,
    val tileOpacity: Float = 0.28f,
    val panelBlurDp: Float = 32f,
    val panelOpacity: Float = 0.35f,
    val borderOpacity: Float = 0.35f,
    val activeColor: Long = 0xFF7C9CFF,
    val inactiveColor: Long = 0xFFFFFFFF,
    val panelColor: Long = 0xFF0B1020,
    val showLabels: Boolean = true,
    val showBrightness: Boolean = true,
    val backdrop: BackdropPreset = BackdropPreset.AURORA,
) {
    /** Returns a copy with every field clamped into its supported range. */
    fun sanitized(): GlassStyle = copy(
        columns = columns.coerceIn(Limits.COLUMNS),
        tileHeightDp = tileHeightDp.coerceIn(Limits.TILE_HEIGHT),
        spacingDp = spacingDp.coerceIn(Limits.SPACING),
        cornerDp = cornerDp.coerceIn(Limits.CORNER),
        tileBlurDp = tileBlurDp.coerceIn(Limits.BLUR),
        tileOpacity = tileOpacity.coerceIn(0f, 1f),
        panelBlurDp = panelBlurDp.coerceIn(Limits.BLUR),
        panelOpacity = panelOpacity.coerceIn(0f, 1f),
        borderOpacity = borderOpacity.coerceIn(0f, 1f),
    )

    object Limits {
        val COLUMNS = 2..5
        val TILE_HEIGHT = 56f..110f
        val SPACING = 4f..20f
        val CORNER = 0f..36f
        val BLUR = 0f..48f
    }
}

data class Profile(
    val id: String,
    val name: String,
    val tiles: List<Tile>,
    val style: GlassStyle = GlassStyle(),
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

data class StudioState(
    val profiles: List<Profile>,
    val activeId: String,
) {
    init {
        require(profiles.isNotEmpty()) { "At least one profile is required" }
    }

    val active: Profile get() = profiles.firstOrNull { it.id == activeId } ?: profiles.first()

    inline fun updateActive(transform: (Profile) -> Profile): StudioState {
        val current = active
        return copy(
            profiles = profiles.map { if (it.id == current.id) transform(it) else it },
            activeId = current.id,
        )
    }
}
