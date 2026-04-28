package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.models.PullType
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.FetchAllBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.PopStashUseCase
import com.jetpackduba.gitnuro.domain.usecases.PullBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.PushBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.StashChangesUseCase
import com.jetpackduba.gitnuro.terminal.OpenRepositoryInTerminalGitAction
import javax.inject.Inject

interface IGlobalMenuActionsViewModel {
    fun pull(pullType: PullType)
    fun fetchAll()
    fun push(force: Boolean = false, pushTags: Boolean = false)
    fun stash()
    fun popStash()
    fun openTerminal()
}

class GlobalMenuActionsViewModel @Inject constructor(
    private val fetchAllUseCase: FetchAllBranchUseCase,
    private val pullBranchUseCase: PullBranchUseCase,
    private val pushBranchUseCase: PushBranchUseCase,
    private val stashChangesUseCase: StashChangesUseCase,
    private val popStashUseCase: PopStashUseCase,
    private val openRepositoryInTerminalGitAction: OpenRepositoryInTerminalGitAction,
) : IGlobalMenuActionsViewModel {
    override fun pull(pullType: PullType) = pullBranchUseCase(pullType)

    override fun push(force: Boolean, pushTags: Boolean) = pushBranchUseCase(force, pushTags)

    override fun fetchAll() = fetchAllUseCase()

    override fun stash() = stashChangesUseCase(null)

    override fun popStash() = popStashUseCase(null)

    override fun openTerminal() {
        openRepositoryInTerminalGitAction()
    }
}