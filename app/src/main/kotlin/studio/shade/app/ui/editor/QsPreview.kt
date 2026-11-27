package studio.shade.app.ui.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import studio.shade.app.ui.glass.BackdropHost
import studio.shade.app.ui.glass.GlassSurface
import studio.shade.app.ui.glass.TileIcon
import studio.shade.core.layout.TileLayout
import studio.shade.core.model.Profile
import studio.shade.core.model.Tile
import studio.shade.core.model.TileSize
import kotlin.math.roundToInt

/**
 * Live preview of the Quick Settings panel. It renders straight from the [Profile], so every control
 * change is reflected on the next frame. Tap a tile to select it (and toggle its preview state);
 * long-press and drag to reorder.
 */
@Composable
fun QsPreview(
    profile: Profile,
    activeTiles: Set<String>,
    selectedId: String?,
    onTap: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val st = profile.style
    BackdropHost(
        preset = st.backdrop,
        tileBlurDp = st.tileBlurDp,
        panelBlurDp = st.panelBlurDp,
        modifier = modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp), clip = true),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("12:30", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.weight(1f))
                Text(profile.name, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GlassSurface(
                cornerDp = st.cornerDp + 6f,
                tint = Color(st.panelColor.toInt()),
                tintAlpha = st.panelOpacity,
                borderAlpha = st.borderOpacity,
                panel = true,
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TileGrid(profile, activeTiles, selectedId, onTap, onMove)
                    if (st.showBrightness) BrightnessBar(profile)
                }
            }
        }
    }
}

@Composable
private fun BrightnessBar(profile: Profile) {
    var value by remember { mutableStateOf(0.6f) }
    val st = profile.style
    GlassSurface(Modifier.fillMaxWidth().height(40.dp), cornerDp = 20f, tint = Color(st.inactiveColor.toInt()), tintAlpha = st.tileOpacity * 0.7f, borderAlpha = st.borderOpacity * 0.7f) {
        Slider(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.padding(horizontal = 10.dp).align(Alignment.Center),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(st.activeColor.toInt()),
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            ),
        )
    }
}

@Composable
private fun TileGrid(
    profile: Profile,
    activeTiles: Set<String>,
    selectedId: String?,
    onTap: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val st = profile.style
    val density = LocalDensity.current
    val tiles = profile.tiles
    val placements = TileLayout.place(tiles, st.columns)
    val rows = TileLayout.rowCount(placements)

    val currentTiles by rememberUpdatedState(tiles)
    val currentPlacements by rememberUpdatedState(placements)
    val currentOnMove by rememberUpdatedState(onMove)

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragCenter by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val spacing = with(density) { st.spacingDp.dp.toPx() }
        val cellH = with(density) { st.tileHeightDp.dp.toPx() }
        val cellW = TileLayout.cellWidth(with(density) { maxWidth.toPx() }, st.columns, spacing)
        val totalH = if (rows == 0) 0f else rows * cellH + (rows - 1) * spacing

        if (tiles.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(st.tileHeightDp.dp), contentAlignment = Alignment.Center) {
                Text("No tiles — add some below", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(with(density) { totalH.toDp() })) {
            for (p in placements) {
                val tile = tiles[p.index]
                androidx.compose.runtime.key(tile.id) {
                    val w = TileLayout.spanWidth(p.span, cellW, spacing)
                    val home = IntOffset((p.col * (cellW + spacing)).roundToInt(), (p.row * (cellH + spacing)).roundToInt())
                    val animatedHome by animateIntOffsetAsState(home, spring(stiffness = 500f), label = "tileHome")
                    val dragging = draggingId == tile.id
                    val pos = if (dragging) IntOffset((dragCenter.x - w / 2f).roundToInt(), (dragCenter.y - cellH / 2f).roundToInt()) else animatedHome
                    val lift by animateFloatAsState(if (dragging) 1.06f else 1f, label = "lift")

                    GlassTile(
                        tile = tile,
                        profile = profile,
                        active = tile.id in activeTiles,
                        selected = tile.id == selectedId,
                        modifier = Modifier
                            .offset { pos }
                            .size(with(density) { w.toDp() }, with(density) { cellH.toDp() })
                            .zIndex(if (dragging) 1f else 0f)
                            .scale(lift)
                            .alpha(if (dragging) 0.92f else 1f)
                            .pointerInput(tile.id) { detectTapGestures(onTap = { onTap(tile.id) }) }
                            .pointerInput(tile.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        val cur = currentPlacements.firstOrNull { currentTiles.getOrNull(it.index)?.id == tile.id }
                                        if (cur != null) {
                                            draggingId = tile.id
                                            dragCenter = Offset(
                                                cur.col * (cellW + spacing) + TileLayout.spanWidth(cur.span, cellW, spacing) / 2f,
                                                cur.row * (cellH + spacing) + cellH / 2f,
                                            )
                                        }
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragCenter += amount
                                        val from = currentTiles.indexOfFirst { it.id == tile.id }
                                        val to = TileLayout.nearest(currentPlacements, cellW, cellH, spacing, dragCenter.x, dragCenter.y)
                                        if (from >= 0 && to >= 0 && to != from) currentOnMove(from, to)
                                    },
                                    onDragEnd = { draggingId = null },
                                    onDragCancel = { draggingId = null },
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTile(tile: Tile, profile: Profile, active: Boolean, selected: Boolean, modifier: Modifier) {
    val st = profile.style
    val accent = Color((tile.accent ?: st.activeColor).toInt())
    val inactive = Color(st.inactiveColor.toInt())
    val tint by animateColorAsState(if (active) accent else inactive, label = "tint")
    val alpha by animateFloatAsState(if (active) (st.tileOpacity + 0.45f).coerceAtMost(0.95f) else st.tileOpacity, label = "alpha")
    val fg = if (active && tint.luminance() > 0.6f) Color(0xFF10131F) else Color.White
    val wide = tile.size == TileSize.WIDE && st.columns > 1

    GlassSurface(
        modifier = modifier.then(if (selected) Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(st.cornerDp.dp)) else Modifier),
        cornerDp = st.cornerDp,
        tint = tint,
        tintAlpha = alpha,
        borderAlpha = st.borderOpacity,
    ) {
        val label = tile.displayLabel
        val status = if (active) "On" else "Off"
        if (wide) {
            Row(Modifier.padding(horizontal = 14.dp).align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                TileIcon(tile.kind, fg, Modifier.size(24.dp))
                Spacer(Modifier.size(10.dp))
                Column {
                    if (st.showLabels) Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(status, color = fg.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        } else {
            Column(Modifier.align(Alignment.Center).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TileIcon(tile.kind, fg, Modifier.size(24.dp))
                if (st.showLabels) {
                    Spacer(Modifier.size(4.dp))
                    Text(label, color = fg, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
