package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.extensions.runCommand
import com.jetpackduba.gitnuro.extensions.runCommandInPath
import javax.inject.Inject

class LinuxTerminalProvider @Inject constructor() : ITerminalProvider {
    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return listOf(
            TerminalEmulator("Gnome Terminal", "gnome-terminal"),
            TerminalEmulator("KDE Terminal", "kde-terminal"),
            TerminalEmulator("XFCE Terminal", "xfce4-terminal"),
            TerminalEmulator("Mate Terminal", "mate-terminal"),
            TerminalEmulator("LXQT Terminal", "qterminal"),
        )
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        val checkTerminalInstalled = runCommand("which ${terminalEmulator.path} 2>/dev/null")

        return !checkTerminalInstalled.isNullOrEmpty()
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        runCommandInPath(listOf(terminalEmulator.path), repositoryPath)
    }
}