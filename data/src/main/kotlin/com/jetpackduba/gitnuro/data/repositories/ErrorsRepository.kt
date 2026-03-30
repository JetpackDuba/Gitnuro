package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.exceptions.GitnuroException
import com.jetpackduba.gitnuro.domain.models.Error
import com.jetpackduba.gitnuro.domain.models.Notification
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.IErrorsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

const val NOTIFICATION_DURATION = 2_500L

class ErrorsRepository @Inject constructor(
    private val coroutineScope: TabCoroutineScope,
) : IErrorsRepository {
    private val _errorsList = MutableStateFlow(listOf<Error>())
    override val errorsList: StateFlow<List<Error>>
        get() = _errorsList

    private val _error = MutableSharedFlow<Error?>()
    override val error: SharedFlow<Error?> = _error

    private val _notification = MutableStateFlow<Map<Long, Notification>>(hashMapOf())
    override val notification: StateFlow<Map<Long, Notification>> = _notification

    private val notificationsMutex = Mutex()

    override suspend fun emitNotification(notification: Notification) = coroutineScope.launch {
        val time = System.currentTimeMillis()
        notificationsMutex.withLock {
            _notification.update { notifications ->
                notifications
                    .toMutableMap()
                    .apply { put(time, notification) }
            }
        }

        launch {
            delay(NOTIFICATION_DURATION)

            notificationsMutex.withLock {
                _notification.update { notifications ->
                    notifications
                        .toMutableMap()
                        .apply { remove(time) }
                }
            }
        }
    }

    override suspend fun addError(error: Error) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            add(error)
        }

        _error.emit(error)
    }

    override fun removeError(error: Error) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            remove(error)
        }
    }
}
