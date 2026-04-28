package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.interfaces.IPopLastStashGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IStashChangesGitAction
import com.jetpackduba.gitnuro.domain.models.PullType
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.errorNotification
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.FetchAllBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.PullBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.PushBranchUseCase
import com.jetpackduba.gitnuro.terminal.OpenRepositoryInTerminalGitAction
import kotlinx.coroutines.Job
import javax.inject.Inject

interface IGlobalMenuActionsViewModel {
    fun pull(pullType: PullType)
    fun fetchAll()
    fun push(force: Boolean = false, pushTags: Boolean = false)
    fun stash(): Job
    fun popStash(): Job
    fun openTerminal()
}

class GlobalMenuActionsViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val fetchAllUseCase: FetchAllBranchUseCase,
    private val popLastStashGitAction: IPopLastStashGitAction,
    private val pullBranchUseCase: PullBranchUseCase,
    private val pushBranchUseCase: PushBranchUseCase,
    private val stashChangesGitAction: IStashChangesGitAction,
    private val openRepositoryInTerminalGitAction: OpenRepositoryInTerminalGitAction,
) : IGlobalMenuActionsViewModel {
    override fun pull(pullType: PullType) = pullBranchUseCase(pullType)

    override fun push(force: Boolean, pushTags: Boolean) = pushBranchUseCase(force, pushTags)

    override fun fetchAll() = fetchAllUseCase()

    override fun stash() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        taskType = TaskType.Stash,
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
        taskType = TaskType.PopStash,
    ) { git ->
        popLastStashGitAction(git)

        positiveNotification("Stash popped")
    }

    override fun openTerminal() {
        openRepositoryInTerminalGitAction()
    }
}