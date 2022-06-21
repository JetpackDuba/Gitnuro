package app.viewmodels

import app.extensions.nullIfEmpty
import app.git.AuthorManager
import app.git.RefreshType
import app.git.TabState
import app.models.AuthorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AuthorViewModel @Inject constructor(
    private val tabState: TabState,
    private val authorManager: AuthorManager,
) {

    private val _authorInfo = MutableStateFlow(AuthorInfo(null, null, null, null))
    val authorInfo: StateFlow<AuthorInfo> = _authorInfo

    fun loadAuthorInfo() = tabState.runOperation(
        refreshType = RefreshType.NONE,
        showError = true,
    ) { git ->
        _authorInfo.value = authorManager.loadAuthor(git)
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

        authorManager.saveAuthorInfo(git, newAuthorInfo)
    }
}
