package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.models.Notification
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.jetpackduba.gitnuro.domain.models.Error

interface IErrorsRepository {
    val errorsList: StateFlow<List<Error>>
    val error: SharedFlow<Error?>
    val notification: StateFlow<Map<Long, Notification>>

    suspend fun emitNotification(notification: Notification): Job

    suspend fun addError(error: Error)
    fun removeError(error: Error)
}