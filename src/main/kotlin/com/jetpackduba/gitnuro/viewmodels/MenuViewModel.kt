package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import javax.inject.Inject

class MenuViewModel @Inject constructor(
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
    settings: AppSettingsRepository,
    appStateManager: AppStateManager,
): IGlobalMenuActionsViewModel by globalMenuActionsViewModel {
    val isPullWithRebaseDefault = settings.pullRebaseFlow
    val lastLoadedTabs = appStateManager.latestOpenedRepositoriesPaths

}