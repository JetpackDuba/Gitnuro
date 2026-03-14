package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.usecases.ResetBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.ResetType
import com.jetpackduba.gitnuro.domain.usecases.StashChangesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class StashWithMessageViewModel @Inject constructor(
    private val stashChangesUseCase: StashChangesUseCase,
) : TabViewModel() {

    fun stash(message: String) {
        stashChangesUseCase(message)
    }
}