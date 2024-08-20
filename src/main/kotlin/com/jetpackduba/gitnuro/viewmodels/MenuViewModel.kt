package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.remote_operations.FetchAllRemotesUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullType
import com.jetpackduba.gitnuro.git.remote_operations.PushBranchUseCase
import com.jetpackduba.gitnuro.git.stash.PopLastStashUseCase
import com.jetpackduba.gitnuro.git.stash.StashChangesUseCase
import com.jetpackduba.gitnuro.git.workspace.StageUntrackedFileUseCase
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.models.errorNotification
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.models.warningNotification
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.terminal.OpenRepositoryInTerminalUseCase
import javax.inject.Inject

class MenuViewModel @Inject constructor(
    private val tabState: TabState,
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
    settings: AppSettingsRepository,
    appStateManager: AppStateManager,
): IGlobalMenuActionsViewModel by globalMenuActionsViewModel {
    val isPullWithRebaseDefault = settings.pullRebaseFlow
    val lastLoadedTabs = appStateManager.latestOpenedRepositoriesPaths

}