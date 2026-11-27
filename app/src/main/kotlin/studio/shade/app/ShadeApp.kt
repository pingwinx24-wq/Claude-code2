package studio.shade.app

import android.app.Application
import studio.shade.app.data.ProfileRepository
import studio.shade.app.system.ShizukuBridge

class ShadeApp : Application() {
    val repository by lazy { ProfileRepository(this) }
    val shizuku by lazy { ShizukuBridge(this) }
}
