package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.usecases.CreateTagUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.eclipse.jgit.revwalk.RevCommit

class CreateTagViewModel @AssistedInject constructor(
    private val createTagUseCase: CreateTagUseCase,
    @Assisted private val targetCommit: RevCommit,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(commit: RevCommit?): CreateTagViewModel
    }

    fun createTag(name: String) {
        createTagUseCase(name, targetCommit)
    }
}