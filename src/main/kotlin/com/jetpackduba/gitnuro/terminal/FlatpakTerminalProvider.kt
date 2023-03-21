package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.extensions.runCommand
import com.jetpackduba.gitnuro.extensions.runCommandInPath
import javax.inject.Inject

private const val FLATPAK_PREFIX = "/usr/bin/flatpak-spawn --host --env=TERM=xterm-256color"

// TODO Test in flatpak
class FlatpakTerminalProvider @Inject constructor(
    private val linuxTerminalProvider: LinuxTerminalProvider,
) : ITerminalProvider {

    override fun getTerminalEmulators(): List<TerminalEmulator> {
        return linuxTerminalProvider.getTerminalEmulators()
    }

    override fun isTerminalInstalled(terminalEmulator: TerminalEmulator): Boolean {
        val checkTerminalInstalled = runCommand("$FLATPAK_PREFIX which ${terminalEmulator.path} 2>/dev/null")

        return !checkTerminalInstalled.isNullOrEmpty()
    }

    override fun startTerminal(terminalEmulator: TerminalEmulator, repositoryPath: String) {
        runCommandInPath("$FLATPAK_PREFIX ${terminalEmulator.path}", repositoryPath)
    }
}