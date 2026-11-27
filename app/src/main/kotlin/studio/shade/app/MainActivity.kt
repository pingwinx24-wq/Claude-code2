package studio.shade.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import studio.shade.app.ui.editor.EditorScreen
import studio.shade.app.ui.profiles.ProfilesScreen
import studio.shade.app.ui.system.SystemScreen
import studio.shade.app.ui.theme.ShadeColors
import studio.shade.app.ui.theme.ShadeTheme

class MainActivity : ComponentActivity() {
    private val vm: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShadeTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Column(Modifier.fillMaxSize().background(ShadeColors.Bg).statusBarsPadding()) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            0 -> EditorScreen(vm)
                            1 -> ProfilesScreen(vm)
                            else -> SystemScreen(vm)
                        }
                    }
                    NavigationBar(containerColor = Color(0xFF0D1122), windowInsets = WindowInsets.navigationBars) {
                        listOf("Editor", "Profiles", "System").forEachIndexed { i, label ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Text(listOf("◧", "☰", "⚙")[i]) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0x338EA8FF)),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.reload()
    }

    override fun onStop() {
        vm.flush()
        super.onStop()
    }
}
