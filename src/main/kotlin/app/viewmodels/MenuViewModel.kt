package app.viewmodels

import app.git.RefreshType
import app.git.RemoteOperationsManager
import app.git.StashManager
import app.git.TabState
import java.awt.Desktop
import java.io.File
import javax.inject.Inject

class MenuViewModel @Inject constructor(
    private val tabState: TabState,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val stashManager: StashManager,
) {
    fun pull() = tabState.safeProcessing { git ->
        remoteOperationsManager.pull(git)

        return@safeProcessing RefreshType.ONLY_LOG
    }

    fun push() = tabState.safeProcessing { git ->
        try {
            remoteOperationsManager.push(git)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

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

    fun openFolderInFileExplorer() = tabState.runOperation { git ->
        Desktop.getDesktop().open(git.repository.directory.parentFile)

        return@runOperation RefreshType.NONE
    }
}