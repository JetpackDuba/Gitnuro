package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.extensions.nullIfEmpty
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.author.LoadAuthorUseCase
import com.jetpackduba.gitnuro.git.author.SaveAuthorUseCase
import com.jetpackduba.gitnuro.models.AuthorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AuthorViewModel @Inject constructor(
    private val tabState: TabState,
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val loadAuthorUseCase: LoadAuthorUseCase,
) {

    private val _authorInfo = MutableStateFlow(AuthorInfo(null, null, null, null))
    val authorInfo: StateFlow<AuthorInfo> = _authorInfo

    fun loadAuthorInfo() = tabState.runOperation(
        refreshType = RefreshType.NONE,
        showError = true,
    ) { git ->
        _authorInfo.value = loadAuthorUseCase(git)
    }

    fun saveAuthorInfo(globalName: String, globalEmail: String, name: String, email: String) = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.REPO_STATE,
    ) { git ->
        val newAuthorInfo = AuthorInfo(
            globalName.nullIfEmpty,
            globalEmail.nullIfEmpty,
            name.nullIfEmpty,
            email.nullIfEmpty,
        )

        saveAuthorUseCase(git, newAuthorInfo)
    }
}
