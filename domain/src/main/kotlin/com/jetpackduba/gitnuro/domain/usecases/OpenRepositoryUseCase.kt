package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.AppStateManager
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.interfaces.IOpenRepositoryGitAction
import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.FailureSeverity
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import javax.inject.Inject

class OpenRepositoryUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val openRepositoryGitAction: IOpenRepositoryGitAction,
    private val refreshDataUseCase: RefreshDataUseCase,
    private val observeRepositoryToRefreshUseCase: ObserveRepositoryToRefreshUseCase,
    private val getWorktreeUseCase: GetWorktreeUseCase,
    private val appStateManager: AppStateManager,
) {
    suspend operator fun invoke(directory: String) {
        val repositoryPathResult = openRepositoryGitAction(directory)

        when (repositoryPathResult) {
            is Either.Err ->  {
                repositoryDataRepository.setRepositorySelectionState(RepositorySelectionState.None)
                repositoryStateRepository.addCompletedTaskFailed(
                    TaskType.RepositoryOpen,
                    repositoryPathResult.error,
                    FailureSeverity.HIGH,
                )
            }
            is Either.Ok -> {
                repositoryDataRepository.setRepositorySelectionState(RepositorySelectionState.Open(repositoryPathResult.value))

                val worktree = getWorktreeUseCase().okOrNull()
                if (worktree != null) {
                    appStateManager.repositoryTabChanged(worktree)
                }

                refreshDataUseCase(DataToRefresh.ALL)
                observeRepositoryToRefreshUseCase()
            }
        }
    }
}