package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IGetStatusGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshStatusUseCase @Inject constructor(
    private val getStatusGitAction: IGetStatusGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke() {
        useCaseExecutor.executeOnTabScope(TaskType.RefreshStatus) { repositoryPath ->
            val status = getStatusGitAction(repositoryPath).bind()
            repositoryDataRepository.updateStatus(status)
        }
    }
}