package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.data.repositories.configuration.AppSettingsRepository
import javax.inject.Inject

class MenuViewModel @Inject constructor(
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
    settings: AppSettingsRepository,
    appStateManager: AppStateManager,
) : IGlobalMenuActionsViewModel by globalMenuActionsViewModel {
    val isPullWithRebaseDefault = settings.pullWithRebase
    val lastLoadedTabs = appStateManager.latestOpenedRepositoriesPaths

}