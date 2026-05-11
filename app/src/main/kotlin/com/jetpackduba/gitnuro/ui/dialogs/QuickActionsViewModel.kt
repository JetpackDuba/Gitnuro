package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.usecases.RefreshAllUseCase
import javax.inject.Inject

class QuickActionsViewModel @Inject constructor(
    private val refreshAllUseCase: RefreshAllUseCase,
) : TabViewModel() {

    // TODO Implement bunch of methods

    fun refreshRepository() = refreshAllUseCase()

    fun openProjectInFileExplorer() {
        TODO()
    }
}