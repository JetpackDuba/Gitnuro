package com.jetpackduba.gitnuro.terminal


import com.jetpackduba.gitnuro.managers.IShellManager
import javax.inject.Inject

class MacTerminalProvider @Inject constructor(
    private val shellManager: IShellManager
) : ITerminalProvider {
    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return listOf(
            TerminalEmulator("MacOS Terminal", "Terminal")
        )
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        return true // TODO Return true always until we support multiple terminals
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        // TODO Check if passing the path as argument is required for other terminal emulators
        shellManager.runCommandInPath(
            listOf("open", "-a", terminalEmulator.path, "-n", "--args", repositoryPath),
            repositoryPath
        )
    }
}