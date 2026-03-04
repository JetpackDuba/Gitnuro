package com.jetpackduba.gitnuro.system

import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.ShellManager
import javax.inject.Inject

private const val TAG = "OpenPathInSystemGitAction"

/**
 * Open a directory in the file explorer.
 * A use case for this is opening a repository's folder in the file explorer to view or modify the files
 */
class OpenPathInSystemGitAction @Inject constructor(
    /**
     * We want specifically [ShellManager] implementation and not [com.jetpackduba.gitnuro.domain.IShellManager],
     * to run commands without any modification
     * (such as ones done by [com.jetpackduba.gitnuro.domain.FlatpakShellManager], because it has to run in the sandbox)
     */
    private val shellManager: ShellManager,
) {
    operator fun invoke(path: String): Boolean {
        when (currentOs) {
            OS.LINUX -> {
                if (shellManager.runCommandWithoutResult(listOf("xdg-open", path)))
                    return true
                if (shellManager.runCommandWithoutResult(listOf("kde-open", path)))
                    return true
                if (shellManager.runCommandWithoutResult(listOf("gnome-open", path)))
                    return true
            }

            OS.WINDOWS -> if (shellManager.runCommandWithoutResult(listOf("explorer", path))) return true
            OS.MAC -> if (shellManager.runCommandWithoutResult(listOf("open", path))) return true
            else -> printError(TAG, "Unknown OS")
        }

        return false
    }
}