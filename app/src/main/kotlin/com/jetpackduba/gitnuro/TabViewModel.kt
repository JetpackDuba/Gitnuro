package com.jetpackduba.gitnuro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.jetpackduba.gitnuro.di.TabComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class TabViewModel : ViewModel() {
    val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    open fun onClear() {
        viewModelScope.coroutineContext.cancelChildren()
    }
}

context(viewModel: TabViewModel)
fun <T> Flow<T>.collectLatestInViewModel(action: suspend (value: T) -> Unit) {
    viewModel.viewModelScope.launch {
        this@collectLatestInViewModel.collectLatest(action)
    }
}


context(scope: CoroutineScope)
fun <T> Flow<T>.collectLatestInCoroutineScope(action: suspend (value: T) -> Unit) {
    scope.launch {
        this@collectLatestInCoroutineScope.collectLatest(action)
    }
}

@Composable
inline fun <reified T : TabViewModel> tabViewModel(key: NavKey, noinline provideVM: (TabComponent) -> T): T {
    val tab = LocalTab.current

    DisposableEffect(Unit) {
        onDispose {
            tab.removeViewModel(key)
        }
    }

    return tab.getViewModel(key, provideVM)
}

