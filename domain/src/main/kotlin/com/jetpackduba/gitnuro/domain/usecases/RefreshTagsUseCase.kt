package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IGetTagsGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshTagsUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getTagsGitAction: IGetTagsGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke() {
        useCaseExecutor.executeOnTabScope(TaskType.RefreshSubmodules) { repositoryPath ->
            val tags = getTagsGitAction(repositoryPath).bind()
            repositoryDataRepository.updateTags(tags)
        }
    }
}