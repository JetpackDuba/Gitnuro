package com.jetpackduba.gitnuro.ui

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import com.jetpackduba.gitnuro.ui.status.CommitterDataRequestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@TabScope
class CommitAuthorRequestHolder @Inject constructor() {
    val requestState: StateFlow<CommitterDataRequestState>
        field = MutableStateFlow<CommitterDataRequestState>(CommitterDataRequestState.None)

    fun rejectCommitterData() {
        this.requestState.value = CommitterDataRequestState.Reject
    }

    fun acceptCommitterData(newAuthorInfo: AuthorInfo, persist: Boolean) {
        this.requestState.value = CommitterDataRequestState.Accepted(newAuthorInfo, persist)
    }

    fun setStateToWaiting(author: AuthorInfo) {
        this.requestState.value = CommitterDataRequestState.WaitingInput(author)
    }
}