package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.models.ui.WindowPlacement
import kotlinx.coroutines.flow.Flow

interface UiStateRepository {
    val windowPlacement: Flow<WindowPlacement?>

    val firstPaneWidth: Flow<Float?>
    val thirdPaneWidth: Flow<Float?>

    suspend fun windowPlacement(value: WindowPlacement)
    suspend fun firstPaneWidth(value: Float)
    suspend fun thirdPaneWidth(value: Float)
}