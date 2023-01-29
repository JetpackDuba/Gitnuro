package com.jetpackduba.gitnuro.di.factories

import com.jetpackduba.gitnuro.viewmodels.sidepanel.BranchesViewModel
import com.jetpackduba.gitnuro.viewmodels.sidepanel.RemotesViewModel
import com.jetpackduba.gitnuro.viewmodels.sidepanel.StashesViewModel
import com.jetpackduba.gitnuro.viewmodels.sidepanel.TagsViewModel
import dagger.assisted.AssistedFactory
import kotlinx.coroutines.flow.StateFlow

@AssistedFactory
interface BranchesViewModelFactory {
    fun create(filter: StateFlow<String>): BranchesViewModel
}

@AssistedFactory
interface RemotesViewModelFactory {
    fun create(filter: StateFlow<String>): RemotesViewModel
}

@AssistedFactory
interface TagsViewModelFactory {
    fun create(filter: StateFlow<String>): TagsViewModel
}

@AssistedFactory
interface StashesViewModelFactory {
    fun create(filter: StateFlow<String>): StashesViewModel
}