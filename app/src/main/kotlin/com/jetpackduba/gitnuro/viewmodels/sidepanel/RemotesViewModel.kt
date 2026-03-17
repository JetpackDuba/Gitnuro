package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.extensions.simpleName
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.AddRemoteUseCase
import com.jetpackduba.gitnuro.domain.usecases.UpdateRemoteUseCase
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import com.jetpackduba.gitnuro.viewmodels.ISharedBranchesViewModel
import com.jetpackduba.gitnuro.viewmodels.ISharedRemotesViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedBranchesViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedRemotesViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.jetbrains.skiko.ClipboardManager

class RemotesViewModel @AssistedInject constructor(
    private val tabState: TabInstanceRepository,
    private val getRemoteBranchesGitAction: IGetRemoteBranchesGitAction,
    private val getRemotesGitAction: IGetRemotesGitAction,
    private val getCurrentBranchGitAction: IGetCurrentBranchGitAction,
    private val deleteRemoteGitAction: IDeleteRemoteGitAction,
    private val fetchAllRemotesGitAction: IFetchAllRemotesGitAction,
    private val addRemoteGitAction: IAddRemoteGitAction,
    private val updateRemoteGitAction: IUpdateRemoteGitAction,
    private val deleteLocallyRemoteBranchesGitAction: IDeleteLocallyRemoteBranchesGitAction,
    private val sharedBranchesViewModel: SharedBranchesViewModel,
    private val clipboardManager: ClipboardManager,
    private val addRemoteUseCase: AddRemoteUseCase,
    private val updateRemoteUseCase: UpdateRemoteUseCase,
    tabScope: CoroutineScope,
    sharedRemotesViewModel: SharedRemotesViewModel,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(false), ISharedRemotesViewModel by sharedRemotesViewModel,
    ISharedBranchesViewModel by sharedBranchesViewModel {

    private val remotes = MutableStateFlow<List<RemoteView>>(listOf())
    private val currentBranch = MutableStateFlow<Branch?>(null)

    private val _remoteUpdated = MutableSharedFlow<Unit>()
    val remoteUpdated = _remoteUpdated.asSharedFlow()

    val remoteState: StateFlow<RemotesState> =
        combine(remotes, isExpanded, filter, currentBranch) { remotes, isExpanded, filter, currentBranch ->
            val remotesFiltered = remotes.map { remote ->
                val remoteInfo = remote.remoteInfo

                val newRemoteInfo = remoteInfo.copy(
                    branchesList = remoteInfo.branchesList.filter { branch ->
                        branch.simpleName.lowercaseContains(filter)
                    }
                )

                remote.copy(remoteInfo = newRemoteInfo)
            }

            RemotesState(
                remotesFiltered,
                isExpanded,
                currentBranch
            )
        }.stateIn(
            scope = tabScope,
            started = SharingStarted.Eagerly,
            initialValue = RemotesState(emptyList(), isExpanded.value, null)
        )

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.REMOTES) {
                refresh(tabState.git)
            }
        }
    }

    private suspend fun loadRemotes(git: Git) = withContext(Dispatchers.IO) {
        val allRemoteBranches = getRemoteBranchesGitAction(git)

        val remoteInfoList = getRemotesGitAction(git, allRemoteBranches)
        val currentBranch = getCurrentBranchGitAction(git)

        val remoteViewList = remoteInfoList.map { remoteInfo ->
            RemoteView(remoteInfo, true)
        }

        this@RemotesViewModel.remotes.value = remoteViewList
        this@RemotesViewModel.currentBranch.value = currentBranch
    }

    suspend fun refresh(git: Git) = withContext(Dispatchers.IO) {
        loadRemotes(git)
    }

    fun onRemoteClicked(remoteClicked: RemoteView) {
        val remoteName = remoteClicked.remoteInfo.remoteConfig.name
        val remotes = this.remotes.value
        val remoteInfo = remotes.firstOrNull { it.remoteInfo.remoteConfig.name == remoteName }

        if (remoteInfo != null) {
            val newRemoteInfo = remoteInfo.copy(isExpanded = !remoteClicked.isExpanded)
            val newRemotesList = remotes.toMutableList()
            val indexToReplace = newRemotesList.indexOf(remoteInfo)
            newRemotesList[indexToReplace] = newRemoteInfo

            this.remotes.value = newRemotesList
        }
    }

    fun selectBranch(ref: Branch) {
        tabState.newSelectedRef(ref, ref.hash)
    }

    fun deleteRemote(remoteName: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.DELETE_REMOTE,
    ) { git ->
        deleteRemoteGitAction(git, remoteName)

        val remoteBranches = getRemoteBranchesGitAction(git)
        val remoteToDeleteBranchesNames = remoteBranches.filter {
            it.name.startsWith("refs/remotes/$remoteName/")
        }.map {
            it.name
        }

        deleteLocallyRemoteBranchesGitAction(git, remoteToDeleteBranchesNames)

        positiveNotification("Remote $remoteName deleted")
    }

    fun onFetchRemoteBranches(remote: RemoteView) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.FETCH,
    ) { git ->
        val remoteConfig = remote.remoteInfo.remoteConfig
        fetchAllRemotesGitAction(git, remoteConfig)

        positiveNotification("Fetched branches from ${remoteConfig.name}")
    }


    fun addRemote(selectedRemoteConfig: Remote) = addRemoteUseCase(selectedRemoteConfig)

    fun updateRemote(selectedRemoteConfig: Remote) = addRemoteUseCase(selectedRemoteConfig)

    override fun copyBranchNameToClipboard(branch: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
        taskType = TaskType.UNSPECIFIED
    ) {
        copyBranchNameToClipboardAndGetNotification(
            branch,
            clipboardManager
        )
    }
}

data class RemoteView(val remoteInfo: RemoteInfo, val isExpanded: Boolean)

data class RemotesState(val remotes: List<RemoteView>, val isExpanded: Boolean, val currentBranch: Branch?)