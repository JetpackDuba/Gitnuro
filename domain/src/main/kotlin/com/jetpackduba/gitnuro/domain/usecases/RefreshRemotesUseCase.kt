package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemotesGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshRemotesUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getRemotesGitAction: IGetRemotesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
//    private val appStateRepository: AppStateRepository,
) {
    suspend operator fun invoke() {
//        val repository = appStateRepository.repositoryPath ?: return

        // TODO Refresh remotes
    }
}