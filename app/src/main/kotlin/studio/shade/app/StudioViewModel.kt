package studio.shade.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import studio.shade.core.json.ProfileJson
import studio.shade.core.layout.TileLayout
import studio.shade.core.model.Defaults
import studio.shade.core.model.GlassStyle
import studio.shade.core.model.Profile
import studio.shade.core.model.StudioState
import studio.shade.core.model.Tile
import studio.shade.core.model.TileKind
import studio.shade.core.system.QsSpec

/** Single source of truth for the editor. Every mutation goes through [StudioState] and is persisted. */
@OptIn(FlowPreview::class)
class StudioViewModel(app: Application) : AndroidViewModel(app) {
    private val shade = app as ShadeApp
    private val repo = shade.repository
    val shizuku = shade.shizuku

    private val _state = MutableStateFlow(repo.load())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    private val _selectedTileId = MutableStateFlow<String?>(null)
    val selectedTileId: StateFlow<String?> = _selectedTileId.asStateFlow()

    /** Preview-only on/off state of tiles (tapping a tile in the preview toggles it). */
    private val _activeTiles = MutableStateFlow(setOf<String>())
    val activeTiles: StateFlow<Set<String>> = _activeTiles.asStateFlow()

    init {
        viewModelScope.launch { _state.drop(1).debounce(250).collect { repo.save(it) } }
    }

    fun flush() = repo.save(_state.value)

    /** Re-reads storage (the QS tile can change the active profile while the app is in the background). */
    fun reload() { _state.value = repo.load() }

    override fun onCleared() = flush()

    // --- selection / preview ---------------------------------------------------------------
    fun selectTile(id: String?) { _selectedTileId.value = id }
    fun togglePreview(id: String) = _activeTiles.update { if (id in it) it - id else it + id }

    // --- active profile edits --------------------------------------------------------------
    fun updateStyle(transform: (GlassStyle) -> GlassStyle) =
        _state.update { s -> s.updateActive { it.copy(style = transform(it.style).sanitized()) } }

    fun addTile(kind: TileKind) {
        val tile = Tile.create(kind)
        _state.update { s -> s.updateActive { it.copy(tiles = it.tiles + tile) } }
        _selectedTileId.value = tile.id
    }

    fun removeTile(id: String) {
        _state.update { s -> s.updateActive { p -> p.copy(tiles = p.tiles.filterNot { it.id == id }) } }
        if (_selectedTileId.value == id) _selectedTileId.value = null
    }

    fun updateTile(id: String, transform: (Tile) -> Tile) =
        _state.update { s -> s.updateActive { p -> p.copy(tiles = p.tiles.map { if (it.id == id) transform(it) else it }) } }

    fun moveTile(from: Int, to: Int) =
        _state.update { s -> s.updateActive { it.copy(tiles = TileLayout.move(it.tiles, from, to)) } }

    // --- profiles --------------------------------------------------------------------------
    fun selectProfile(id: String) {
        _state.update { if (it.profiles.any { p -> p.id == id }) it.copy(activeId = id) else it }
        _selectedTileId.value = null
    }

    fun createProfile(template: Profile = Defaults.frostedMidnight(), name: String? = null) {
        val p = template.copy(id = Profile.newId(), name = (name?.takeIf { it.isNotBlank() } ?: template.name).trim().take(40))
        _state.update { it.copy(profiles = it.profiles + p, activeId = p.id) }
        _selectedTileId.value = null
    }

    fun duplicateProfile(id: String) {
        val src = _state.value.profiles.firstOrNull { it.id == id } ?: return
        createProfile(src.copy(tiles = src.tiles.map { it.copy(id = Tile.create(it.kind).id) }), "${src.name} copy")
    }

    fun renameProfile(id: String, name: String) {
        val clean = name.trim().take(40)
        if (clean.isEmpty()) return
        _state.update { s -> s.copy(profiles = s.profiles.map { if (it.id == id) it.copy(name = clean) else it }) }
    }

    fun deleteProfile(id: String) = _state.update { s ->
        if (s.profiles.size <= 1) s
        else {
            val remaining = s.profiles.filterNot { it.id == id }
            s.copy(profiles = remaining, activeId = if (s.activeId == id) remaining.first().id else s.activeId)
        }
    }

    fun exportActive(): String = ProfileJson.encode(_state.value.active)

    /** Returns true if [text] was a valid exported profile and was added. */
    fun importProfile(text: String): Boolean {
        val p = ProfileJson.decodeProfile(text.trim()) ?: return false
        _state.update { it.copy(profiles = it.profiles + p, activeId = p.id) }
        return true
    }

    // --- Shizuku-backed system features ----------------------------------------------------
    fun systemTileSpec(): String = QsSpec.fromTiles(_state.value.active.tiles)

    suspend fun readSystemTiles() = shizuku.readQsTiles()

    /** Saves the current system value as a backup (first time only) and writes the active profile's tile order. */
    suspend fun applyToSystem(): Result<String> {
        val csv = systemTileSpec()
        if (csv.isEmpty()) return Result.failure(IllegalStateException("No tiles with a system equivalent"))
        if (repo.qsBackup == null) {
            val current = shizuku.readQsTiles().getOrElse { return Result.failure(it) }
            repo.qsBackup = current
        }
        return shizuku.writeQsTiles(csv).map { csv }
    }

    fun hasSystemBackup(): Boolean = repo.qsBackup != null

    suspend fun restoreSystem(): Result<Unit> {
        val backup = repo.qsBackup ?: return Result.failure(IllegalStateException("No backup stored"))
        return shizuku.restoreQsTiles(backup).onSuccess { repo.qsBackup = null }
    }
}
