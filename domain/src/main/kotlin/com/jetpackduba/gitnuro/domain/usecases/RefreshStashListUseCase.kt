package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IGetStashListGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshStashListUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getStashListGitAction: IGetStashListGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke() {
        useCaseExecutor.executeOnTabScope(TaskType.RefreshStashes) { repositoryPath ->
            val stashes = getStashListGitAction(repositoryPath).bind()
            repositoryDataRepository.updateStashes(stashes)
        }
    }
}