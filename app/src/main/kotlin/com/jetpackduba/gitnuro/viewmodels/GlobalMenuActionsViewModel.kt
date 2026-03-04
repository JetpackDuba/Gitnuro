package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.git.remote_operations.FetchAllRemotesGitAction
import com.jetpackduba.gitnuro.domain.git.remote_operations.PullBranchGitAction
import com.jetpackduba.gitnuro.domain.git.remote_operations.PullType
import com.jetpackduba.gitnuro.domain.git.remote_operations.PushBranchGitAction
import com.jetpackduba.gitnuro.domain.git.stash.PopLastStashGitAction
import com.jetpackduba.gitnuro.domain.git.stash.StashChangesGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.errorNotification
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.terminal.OpenRepositoryInTerminalGitAction
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
    private val tabState: TabInstanceRepository,
    private val pullBranchGitAction: PullBranchGitAction,
    private val pushBranchGitAction: PushBranchGitAction,
    private val fetchAllRemotesGitAction: FetchAllRemotesGitAction,
    private val popLastStashGitAction: PopLastStashGitAction,
    private val stashChangesGitAction: StashChangesGitAction,
    private val openRepositoryInTerminalGitAction: OpenRepositoryInTerminalGitAction,
) : IGlobalMenuActionsViewModel {
    override fun pull(pullType: PullType) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Pulling",
        subtitle = "Pulling changes from the remote branch to the current branch",
        refreshEvenIfCrashes = true,
        taskType = TaskType.PULL,
    ) { git ->
        if (pullBranchGitAction(git, pullType)) {
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
        fetchAllRemotesGitAction(git)

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
        pushBranchGitAction(git, force, pushTags, pushWithLease = true) // TODO Fix this after refactor to use cases

        positiveNotification("Push completed")
    }

    override fun stash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        taskType = TaskType.STASH,
    ) { git ->
        if (stashChangesGitAction(git, null)) {
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
        popLastStashGitAction(git)

        positiveNotification("Stash popped")
    }

    override fun openTerminal() = tabState.runOperation(
        refreshType = RefreshType.NONE
    ) { git ->
        openRepositoryInTerminalGitAction(git.repository.workTree.absolutePath)
    }
}