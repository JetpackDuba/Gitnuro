package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.managers.IShellManager
import javax.inject.Inject

class WindowsTerminalProvider @Inject constructor(
    private val shellManager: IShellManager
) : ITerminalProvider {
    private val powerShellEmulator =
        TerminalEmulator("Powershell", "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe")

    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return listOf(powerShellEmulator)
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        // We don't care if it's not installed
        return true
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        if (terminalEmulator == powerShellEmulator) {
            shellManager.runCommandInPath(listOf("cmd", "/c", "start", terminalEmulator.path), repositoryPath)
        } else {
            shellManager.runCommandInPath(listOf("cmd", "/c", terminalEmulator.path), repositoryPath)
        }
    }
}