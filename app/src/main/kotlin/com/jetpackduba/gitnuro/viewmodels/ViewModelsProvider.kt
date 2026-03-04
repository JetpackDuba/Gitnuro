package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.ui.dialogs.AddEditRemoteViewModel
import com.jetpackduba.gitnuro.ui.dialogs.CreateBranchViewModel
import com.jetpackduba.gitnuro.ui.dialogs.CreateTagViewModel
import com.jetpackduba.gitnuro.ui.dialogs.QuickActionsViewModel
import com.jetpackduba.gitnuro.ui.dialogs.ResetBranchViewModel
import com.jetpackduba.gitnuro.ui.dialogs.SignOffDialogViewModel
import com.jetpackduba.gitnuro.ui.diff.DiffViewModel
import com.jetpackduba.gitnuro.ui.status.StatusPaneViewModel
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SidePanelViewModel
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SubmoduleDialogViewModel
import javax.inject.Inject
import javax.inject.Provider

interface IViewModelsProvider {
    val logViewModel: LogViewModel
    val statusPaneViewModel: StatusPaneViewModel
    val menuViewModel: MenuViewModel
    val commitChangesViewModel: CommitChangesViewModel
    val cloneViewModel: CloneViewModel
    val settingsViewModel: SettingsViewModel
    val sidePanelViewModel: SidePanelViewModel
    val rebaseInteractiveViewModel: RebaseInteractiveViewModel
    val repositoryOpenViewModel: RepositoryOpenViewModel
    val diffViewModel: DiffViewModel
    val historyViewModel: HistoryViewModel
    val authorViewModel: AuthorViewModel
    val quickActionsViewModel: QuickActionsViewModel
    val setUpstreamBranchDialogViewModelFactory: SetUpstreamBranchDialogViewModel.Factory
    val renameBranchDialogViewModelFactory: RenameBranchDialogViewModel.Factory
    val createBranchViewModelFactory: CreateBranchViewModel.Factory
    val createTagViewModelFactory: CreateTagViewModel.Factory
    val resetBranchViewModelFactory: ResetBranchViewModel.Factory
    val addEditRemoteViewModelFactory: AddEditRemoteViewModel.Factory
    val submoduleDialogViewModel: SubmoduleDialogViewModel
    val signOffDialogViewModel: SignOffDialogViewModel
}

@TabScope
class ViewModelsProvider @Inject constructor(
    override val logViewModel: LogViewModel,
    override val statusPaneViewModel: StatusPaneViewModel,
    override val menuViewModel: MenuViewModel,
    override val commitChangesViewModel: CommitChangesViewModel,
    override val cloneViewModel: CloneViewModel,
    override val settingsViewModel: SettingsViewModel,
    override val sidePanelViewModel: SidePanelViewModel,
    override val rebaseInteractiveViewModel: RebaseInteractiveViewModel,
    private val repositoryOpenViewModelProvider: Provider<RepositoryOpenViewModel>,
    private val diffViewModelProvider: Provider<DiffViewModel>,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    private val authorViewModelProvider: Provider<AuthorViewModel>,
    private val setUpstreamBranchDialogViewModelProvider: Provider<SetUpstreamBranchDialogViewModel.Factory>,
    private val renameBranchDialogViewModelProvider: Provider<RenameBranchDialogViewModel.Factory>,
    private val addEditRemoteViewModelProvider: Provider<AddEditRemoteViewModel.Factory>,
    private val submoduleDialogViewModelProvider: Provider<SubmoduleDialogViewModel>,
    private val signOffDialogViewModelProvider: Provider<SignOffDialogViewModel>,
    private val createBranchViewModelProvider: Provider<CreateBranchViewModel.Factory>,
    private val createTagViewModelProvider: Provider<CreateTagViewModel.Factory>,
    private val quickActionsViewModelProvider: Provider<QuickActionsViewModel>,
    private val resetBranchViewModelProvider: Provider<ResetBranchViewModel.Factory>,
) : IViewModelsProvider {
    override val repositoryOpenViewModel: RepositoryOpenViewModel
        get() = repositoryOpenViewModelProvider.get()
    override val diffViewModel: DiffViewModel
        get() = diffViewModelProvider.get()
    override val historyViewModel: HistoryViewModel
        get() = historyViewModelProvider.get()
    override val authorViewModel: AuthorViewModel
        get() = authorViewModelProvider.get()
    override val setUpstreamBranchDialogViewModelFactory: SetUpstreamBranchDialogViewModel.Factory
        get() = setUpstreamBranchDialogViewModelProvider.get()
    override val renameBranchDialogViewModelFactory: RenameBranchDialogViewModel.Factory
        get() = renameBranchDialogViewModelProvider.get()
    override val submoduleDialogViewModel: SubmoduleDialogViewModel
        get() = submoduleDialogViewModelProvider.get()
    override val signOffDialogViewModel: SignOffDialogViewModel
        get() = signOffDialogViewModelProvider.get()
    override val addEditRemoteViewModelFactory: AddEditRemoteViewModel.Factory
        get() = addEditRemoteViewModelProvider.get()
    override val createBranchViewModelFactory: CreateBranchViewModel.Factory
        get() = createBranchViewModelProvider.get()
    override val createTagViewModelFactory: CreateTagViewModel.Factory
        get() = createTagViewModelProvider.get()
    override val resetBranchViewModelFactory: ResetBranchViewModel.Factory
        get() = resetBranchViewModelProvider.get()
    override val quickActionsViewModel: QuickActionsViewModel
        get() = quickActionsViewModelProvider.get()
}