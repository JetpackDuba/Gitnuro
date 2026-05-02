package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetSubmodulesGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class RefreshSubmodulesUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val tabCoroutineScope: TabCoroutineScope,
    private val getSubmodulesGitAction: IGetSubmodulesGitAction,
) {
    operator fun invoke() {
        val repository = repositoryDataRepository.repositoryPath ?: return

        tabCoroutineScope.launch {
            when (val submodules = getSubmodulesGitAction(repository)) {
                is Either.Err -> {
                    // TODO Notify user about error?
                }

                is Either.Ok -> {
                    repositoryDataRepository.updateSubmodules(submodules.value)
                }
            }
        }
    }
}
