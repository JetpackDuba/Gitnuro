package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.interfaces.IFetchAllRemotesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPullBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPushBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPopLastStashGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IStashChangesGitAction
import com.jetpackduba.gitnuro.domain.models.PullType
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
    fun openTerminal()
}

class GlobalMenuActionsViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val pullBranchGitAction: IPullBranchGitAction,
    private val pushBranchGitAction: IPushBranchGitAction,
    private val fetchAllRemotesGitAction: IFetchAllRemotesGitAction,
    private val popLastStashGitAction: IPopLastStashGitAction,
    private val stashChangesGitAction: IStashChangesGitAction,
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

    override fun openTerminal() {
        openRepositoryInTerminalGitAction()
    }
}