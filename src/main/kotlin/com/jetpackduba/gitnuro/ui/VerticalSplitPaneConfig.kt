package com.jetpackduba.gitnuro.ui

import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface IVerticalSplitPaneConfig {
    val firstPaneWidth: StateFlow<Float>
    val onPanelsWidthPersisted: SharedFlow<Unit>
    val thirdPaneWidth: StateFlow<Float>
    fun setFirstPaneWidth(firstPaneWidth: Float)
    fun setThirdPaneWidth(thirdPaneWidth: Float)
    suspend fun persistFirstPaneWidth()
    suspend fun persistThirdPaneWidth()
}

@Singleton
class VerticalSplitPaneConfig @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
) : IVerticalSplitPaneConfig {
    private val _firstPaneWidth = MutableStateFlow<Float>(appSettingsRepository.firstPaneWidth)
    override val firstPaneWidth: StateFlow<Float> = _firstPaneWidth

    private val _thirdPaneWidth = MutableStateFlow<Float>(appSettingsRepository.thirdPaneWidth)
    override val thirdPaneWidth: StateFlow<Float> = _thirdPaneWidth

    private val _onPanelsWidthPersisted = MutableSharedFlow<Unit>()
    override val onPanelsWidthPersisted: SharedFlow<Unit> = _onPanelsWidthPersisted

    override fun setFirstPaneWidth(firstPaneWidth: Float) {
        this._firstPaneWidth.value = firstPaneWidth
    }

    override fun setThirdPaneWidth(thirdPaneWidth: Float) {
        this._thirdPaneWidth.value = thirdPaneWidth
    }

    override suspend fun persistFirstPaneWidth() {
        appSettingsRepository.firstPaneWidth = _firstPaneWidth.value
        _onPanelsWidthPersisted.emit(Unit)
    }

    override suspend fun persistThirdPaneWidth() {
        appSettingsRepository.thirdPaneWidth = _thirdPaneWidth.value
        _onPanelsWidthPersisted.emit(Unit)
    }
}