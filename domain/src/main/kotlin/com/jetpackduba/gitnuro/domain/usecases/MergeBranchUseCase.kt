package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.ICheckHasUncommittedChangesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ICreateSnapshotStashGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteStashGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IMergeBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MergeBranchUseCase @Inject constructor(
    private val mergeBranchGitAction: IMergeBranchGitAction,
    private val appSettingsService: AppSettingsService,
    private val checkHasUncommittedChangesGitAction: ICheckHasUncommittedChangesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val deleteStashGitAction: IDeleteStashGitAction,
    private val createSnapshotStashGitAction: ICreateSnapshotStashGitAction,
) {

    operator fun invoke(branch: Branch) {
        useCaseExecutor.executeLaunch(
            TaskType.MergeBranch,
            refreshEvenIfFailed = true,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            val mergeAutoStash = appSettingsService.autoStashOnMerge.first()
            val fastForwardMerge = appSettingsService.fastForwardMerge.first()
            var backupStash: Commit? = null

            if (mergeAutoStash) {
                val hasUncommitedChanges = checkHasUncommittedChangesGitAction(repositoryPath).bind()
                if (hasUncommitedChanges) {
                    backupStash = createSnapshotStashGitAction(
                        repositoryPath,
                        message = "TMP MESSAGE",
                        includeUntracked = true
                    ).bind()
                }
            }

            val result = mergeBranchGitAction(repositoryPath, branch, fastForwardMerge)

            if (result is Either.Ok) {
                val hasConflicts = result.value

                if (!hasConflicts && backupStash != null) {
                    deleteStashGitAction(repositoryPath, backupStash)
                }
            }

            result
        }
    }
}