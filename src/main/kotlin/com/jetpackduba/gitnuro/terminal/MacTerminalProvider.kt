package com.jetpackduba.gitnuro.terminal


import com.jetpackduba.gitnuro.managers.IShellManager
import javax.inject.Inject

// TODO Test this on MacOS
class MacTerminalProvider @Inject constructor(
    private val shellManager: IShellManager
) : ITerminalProvider {
    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return listOf(
            TerminalEmulator("MacOS Terminal", "Terminal")
        )
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        val checkTerminalInstalled = shellManager.runCommand(listOf("which", terminalEmulator.path, "2>/dev/null"))

        return !checkTerminalInstalled.isNullOrEmpty()
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        shellManager.runCommandInPath(listOf("open", "-a", terminalEmulator.path), repositoryPath)
    }
}