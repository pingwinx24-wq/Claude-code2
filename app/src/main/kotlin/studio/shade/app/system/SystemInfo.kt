package studio.shade.app.system

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.shade.app.R

data class DeviceInfo(
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val skin: String,
    val realtimeBlurSupported: Boolean,
)

object SystemInfo {
    suspend fun read(): DeviceInfo = withContext(Dispatchers.IO) {
        val hyperOs = prop("ro.mi.os.version.name")
        val miui = prop("ro.miui.ui.version.name")
        val skin = when {
            hyperOs.isNotBlank() -> "HyperOS ($hyperOs, ${prop("ro.mi.os.version.incremental").ifBlank { "unknown build" }})"
            miui.isNotBlank() -> "MIUI ($miui)"
            else -> "Not a Xiaomi skin (or properties hidden)"
        }
        DeviceInfo(
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            skin = skin,
            realtimeBlurSupported = Build.VERSION.SDK_INT >= 31,
        )
    }

    private fun prop(key: String): String = try {
        val p = ProcessBuilder("getprop", key).redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
    } catch (_: Exception) {
        ""
    }

    /** Result codes of StatusBarManager.requestAddTileService, mapped to messages. */
    @RequiresApi(33)
    fun requestAddTile(context: Context, onResult: (String) -> Unit) {
        val sbm = context.getSystemService(StatusBarManager::class.java)
        if (sbm == null) {
            onResult("Status bar service unavailable")
            return
        }
        sbm.requestAddTileService(
            ComponentName(context, ProfileTileService::class.java),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_tile),
            context.mainExecutor,
        ) { code ->
            onResult(
                when (code) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Tile added"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Tile is already in Quick Settings"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_DIALOG_DISMISSED -> "Request dismissed"
                    else -> "System declined the request (code $code). Add the tile manually from the Quick Settings edit screen."
                },
            )
        }
    }
}
