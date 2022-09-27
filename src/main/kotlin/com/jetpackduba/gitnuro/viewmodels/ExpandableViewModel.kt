package com.jetpackduba.gitnuro.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ExpandableViewModel(expandedDefault: Boolean = false) {
    private val _isExpanded = MutableStateFlow(expandedDefault)
    val isExpanded: StateFlow<Boolean> = _isExpanded

    fun onExpand() {
        _isExpanded.value = !isExpanded.value
    }
}