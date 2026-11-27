package studio.shade.core

import studio.shade.core.wallpaper.WallCatalog
import studio.shade.core.wallpaper.WallStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WallCatalogTest {
    @Test fun sameSeedSameCarousel() = assertEquals(WallCatalog.build(7), WallCatalog.build(7))

    @Test fun differentSeedDifferentCarousel() = assertNotEquals(WallCatalog.build(7), WallCatalog.build(8))

    @Test fun filtersAreRespected() {
        val p = WallCatalog.palettes.take(2)
        val specs = WallCatalog.build(1, listOf(WallStyle.ORBS), p, count = 30)
        assertEquals(30, specs.size)
        assertTrue(specs.all { it.style == WallStyle.ORBS && it.palette in p })
    }

    @Test fun roundRobinCoversAllStyles() {
        val specs = WallCatalog.build(3, count = WallStyle.entries.size)
        assertEquals(WallStyle.entries.toSet(), specs.map { it.style }.toSet())
    }

    @Test fun paletteIdsUnique() =
        assertEquals(WallCatalog.palettes.size, WallCatalog.palettes.map { it.id }.toSet().size)
}
