package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import com.jetpackduba.gitnuro.domain.models.Identity
import com.jetpackduba.gitnuro.domain.models.emptyIdentity
import com.jetpackduba.gitnuro.domain.usecases.GetAuthorUseCase
import com.jetpackduba.gitnuro.domain.usecases.SaveAuthorUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthorViewModel @Inject constructor(
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val getAuthorUseCase: GetAuthorUseCase,
) : TabViewModel() {
    val authorInfo: StateFlow<AuthorInfo> = flow {
        val author = when (val author = getAuthorUseCase()) {
            is Either.Ok -> author.value
            else -> AuthorInfo(emptyIdentity(), emptyIdentity())
        }

        emit(author)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = AuthorInfo(emptyIdentity(), emptyIdentity()),
        )


    fun saveAuthorInfo(globalName: String?, globalEmail: String?, name: String?, email: String?) = viewModelScope.launch {
        saveAuthorUseCase(
            AuthorInfo(Identity(globalName, globalEmail), Identity(name, email))
        )
    }
}
