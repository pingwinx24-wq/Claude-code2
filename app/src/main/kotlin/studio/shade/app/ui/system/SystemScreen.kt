package studio.shade.app.ui.system

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import studio.shade.app.StudioViewModel
import studio.shade.app.system.DeviceInfo
import studio.shade.app.system.ShizukuBridge
import studio.shade.app.system.ShizukuStatus
import studio.shade.app.system.SystemInfo
import studio.shade.app.ui.editor.PillButton
import studio.shade.app.ui.editor.Section

@Composable
fun SystemScreen(vm: StudioViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by vm.shizuku.status.collectAsState()
    var info by remember { mutableStateOf<DeviceInfo?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmApply by remember { mutableStateOf(false) }
    var backupPresent by remember { mutableStateOf(vm.hasSystemBackup()) }

    LaunchedEffect(Unit) { info = SystemInfo.read(); vm.shizuku.refresh() }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("System", style = MaterialTheme.typography.headlineSmall)

        Section("What this app can and cannot do") {
            Text(
                "Android does not let ordinary apps restyle the system Quick Settings / Control Center — it is drawn by the privileged SystemUI process. " +
                    "Shade Studio therefore designs and previews your layout, ships its own real Quick Settings tile, and (optionally, via Shizuku) writes the " +
                    "standard AOSP tile list. It does not change the system panel's blur, colours or geometry.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section("This device") {
            val d = info
            if (d == null) Text("Reading…") else {
                Info("Shade Studio version", appVersion(context))
                Info("Device", d.model)
                Info("Android", "${d.androidVersion} (API ${d.sdk})")
                Info("Vendor skin", d.skin)
                Info("Real-time blur in preview", if (d.realtimeBlurSupported) "Supported (API 31+)" else "Not available below Android 12 — tint only")
            }
        }

        Section("Quick Settings tile (no extra permission)") {
            Text("Adds a \"Shade profile\" tile to the system panel. Tapping it cycles the active profile.", style = MaterialTheme.typography.bodySmall)
            PillButton(
                "Add tile to Quick Settings",
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) SystemInfo.requestAddTile(context) { message = it }
                    else message = "Android 13+ is needed for the add-tile prompt. Open the Quick Settings edit screen and drag \"Shade profile\" in manually."
                },
            )
        }

        Section("Shizuku (optional, experimental)") {
            Info("Status", statusText(status))
            when (status) {
                ShizukuStatus.NOT_INSTALLED -> {
                    Text("Install the Shizuku app from its official project page, start it (wireless debugging or ADB), then return here.", style = MaterialTheme.typography.bodySmall)
                    PillButton("Open Shizuku project page", {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    })
                }
                ShizukuStatus.NOT_RUNNING -> {
                    Text("Shizuku is installed but its service is not running. Open Shizuku and start it, then refresh.", style = MaterialTheme.typography.bodySmall)
                    PillButton("Open Shizuku", {
                        context.packageManager.getLaunchIntentForPackage(ShizukuBridge.MANAGER_PACKAGE)?.let { context.startActivity(it) }
                    })
                    PillButton("Refresh", { vm.shizuku.refresh() })
                }
                ShizukuStatus.UNSUPPORTED_VERSION -> Text("The running Shizuku is too old (pre-v11). Update it.", style = MaterialTheme.typography.bodySmall)
                ShizukuStatus.NEEDS_PERMISSION -> PillButton("Grant Shizuku permission", { vm.shizuku.requestPermission() })
                ShizukuStatus.PERMISSION_DENIED -> Text("Permission was denied. Re-enable it for Shade Studio inside the Shizuku app.", style = MaterialTheme.typography.bodySmall)
                ShizukuStatus.READY -> {
                    Text(
                        "Writes the AOSP setting secure:sysui_qs_tiles. Whether HyperOS' Control Center honours this setting has NOT been verified by this project — " +
                            "use \"Read\" first, and \"Restore\" if anything looks wrong.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Info("Would write", vm.systemTileSpec().ifEmpty { "(no system equivalent)" })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("Read current", {
                            scope.launch { message = vm.readSystemTiles().fold({ "sysui_qs_tiles = $it" }, { "Failed: ${it.message}" }) }
                        })
                        PillButton("Apply", { confirmApply = true })
                        PillButton("Restore", {
                            scope.launch {
                                message = vm.restoreSystem().fold({ "Restored previous value" }, { "Failed: ${it.message}" })
                                backupPresent = vm.hasSystemBackup()
                            }
                        }, enabled = backupPresent, danger = true)
                    }
                }
            }
        }

        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
    }

    if (confirmApply) {
        AlertDialog(
            onDismissRequest = { confirmApply = false },
            title = { Text("Write system tile list?") },
            text = { Text("This changes a system setting through Shizuku. The current value is saved first so you can restore it. HyperOS may ignore it or show unexpected tiles.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmApply = false
                    scope.launch {
                        message = vm.applyToSystem().fold({ "Wrote: $it" }, { "Failed: ${it.message}" })
                        backupPresent = vm.hasSystemBackup()
                    }
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { confirmApply = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Info(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun statusText(s: ShizukuStatus) = when (s) {
    ShizukuStatus.NOT_INSTALLED -> "Not installed"
    ShizukuStatus.NOT_RUNNING -> "Installed, service not running"
    ShizukuStatus.UNSUPPORTED_VERSION -> "Unsupported (too old)"
    ShizukuStatus.NEEDS_PERMISSION -> "Running — permission needed"
    ShizukuStatus.PERMISSION_DENIED -> "Running — permission denied"
    ShizukuStatus.READY -> "Ready"
}

private fun appVersion(context: android.content.Context): String = try {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
} catch (_: Exception) {
    "unknown"
}
