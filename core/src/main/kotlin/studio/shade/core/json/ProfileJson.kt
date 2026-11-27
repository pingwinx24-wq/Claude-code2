package studio.shade.core.json

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import studio.shade.core.model.BackdropPreset
import studio.shade.core.model.GlassStyle
import studio.shade.core.model.Profile
import studio.shade.core.model.StudioState
import studio.shade.core.model.Tile
import studio.shade.core.model.TileKind
import studio.shade.core.model.TileSize

/** Versioned JSON (de)serialisation. Parsing is lenient: unknown tiles are skipped, bad values clamped. */
object ProfileJson {
    const val VERSION = 1

    fun toJson(profile: Profile): JSONObject = JSONObject().apply {
        put("id", profile.id)
        put("name", profile.name)
        put("tiles", JSONArray().apply { profile.tiles.forEach { put(tileToJson(it)) } })
        put("style", styleToJson(profile.style))
    }

    fun toJson(state: StudioState): JSONObject = JSONObject().apply {
        put("version", VERSION)
        put("activeId", state.activeId)
        put("profiles", JSONArray().apply { state.profiles.forEach { put(toJson(it)) } })
    }

    fun encode(state: StudioState): String = toJson(state).toString()
    fun encode(profile: Profile): String = toJson(profile).put("version", VERSION).toString(2)

    /** Parses stored state; returns null when the text is not valid or holds no profile. */
    fun decodeState(text: String): StudioState? = try {
        val root = JSONObject(text)
        val profiles = root.optJSONArray("profiles").objects().mapNotNull(::profileFromJson)
        if (profiles.isEmpty()) null
        else StudioState(profiles, root.optString("activeId").takeIf { id -> profiles.any { it.id == id } } ?: profiles.first().id)
    } catch (_: JSONException) {
        null
    }

    /** Parses one exported profile. A fresh id is assigned when [regenerateId] is true. */
    fun decodeProfile(text: String, regenerateId: Boolean = true): Profile? = try {
        profileFromJson(JSONObject(text))?.let { if (regenerateId) it.copy(id = Profile.newId()) else it }
    } catch (_: JSONException) {
        null
    }

    private fun profileFromJson(o: JSONObject): Profile? {
        val tiles = o.optJSONArray("tiles").objects().mapNotNull(::tileFromJson)
        val name = o.optString("name").trim()
        if (name.isEmpty()) return null
        val id = o.optString("id").ifBlank { Profile.newId() }
        return Profile(id, name.take(40), tiles, styleFromJson(o.optJSONObject("style")))
    }

    private fun tileToJson(t: Tile) = JSONObject().apply {
        put("id", t.id)
        put("kind", t.kind.id)
        put("size", t.size.name)
        t.label?.let { put("label", it) }
        t.accent?.let { put("accent", it) }
    }

    private fun tileFromJson(o: JSONObject): Tile? {
        val kind = TileKind.fromId(o.optString("kind")) ?: return null
        return Tile(
            id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            kind = kind,
            size = TileSize.entries.firstOrNull { it.name == o.optString("size") } ?: TileSize.SMALL,
            label = if (o.has("label")) o.optString("label").take(24) else null,
            accent = if (o.has("accent")) o.optLong("accent") else null,
        )
    }

    private fun styleToJson(s: GlassStyle) = JSONObject().apply {
        put("columns", s.columns)
        put("tileHeightDp", s.tileHeightDp.toDouble())
        put("spacingDp", s.spacingDp.toDouble())
        put("cornerDp", s.cornerDp.toDouble())
        put("tileBlurDp", s.tileBlurDp.toDouble())
        put("tileOpacity", s.tileOpacity.toDouble())
        put("panelBlurDp", s.panelBlurDp.toDouble())
        put("panelOpacity", s.panelOpacity.toDouble())
        put("borderOpacity", s.borderOpacity.toDouble())
        put("activeColor", s.activeColor)
        put("inactiveColor", s.inactiveColor)
        put("panelColor", s.panelColor)
        put("showLabels", s.showLabels)
        put("showBrightness", s.showBrightness)
        put("backdrop", s.backdrop.id)
    }

    private fun styleFromJson(o: JSONObject?): GlassStyle {
        val d = GlassStyle()
        if (o == null) return d
        return GlassStyle(
            columns = o.optInt("columns", d.columns),
            tileHeightDp = o.optDouble("tileHeightDp", d.tileHeightDp.toDouble()).toFloat(),
            spacingDp = o.optDouble("spacingDp", d.spacingDp.toDouble()).toFloat(),
            cornerDp = o.optDouble("cornerDp", d.cornerDp.toDouble()).toFloat(),
            tileBlurDp = o.optDouble("tileBlurDp", d.tileBlurDp.toDouble()).toFloat(),
            tileOpacity = o.optDouble("tileOpacity", d.tileOpacity.toDouble()).toFloat(),
            panelBlurDp = o.optDouble("panelBlurDp", d.panelBlurDp.toDouble()).toFloat(),
            panelOpacity = o.optDouble("panelOpacity", d.panelOpacity.toDouble()).toFloat(),
            borderOpacity = o.optDouble("borderOpacity", d.borderOpacity.toDouble()).toFloat(),
            activeColor = o.optLong("activeColor", d.activeColor),
            inactiveColor = o.optLong("inactiveColor", d.inactiveColor),
            panelColor = o.optLong("panelColor", d.panelColor),
            showLabels = o.optBoolean("showLabels", d.showLabels),
            showBrightness = o.optBoolean("showBrightness", d.showBrightness),
            backdrop = BackdropPreset.fromId(o.optString("backdrop")),
        ).sanitized()
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
