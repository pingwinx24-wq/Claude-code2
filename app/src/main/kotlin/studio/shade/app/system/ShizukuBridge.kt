package studio.shade.app.system

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import studio.shade.core.system.QsSpec

enum class ShizukuStatus { NOT_INSTALLED, NOT_RUNNING, UNSUPPORTED_VERSION, NEEDS_PERMISSION, PERMISSION_DENIED, READY }

data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val ok get() = exitCode == 0
}

/**
 * Thin wrapper over Shizuku. The only operations exposed are reading/writing the AOSP
 * `sysui_qs_tiles` secure setting; arguments are validated by [QsSpec.isSafe] before reaching a shell.
 */
class ShizukuBridge(private val context: Context) {
    private val _status = MutableStateFlow(computeStatus())
    val status: StateFlow<ShizukuStatus> = _status

    private val permissionCode = 4242

    init {
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener { refresh() }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
    }

    fun refresh() { _status.value = computeStatus() }

    fun requestPermission() {
        if (_status.value == ShizukuStatus.NEEDS_PERMISSION) Shizuku.requestPermission(permissionCode)
    }

    private fun managerInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(MANAGER_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun computeStatus(): ShizukuStatus = try {
        when {
            !Shizuku.pingBinder() ->
                if (managerInstalled()) ShizukuStatus.NOT_RUNNING else ShizukuStatus.NOT_INSTALLED
            Shizuku.isPreV11() -> ShizukuStatus.UNSUPPORTED_VERSION
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuStatus.READY
            Shizuku.shouldShowRequestPermissionRationale() -> ShizukuStatus.PERMISSION_DENIED
            else -> ShizukuStatus.NEEDS_PERMISSION
        }
    } catch (_: Throwable) {
        if (managerInstalled()) ShizukuStatus.NOT_RUNNING else ShizukuStatus.NOT_INSTALLED
    }

    suspend fun readQsTiles(): Result<String> = shell("settings get secure $QS_KEY").map {
        if (!it.ok) error(it.stderr.ifBlank { "settings get failed (exit ${it.exitCode})" })
        it.stdout.trim()
    }

    suspend fun writeQsTiles(csv: String): Result<Unit> {
        if (!QsSpec.isSafe(csv)) return Result.failure(IllegalArgumentException("Unsafe tile list"))
        return shell("settings put secure $QS_KEY '$csv'").mapCatching {
            if (!it.ok) error(it.stderr.ifBlank { "settings put failed (exit ${it.exitCode})" })
        }
    }

    /** Restores a previously saved raw value; "null" (the output of `settings get` for unset) deletes the key. */
    suspend fun restoreQsTiles(previous: String): Result<Unit> =
        if (previous == "null" || previous.isBlank()) shell("settings delete secure $QS_KEY").mapCatching {
            if (!it.ok) error(it.stderr.ifBlank { "settings delete failed" })
        } else writeQsTiles(previous)

    private suspend fun shell(command: String): Result<ShellResult> = withContext(Dispatchers.IO) {
        runCatching {
            check(computeStatus() == ShizukuStatus.READY) { "Shizuku is not ready" }
            // Shizuku.newProcess is private since API 13; reflection is the documented stop-gap.
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java,
            ).apply { isAccessible = true }
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val out = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            ShellResult(process.waitFor(), out, err)
        }
    }

    companion object {
        const val MANAGER_PACKAGE = "moe.shizuku.privileged.api"
        const val QS_KEY = "sysui_qs_tiles"
    }
}
