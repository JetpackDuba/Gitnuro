package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.di.modules.FileWatcherModule
import com.jetpackduba.gitnuro.di.modules.TabModule
import com.jetpackduba.gitnuro.di.modules.TabRepositoriesModule
import com.jetpackduba.gitnuro.repositoryopen.RepositoryOpenViewModel
import com.jetpackduba.gitnuro.ui.dialogs.*
import com.jetpackduba.gitnuro.viewmodels.*
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SubmoduleDialogViewModel
import dagger.Subcomponent

@TabScope
@Subcomponent(
    modules = [
        TabModule::class,
        TabRepositoriesModule::class,
        FileWatcherModule::class,
    ],
)
interface TabComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(): TabComponent
    }

    fun cloneViewModel(): CloneViewModel
    fun settingsViewModel(): SettingsViewModel
    fun repositoryTabViewModelFactory(): RepositoryTabViewModel.Factory
    fun repositoryOpenViewModel(): RepositoryOpenViewModel
    fun historyViewModel(): HistoryViewModel
    fun authorViewModel(): AuthorViewModel
    fun stashWithMessageViewModel(): StashWithMessageViewModel
    fun quickActionsViewModel(): QuickActionsViewModel
    fun setUpstreamBranchDialogViewModelFactory(): SetUpstreamBranchDialogViewModel.Factory
    fun renameBranchDialogViewModelFactory(): RenameBranchDialogViewModel.Factory
    fun createBranchViewModelFactory(): CreateBranchViewModel.Factory
    fun createTagViewModelFactory(): CreateTagViewModel.Factory
    fun resetBranchViewModelFactory(): ResetBranchViewModel.Factory
    fun addEditRemoteViewModelFactory(): AddEditRemoteViewModel.Factory
    fun submoduleDialogViewModel(): SubmoduleDialogViewModel
    fun signOffDialogViewModel(): SignOffDialogViewModel
}