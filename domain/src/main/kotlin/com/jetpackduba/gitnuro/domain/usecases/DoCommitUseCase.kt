package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.extensions.TAG
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.interfaces.IDoCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Identity
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class DoCommitUseCase @Inject constructor(
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val doCommitGitAction: IDoCommitGitAction,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val tabScope: TabCoroutineScope,
) {
    suspend operator fun invoke(
        message: String,
        amend: Boolean,
        author: Identity?,
    ) {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return

        repositoryStateRepository.runOperationInTabScope(tabScope) {
            when (val commit = doCommitGitAction(repositoryPath, message, amend, author)) {
                is Either.Err -> {
                    printError(TAG, "Failed to commit changes: ${commit.error}")
                }

                is Either.Ok -> {
                    refreshStatusUseCase()
                    refreshLogUseCase()
                }
            }
        }
    }
}