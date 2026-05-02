package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetTagsGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class RefreshTagsUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getTagsGitAction: IGetTagsGitAction,
    private val tabCoroutineScope: TabCoroutineScope,
) {
    operator fun invoke() {
        val repository = repositoryDataRepository.repositoryPath ?: return

        tabCoroutineScope.launch {
            when (val tags = getTagsGitAction(repository)) {
                is Either.Err -> {
                    // TODO Notify user?
                }

                is Either.Ok -> {
                    repositoryDataRepository.updateTags(tags.value)
                }
            }
        }
    }
}