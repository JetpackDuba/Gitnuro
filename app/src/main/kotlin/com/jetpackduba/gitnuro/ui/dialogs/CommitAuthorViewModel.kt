package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import com.jetpackduba.gitnuro.ui.CommitAuthorRequestHolder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CommitAuthorViewModel @AssistedInject constructor(
    private val commitAuthorRequestHolder: CommitAuthorRequestHolder,
    @Assisted val authorInfo: AuthorInfo,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(commit: AuthorInfo?): CommitAuthorViewModel
    }

    fun accept(authorInfo: AuthorInfo, persist: Boolean) {
        commitAuthorRequestHolder.acceptCommitterData(authorInfo, persist)
    }

    fun reject() {
        commitAuthorRequestHolder.rejectCommitterData()
    }
}