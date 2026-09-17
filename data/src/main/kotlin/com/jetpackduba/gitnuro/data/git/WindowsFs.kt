package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.domain.ShellManager
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.util.FS_Win32
import org.eclipse.jgit.util.ProcessResult
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

/*
* Version of the Windows FS with support for hooks by using Git Bash if present.
* */
class WindowsFs @Inject constructor(
    private val shellManager: ShellManager,
) : FS_Win32() {
    override fun runHookIfPresent(
        repository: Repository,
        hookName: String,
        args: Array<out String?>,
        outRedirect: OutputStream?,
        errRedirect: OutputStream?,
        stdinArgs: String?
    ): ProcessResult? {
        val hookPath = "${repository.directory.absolutePath}\\hooks\\$hookName"
        val hookFile = File(hookPath)

        if (!hookFile.exists()) {
            return ProcessResult(ProcessResult.Status.NOT_PRESENT)
        }

        val gitBashPath = (System.getenv("PATH")
            .split(";")
            .firstOrNull { it.endsWith("Git\\cmd") }
            ?.removeSuffix("\\cmd"))
            ?.let {
                "$it\\bin\\bash.exe"
            }

        if (gitBashPath.isNullOrBlank() || !File(gitBashPath).exists()) {
            throw IllegalStateException("Git bash not found, it is needed to run hook '$hookName' on Windows")
        }

        val params = listOf("cmd", "/C", gitBashPath, hookPath)
        val process = shellManager.runCommandProcess(params, repository.directory.parentFile)

        process.waitFor()

        process.inputStream.transferTo(outRedirect)
        process.errorStream.transferTo(errRedirect)

        return ProcessResult(ProcessResult.Status.OK)
    }
}