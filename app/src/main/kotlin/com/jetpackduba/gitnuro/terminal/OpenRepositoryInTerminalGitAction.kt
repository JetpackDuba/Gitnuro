package com.jetpackduba.gitnuro.terminal

import com.jetpackduba.gitnuro.data.git.GetWorktreePathGitAction
import com.jetpackduba.gitnuro.data.repositories.configuration.DataStoreGlobalSettingsRepository
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// For flatpak: https://github.com/flathub/com.visualstudio.code#use-host-shell-in-the-integrated-terminal

class OpenRepositoryInTerminalGitAction @Inject constructor(
    private val terminalProvider: ITerminalProvider,
    private val settings: DataStoreGlobalSettingsRepository,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val tabScope: TabCoroutineScope,
    private val getWorktreePathGitAction: GetWorktreePathGitAction,
) {
    operator fun invoke() = tabScope.launch {
        val terminalEmulators = terminalProvider.getTerminalEmulators()

        val repositoryPath = repositoryDataRepository.repositoryPath ?: return@launch
        val repositoryDir = getWorktreePathGitAction(repositoryPath).okOrNull() ?: return@launch

        val terminalPath = settings.terminalPath.firstOrNull()

        if (!terminalPath.isNullOrBlank()) {
            terminalProvider.startTerminal(TerminalEmulator("CUSTOM_TERMINAL", terminalPath), repositoryDir)
        } else {
            for (terminal in terminalEmulators) {
                val isTerminalEmulatorInstalled = terminalProvider.isTerminalInstalled(terminal)
                if (isTerminalEmulatorInstalled) {
                    terminalProvider.startTerminal(terminal, repositoryDir)
                    break
                }
            }
        }
    }
}
