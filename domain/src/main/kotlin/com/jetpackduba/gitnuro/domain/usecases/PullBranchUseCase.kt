package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IPullBranchGitAction
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.services.GlobalSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PullBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val pullBranchGitAction: IPullBranchGitAction,
    private val globalSettingsService: GlobalSettingsService,
) {
    operator fun invoke(
        pullType: PullType,
        remoteBranch: Branch? = null,
        automaticStashDescription: String,
    ) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Pull(remoteBranch),
        dataToRefresh = arrayOf(DataToRefresh.ALL),
    ) { repositoryPath ->
        val autoStashOnMerge = globalSettingsService.autoStashOnMerge.first()

        val pullTypeWithSettings = if (pullType == PullType.DEFAULT) {
            val isPullWithRebase = globalSettingsService.pullWithRebase.first()

            if (isPullWithRebase) {
                PullType.REBASE
            } else {
                PullType.MERGE
            }
        } else {
            pullType
        }

        val result = pullBranchGitAction(repositoryPath, pullTypeWithSettings, autoStashOnMerge, remoteBranch, automaticStashDescription)

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