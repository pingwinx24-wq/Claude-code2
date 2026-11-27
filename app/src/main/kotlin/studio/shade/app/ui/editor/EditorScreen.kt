package studio.shade.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import studio.shade.app.StudioViewModel
import studio.shade.core.model.BackdropPreset
import studio.shade.core.model.GlassStyle
import studio.shade.core.model.Profile
import studio.shade.core.model.Tile
import studio.shade.core.model.TileKind
import studio.shade.core.model.TileSize
import kotlin.math.roundToInt

private val tabs = listOf("Tiles", "Layout", "Glass", "Colors")

@Composable
fun EditorScreen(vm: StudioViewModel, onOpenSystem: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()
    val selectedId by vm.selectedTileId.collectAsState()
    val activeTiles by vm.activeTiles.collectAsState()
    val profile = state.active
    val showIntro by vm.showIntro.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showIntro) {
            Section("How Shade Studio works") {
                Text(
                    "This screen is a designer and live preview. Android does not let apps replace the system shade or " +
                        "Xiaomi's Control Center, so your design is not applied there automatically. What does reach the " +
                        "system: the \"Shade profile\" Quick Settings tile and, with Shizuku, the tile order. See the System tab.",
                    style = MaterialTheme.typography.bodySmall,
                )
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Open System tab", onOpenSystem)
                    PillButton("Got it", { vm.dismissIntro() })
                }
            }
        }
        QsPreview(
            profile = profile,
            activeTiles = activeTiles,
            selectedId = selectedId,
            onTap = { id -> vm.selectTile(id); vm.togglePreview(id) },
            onMove = vm::moveTile,
        )
        Text(
            "Tap a tile to select/toggle it · long-press and drag to reorder",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ScrollableTabRow(selectedTabIndex = tab, containerColor = Color.Transparent, edgePadding = 0.dp) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> TilesTab(vm, profile, selectedId)
            1 -> LayoutTab(vm, profile.style)
            2 -> GlassTab(vm, profile.style)
            else -> ColorsTab(vm, profile.style)
        }
    }
}

@Composable
private fun TilesTab(vm: StudioViewModel, profile: Profile, selectedId: String?) {
    val selected = profile.tiles.firstOrNull { it.id == selectedId }
    if (selected != null) {
        Section("Selected: ${selected.kind.defaultLabel}") {
            ChipRow(TileSize.entries, selected.size, { if (it == TileSize.SMALL) "Small (1×1)" else "Wide (2×1)" }) { size ->
                vm.updateTile(selected.id) { it.copy(size = size) }
            }
            OutlinedTextField(
                value = selected.label.orEmpty(),
                onValueChange = { v -> vm.updateTile(selected.id) { it.copy(label = v.take(24).ifEmpty { null }) } },
                label = { Text("Custom label (blank = ${selected.kind.defaultLabel})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth(),
            )
            ColorPickerRow(
                title = "Accent override",
                argb = selected.accent ?: profile.style.activeColor,
                onChange = { c -> vm.updateTile(selected.id) { it.copy(accent = c) } },
                allowReset = selected.accent != null,
                onReset = { vm.updateTile(selected.id) { it.copy(accent = null) } },
            )
            PillButton("Remove tile", onClick = { vm.removeTile(selected.id) }, danger = true)
        }
    } else {
        Section("Edit a tile") {
            Text("Select a tile in the preview to change its size, label and accent colour.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Section("Add tile") {
        ChipRow(TileKind.entries, null, { it.defaultLabel }) { vm.addTile(it) }
    }
}

@Composable
private fun LayoutTab(vm: StudioViewModel, s: GlassStyle) {
    Section("Grid") {
        SliderRow("Columns", s.columns.toFloat(), 2f..5f, { v -> vm.updateStyle { it.copy(columns = v.roundToInt()) } }, { it.roundToInt().toString() }, steps = 2)
        SliderRow("Tile height", s.tileHeightDp, GlassStyle.Limits.TILE_HEIGHT, { v -> vm.updateStyle { it.copy(tileHeightDp = v) } }, { "${it.roundToInt()} dp" })
        SliderRow("Spacing", s.spacingDp, GlassStyle.Limits.SPACING, { v -> vm.updateStyle { it.copy(spacingDp = v) } }, { "${it.roundToInt()} dp" })
        SliderRow("Corner radius", s.cornerDp, GlassStyle.Limits.CORNER, { v -> vm.updateStyle { it.copy(cornerDp = v) } }, { "${it.roundToInt()} dp" })
    }
    Section("Content") {
        SwitchRow("Show tile labels", s.showLabels) { v -> vm.updateStyle { it.copy(showLabels = v) } }
        SwitchRow("Show brightness slider", s.showBrightness) { v -> vm.updateStyle { it.copy(showBrightness = v) } }
    }
}

@Composable
private fun GlassTab(vm: StudioViewModel, s: GlassStyle) {
    Section("Tiles") {
        SliderRow("Blur", s.tileBlurDp, GlassStyle.Limits.BLUR, { v -> vm.updateStyle { it.copy(tileBlurDp = v) } }, { "${it.roundToInt()} dp" })
        SliderRow("Transparency (fill opacity)", s.tileOpacity, 0f..1f, { v -> vm.updateStyle { it.copy(tileOpacity = v) } }, { "${(it * 100).roundToInt()}%" })
    }
    Section("Panel") {
        SliderRow("Blur", s.panelBlurDp, GlassStyle.Limits.BLUR, { v -> vm.updateStyle { it.copy(panelBlurDp = v) } }, { "${it.roundToInt()} dp" })
        SliderRow("Transparency (fill opacity)", s.panelOpacity, 0f..1f, { v -> vm.updateStyle { it.copy(panelOpacity = v) } }, { "${(it * 100).roundToInt()}%" })
        SliderRow("Border highlight", s.borderOpacity, 0f..1f, { v -> vm.updateStyle { it.copy(borderOpacity = v) } }, { "${(it * 100).roundToInt()}%" })
    }
    Section("Preview wallpaper") {
        ChipRow(BackdropPreset.entries, s.backdrop, { it.title }) { p -> vm.updateStyle { it.copy(backdrop = p) } }
        Text(
            "The wallpaper only exists in this preview so you can judge the glass effect.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorsTab(vm: StudioViewModel, s: GlassStyle) {
    Section("Palette") {
        ColorPickerRow("Active tile colour", s.activeColor, { c -> vm.updateStyle { it.copy(activeColor = c) } })
        ColorPickerRow("Inactive tile tint", s.inactiveColor, { c -> vm.updateStyle { it.copy(inactiveColor = c) } })
        ColorPickerRow("Panel tint", s.panelColor, { c -> vm.updateStyle { it.copy(panelColor = c) } })
    }
}
