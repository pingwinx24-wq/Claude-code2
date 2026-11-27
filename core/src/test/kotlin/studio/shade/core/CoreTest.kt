package studio.shade.core

import studio.shade.core.json.ProfileJson
import studio.shade.core.layout.TileLayout
import studio.shade.core.model.Defaults
import studio.shade.core.model.GlassStyle
import studio.shade.core.model.Profile
import studio.shade.core.model.Tile
import studio.shade.core.model.TileKind
import studio.shade.core.model.TileSize
import studio.shade.core.system.QsSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TileLayoutTest {
    private fun t(kind: TileKind, size: TileSize = TileSize.SMALL) = Tile.create(kind, size)

    @Test fun flowsSmallTilesAcrossRows() {
        val tiles = List(5) { t(TileKind.WIFI) }
        val p = TileLayout.place(tiles, 2)
        assertEquals(listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1, 2 to 0), p.map { it.row to it.col })
        assertEquals(3, TileLayout.rowCount(p))
    }

    @Test fun wideTileWrapsWhenItDoesNotFit() {
        val tiles = listOf(t(TileKind.WIFI), t(TileKind.NFC, TileSize.WIDE), t(TileKind.DND))
        val p = TileLayout.place(tiles, 2)
        assertEquals(0 to 0, p[0].row to p[0].col)
        assertEquals(1 to 0, p[1].row to p[1].col) // wrapped
        assertEquals(2 to 0, p[2].row to p[2].col)
    }

    @Test fun wideTileIsCappedInSingleColumnGrid() {
        val p = TileLayout.place(listOf(t(TileKind.WIFI, TileSize.WIDE)), 1)
        assertEquals(1, p.single().span)
    }

    @Test fun nearestFindsTileUnderPoint() {
        val tiles = List(4) { t(TileKind.WIFI) }
        val p = TileLayout.place(tiles, 2)
        // cell 100x50, spacing 10: tile 3 centre = (110+50, 60+25)
        assertEquals(3, TileLayout.nearest(p, 100f, 50f, 10f, 160f, 85f))
        assertEquals(0, TileLayout.nearest(p, 100f, 50f, 10f, 1f, 1f))
        assertEquals(-1, TileLayout.nearest(emptyList(), 100f, 50f, 10f, 0f, 0f))
    }

    @Test fun moveReorders() {
        assertEquals(listOf(2, 0, 1), TileLayout.move(listOf(0, 1, 2), 2, 0))
        assertEquals(listOf(0, 1, 2), TileLayout.move(listOf(0, 1, 2), 1, 1))
        assertEquals(listOf(0, 1, 2), TileLayout.move(listOf(0, 1, 2), 5, 0))
    }

    @Test fun cellMath() {
        assertEquals(17.5f, TileLayout.cellWidth(100f, 4, 10f))
        assertEquals(45f, TileLayout.spanWidth(2, 17.5f, 10f))
    }
}

class ProfileJsonTest {
    @Test fun stateRoundTrips() {
        val state = Defaults.initialState()
        val decoded = assertNotNull(ProfileJson.decodeState(ProfileJson.encode(state)))
        assertEquals(state, decoded)
    }

    @Test fun customValuesSurvive() {
        val tile = Tile.create(TileKind.NFC, TileSize.WIDE).copy(label = "Pay", accent = 0xFF112233)
        val style = GlassStyle(columns = 3, tileBlurDp = 12f, activeColor = 0xFFABCDEF)
        val p = Profile("x", "Mine", listOf(tile), style)
        assertEquals(p, ProfileJson.decodeProfile(ProfileJson.encode(p), regenerateId = false))
    }

    @Test fun importAssignsFreshId() {
        val p = Defaults.minimal()
        assertNotEquals(p.id, ProfileJson.decodeProfile(ProfileJson.encode(p))!!.id)
    }

    @Test fun garbageIsRejected() {
        assertNull(ProfileJson.decodeState("not json"))
        assertNull(ProfileJson.decodeState("{\"profiles\":[]}"))
        assertNull(ProfileJson.decodeProfile("{}"))
    }

    @Test fun unknownTilesSkippedAndValuesClamped() {
        val json = """{"name":"P","tiles":[{"kind":"warp-drive"},{"kind":"wifi"}],
            "style":{"columns":99,"tileBlurDp":-5,"tileOpacity":7}}"""
        val p = assertNotNull(ProfileJson.decodeProfile(json))
        assertEquals(1, p.tiles.size)
        assertEquals(5, p.style.columns)
        assertEquals(0f, p.style.tileBlurDp)
        assertEquals(1f, p.style.tileOpacity)
    }
}

class QsSpecTest {
    @Test fun buildsDistinctCsvSkippingUnmapped() {
        val tiles = listOf(TileKind.WIFI, TileKind.BLUETOOTH, TileKind.WIFI).map { Tile.create(it) }
        assertEquals("internet,bt", QsSpec.fromTiles(tiles))
    }

    @Test fun everyCatalogSpecIsShellSafe() {
        TileKind.entries.forEach { k -> k.aospSpec?.let { assertTrue(QsSpec.isSafe(it), it) } }
    }

    @Test fun rejectsInjection() {
        assertTrue(!QsSpec.isSafe("wifi;rm -rf /"))
        assertTrue(!QsSpec.isSafe("a b"))
        assertTrue(!QsSpec.isSafe(""))
        assertTrue(!QsSpec.isSafe("a,,b"))
        assertTrue(!QsSpec.isSafe("a'"))
    }
}

class StyleTest {
    @Test fun sanitizeClamps() {
        val s = GlassStyle(columns = 0, tileHeightDp = 1000f, cornerDp = -1f, panelOpacity = 2f).sanitized()
        assertEquals(2, s.columns)
        assertEquals(110f, s.tileHeightDp)
        assertEquals(0f, s.cornerDp)
        assertEquals(1f, s.panelOpacity)
    }
}
