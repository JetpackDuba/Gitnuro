package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class RefreshAllUseCase @Inject constructor(
    private val refreshBranchesUseCase: RefreshBranchesUseCase,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val refreshRemotesUseCase: RefreshRemotesUseCase,
    private val refreshStashListUseCase: RefreshStashListUseCase,
    private val refreshSubmodulesUseCase: RefreshSubmodulesUseCase,
    private val tabCoroutineScope: TabCoroutineScope,
) {
    operator fun invoke() = tabCoroutineScope.launch {
        refreshBranchesUseCase()
        refreshStatusUseCase()
        refreshLogUseCase()
        refreshRemotesUseCase()
        refreshStashListUseCase()
        refreshSubmodulesUseCase()
    }
}