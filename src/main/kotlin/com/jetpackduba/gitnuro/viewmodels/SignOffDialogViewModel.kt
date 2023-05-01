package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.config.LoadSignOffConfigUseCase
import com.jetpackduba.gitnuro.git.config.SaveLocalRepositoryConfigUseCase
import com.jetpackduba.gitnuro.models.SignOffConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class SignOffDialogViewModel @Inject constructor(
    private val tabState: TabState,
    private val loadSignOffConfigUseCase: LoadSignOffConfigUseCase,
    private val saveLocalRepositoryConfigUseCase: SaveLocalRepositoryConfigUseCase,
) {
    private val _state = MutableStateFlow<SignOffState>(SignOffState.Loading)
    val state = _state.asStateFlow()

    fun loadSignOffFormat() = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        val signOffConfig = loadSignOffConfigUseCase(git.repository)

        _state.value = SignOffState.Loaded(signOffConfig)
    }

    fun saveSignOffFormat(newIsEnabled: Boolean, newFormat: String) = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        saveLocalRepositoryConfigUseCase(git.repository, SignOffConfig(newIsEnabled, newFormat))
    }
}

sealed interface SignOffState {
    object Loading : SignOffState
    data class Loaded(val signOffConfig: SignOffConfig) : SignOffState
}