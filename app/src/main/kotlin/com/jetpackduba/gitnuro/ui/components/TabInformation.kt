package com.jetpackduba.gitnuro.ui.components

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow

@Stable
class TabInformation<T: TabInformationProvider>(val data: T) {
    val name = data.tabName
    val extraInfo = data.extraInfo
}

interface TabInformationProvider {
    val tabName: StateFlow<String?>
    val extraInfo: StateFlow<String?>

    fun dispose()
}