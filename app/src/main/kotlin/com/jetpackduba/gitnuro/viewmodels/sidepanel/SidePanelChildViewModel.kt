package com.jetpackduba.gitnuro.viewmodels.sidepanel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class SidePanelChildViewModel(expandedDefault: Boolean) {
    private val _isExpanded = MutableStateFlow(expandedDefault)
    val isExpanded: StateFlow<Boolean> = _isExpanded

    fun onExpand() {
        _isExpanded.value = !isExpanded.value
    }
}