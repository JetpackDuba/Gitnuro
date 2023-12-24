package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SidePanelViewModel
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SubmoduleDialogViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass

@TabScope
class TabViewModelsHolder @Inject constructor(
    logViewModel: LogViewModel,
    statusViewModel: StatusViewModel,
    menuViewModel: MenuViewModel,
    commitChangesViewModel: CommitChangesViewModel,
    cloneViewModel: CloneViewModel,
    settingsViewModel: SettingsViewModel,
    sidePanelViewModel: SidePanelViewModel,
    rebaseInteractiveViewModel: RebaseInteractiveViewModel,
    // Dynamic VM
    private val diffViewModelProvider: Provider<DiffViewModel>,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    private val authorViewModelProvider: Provider<AuthorViewModel>,
    private val changeDefaultUpstreamBranchViewModelProvider: Provider<ChangeDefaultUpstreamBranchViewModel>,
    private val submoduleDialogViewModelProvider: Provider<SubmoduleDialogViewModel>,
    private val signOffDialogViewModelProvider: Provider<SignOffDialogViewModel>,
) {
    val viewModels = mapOf(
        logViewModel::class to logViewModel,
        sidePanelViewModel::class to sidePanelViewModel,
        statusViewModel::class to statusViewModel,
        menuViewModel::class to menuViewModel,
        commitChangesViewModel::class to commitChangesViewModel,
        cloneViewModel::class to cloneViewModel,
        settingsViewModel::class to settingsViewModel,
        rebaseInteractiveViewModel::class to rebaseInteractiveViewModel,
    )

    // TODO Call this when required
    fun dynamicViewModel(type: KClass<*>): Any {
        return when (type) {
            DiffViewModel::class -> diffViewModelProvider.get()
            HistoryViewModel::class -> historyViewModelProvider.get()
            AuthorViewModel::class -> authorViewModelProvider.get()
            ChangeDefaultUpstreamBranchViewModel::class -> changeDefaultUpstreamBranchViewModelProvider.get()
            SubmoduleDialogViewModel::class -> submoduleDialogViewModelProvider.get()
            SignOffDialogViewModel::class -> signOffDialogViewModelProvider.get()
            else -> throw NotImplementedError("View model provider not implemented")
        }
    }
}