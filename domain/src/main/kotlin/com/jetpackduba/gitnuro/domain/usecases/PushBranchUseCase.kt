package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IPushBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.services.GlobalSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PushBranchUseCase @Inject constructor(
    private val pushBranchGitAction: IPushBranchGitAction,
    private val globalSettingsService: GlobalSettingsService,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(force: Boolean, pushTags: Boolean, targetRemoteBranch: Branch? = null) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.Push(targetRemoteBranch),
            dataToRefresh = arrayOf(DataToRefresh.LOG, DataToRefresh.REMOTES, DataToRefresh.BRANCHES),
        ) { repositoryPath ->
            val pushWithLease = globalSettingsService.pushWithLease.first()

            pushBranchGitAction(repositoryPath, force, pushTags, pushWithLease, targetRemoteBranch)
        }
    }
}