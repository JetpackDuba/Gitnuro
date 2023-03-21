package com.jetpackduba.gitnuro.terminal

interface ITerminalProvider {
    fun getTerminalEmulators(): List<TerminalEmulator>

    fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean

    fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String)
}