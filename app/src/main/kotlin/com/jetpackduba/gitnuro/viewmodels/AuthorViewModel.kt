package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.extensions.nullIfEmpty
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.git.author.LoadAuthorGitAction
import com.jetpackduba.gitnuro.domain.git.author.SaveAuthorGitAction
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AuthorViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val saveAuthorGitAction: SaveAuthorGitAction,
    private val loadAuthorGitAction: LoadAuthorGitAction,
) {

    private val _authorInfo = MutableStateFlow(AuthorInfo(null, null, null, null))
    val authorInfo: StateFlow<AuthorInfo> = _authorInfo

    fun loadAuthorInfo() = tabState.runOperation(
        refreshType = RefreshType.NONE,
        showError = true,
    ) { git ->
        _authorInfo.value = loadAuthorGitAction(git)
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

        saveAuthorGitAction(git, newAuthorInfo)
    }
}
