package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IPullBranchGitAction
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PullBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val pullBranchGitAction: IPullBranchGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val appSettingsService: AppSettingsService,
) {
    operator fun invoke(pullType: PullType, remoteBranch: Branch? = null) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Pull,
        onRefresh = {
            refreshAllUseCase()
        }
    ) { repositoryPath ->
        val autoStashOnMerge = appSettingsService.autoStashOnMerge.first()

        val result = pullBranchGitAction(repositoryPath, pullType, autoStashOnMerge, remoteBranch)

        if (result is Either.Ok) {
            if (result.value) {
                warningNotification("Pull produced conflicts, fix them to continue")
            } else {
                positiveNotification("Pull completed")
            }
        }

        result
    }
}