package app.viewmodels

import app.git.StashManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class StashesViewModel @Inject constructor(
    private val stashManager: StashManager,
) {
    private val _stashStatus = MutableStateFlow<StashStatus>(StashStatus.Loaded(listOf()))
    val stashStatus: StateFlow<StashStatus>
        get() = _stashStatus

    suspend fun loadStashes(git: Git) {
        _stashStatus.value = StashStatus.Loading
        val stashList = stashManager.getStashList(git)
        _stashStatus.value = StashStatus.Loaded(stashList.toList()) // TODO: Is the list cast necessary?
    }

    suspend fun refresh(git: Git) {
        loadStashes(git)
    }
}


sealed class StashStatus {
    object Loading : StashStatus()
    data class Loaded(val stashes: List<RevCommit>) : StashStatus()
}