package studio.shade.app.data

import android.content.Context
import studio.shade.core.json.ProfileJson
import studio.shade.core.model.Defaults
import studio.shade.core.model.StudioState

/** Persists the whole [StudioState] as one JSON document in SharedPreferences. */
class ProfileRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("shade_studio", Context.MODE_PRIVATE)

    fun load(): StudioState =
        prefs.getString(KEY_STATE, null)?.let(ProfileJson::decodeState) ?: Defaults.initialState()

    fun save(state: StudioState) {
        prefs.edit().putString(KEY_STATE, ProfileJson.encode(state)).apply()
    }

    /** The system tile list captured before Shizuku wrote to it, so it can be restored. */
    var qsBackup: String?
        get() = prefs.getString(KEY_QS_BACKUP, null)
        set(value) { prefs.edit().putString(KEY_QS_BACKUP, value).apply() }

    var introDismissed: Boolean
        get() = prefs.getBoolean(KEY_INTRO, false)
        set(value) { prefs.edit().putBoolean(KEY_INTRO, value).apply() }

    private companion object {
        const val KEY_INTRO = "intro_dismissed"
        const val KEY_STATE = "state"
        const val KEY_QS_BACKUP = "qs_backup"
    }
}
