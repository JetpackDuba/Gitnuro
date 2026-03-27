package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.extensions.TAG
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageAllGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class StatusUnstageAllUseCase @Inject constructor(
    private val unstageAllGitAction: IUnstageAllGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val tabScope: TabCoroutineScope,
) {
    operator fun invoke(statusEntries: List<StatusEntry>?) {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return

        repositoryStateRepository.runOperationInTabScope(tabScope) {
            when (val result = unstageAllGitAction(repositoryPath, statusEntries)) {
                is Either.Err -> printError(TAG, result.toString())
                is Either.Ok -> refreshStatusUseCase()
            }
        }
    }
}