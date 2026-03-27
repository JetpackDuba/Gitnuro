package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

private const val TAG = "StatusUnstageUseCase"


class StatusUnstageUseCase @Inject constructor(
    private val unstageEntryGitAction: IUnstageEntryGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val tabScope: TabCoroutineScope,
) {
    operator fun invoke(statusEntry: StatusEntry) {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return

        repositoryStateRepository.runOperationInTabScope(tabScope) {
            when (val result = unstageEntryGitAction(repositoryPath, statusEntry)) {
                is Either.Err -> printError(TAG, result.toString())
                is Either.Ok -> refreshStatusUseCase()
            }
        }
    }
}