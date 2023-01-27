package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.di.TabScope
import javax.inject.Inject
import javax.inject.Provider

@TabScope
class TabViewModelsHolder @Inject constructor(
    logViewModel: LogViewModel,
    branchesViewModel: BranchesViewModel,
    tagsViewModel: TagsViewModel,
    remotesViewModel: RemotesViewModel,
    statusViewModel: StatusViewModel,
    menuViewModel: MenuViewModel,
    stashesViewModel: StashesViewModel,
    submodulesViewModel: SubmodulesViewModel,
    commitChangesViewModel: CommitChangesViewModel,
    multiCommitChangesViewModel: MultiCommitChangesViewModel,
    cloneViewModel: CloneViewModel,
    settingsViewModel: SettingsViewModel,
    // Dynamic VM
    private val diffViewModelProvider: Provider<DiffViewModel>,
    private val rebaseInteractiveViewModelProvider: Provider<RebaseInteractiveViewModel>,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    private val authorViewModelProvider: Provider<AuthorViewModel>,
) {
    val viewModels = mapOf(
        logViewModel::class to logViewModel,
        branchesViewModel::class to branchesViewModel,
        tagsViewModel::class to tagsViewModel,
        remotesViewModel::class to remotesViewModel,
        statusViewModel::class to statusViewModel,
        menuViewModel::class to menuViewModel,
        stashesViewModel::class to stashesViewModel,
        submodulesViewModel::class to submodulesViewModel,
        commitChangesViewModel::class to commitChangesViewModel,
        multiCommitChangesViewModel::class to multiCommitChangesViewModel,
        cloneViewModel::class to cloneViewModel,
        settingsViewModel::class to settingsViewModel,
    )


    fun getVMAndCacheIt() {

    }
}