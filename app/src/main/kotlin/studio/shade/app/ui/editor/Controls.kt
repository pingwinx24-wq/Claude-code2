@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package studio.shade.app.ui.editor

import android.graphics.Color as AColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import studio.shade.app.ui.glass.GlassSurface

@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    GlassSurface(modifier.fillMaxWidth(), cornerDp = 20f, tintAlpha = 0.06f, borderAlpha = 0.18f) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    valueText: (Float) -> String,
    steps: Int = 0,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onChange)
    }
}

@Composable
fun <T> ChipRow(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
        }
    }
}

private val swatches = listOf(
    0xFF7C9CFF, 0xFF59E3B0, 0xFFFF4FD8, 0xFFFFB347, 0xFFFF5F6D, 0xFF48CAE4, 0xFFB388FF, 0xFFFFFFFF,
    0xFF0B1020, 0xFF000000,
)

/** Swatches plus an HSV editor, so any colour can be reached without a colour-picker dependency. */
@Composable
fun ColorPickerRow(title: String, argb: Long, onChange: (Long) -> Unit, allowReset: Boolean = false, onReset: (() -> Unit)? = null) {
    var custom by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (allowReset && onReset != null) {
                    Text("Reset", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { onReset() })
                }
                Text(if (custom) "Hide custom" else "Custom", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { custom = !custom })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { sw ->
                val selected = sw == argb
                Row(
                    Modifier.size(32.dp).clip(CircleShape).background(Color(sw.toInt()))
                        .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable { onChange(sw) },
                ) {}
            }
        }
        if (custom) {
            val hsv = FloatArray(3).also { AColor.colorToHSV(argb.toInt(), it) }
            fun emit(h: Float, s: Float, v: Float) = onChange(AColor.HSVToColor(0xFF, floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFFL)
            SliderRow("Hue", hsv[0], 0f..360f, { emit(it, hsv[1], hsv[2]) }, { "${it.toInt()}°" })
            SliderRow("Saturation", hsv[1], 0f..1f, { emit(hsv[0], it, hsv[2]) }, { "${(it * 100).toInt()}%" })
            SliderRow("Brightness", hsv[2], 0f..1f, { emit(hsv[0], hsv[1], it) }, { "${(it * 100).toInt()}%" })
        }
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false) {
    val c = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(c.copy(alpha = if (enabled) 0.16f else 0.06f))
            .border(1.dp, c.copy(alpha = if (enabled) 0.5f else 0.15f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, color = c.copy(alpha = if (enabled) 1f else 0.4f), style = MaterialTheme.typography.labelLarge)
    }
}
