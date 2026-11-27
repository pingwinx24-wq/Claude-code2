package studio.shade.app.ui.profiles

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import studio.shade.app.StudioViewModel
import studio.shade.app.ui.editor.PillButton
import studio.shade.app.ui.editor.Section
import studio.shade.core.model.Defaults
import studio.shade.core.model.Profile

@Composable
fun ProfilesScreen(vm: StudioViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var renaming by remember { mutableStateOf<Profile?>(null) }
    var creating by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Profiles", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Each profile stores its tiles, order, sizes and glass style. The active profile is the one shown in the editor and by the Shade profile tile.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.profiles.forEach { p ->
            Section(p.name) {
                Row(Modifier.fillMaxWidth().clickable { vm.selectProfile(p.id) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = p.id == state.activeId, onClick = { vm.selectProfile(p.id) })
                    Text(
                        "${p.tiles.size} tiles · ${p.style.columns} columns" + if (p.id == state.activeId) " · active" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Rename", { renaming = p })
                    PillButton("Duplicate", { vm.duplicateProfile(p.id) })
                    PillButton("Delete", { vm.deleteProfile(p.id) }, enabled = state.profiles.size > 1, danger = true)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("New profile", { creating = true })
            PillButton("Import", { importing = true })
            PillButton("Copy active as JSON", {
                clipboard.setText(AnnotatedString(vm.exportActive()))
                toast("Profile JSON copied")
            })
        }
    }

    renaming?.let { p ->
        TextDialog("Rename profile", p.name, "Save", { renaming = null }) { vm.renameProfile(p.id, it); renaming = null }
    }
    if (importing) {
        TextDialog("Paste exported profile JSON", clipboard.getText()?.text.orEmpty().takeIf { it.trimStart().startsWith("{") } ?: "", "Import", { importing = false }, singleLine = false) {
            toast(if (vm.importProfile(it)) "Profile imported" else "Not a valid Shade Studio profile")
            importing = false
        }
    }
    if (creating) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New profile from template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name (optional)") }, singleLine = true)
                    Defaults.templates.forEach { (title, factory) ->
                        TextButton(onClick = { vm.createProfile(factory(), name); creating = false }) { Text(title) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TextDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, singleLine: Boolean = true, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = singleLine, maxLines = if (singleLine) 1 else 8, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
