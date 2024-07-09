package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.remote_operations.FetchAllBranchesUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullType
import com.jetpackduba.gitnuro.git.remote_operations.PushBranchUseCase
import com.jetpackduba.gitnuro.git.stash.PopLastStashUseCase
import com.jetpackduba.gitnuro.git.stash.StashChangesUseCase
import com.jetpackduba.gitnuro.git.workspace.StageUntrackedFileUseCase
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.terminal.OpenRepositoryInTerminalUseCase
import javax.inject.Inject

class MenuViewModel @Inject constructor(
    private val tabState: TabState,
    private val pullBranchUseCase: PullBranchUseCase,
    private val pushBranchUseCase: PushBranchUseCase,
    private val fetchAllBranchesUseCase: FetchAllBranchesUseCase,
    private val popLastStashUseCase: PopLastStashUseCase,
    private val stashChangesUseCase: StashChangesUseCase,
    private val stageUntrackedFileUseCase: StageUntrackedFileUseCase,
    private val openRepositoryInTerminalUseCase: OpenRepositoryInTerminalUseCase,
    private val settings: AppSettingsRepository,
    private val appStateManager: AppStateManager,
) {
    val isPullWithRebaseDefault = settings.pullRebaseFlow
    val lastLoadedTabs = appStateManager.latestOpenedRepositoriesPaths

    fun pull(pullType: PullType) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Pulling",
        subtitle = "Pulling changes from the remote branch to the current branch",
        positiveFeedbackText = "Pull completed",
        refreshEvenIfCrashes = true,
        taskType = TaskType.PULL,
    ) { git ->
        pullBranchUseCase(git, pullType)
    }

    fun fetchAll() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Fetching",
        subtitle = "Updating references from the remote repositories...",
        isCancellable = false,
        refreshEvenIfCrashes = true,
        taskType = TaskType.FETCH,
        positiveFeedbackText = "Fetch all completed",
    ) { git ->
        fetchAllBranchesUseCase(git)
    }

    fun push(force: Boolean = false, pushTags: Boolean = false) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Push",
        subtitle = "Pushing current branch to the remote repository",
        isCancellable = false,
        refreshEvenIfCrashes = true,
        taskType = TaskType.PUSH,
        positiveFeedbackText = "Push completed",
    ) { git ->
        pushBranchUseCase(git, force, pushTags)
    }

    fun stash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        taskType = TaskType.STASH,
        positiveFeedbackText = "Changes stashed",
    ) { git ->
        stashChangesUseCase(git, null)
    }

    fun popStash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
        taskType = TaskType.POP_STASH,
        positiveFeedbackText = "Stash popped",
    ) { git ->
        popLastStashUseCase(git)
    }

    fun openTerminal() = tabState.runOperation(
        refreshType = RefreshType.NONE
    ) { git ->
        openRepositoryInTerminalUseCase(git.repository.workTree.absolutePath)
    }
}