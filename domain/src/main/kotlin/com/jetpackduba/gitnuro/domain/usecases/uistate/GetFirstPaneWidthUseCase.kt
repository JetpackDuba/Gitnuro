package com.jetpackduba.gitnuro.domain.usecases.uistate

import com.jetpackduba.gitnuro.domain.repositories.UiStateRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

const val DEFAULT_FIRST_PANE_WIDTH = 220f

class GetFirstPaneWidthUseCase @Inject constructor(
    private val uiStateRepository: UiStateRepository,
) {
    suspend operator fun invoke(): Float {
        return uiStateRepository.firstPaneWidth.firstOrNull() ?: DEFAULT_FIRST_PANE_WIDTH
    }
}