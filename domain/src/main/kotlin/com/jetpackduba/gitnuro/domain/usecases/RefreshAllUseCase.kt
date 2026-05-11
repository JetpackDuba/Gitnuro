package com.jetpackduba.gitnuro.domain.usecases

import javax.inject.Inject

// TODO Some parts of the app abuse this use case when not everything needs to be updated.
class RefreshAllUseCase @Inject constructor(
    private val refreshBranchesUseCase: RefreshBranchesUseCase,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val refreshRemotesUseCase: RefreshRemotesUseCase,
    private val refreshStashListUseCase: RefreshStashListUseCase,
    private val refreshSubmodulesUseCase: RefreshSubmodulesUseCase,
    private val refreshGitConfigUseCase: RefreshGitConfigUseCase,
    private val refreshTagsUseCase: RefreshTagsUseCase,
) {
    operator fun invoke() {
        refreshBranchesUseCase()
        refreshStatusUseCase()
        refreshLogUseCase()
        refreshRemotesUseCase()
        refreshStashListUseCase()
        refreshSubmodulesUseCase()
        refreshGitConfigUseCase()
        refreshTagsUseCase()
    }
}