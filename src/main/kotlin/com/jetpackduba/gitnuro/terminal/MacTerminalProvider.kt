package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.extensions.runCommand
import com.jetpackduba.gitnuro.extensions.runCommandInPath
import javax.inject.Inject

// TODO Test this on MacOS
class MacTerminalProvider @Inject constructor() : ITerminalProvider {
    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return listOf(
            TerminalEmulator("MacOS Terminal", "Terminal")
        )
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        val checkTerminalInstalled = runCommand("which ${terminalEmulator.path} 2>/dev/null")

        return !checkTerminalInstalled.isNullOrEmpty()
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        runCommandInPath(listOf("open", "-a", terminalEmulator.path), repositoryPath)
    }
}