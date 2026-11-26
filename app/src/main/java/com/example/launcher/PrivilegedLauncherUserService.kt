package com.example.launcher

import android.content.Context
import com.example.IRemoteShellService
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class PrivilegedLauncherUserService : IRemoteShellService.Stub {

    @Suppress("unused")
    constructor(context: Context) : super()

    @Suppress("unused")
    constructor() : super()

    override fun destroy() {
        exitProcess(0)
    }

    override fun runPrivilegedCommand(args: Array<out String>): String {
        val isAmStart = args.size >= 3 && args[0] == "am" && args[1] == "start"
        val isStatusBarCmd = args.size >= 3 && args[0] == "cmd" && args[1] == "statusbar"
        if (!isAmStart && !isStatusBarCmd) {
            return "Unsupported privileged command."
        }
        return try {
            val process = ProcessBuilder(*args)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(8, TimeUnit.SECONDS)
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (!finished) {
                process.destroyForcibly()
                return "Timed out waiting for privileged command."
            }
            val exitCode = process.exitValue()
            val hasError = output.contains("Error:", ignoreCase = true) ||
                output.contains("Exception", ignoreCase = true) ||
                output.contains("Permission Denial", ignoreCase = true) ||
                output.contains("Unknown command", ignoreCase = true)
            if (exitCode == 0 && !hasError) {
                ""
            } else {
                output.ifBlank { "Command failed with exit code $exitCode" }
            }
        } catch (t: Throwable) {
            t.localizedMessage ?: "Failed to execute privileged command"
        }
    }
}
