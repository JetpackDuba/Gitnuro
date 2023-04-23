package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.GetRemoteBranchesUseCase
import com.jetpackduba.gitnuro.git.branches.GetTrackingBranchUseCase
import com.jetpackduba.gitnuro.git.branches.SetTrackingBranchUseCase
import com.jetpackduba.gitnuro.git.branches.TrackingBranch
import com.jetpackduba.gitnuro.git.remotes.GetRemotesUseCase
import com.jetpackduba.gitnuro.git.remotes.RemoteInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class ChangeDefaultUpstreamBranchViewModel @Inject constructor(
    private val tabState: TabState,
    private val getRemoteBranchesUseCase: GetRemoteBranchesUseCase,
    private val getRemotesUseCase: GetRemotesUseCase,
    private val getTrackingBranchUseCase: GetTrackingBranchUseCase,
    private val setTrackingBranchUseCase: SetTrackingBranchUseCase,
) {
    private val _setDefaultUpstreamBranchState =
        MutableStateFlow<SetDefaultUpstreamBranchState>(SetDefaultUpstreamBranchState.Loading)
    val setDefaultUpstreamBranchState: StateFlow<SetDefaultUpstreamBranchState> =
        _setDefaultUpstreamBranchState

    fun init(branch: Ref) = tabState.runOperation(
        refreshType = RefreshType.NONE
    ) { git ->
        _setDefaultUpstreamBranchState.value = SetDefaultUpstreamBranchState.Loading

        val trackingBranch = getTrackingBranchUseCase(git, branch)
        val remoteBranches = getRemoteBranchesUseCase(git)
        val remotes = getRemotesUseCase(git, remoteBranches)

        var remote: RemoteInfo? = null
        var remoteBranch: Ref? = null

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
            setTrackingBranchUseCase(
                git = git,
                ref = state.branch,
                remoteName = state.selectedRemote?.remoteConfig?.name,
                remoteBranch = state.selectedBranch
            )

            _setDefaultUpstreamBranchState.value = SetDefaultUpstreamBranchState.UpstreamChanged
        }
    }

    fun setSelectedBranch(branchOption: Ref) {
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
                remote.branchesList.firstOrNull { it.simpleName == state.trackingBranch?.branch }
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
        val branch: Ref,
        val trackingBranch: TrackingBranch?,
        val remotes: List<RemoteInfo>,
        val selectedRemote: RemoteInfo?,
        val selectedBranch: Ref?,
    ) : SetDefaultUpstreamBranchState

    object UpstreamChanged : SetDefaultUpstreamBranchState
}