package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.AppStateManager
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.git.FileChangesWatcher
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.repository.GetRepositoryStateUseCase
import com.jetpackduba.gitnuro.git.repository.InitLocalRepositoryUseCase
import com.jetpackduba.gitnuro.git.repository.OpenRepositoryUseCase
import com.jetpackduba.gitnuro.updates.UpdatesRepository
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
        cloneViewModel::class to cloneViewModel,
        settingsViewModel::class to settingsViewModel,
    )


    fun getVMAndCacheIt() {

    }
}