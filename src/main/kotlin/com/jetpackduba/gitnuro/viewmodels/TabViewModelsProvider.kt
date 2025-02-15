package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SidePanelViewModel
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SubmoduleDialogViewModel
import javax.inject.Inject
import javax.inject.Provider

interface ITabViewModelsProvider {
    val logViewModel: LogViewModel
    val statusViewModel: StatusViewModel
    val menuViewModel: MenuViewModel
    val commitChangesViewModel: CommitChangesViewModel
    val cloneViewModel: CloneViewModel
    val settingsViewModel: SettingsViewModel
    val sidePanelViewModel: SidePanelViewModel
    val rebaseInteractiveViewModel: RebaseInteractiveViewModel
    val diffViewModel: DiffViewModel
    val historyViewModel: HistoryViewModel
    val authorViewModel: AuthorViewModel
    val changeUpstreamBranchDialogViewModel: ChangeUpstreamBranchDialogViewModel
    val submoduleDialogViewModel: SubmoduleDialogViewModel
    val signOffDialogViewModel: SignOffDialogViewModel
}

@TabScope
class TabViewModelsProvider @Inject constructor(
    override val logViewModel: LogViewModel,
    override val statusViewModel: StatusViewModel,
    override val menuViewModel: MenuViewModel,
    override val commitChangesViewModel: CommitChangesViewModel,
    override val cloneViewModel: CloneViewModel,
    override val settingsViewModel: SettingsViewModel,
    override val sidePanelViewModel: SidePanelViewModel,
    override val rebaseInteractiveViewModel: RebaseInteractiveViewModel,
    private val diffViewModelProvider: Provider<DiffViewModel>,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    private val authorViewModelProvider: Provider<AuthorViewModel>,
    private val changeUpstreamBranchDialogViewModelProvider: Provider<ChangeUpstreamBranchDialogViewModel>,
    private val submoduleDialogViewModelProvider: Provider<SubmoduleDialogViewModel>,
    private val signOffDialogViewModelProvider: Provider<SignOffDialogViewModel>,
) : ITabViewModelsProvider {
    override val diffViewModel: DiffViewModel
        get() = diffViewModelProvider.get()
    override val historyViewModel: HistoryViewModel
        get() = historyViewModelProvider.get()
    override val authorViewModel: AuthorViewModel
        get() = authorViewModelProvider.get()
    override val changeUpstreamBranchDialogViewModel: ChangeUpstreamBranchDialogViewModel
        get() = changeUpstreamBranchDialogViewModelProvider.get()
    override val submoduleDialogViewModel: SubmoduleDialogViewModel
        get() = submoduleDialogViewModelProvider.get()
    override val signOffDialogViewModel: SignOffDialogViewModel
        get() = signOffDialogViewModelProvider.get()
}