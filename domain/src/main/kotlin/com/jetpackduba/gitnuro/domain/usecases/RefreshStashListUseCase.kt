package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetStashListGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class RefreshStashListUseCase @Inject constructor(
    private val tabCoroutineScope: TabCoroutineScope,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getStashListGitAction: IGetStashListGitAction,
) {
    operator fun invoke() {
        val repository = repositoryDataRepository.repositoryPath ?: return

        tabCoroutineScope.launch {
            when (val stashes = getStashListGitAction(repository)) {
                is Either.Err -> {
                    // TODO Notify user about error?
                }
                is Either.Ok -> {
                    repositoryDataRepository.updateStashes(stashes.value)
                }
            }
        }
    }
}