package com.jetpackduba.gitnuro.domain.usecases

import javax.inject.Inject

class RefreshAllUseCase @Inject constructor(
    private val refreshBranchesUseCase: RefreshBranchesUseCase,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
) {
    suspend operator fun invoke() {
        refreshBranchesUseCase()
        refreshStatusUseCase()
        refreshLogUseCase()
    }
}