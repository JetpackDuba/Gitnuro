package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SidePanelViewModel
import javax.inject.Inject
import javax.inject.Provider

@TabScope
class TabViewModelsHolder @Inject constructor(
    logViewModel: LogViewModel,
    statusViewModel: StatusViewModel,
    menuViewModel: MenuViewModel,
    commitChangesViewModel: CommitChangesViewModel,
    cloneViewModel: CloneViewModel,
    settingsViewModel: SettingsViewModel,
    sidePanelViewModel: SidePanelViewModel,
    // Dynamic VM
    private val diffViewModelProvider: Provider<DiffViewModel>,
    private val rebaseInteractiveViewModelProvider: Provider<RebaseInteractiveViewModel>,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    private val authorViewModelProvider: Provider<AuthorViewModel>,
) {
    val viewModels = mapOf(
        logViewModel::class to logViewModel,
        sidePanelViewModel::class to sidePanelViewModel,
        statusViewModel::class to statusViewModel,
        menuViewModel::class to menuViewModel,
        commitChangesViewModel::class to commitChangesViewModel,
        cloneViewModel::class to cloneViewModel,
        settingsViewModel::class to settingsViewModel,
    )


    fun getVMAndCacheIt() {

    }
}