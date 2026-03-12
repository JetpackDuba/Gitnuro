package com.jetpackduba.gitnuro.domain

import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.newErrorNow
import com.jetpackduba.gitnuro.domain.repositories.IErrorsRepository
import javax.inject.Inject

class UseCaseExecutor @Inject constructor(
    private val errorsRepository: IErrorsRepository,
) {
    suspend operator fun <T> invoke(
        taskType: TaskType,
        block: suspend () -> T,
    ): T? {
        try {
            return block()
        } catch (e: Exception) {
            errorsRepository.addError(newErrorNow(taskType, e))

            return null
        }
    }
}