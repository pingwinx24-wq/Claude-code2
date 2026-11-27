package studio.shade.core.system

import studio.shade.core.model.Tile

/**
 * Maps editor tiles to the AOSP `sysui_qs_tiles` secure setting (comma separated tile specs).
 * Only specs that pass [isSafe] may ever be handed to a shell.
 */
object QsSpec {
    private val SAFE = Regex("^[A-Za-z0-9_().:-]+(,[A-Za-z0-9_().:-]+)*$")

    fun fromTiles(tiles: List<Tile>): String =
        tiles.mapNotNull { it.kind.aospSpec }.distinct().joinToString(",")

    fun isSafe(csv: String): Boolean = SAFE.matches(csv)
}
