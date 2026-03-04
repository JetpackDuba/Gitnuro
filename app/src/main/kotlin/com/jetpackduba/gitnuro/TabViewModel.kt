package com.jetpackduba.gitnuro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation3.runtime.NavKey
import com.jetpackduba.gitnuro.viewmodels.IViewModelsProvider

open class TabViewModel {
    open fun onClear() {}
}

@Composable
inline fun <reified T : TabViewModel> tabViewModel(key: NavKey, provideVM: (IViewModelsProvider) -> T): T {
    val tab = LocalTab.current

    DisposableEffect(Unit) {
        onDispose {
            tab.removeViewModel(key)
        }
    }

    return tab.getViewModel(key, provideVM)
}
