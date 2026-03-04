package com.jetpackduba.gitnuro.ui.components

import androidx.compose.runtime.MutableState
import androidx.navigation3.runtime.NavKey
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.viewmodels.RepositoryTabViewModel
import com.jetpackduba.gitnuro.viewmodels.ViewModelsProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.io.path.Path
import kotlin.io.path.name

class TabInformation @AssistedInject constructor(
    private val appStateManager: AppStateManager,
    val tabViewModelsProvider: ViewModelsProvider,
    @Assisted val tabName: MutableState<String>,
    @Assisted val initialPath: String?,
    @Assisted val onTabPathChanged: () -> Unit,
    repositoryTabViewModelFactory: RepositoryTabViewModel.Factory,
) {
    private val tag = "TabInformation"

    @AssistedFactory
    interface Factory {
        fun create(tabName: MutableState<String>, initialPath: String?, onTabPathChanged: () -> Unit): TabInformation
    }

    val repositoryTabViewModel = repositoryTabViewModelFactory.create(initialPath)

    val viewModelsMap = mutableMapOf<NavKey, TabViewModel>()

    inline fun <T : TabViewModel> getViewModel(key: NavKey, provideVM: (ViewModelsProvider) -> T): T {
        repositoryTabViewModel.backStack

        if (!viewModelsMap.contains(key)) {
            viewModelsMap[key] = provideVM(tabViewModelsProvider)
        }

        return viewModelsMap.getValue(key) as T
    }

    var path = initialPath
        private set

    init {
        if (initialPath != null) {
            tabName.value = Path(initialPath).name
        }

        repositoryTabViewModel.onRepositoryChanged = { newPath ->
            this.path = newPath

            if (newPath == null) {
                tabName.value = NEW_TAB_DEFAULT_NAME
            } else {
                tabName.value = Path(newPath).name
                appStateManager.repositoryTabChanged(newPath)
            }

            onTabPathChanged()
        }
    }

    fun dispose() {
        repositoryTabViewModel.dispose()
    }

    fun removeViewModel(key: NavKey) {
        if (!repositoryTabViewModel.backStack.contains(key)) {
            printLog(tag, "TAB ${tabName.value} - Removing view model for key $key")
            viewModelsMap[key]?.onClear()
            viewModelsMap.remove(key)
        } else {
            printLog(tag, "TAB ${tabName.value} - Keeping view model for key $key")
        }
    }

    companion object {
        const val NEW_TAB_DEFAULT_NAME = "New tab"
    }
}