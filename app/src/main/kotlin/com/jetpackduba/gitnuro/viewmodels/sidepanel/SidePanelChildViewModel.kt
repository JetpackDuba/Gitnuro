package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.TabViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class SidePanelChildViewModel(expandedDefault: Boolean): TabViewModel() {
    private val _isExpanded = MutableStateFlow(expandedDefault)
    val isExpanded: StateFlow<Boolean> = _isExpanded

    fun onExpand() {
        _isExpanded.value = !isExpanded.value
    }
}