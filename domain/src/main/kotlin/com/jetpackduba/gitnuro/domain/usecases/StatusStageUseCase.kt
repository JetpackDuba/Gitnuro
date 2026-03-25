package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.interfaces.IStageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

private const val TAG = "StatusStageUseCase"

class StatusStageUseCase @Inject constructor(
    private val stageEntryGitAction: IStageEntryGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val tabScope: CoroutineScope,
) {
    operator fun invoke(statusEntry: StatusEntry) {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return

        repositoryStateRepository.runOperationInTabScope(tabScope) {
            when (val result = stageEntryGitAction(repositoryPath, statusEntry)) {
                is Either.Err -> printError(TAG, result.toString())
                is Either.Ok -> refreshStatusUseCase()
            }
        }
    }
}