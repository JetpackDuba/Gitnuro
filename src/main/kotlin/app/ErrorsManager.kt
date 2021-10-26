package app.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ErrorsManager @Inject constructor() {
    private val _errorsList = MutableStateFlow(listOf<Error>())
    val errorsList: StateFlow<List<Error>>
        get() = _errorsList

    private val _lastError = MutableStateFlow<Error?>(null)
    val lastError: StateFlow<Error?>
        get() = _lastError

    fun addError(error: Error) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            add(error)
        }

        _lastError.value = error
    }

    fun removeError(error: Error) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            remove(error)
        }
    }
}


data class Error(val date: Long, val exception: Exception, val message: String)

fun newErrorNow(exception: Exception,message: String) = Error(System.currentTimeMillis(), exception, message)