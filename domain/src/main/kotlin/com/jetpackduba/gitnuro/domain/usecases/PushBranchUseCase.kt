package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IPushBranchGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PushBranchUseCase @Inject constructor(
    private val pushBranchGitAction: IPushBranchGitAction,
    private val appSettingsService: AppSettingsService,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(force: Boolean, pushTags: Boolean) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.PUSH,
            onRefresh = {
                refreshLogUseCase()
            }
        ) { repositoryPath ->
            val pushWithLease = appSettingsService.pushWithLease.first()

            pushBranchGitAction(repositoryPath, force, pushTags, pushWithLease)
        }
    }
}