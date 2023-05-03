package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.managers.IShellManager
import javax.inject.Inject

class WindowsTerminalProvider @Inject constructor(
    private val shellManager: IShellManager
) : ITerminalProvider {
    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return listOf(
            // TODO powershell is the only terminal emulator supported until we add support for custom
            TerminalEmulator("Powershell", "powershell.exe"),
        )
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        // TODO how do we know if it's installed? We must check the output when trying to start an app that doesn't exist
        return true
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        shellManager.runCommandInPath(listOf("cmd", "/c", "start", terminalEmulator.path), repositoryPath)
    }
}