package studio.shade.app.ui.walls

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.shade.app.StudioViewModel
import studio.shade.app.ui.editor.PillButton
import studio.shade.app.wallpaper.WallTarget
import studio.shade.app.wallpaper.WallpaperRenderer
import studio.shade.app.wallpaper.WallpaperSetter
import studio.shade.core.wallpaper.WallCatalog
import studio.shade.core.wallpaper.WallSpec
import studio.shade.core.wallpaper.WallStyle
import kotlin.math.absoluteValue

private const val THUMB_WIDTH = 360

/** Swipeable wallpaper carousel (rofi/swww-picker style): browse, filter, shuffle, apply. */
@Composable
fun WallpapersScreen(vm: StudioViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()
    val activeStyle = state.active.style

    val profilePalette = remember(activeStyle.activeColor, activeStyle.panelColor) { WallpaperSetter.profilePalette(activeStyle) }
    val allPalettes = remember(profilePalette) { listOf(profilePalette) + WallCatalog.palettes }

    var styleFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var paletteFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var seedBase by rememberSaveable { mutableLongStateOf(2026L) }

    val specs = remember(styleFilter, paletteFilter, seedBase, allPalettes) {
        WallCatalog.build(
            seedBase = seedBase,
            styles = WallStyle.entries.filter { styleFilter == null || it.name == styleFilter },
            palettes = allPalettes.filter { paletteFilter == null || it.id == paletteFilter },
        )
    }
    val pager = rememberPagerState { specs.size }
    LaunchedEffect(specs) { pager.scrollToPage(0) }

    val (screenW, screenH) = remember { WallpaperSetter.screenSize(context) }
    val ratio = screenW.toFloat() / screenH
    val thumbH = (THUMB_WIDTH / ratio).toInt()
    // Thumbnails are cached per spec; the cache is reset when it grows past a few carousels.
    val thumbs = remember { mutableStateMapOf<WallSpec, ImageBitmap>() }

    var applying by remember { mutableStateOf(false) }
    var chooseTarget by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Wallpapers", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp))

        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            val spec = specs[page]
            LaunchedEffect(spec) {
                if (thumbs[spec] == null) {
                    val img = withContext(Dispatchers.Default) { WallpaperRenderer.render(spec, THUMB_WIDTH, thumbH).asImageBitmap() }
                    if (thumbs.size > 72) thumbs.clear()
                    thumbs[spec] = img
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WallCard(thumbs[spec], ratio, pager, page)
            }
        }

        val current = specs.getOrNull(pager.currentPage)
        Text(
            if (current == null) "" else "${current.style.title} · ${current.palette.title}   ${pager.currentPage + 1}/${specs.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        ChipStrip(listOf<String?>(null) + WallStyle.entries.map { it.name }, styleFilter, { it?.let { n -> WallStyle.valueOf(n).title } ?: "All styles" }) { styleFilter = it }
        ChipStrip(listOf<String?>(null) + allPalettes.map { it.id }, paletteFilter, { id -> allPalettes.firstOrNull { it.id == id }?.title ?: "All palettes" }) { paletteFilter = it }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Shuffle", { seedBase = System.nanoTime() }, Modifier.weight(1f))
            PillButton(if (applying) "Applying…" else "Apply", { chooseTarget = true }, Modifier.weight(1f), enabled = !applying && current != null)
        }
    }

    if (chooseTarget) {
        AlertDialog(
            onDismissRequest = { chooseTarget = false },
            title = { Text("Set wallpaper on") },
            text = {
                Column {
                    WallTarget.entries.forEach { target ->
                        TextButton(onClick = {
                            chooseTarget = false
                            val spec = specs.getOrNull(pager.currentPage) ?: return@TextButton
                            applying = true
                            scope.launch {
                                val result = WallpaperSetter.apply(context, spec, target)
                                applying = false
                                Toast.makeText(
                                    context,
                                    result.fold({ "Wallpaper set: ${target.title}" }, { "Failed: ${it.message}" }),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }) { Text(target.title) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseTarget = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WallCard(image: ImageBitmap?, ratio: Float, pager: PagerState, page: Int) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxHeight()
            .aspectRatio(ratio, matchHeightConstraintsFirst = true)
            .graphicsLayer {
                // Carousel depth: side pages shrink and fade.
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                val s = lerp(1f, 0.86f, offset)
                scaleX = s
                scaleY = s
                alpha = lerp(1f, 0.55f, offset)
            }
            .clip(shape)
            .background(Color(0xFF111628))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape),
    ) {
        if (image == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("12:30", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light)
            Text("Friday, 27 November", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChipStrip(options: List<String?>, selected: String?, label: (String?) -> String, onSelect: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) }) }
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
