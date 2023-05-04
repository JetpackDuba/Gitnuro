package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.managers.IShellManager
import javax.inject.Inject

class LinuxTerminalProvider @Inject constructor(
    private val shellManager: IShellManager
) : ITerminalProvider {
    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return listOf(
            TerminalEmulator("Gnome Terminal", "gnome-terminal"),
            TerminalEmulator("KDE Terminal", "konsole"),
            TerminalEmulator("XFCE Terminal", "xfce4-terminal"),
            TerminalEmulator("Mate Terminal", "mate-terminal"),
            TerminalEmulator("LXQT Terminal", "qterminal"),
        )
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        val checkTerminalInstalled = shellManager.runCommand(listOf("which", terminalEmulator.path, "2>/dev/null"))

        return !checkTerminalInstalled.isNullOrEmpty()
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        shellManager.runCommandInPath(listOf(terminalEmulator.path), repositoryPath)
    }
}