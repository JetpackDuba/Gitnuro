package app.viewmodels

import app.git.*
import java.awt.Desktop
import javax.inject.Inject

class MenuViewModel @Inject constructor(
    private val tabState: TabState,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val stashManager: StashManager,
    private val statusManager: StatusManager,
) {
    fun pull(rebase: Boolean = false) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashes = true,
    ) { git ->
        remoteOperationsManager.pull(git, rebase)
    }

    fun fetchAll() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashes = true,
    ) { git ->
        remoteOperationsManager.fetchAll(git)
    }

    fun push(force: Boolean = false) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashes = true,
    ) { git ->
        remoteOperationsManager.push(git, force)
    }

    fun stash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        statusManager.stageUntrackedFiles(git)
        stashManager.stash(git)
    }

    fun popStash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        stashManager.popStash(git)
    }

    fun openFolderInFileExplorer() = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        Desktop.getDesktop().open(git.repository.directory.parentFile)
    }

    fun refresh() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) {
        // Nothing to do here
    }
}