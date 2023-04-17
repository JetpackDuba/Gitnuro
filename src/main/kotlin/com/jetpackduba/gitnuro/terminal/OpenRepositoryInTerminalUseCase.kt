package com.jetpackduba.gitnuro.terminal

import javax.inject.Inject

// For flatpak: https://github.com/flathub/com.visualstudio.code#use-host-shell-in-the-integrated-terminal

class OpenRepositoryInTerminalUseCase @Inject constructor(
    private val terminalProvider: ITerminalProvider
) {
    operator fun invoke(path: String) {
        val terminalEmulators = terminalProvider.getTerminalEmulators()

        for (terminal in terminalEmulators) {
            val isTerminalEmulatorInstalled = terminalProvider.isTerminalInstalled(terminal)
            if (isTerminalEmulatorInstalled) {
                terminalProvider.startTerminal(terminal, path)
                break
            }
        }
    }
}
