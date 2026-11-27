package studio.shade.core.layout

import studio.shade.core.model.Tile
import kotlin.math.hypot

/** Grid position of the tile at [index] in the tile list. */
data class Placement(val index: Int, val row: Int, val col: Int, val span: Int)

object TileLayout {
    /**
     * Flows tiles left-to-right. A tile wider than the space left on its row wraps to the next row;
     * spans are capped at [columns] so wide tiles still fit in a 1-column grid.
     */
    fun place(tiles: List<Tile>, columns: Int): List<Placement> {
        require(columns >= 1)
        val out = ArrayList<Placement>(tiles.size)
        var row = 0
        var col = 0
        tiles.forEachIndexed { i, tile ->
            val span = tile.size.span.coerceAtMost(columns)
            if (col + span > columns) {
                row++
                col = 0
            }
            out += Placement(i, row, col, span)
            col += span
            if (col >= columns) {
                row++
                col = 0
            }
        }
        return out
    }

    fun rowCount(placements: List<Placement>): Int = (placements.maxOfOrNull { it.row } ?: -1) + 1

    /** Pixel width of a tile spanning [span] columns. */
    fun spanWidth(span: Int, cellWidth: Float, spacing: Float): Float = span * cellWidth + (span - 1) * spacing

    /** Width of one column given the container width. */
    fun cellWidth(containerWidth: Float, columns: Int, spacing: Float): Float =
        (containerWidth - spacing * (columns - 1)) / columns

    /** Returns the index of the tile whose centre is closest to ([x], [y]) in pixels, or -1 if empty. */
    fun nearest(
        placements: List<Placement>,
        cellWidth: Float,
        cellHeight: Float,
        spacing: Float,
        x: Float,
        y: Float,
    ): Int {
        var best = -1
        var bestDist = Float.MAX_VALUE
        for (p in placements) {
            val cx = p.col * (cellWidth + spacing) + spanWidth(p.span, cellWidth, spacing) / 2f
            val cy = p.row * (cellHeight + spacing) + cellHeight / 2f
            val d = hypot(cx - x, cy - y)
            if (d < bestDist) {
                bestDist = d
                best = p.index
            }
        }
        return best
    }

    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val mutable = list.toMutableList()
        mutable.add(to, mutable.removeAt(from))
        return mutable
    }
}
