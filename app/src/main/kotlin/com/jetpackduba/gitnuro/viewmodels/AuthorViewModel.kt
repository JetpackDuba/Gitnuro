package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.extensions.nullIfEmpty
import com.jetpackduba.gitnuro.domain.interfaces.ILoadAuthorGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISaveAuthorGitAction
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.GetAuthorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthorViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val saveAuthorGitAction: ISaveAuthorGitAction,
    private val getAuthorUseCase: GetAuthorUseCase,
) : TabViewModel() {
    val authorInfo: StateFlow<AuthorInfo> = flow {
        val author = when (val author = getAuthorUseCase()) {
            is Either.Ok -> author.value
            else -> AuthorInfo(null, null, null, null)
        }

        emit(author)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = AuthorInfo(null, null, null, null),
        )


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
