package app.viewmodels

import app.extensions.nullIfEmpty
import app.git.RefreshType
import app.git.TabState
import app.git.author.LoadAuthorUseCase
import app.git.author.SaveAuthorUseCase
import app.models.AuthorInfo
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
