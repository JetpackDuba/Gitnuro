package com.jetpackduba.gitnuro.domain

import com.jetpackduba.gitnuro.domain.errors.*
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.FailureSeverity
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class UseCaseExecutor @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val scope: TabCoroutineScope,
) {
    suspend fun <T> execute(
        taskType: TaskType,
        onRefresh: suspend () -> Unit = {},
        refreshEvenIfFailed: Boolean = false,
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>,
    ): Either<T, AppError> {
        return executeTask(refreshEvenIfFailed, onRefresh, block)
    }

    fun executeOnTabScope(
        taskType: TaskType,
        block: suspend EitherContext<AppError>.(String) -> Unit,
    ) {
        scope.launch {
            executeTask(
                refreshEvenIfFailed = false,
                onRefresh = {},
                block = {
                    block(it)
                    Either.Ok(Unit)
                }
            )
        }
    }

    fun <T> executeLaunch(
        taskType: TaskType,
        refreshEvenIfFailed: Boolean = false,
        onRefresh: () -> Unit,
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>,
    ) {
        repositoryStateRepository.runOperationInTabScope(taskType, scope) {
            executeTask(refreshEvenIfFailed, onRefresh, block).apply {
                when (this) {
                    is Either.Err -> repositoryStateRepository.addCompletedTaskFailed(
                        taskType,
                        this.error,
                        FailureSeverity.HIGH,
                    )

                    is Either.Ok -> repositoryStateRepository.addCompletedTaskSuccessfully(taskType)
                }
            }
        }
    }

    private suspend fun <T> executeTask(
        refreshEvenIfFailed: Boolean,
        onRefresh: suspend () -> Unit = {},
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>
    ): Either<T, AppError> {
        try {
            val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
            return either { block(repositoryPath) }.apply {
                if (this is Either.Ok || refreshEvenIfFailed) {
                    onRefresh()
                }
            }
        } catch (e: Exception) {
            return Either.Err(GenericError(e.message.orEmpty(), e))
        }
    }
}