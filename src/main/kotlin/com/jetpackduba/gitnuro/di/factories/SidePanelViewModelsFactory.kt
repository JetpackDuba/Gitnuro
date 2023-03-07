package com.jetpackduba.gitnuro.di.factories

import com.jetpackduba.gitnuro.viewmodels.sidepanel.*
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

@AssistedFactory
interface SubmodulesViewModelFactory {
    fun create(filter: StateFlow<String>): SubmodulesViewModel
}