package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.SignOffConfig
import com.jetpackduba.gitnuro.domain.usecases.LoadSignOffConfigUseCase
import com.jetpackduba.gitnuro.domain.usecases.SaveSignOffConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class SignOffDialogViewModel @Inject constructor(
    private val loadSignOffConfigUseCase: LoadSignOffConfigUseCase,
    private val saveSignOffConfigUseCase: SaveSignOffConfigUseCase,
) : TabViewModel() {
    private val _state = MutableStateFlow<SignOffState>(SignOffState.Loading)
    val state = _state.asStateFlow()

    fun loadSignOffFormat() {
        viewModelScope.launch {
            val signOffConfig = loadSignOffConfigUseCase()

            if (signOffConfig is Either.Ok) {
                _state.value = SignOffState.Loaded(signOffConfig.value)
            }
        }
    }

    fun saveSignOffFormat(newIsEnabled: Boolean, newFormat: String) {
        viewModelScope.launch {
            saveSignOffConfigUseCase(SignOffConfig(newIsEnabled, newFormat))
        }
    }
}

sealed interface SignOffState {
    object Loading : SignOffState
    data class Loaded(val signOffConfig: SignOffConfig) : SignOffState
}