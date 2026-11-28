package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.common.extensions.TAG
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.domain.FlatpakShellManager
import com.jetpackduba.gitnuro.domain.ShellManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.util.FS_POSIX
import org.eclipse.jgit.util.ProcessResult
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

/**
 * Version of the POSIX FS with support for running commands taking in consideration the sandboxing
 * */
class FlatpakFs @Inject constructor(
    private val shellManager: FlatpakShellManager,
) : FS_POSIX() {
    override fun runInShell(cmd: String, args: Array<out String?>?): ProcessBuilder? {
        val command = (cmd.split(" ") + args?.toList().orEmpty()).filterNotNull()
        return shellManager.prepareProcessBuilderOrNull(command)
    }

    override fun runHookIfPresent(
        repository: Repository?,
        hookName: String?,
        args: Array<out String?>?,
        outRedirect: OutputStream?,
        errRedirect: OutputStream?,
        stdinArgs: String?
    ): ProcessResult {
        println("hookName: $hookName")

        val hookPath = "${repository?.directory?.absolutePath}/hooks/$hookName"
        val hookFile = File(hookPath)

        if (!hookFile.exists()) {
            return ProcessResult(ProcessResult.Status.NOT_PRESENT)
        }

        val varEnvShell = try {
            System.getenv("SHELL")
        } catch (e: Exception) {
            null
        }

        val shell = if (varEnvShell == null) {
            printLog(TAG, "shell is null, fallback to 'sh'")
            "sh"
        } else {
            printLog(TAG, "shell is $varEnvShell")
            varEnvShell
        }

        val command = listOf(shell, hookPath) + args?.toList().orEmpty().filterNotNull()

        shellManager.runCommandProcess(command).waitFor()

        return ProcessResult(ProcessResult.Status.OK)
    }
}