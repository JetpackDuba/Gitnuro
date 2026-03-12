package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.ILoadAuthorGitAction
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class GetAuthorUseCase @Inject constructor(
    private val loadAuthorGitAction: ILoadAuthorGitAction,
    private val tabState: TabInstanceRepository,
) {
    operator fun invoke() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        loadAuthorGitAction(git)
    }
}