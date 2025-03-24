package com.jetpackduba.gitnuro.managers

import com.jetpackduba.gitnuro.exceptions.CommandExecutionFailed
import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.logging.printLog
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

private const val TAG = "ShellManager"

interface IShellManager {
    fun runCommand(command: List<String>): String?
    fun runCommandInPath(command: List<String>, path: String)
    fun runCommandWithoutResult(command: List<String>): Boolean
    fun runCommandProcess(command: List<String>): Process
}

class ShellManager @Inject constructor() : IShellManager {
    override fun runCommand(command: List<String>): String? {
        printLog(TAG, "runCommand: " + command.joinToString(" "))

        return try {
            var result: String?

            val processBuilder = ProcessBuilder(command)
            processBuilder.start().inputStream.use { inputStream ->
                Scanner(inputStream).useDelimiter("\\A").use { s ->
                    result = if (s.hasNext())
                        s.next()
                    else
                        null
                }
            }

            result
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    override fun runCommandInPath(command: List<String>, path: String) {
        printLog(TAG, "runCommandInPath: " + command.joinToString(" "))

        val processBuilder = ProcessBuilder(command).apply {
            directory(File(path))
        }

        processBuilder.start()
    }

    override fun runCommandWithoutResult(command: List<String>): Boolean {
        printLog(TAG, "runCommandWithoutResult: " + command.joinToString(" "))
        return try {
            val processBuilder = ProcessBuilder(command)
            val p = processBuilder.start() ?: return false

            try {
                val exitValue = p.exitValue()

                if (exitValue == 0) {
                    printLog(TAG, "Process ended immediately.")
                    false
                } else {
                    printError(TAG, "Process crashed.")
                    false
                }
            } catch (itse: IllegalThreadStateException) {
                printLog(TAG, "Process is running.")
                true
            }
        } catch (e: IOException) {
            printError(TAG, "Error running command: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    override fun runCommandProcess(command: List<String>): Process {
        printLog(TAG, "runCommandProcess: " + command.joinToString(" "))
        try {
            return ProcessBuilder(command).start()
//            return ProcessBuilder(command).start()
        } catch (ex: IOException) {
            throw CommandExecutionFailed(ex.message.orEmpty(), ex)
        }
    }
}

/**
 * Encapsulates [ShellManager] to add the required prefix to commands before running them in a flatpak sandbox environment.
 */
class FlatpakShellManager @Inject constructor(
    private val shellManager: ShellManager,
) : IShellManager {
    private val flatpakPrefix = listOf("/usr/bin/flatpak-spawn", "--host", "--env=TERM=xterm-256color")

    override fun runCommand(command: List<String>): String? {
        return shellManager.runCommand(flatpakPrefix + command)
    }

    override fun runCommandInPath(command: List<String>, path: String) {
        shellManager.runCommandInPath(flatpakPrefix + command, path)
    }

    override fun runCommandWithoutResult(command: List<String>): Boolean {
        return shellManager.runCommandWithoutResult(flatpakPrefix + command)
    }

    override fun runCommandProcess(command: List<String>): Process {
        return shellManager.runCommandProcess(flatpakPrefix + command)
    }

}