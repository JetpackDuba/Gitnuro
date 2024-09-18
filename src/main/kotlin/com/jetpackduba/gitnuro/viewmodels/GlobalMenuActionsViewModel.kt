package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.remote_operations.FetchAllRemotesUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullType
import com.jetpackduba.gitnuro.git.remote_operations.PushBranchUseCase
import com.jetpackduba.gitnuro.git.stash.PopLastStashUseCase
import com.jetpackduba.gitnuro.git.stash.StashChangesUseCase
import com.jetpackduba.gitnuro.models.errorNotification
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.models.warningNotification
import com.jetpackduba.gitnuro.terminal.OpenRepositoryInTerminalUseCase
import kotlinx.coroutines.Job
import javax.inject.Inject

interface IGlobalMenuActionsViewModel {
    fun pull(pullType: PullType): Job
    fun fetchAll(): Job
    fun push(force: Boolean = false, pushTags: Boolean = false): Job
    fun stash(): Job
    fun popStash(): Job
    fun openTerminal(): Job
}

class GlobalMenuActionsViewModel @Inject constructor(
    private val tabState: TabState,
    private val pullBranchUseCase: PullBranchUseCase,
    private val pushBranchUseCase: PushBranchUseCase,
    private val fetchAllRemotesUseCase: FetchAllRemotesUseCase,
    private val popLastStashUseCase: PopLastStashUseCase,
    private val stashChangesUseCase: StashChangesUseCase,
    private val openRepositoryInTerminalUseCase: OpenRepositoryInTerminalUseCase,
) : IGlobalMenuActionsViewModel {
    override fun pull(pullType: PullType) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Pulling",
        subtitle = "Pulling changes from the remote branch to the current branch",
        refreshEvenIfCrashes = true,
        taskType = TaskType.PULL,
    ) { git ->
        if (pullBranchUseCase(git, pullType)) {
            warningNotification("Pull produced conflicts, fix them to continue")
        } else {
            positiveNotification("Pull completed")
        }
    }

    override fun fetchAll() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Fetching",
        subtitle = "Updating references from the remote repositories...",
        isCancellable = false,
        refreshEvenIfCrashes = true,
        taskType = TaskType.FETCH,
    ) { git ->
        fetchAllRemotesUseCase(git)

        positiveNotification("Fetch all completed")
    }

    override fun push(force: Boolean, pushTags: Boolean) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Push",
        subtitle = "Pushing current branch to the remote repository",
        isCancellable = false,
        refreshEvenIfCrashes = true,
        taskType = TaskType.PUSH,
    ) { git ->
        pushBranchUseCase(git, force, pushTags)

        positiveNotification("Push completed")
    }

    override fun stash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        taskType = TaskType.STASH,
    ) { git ->
        if (stashChangesUseCase(git, null)) {
            positiveNotification("Changes stashed")
        } else {
            errorNotification("There are no changes to stash")
        }
    }

    override fun popStash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
        taskType = TaskType.POP_STASH,
    ) { git ->
        popLastStashUseCase(git)

        positiveNotification("Stash popped")
    }

    override fun openTerminal() = tabState.runOperation(
        refreshType = RefreshType.NONE
    ) { git ->
        openRepositoryInTerminalUseCase(git.repository.workTree.absolutePath)
    }
}