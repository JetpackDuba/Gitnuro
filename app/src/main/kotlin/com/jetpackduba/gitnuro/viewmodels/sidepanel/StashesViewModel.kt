package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.interfaces.IGetStashListGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.viewmodels.ISharedStashViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedStashViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

class StashesViewModel @AssistedInject constructor(
    private val tabScope: TabCoroutineScope,
    @Assisted private val filter: StateFlow<String>,
    sharedStashViewModel: SharedStashViewModel,
    private val repositoryDataRepository: RepositoryDataRepository,
) : SidePanelChildViewModel(true), ISharedStashViewModel by sharedStashViewModel {

    private val stashes = repositoryDataRepository.stashes

    val stashesState: StateFlow<StashesState> = combine(stashes, isExpanded, filter) { stashes, isExpanded, filter ->
        StashesState(
            stashes = stashes.filter { it.message.lowercaseContains(filter) },
            isExpanded,
        )
    }.stateIn(
        tabScope,
        SharingStarted.Eagerly,
        StashesState(emptyList(), isExpanded.value)
    )
}


data class StashesState(val stashes: List<Commit>, val isExpanded: Boolean)