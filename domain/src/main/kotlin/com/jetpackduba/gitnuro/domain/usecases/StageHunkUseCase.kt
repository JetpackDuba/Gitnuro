package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IStageHunkGitAction
import com.jetpackduba.gitnuro.domain.models.Hunk
import com.jetpackduba.gitnuro.domain.models.TaskType
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class StageHunkUseCase @Inject constructor(
    private val stageHunkGitAction: IStageHunkGitAction,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(diffEntry: DiffEntry, hunk: Hunk) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.StageHunk,
            onRefresh = { refreshStatusUseCase() }
        ) { repositoryPath ->
            stageHunkGitAction(repositoryPath, diffEntry, hunk)
        }
    }
}
