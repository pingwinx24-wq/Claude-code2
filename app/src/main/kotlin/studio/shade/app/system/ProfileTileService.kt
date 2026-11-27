package studio.shade.app.system

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import studio.shade.app.R
import studio.shade.app.ShadeApp
import studio.shade.core.model.StudioState

/** A genuine system Quick Settings tile: shows the active profile and cycles to the next on tap. */
class ProfileTileService : TileService() {
    private val repo get() = (application as ShadeApp).repository

    override fun onStartListening() = render(repo.load())

    override fun onClick() {
        val s = repo.load()
        val next = s.profiles[(s.profiles.indexOfFirst { it.id == s.activeId } + 1).mod(s.profiles.size)]
        val updated = StudioState(s.profiles, next.id)
        repo.save(updated)
        render(updated)
    }

    private fun render(state: StudioState) {
        val tile = qsTile ?: return
        tile.label = state.active.name
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.state = Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = "Tap to switch profile"
        tile.updateTile()
    }
}
