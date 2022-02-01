package app.viewmodels

import app.git.RefreshType
import app.git.RemoteOperationsManager
import app.git.StashManager
import app.git.TabState
import java.awt.Desktop
import javax.inject.Inject

class MenuViewModel @Inject constructor(
    private val tabState: TabState,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val stashManager: StashManager,
) {
    fun pull(rebase: Boolean = false) = tabState.safeProcessing { git ->
        remoteOperationsManager.pull(git, rebase)

        return@safeProcessing RefreshType.ONLY_LOG
    }

    fun push() = tabState.safeProcessing { git ->
        remoteOperationsManager.push(git)

        return@safeProcessing RefreshType.ONLY_LOG
    }

    fun stash() = tabState.safeProcessing { git ->
        stashManager.stash(git)

        return@safeProcessing RefreshType.UNCOMMITED_CHANGES
    }

    fun popStash() = tabState.safeProcessing { git ->
        stashManager.popStash(git)

        return@safeProcessing RefreshType.UNCOMMITED_CHANGES
    }

    fun openFolderInFileExplorer() = tabState.runOperation (showError = true) { git ->
        Desktop.getDesktop().open(git.repository.directory.parentFile)

        return@runOperation RefreshType.NONE
    }
}