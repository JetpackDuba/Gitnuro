package com.jetpackduba.gitnuro.managers

import com.jetpackduba.gitnuro.di.TabScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

@TabScope
class ErrorsManager @Inject constructor() {
    private val _errorsList = MutableStateFlow(listOf<Error>())
    val errorsList: StateFlow<List<Error>>
        get() = _errorsList

    private val _error = MutableSharedFlow<Error?>()
    val error: SharedFlow<Error?> = _error

    suspend fun addError(error: Error) = withContext(Dispatchers.IO) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            add(error)
        }

        _error.emit(error)
    }

    fun removeError(error: Error) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            remove(error)
        }
    }
}


data class Error(
    val date: Long,
    val exception: Exception,
    val title: String?,
    val message: String
)

fun newErrorNow(
    exception: Exception,
    title: String?,
    message: String,
): Error {
    return Error(
        date = System.currentTimeMillis(),
        exception = exception,
        title = title,
        message = message
    )
}