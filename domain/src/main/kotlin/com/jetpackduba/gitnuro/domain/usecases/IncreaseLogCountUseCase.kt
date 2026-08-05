package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.Pagination
import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.repositories.DataState
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class IncreaseLogCountUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val useCaseExecutor: UseCaseExecutor,
    private val getLogUseCase: GetLogUseCase,
) {
    suspend operator fun invoke(newLimit: Int): Either<Unit, AppError> {
        return useCaseExecutor.execute { repositoryPath ->
            // If the data is being loaded or failed, do not try to load more items
            val log = (repositoryDataRepository.log.value as? DataState.Loaded)?.data ?: return@execute Either.Ok(Unit)

            if (newLimit > repositoryDataRepository.maxCommitsToLoadLimit) {
                repositoryDataRepository.maxCommitsToLoadLimit = newLimit
                repositoryDataRepository.updateLog {
                    getLogUseCase(
                        repositoryPath,
                        pagination = Pagination.Paginated(log)
                    )
                }
            }

            Either.Ok(Unit)
        }
    }
}