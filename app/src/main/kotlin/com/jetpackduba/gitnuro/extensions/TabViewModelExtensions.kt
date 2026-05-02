package com.jetpackduba.gitnuro.extensions

import com.jetpackduba.gitnuro.TabViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

context(vm: TabViewModel)
fun <T> Flow<T>.stateIn(
    initialValue: T
): StateFlow<T> {
    return this.stateIn(
        vm.viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = initialValue,
    )
}