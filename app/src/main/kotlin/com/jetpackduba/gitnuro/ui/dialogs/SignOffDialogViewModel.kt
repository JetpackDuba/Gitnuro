package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.interfaces.ILoadSignOffConfigGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISaveLocalRepositoryConfigGitAction
import com.jetpackduba.gitnuro.domain.models.SignOffConfig
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class SignOffDialogViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val loadSignOffConfigGitAction: ILoadSignOffConfigGitAction,
    private val saveLocalRepositoryConfigGitAction: ISaveLocalRepositoryConfigGitAction,
) : TabViewModel() {
    private val _state = MutableStateFlow<SignOffState>(SignOffState.Loading)
    val state = _state.asStateFlow()

    fun loadSignOffFormat() = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        val signOffConfig = loadSignOffConfigGitAction(git.repository)

        _state.value = SignOffState.Loaded(signOffConfig)
    }

    fun saveSignOffFormat(newIsEnabled: Boolean, newFormat: String) = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        saveLocalRepositoryConfigGitAction(git.repository, SignOffConfig(newIsEnabled, newFormat))
    }
}

sealed interface SignOffState {
    object Loading : SignOffState
    data class Loaded(val signOffConfig: SignOffConfig) : SignOffState
}