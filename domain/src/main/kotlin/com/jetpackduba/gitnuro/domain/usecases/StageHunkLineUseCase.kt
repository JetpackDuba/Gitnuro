package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IStageHunkLineGitAction
import com.jetpackduba.gitnuro.domain.models.Hunk
import com.jetpackduba.gitnuro.domain.models.Line
import com.jetpackduba.gitnuro.domain.models.TaskType
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class StageHunkLineUseCase @Inject constructor(
    private val stageHunkLineGitAction: IStageHunkLineGitAction,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(diffEntry: DiffEntry, hunk: Hunk, line: Line) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.StageLine,
            onRefresh = { refreshStatusUseCase() }
        ) { repositoryPath ->
            stageHunkLineGitAction(repositoryPath, diffEntry, hunk, line)
        }
    }
}
