package studio.shade.core.model

object Defaults {
    private fun tiles(vararg kinds: TileKind, wide: Set<TileKind> = emptySet()) =
        kinds.map { Tile.create(it, if (it in wide) TileSize.WIDE else TileSize.SMALL) }

    fun frostedMidnight() = Profile(
        id = Profile.newId(),
        name = "Frosted Midnight",
        tiles = tiles(
            TileKind.WIFI, TileKind.BLUETOOTH, TileKind.MOBILE_DATA, TileKind.FLASHLIGHT,
            TileKind.AIRPLANE, TileKind.DND, TileKind.AUTO_ROTATE, TileKind.LOCATION,
            wide = setOf(TileKind.WIFI, TileKind.BLUETOOTH),
        ),
        style = GlassStyle(),
    )

    fun neonGlass() = Profile(
        id = Profile.newId(),
        name = "Neon Glass",
        tiles = tiles(
            TileKind.WIFI, TileKind.BLUETOOTH, TileKind.FLASHLIGHT, TileKind.DARK_MODE,
            TileKind.HOTSPOT, TileKind.NFC, TileKind.NIGHT_LIGHT, TileKind.SCREEN_CAST,
        ),
        style = GlassStyle(
            columns = 4, cornerDp = 30f, tileOpacity = 0.18f, borderOpacity = 0.6f,
            activeColor = 0xFFFF4FD8, tileBlurDp = 36f, backdrop = BackdropPreset.SUNSET,
        ),
    )

    fun minimal() = Profile(
        id = Profile.newId(),
        name = "Minimal",
        tiles = tiles(
            TileKind.WIFI, TileKind.BLUETOOTH, TileKind.FLASHLIGHT, TileKind.BATTERY_SAVER,
            TileKind.DND, TileKind.AIRPLANE,
        ),
        style = GlassStyle(
            columns = 3, tileHeightDp = 68f, cornerDp = 14f, tileOpacity = 0.12f,
            panelOpacity = 0.2f, borderOpacity = 0.2f, activeColor = 0xFF59E3B0,
            showBrightness = false, backdrop = BackdropPreset.GRAPHITE,
        ),
    )

    /** Names and factories of the built-in templates shown in the "new profile" dialog. */
    val templates: List<Pair<String, () -> Profile>> = listOf(
        "Frosted Midnight" to ::frostedMidnight,
        "Neon Glass" to ::neonGlass,
        "Minimal" to ::minimal,
    )

    fun initialState(): StudioState {
        val first = frostedMidnight()
        return StudioState(listOf(first, neonGlass(), minimal()), first.id)
    }
}
