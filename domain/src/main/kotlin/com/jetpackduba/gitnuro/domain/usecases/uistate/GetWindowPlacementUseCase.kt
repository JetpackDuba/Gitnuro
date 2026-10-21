package com.jetpackduba.gitnuro.domain.usecases.uistate

import com.jetpackduba.gitnuro.domain.models.ui.WindowPlacement
import com.jetpackduba.gitnuro.domain.repositories.UiStateRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class GetWindowPlacementUseCase @Inject constructor(
    private val uiStateRepository: UiStateRepository,
) {
    suspend operator fun invoke(): WindowPlacement {
        val defaultWindowPlacement = WindowPlacement.MAXIMIZED

        return uiStateRepository.windowPlacement.firstOrNull() ?: defaultWindowPlacement
    }
}