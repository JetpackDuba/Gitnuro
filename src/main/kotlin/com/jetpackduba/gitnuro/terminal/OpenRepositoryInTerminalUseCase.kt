package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.preferences.AppSettings
import javax.inject.Inject

// For flatpak: https://github.com/flathub/com.visualstudio.code#use-host-shell-in-the-integrated-terminal

class OpenRepositoryInTerminalUseCase @Inject constructor(
    private val terminalProvider: ITerminalProvider,
    private val settings: AppSettings,
) {
    operator fun invoke(path: String) {
        val terminalEmulators = terminalProvider.getTerminalEmulators()

        if (settings.terminalPath.isNotEmpty()) {
            terminalProvider.startTerminal(TerminalEmulator("CUSTOM_TERMINAL", settings.terminalPath), path)
        } else {
            for (terminal in terminalEmulators) {
                val isTerminalEmulatorInstalled = terminalProvider.isTerminalInstalled(terminal)
                if (isTerminalEmulatorInstalled) {
                    terminalProvider.startTerminal(terminal, path)
                    break
                }
            }
        }
    }
}
