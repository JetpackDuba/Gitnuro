package app.viewmodels

import app.git.*
import app.git.remote_operations.DeleteRemoteBranchUseCase
import app.git.remote_operations.FetchAllBranchesUseCase
import app.git.remote_operations.PullBranchUseCase
import app.git.remote_operations.PushBranchUseCase
import app.git.stash.PopLastStashUseCase
import app.git.stash.StashChangesUseCase
import app.git.workspace.StageUntrackedFileUseCase
import java.awt.Desktop
import javax.inject.Inject

class MenuViewModel @Inject constructor(
    private val tabState: TabState,
    private val pullBranchUseCase: PullBranchUseCase,
    private val pushBranchUseCase: PushBranchUseCase,
    private val fetchAllBranchesUseCase: FetchAllBranchesUseCase,
    private val stashChangesUseCase: StashChangesUseCase,
    private val popLastStashUseCase: PopLastStashUseCase,
    private val stageUntrackedFileUseCase: StageUntrackedFileUseCase,
) {
    fun pull(rebase: Boolean = false) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashes = true,
    ) { git ->
        pullBranchUseCase(git, rebase)
    }

    fun fetchAll() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashes = true,
    ) { git ->
        fetchAllBranchesUseCase(git)
    }

    fun push(force: Boolean = false, pushTags: Boolean = false) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashes = true,
    ) { git ->
        pushBranchUseCase(git, force, pushTags)
    }

    fun stash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        stageUntrackedFileUseCase(git)
        stashChangesUseCase(git, null)
    }

    fun stashWithMessage(message: String) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        stageUntrackedFileUseCase(git)
        stashChangesUseCase(git, message)
    }

    fun popStash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
    ) { git ->
        popLastStashUseCase(git)
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