@file:OptIn(FlowPreview::class)

package com.jetpackduba.gitnuro.ui

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.repositories.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val LOADING_DEBOUNCE = 150

data class UiDataState<T>(val isLoading: Boolean, val data: T?, val error: AppError?)

context(vm: TabViewModel)
fun <T> Flow<DataState<T>>.toUiDataState(debounceForLoading: Int = LOADING_DEBOUNCE): StateFlow<UiDataState<T>> {
    return this.toUiDataState(vm.viewModelScope, debounceForLoading)
}

context(scope: CoroutineScope)
fun <T> Flow<DataState<T>>.toUiDataState(debounceForLoading: Int = LOADING_DEBOUNCE): StateFlow<UiDataState<T>> {
    return this.toUiDataState(scope, debounceForLoading)
}

// TODO This could be moved to the version of this function that uses context(scope: CoroutineScope) once
//  explicit context parameters are stable
private fun <T> Flow<DataState<T>>.toUiDataState(scope: CoroutineScope, debounceForLoading: Int): StateFlow<UiDataState<T>> {
    val stateFlow = MutableStateFlow(UiDataState<T>(isLoading = true, data = null, error = null))
    scope.launch {
        this@toUiDataState
            .debounce { state ->
                if (state is DataState.Loading) {
                    debounceForLoading
                } else {
                    0
                }.milliseconds
            }
            .collect { newState ->
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