package com.jetpackduba.gitnuro.domain

import com.jetpackduba.gitnuro.domain.errors.*
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.FailureSeverity
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import com.jetpackduba.gitnuro.domain.usecases.DataToRefresh
import com.jetpackduba.gitnuro.domain.usecases.RefreshDataUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class UseCaseExecutor @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val refreshDataUseCase: Provider<RefreshDataUseCase>,
    private val scope: TabCoroutineScope,
) {
    suspend fun <T> execute(
        dataToRefresh: Array<DataToRefresh> = emptyArray(),
        refreshEvenIfFailed: Boolean = false,
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>,
    ): Either<T, AppError> {
        return executeTask(dataToRefresh = dataToRefresh, refreshEvenIfFailed, block)
    }

    fun executeOnTabScope(
        block: suspend EitherContext<AppError>.(String) -> Unit,
    ) {
        scope.launch {
            executeTask(
                refreshEvenIfFailed = false,
                dataToRefresh = emptyArray(),
                block = {
                    block(it)
                    Either.Ok(Unit)
                }
            )
        }
    }

    fun <T> executeLaunch(
        taskType: TaskType,
        dataToRefresh: Array<DataToRefresh>,
        refreshEvenIfFailed: Boolean = false,
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>,
    ) {
        repositoryStateRepository.runOperationInTabScope(taskType, scope) {
            executeTask(
                dataToRefresh = dataToRefresh,
                refreshEvenIfFailed,
                block
            ).apply {
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
        dataToRefresh: Array<DataToRefresh>,
        refreshEvenIfFailed: Boolean,
        block: suspend EitherContext<AppError>.(String) -> Either<T, AppError>
    ): Either<T, AppError> {
        try {
            val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
            return either { block(repositoryPath) }.apply {
                if (this is Either.Ok || refreshEvenIfFailed) {
                    refreshDataUseCase.get()(*dataToRefresh)
                }
            }
        } catch (e: Exception) {
            return Either.Err(GenericError(e.message.orEmpty(), e))
        }
    }
}