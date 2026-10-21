package com.jetpackduba.gitnuro.ui

import com.jetpackduba.gitnuro.domain.repositories.UiStateRepository
import com.jetpackduba.gitnuro.domain.usecases.uistate.DEFAULT_FIRST_PANE_WIDTH
import com.jetpackduba.gitnuro.domain.usecases.uistate.DEFAULT_THIRD_PANE_WIDTH
import com.jetpackduba.gitnuro.domain.usecases.uistate.GetFirstPaneWidthUseCase
import com.jetpackduba.gitnuro.domain.usecases.uistate.GetThirdPaneWidthUseCase
import com.jetpackduba.gitnuro.repositoryopen.mutableStateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
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
    private val uiStateRepository: UiStateRepository,
    private val getFirstPaneWidthUseCase: GetFirstPaneWidthUseCase,
    private val getThirdPaneWidthUseCase: GetThirdPaneWidthUseCase,
) : IVerticalSplitPaneConfig {
    // A new scope is fine since this class is a singleton
    private val scope = CoroutineScope(Job())

    override val firstPaneWidth: StateFlow<Float>
        field = paneSizeMutableStateFlow(DEFAULT_FIRST_PANE_WIDTH, getFirstPaneWidthUseCase::invoke)

    override val thirdPaneWidth: StateFlow<Float>
        field = paneSizeMutableStateFlow(DEFAULT_THIRD_PANE_WIDTH, getThirdPaneWidthUseCase::invoke)

    private val _onPanelsWidthPersisted = MutableSharedFlow<Unit>()
    override val onPanelsWidthPersisted: SharedFlow<Unit> = _onPanelsWidthPersisted

    override fun setFirstPaneWidth(firstPaneWidth: Float) {
        this.firstPaneWidth.value = firstPaneWidth
    }

    override fun setThirdPaneWidth(thirdPaneWidth: Float) {
        this.thirdPaneWidth.value = thirdPaneWidth
    }

    override suspend fun persistFirstPaneWidth() {
        uiStateRepository.firstPaneWidth(firstPaneWidth.value)
        _onPanelsWidthPersisted.emit(Unit)
    }

    override suspend fun persistThirdPaneWidth() {
        uiStateRepository.thirdPaneWidth(thirdPaneWidth.value)
        _onPanelsWidthPersisted.emit(Unit)
    }

    private fun paneSizeMutableStateFlow(default: Float, function: suspend () -> Float): MutableStateFlow<Float> {
        return flow {
            emit(function())
        }
            .mutableStateIn(
                scope,
                default,
            )
    }
}