package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.extensions.runCommandInPath
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
                runCommandInPath(terminal.path, path)
                break
            }
        }
    }
}
