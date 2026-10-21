package com.jetpackduba.gitnuro.domain.usecases.uistate

import com.jetpackduba.gitnuro.domain.repositories.UiStateRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

const val DEFAULT_THIRD_PANE_WIDTH = 330f

class GetThirdPaneWidthUseCase @Inject constructor(
    private val uiStateRepository: UiStateRepository,
) {
    suspend operator fun invoke(): Float {
        return uiStateRepository.thirdPaneWidth.firstOrNull() ?: DEFAULT_THIRD_PANE_WIDTH
    }
}