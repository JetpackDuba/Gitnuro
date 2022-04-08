package app

import app.di.TabScope
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
        println("LastError flow: $error")
    }

    fun removeError(error: Error) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            remove(error)
        }
    }
}


data class Error(val date: Long, val exception: Exception, val message: String)

fun newErrorNow(exception: Exception, message: String) = Error(System.currentTimeMillis(), exception, message)