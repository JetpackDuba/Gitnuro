package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class RefreshRemotesUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getRemotesUseCase: GetRemotesUseCase,
    private val scope: TabCoroutineScope,
) {
    operator fun invoke() {
        scope.launch {
            val remotes = getRemotesUseCase()

            if (remotes is Either.Ok) {
                repositoryDataRepository.updateRemotes(remotes.value)
            }
        }
    }
}