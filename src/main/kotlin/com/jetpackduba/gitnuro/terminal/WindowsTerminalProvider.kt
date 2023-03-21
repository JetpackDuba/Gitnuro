package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.extensions.runCommandInPath
import javax.inject.Inject

// TODO Test this on windows
class WindowsTerminalProvider @Inject constructor() : ITerminalProvider {
    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return listOf(
            TerminalEmulator("Powershell", "powershell"),
        )
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        // TODO how do we know if it's installed? We must check the output when trying to start an app that doesn't exist
        return true
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        runCommandInPath("start ${terminalEmulator.path}", repositoryPath)
    }
}