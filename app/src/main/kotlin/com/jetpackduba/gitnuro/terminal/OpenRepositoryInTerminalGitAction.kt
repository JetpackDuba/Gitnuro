package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.data.repositories.configuration.DataStoreAppSettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

// For flatpak: https://github.com/flathub/com.visualstudio.code#use-host-shell-in-the-integrated-terminal

class OpenRepositoryInTerminalGitAction @Inject constructor(
    private val terminalProvider: ITerminalProvider,
    private val settings: DataStoreAppSettingsRepository,
) {
    suspend operator fun invoke(path: String) {
        val terminalEmulators = terminalProvider.getTerminalEmulators()
        val terminalPath = settings.terminalPath.firstOrNull()

        if (!terminalPath.isNullOrBlank()) {
            terminalProvider.startTerminal(TerminalEmulator("CUSTOM_TERMINAL", terminalPath), path)
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
