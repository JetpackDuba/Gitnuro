package com.jetpackduba.gitnuro.domain

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.EitherContext
import com.jetpackduba.gitnuro.domain.errors.GenericError
import com.jetpackduba.gitnuro.domain.errors.RepositoryPathNotSetError
import com.jetpackduba.gitnuro.domain.errors.either
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.newErrorNow
import com.jetpackduba.gitnuro.domain.repositories.IErrorsRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import javax.inject.Inject

class UseCaseExecutor @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val errorsRepository: IErrorsRepository,
    private val scope: TabCoroutineScope,
) {
    suspend fun <T> execute(
        taskType: TaskType,
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>,
    ): Either<T, AppError> {
        return executeTask(taskType, block)
    }

    fun <T> executeLaunch(
        taskType: TaskType,
        refreshEvenIfFailed: Boolean = false,
        onRefresh: suspend () -> Unit,
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>,
    ) {
        repositoryStateRepository.runOperationInTabScope(scope) {
            if (executeTask(taskType, block) is Either.Ok || refreshEvenIfFailed) {
                onRefresh()
            }
        }
    }

    private suspend fun <T> executeTask(
        taskType: TaskType,
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>
    ): Either<T, AppError> {
        try {
            val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
            return either { block(repositoryPath) }
        } catch (e: Exception) {
            errorsRepository.addError(newErrorNow(taskType, e))

            return Either.Err(GenericError(e.message.orEmpty()))
        }
    }
}