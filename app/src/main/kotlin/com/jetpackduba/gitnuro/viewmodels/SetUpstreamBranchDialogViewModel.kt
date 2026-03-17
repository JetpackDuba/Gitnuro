package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.extensions.simpleName
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemoteBranchesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemotesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TrackingBranch
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.lib.Ref

class SetUpstreamBranchDialogViewModel @AssistedInject constructor(
    private val tabState: TabInstanceRepository,
    private val getRemoteBranchesGitAction: IGetRemoteBranchesGitAction,
    private val getRemotesGitAction: IGetRemotesGitAction,
    private val getTrackingBranchGitAction: IGetTrackingBranchGitAction,
    private val setTrackingBranchGitAction: ISetTrackingBranchGitAction,
    @Assisted private val branch: Branch,
) : TabViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(branch: Branch): SetUpstreamBranchDialogViewModel
    }

    private val _setDefaultUpstreamBranchState =
        MutableStateFlow<SetDefaultUpstreamBranchState>(SetDefaultUpstreamBranchState.Loading)
    val setDefaultUpstreamBranchState: StateFlow<SetDefaultUpstreamBranchState> =
        _setDefaultUpstreamBranchState

    init {
        loadData(branch)
    }

    // TODO Refactor this to a flow that is initialized later instead of being a side effect at object construction time
    private fun loadData(branch: Branch) = tabState.runOperation(
        refreshType = RefreshType.NONE
    ) { git ->
        _setDefaultUpstreamBranchState.value = SetDefaultUpstreamBranchState.Loading

        val trackingBranch = getTrackingBranchGitAction(git, branch)
        val remoteBranches = getRemoteBranchesGitAction(git)
        val remotes = getRemotesGitAction(git, remoteBranches)

        var remote: RemoteInfo? = null
        var remoteBranch: Branch? = null

        if (trackingBranch != null) {
            remote = remotes.firstOrNull { it.remoteConfig.name == trackingBranch.remote }
            remoteBranch = remote?.branchesList?.firstOrNull { it.simpleName == trackingBranch.branch }
        }

        _setDefaultUpstreamBranchState.value =
            SetDefaultUpstreamBranchState.Loaded(
                branch = branch,
                trackingBranch = trackingBranch,
                remotes = remotes,
                selectedRemote = remote,
                selectedBranch = remoteBranch
            )
    }

    fun changeDefaultUpstreamBranch() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val state = _setDefaultUpstreamBranchState.value

        if (state is SetDefaultUpstreamBranchState.Loaded) {
            setTrackingBranchGitAction(
                git = git,
                branch = state.branch,
                remoteName = state.selectedRemote?.remoteConfig?.name,
                remoteBranch = state.selectedBranch
            )

            _setDefaultUpstreamBranchState.value = SetDefaultUpstreamBranchState.UpstreamChanged
        }
    }

    fun setSelectedBranch(branchOption: Branch) {
        val state = _setDefaultUpstreamBranchState.value

        if (state is SetDefaultUpstreamBranchState.Loaded) {
            _setDefaultUpstreamBranchState.value = state.copy(selectedBranch = branchOption)
        }
    }

    fun setSelectedRemote(remote: RemoteInfo) {
        val state = setDefaultUpstreamBranchState.value
        val remoteConfig = remote.remoteConfig

        if (state is SetDefaultUpstreamBranchState.Loaded) {
            val branch = if (remoteConfig.name == state.trackingBranch?.remote) {
                remote.branchesList.firstOrNull { it.simpleName == state.trackingBranch.branch }
            } else {
                null
            }

            _setDefaultUpstreamBranchState.value = state.copy(
                selectedRemote = remote,
                selectedBranch = branch,
            )
        }
    }
}

sealed interface SetDefaultUpstreamBranchState {
    object Loading : SetDefaultUpstreamBranchState
    data class Loaded(
        val branch: Branch,
        val trackingBranch: TrackingBranch?,
        val remotes: List<RemoteInfo>,
        val selectedRemote: RemoteInfo?,
        val selectedBranch: Branch?,
    ) : SetDefaultUpstreamBranchState

    object UpstreamChanged : SetDefaultUpstreamBranchState
}