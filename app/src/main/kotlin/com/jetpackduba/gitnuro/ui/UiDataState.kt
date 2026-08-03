package com.jetpackduba.gitnuro.ui

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.repositories.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiDataState<T>(val isLoading: Boolean, val data: T?, val error: AppError?)

context(vm: TabViewModel)
fun <T> Flow<DataState<T>>.toUiDataState(): StateFlow<UiDataState<T>> = this.toUiDataState(vm.viewModelScope)

context(scope: CoroutineScope)
fun <T> Flow<DataState<T>>.toUiDataState(): StateFlow<UiDataState<T>> = this.toUiDataState(scope)

// TODO This could be moved to the version of this function that uses context(scope: CoroutineScope) once
//  explicit context parameters are stable
private fun <T> Flow<DataState<T>>.toUiDataState(scope: CoroutineScope): StateFlow<UiDataState<T>> {
    val stateFlow = MutableStateFlow(UiDataState<T>(isLoading = true, data = null, error = null))
    scope.launch {
        this@toUiDataState.collect { newState ->
            stateFlow.update { previousUiState ->
                when (newState) {
                    is DataState.Error -> previousUiState.copy(isLoading = false, error = newState.error)
                    is DataState.Loaded -> previousUiState.copy(isLoading = false, data = newState.data, error = null)
                    DataState.Loading -> previousUiState.copy(isLoading = true, error = null)
                }
            }
        }
    }

    return stateFlow
}