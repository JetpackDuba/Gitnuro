package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IGetSubmodulesGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshSubmodulesUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getSubmodulesGitAction: IGetSubmodulesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke() {
        useCaseExecutor.executeOnTabScope(TaskType.RefreshSubmodules) { repositoryPath ->
            val submodules = getSubmodulesGitAction(repositoryPath).bind()
            repositoryDataRepository.updateSubmodules(submodules)
        }
    }
}
