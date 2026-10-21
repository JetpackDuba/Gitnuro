package com.jetpackduba.gitnuro.data.repositories.ui

import com.jetpackduba.gitnuro.data.extensions.get
import com.jetpackduba.gitnuro.data.extensions.setValue
import com.jetpackduba.gitnuro.domain.models.ui.WindowPlacement
import com.jetpackduba.gitnuro.domain.repositories.UiStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreUiStateRepository @Inject constructor(
    private val dataStore: UiStateDataStore,
    private val windowPlacementMapper: WindowPlacementMapper,
) : UiStateRepository {
    override val windowPlacement: Flow<WindowPlacement?> = dataStore[windowPlacementPreference].toWindowPlacement()
    override val firstPaneWidth: Flow<Float?> = dataStore[firstPaneWidthPreference]
    override val thirdPaneWidth: Flow<Float?> = dataStore[thirdPaneWidthPreference]

    override suspend fun windowPlacement(value: WindowPlacement) {
        dataStore.setValue(windowPlacementPreference, windowPlacementMapper.toData(value))
    }

    override suspend fun firstPaneWidth(value: Float) {
        dataStore.setValue(firstPaneWidthPreference, value)
    }

    override suspend fun thirdPaneWidth(value: Float) {
        dataStore.setValue(thirdPaneWidthPreference, value)
    }

    private fun Flow<Int?>.toWindowPlacement() = this.map {
        it?.let {
            windowPlacementMapper.toDomain(it)
        }
    }
}
