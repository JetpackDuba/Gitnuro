package app.viewmodels

import app.git.*
import app.git.graph.GraphCommitList
import app.ui.SelectedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class LogViewModel @Inject constructor(
    private val logManager: LogManager,
    private val statusManager: StatusManager,
    private val branchesManager: BranchesManager,
    private val rebaseManager: RebaseManager,
    private val tagsManager: TagsManager,
    private val mergeManager: MergeManager,
    private val repositoryManager: RepositoryManager,
    private val tabState: TabState,
) {
    private val _logStatus = MutableStateFlow<LogStatus>(LogStatus.Loading)

    val logStatus: StateFlow<LogStatus>
        get() = _logStatus

    private suspend fun loadLog(git: Git) {
        _logStatus.value = LogStatus.Loading

        val currentBranch = branchesManager.currentBranchRef(git)
        val log = logManager.loadLog(git, currentBranch)
        val hasUncommitedChanges = statusManager.hasUncommitedChanges(git)

        val statsSummary = if (hasUncommitedChanges) {
            statusManager.getStatusSummary(
                git = git,
                currentBranch = currentBranch,
                repositoryState = repositoryManager.getRepositoryState(git),
            )
        } else
            StatusSummary(0, 0, 0)

        _logStatus.value = LogStatus.Loaded(hasUncommitedChanges, log, currentBranch, statsSummary)
    }

    fun checkoutCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        logManager.checkoutCommit(git, revCommit)
    }

    fun revertCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        logManager.revertCommit(git, revCommit)
    }

    fun resetToCommit(revCommit: RevCommit, resetType: ResetType) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        logManager.resetToCommit(git, revCommit, resetType = resetType)
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.checkoutRef(git, ref)
    }

    fun cherrypickCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ONLY_LOG,
    ) { git ->
        mergeManager.cherryPickCommit(git, revCommit)
    }

    fun createBranchOnCommit(branch: String, revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.createBranchOnCommit(git, branch, revCommit)
    }

    fun createTagOnCommit(tag: String, revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        tagsManager.createTagOnCommit(git, tag, revCommit)
    }

    fun mergeBranch(ref: Ref, fastForward: Boolean) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        mergeManager.mergeBranch(git, ref, fastForward)
    }

    fun deleteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.deleteBranch(git, branch)
    }

    fun deleteTag(tag: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        tagsManager.deleteTag(git, tag)
    }

    suspend fun refreshUncommitedChanges(git: Git) {
        uncommitedChangesLoadLog(git)
    }

    private suspend fun uncommitedChangesLoadLog(git: Git) {
        val currentBranch = branchesManager.currentBranchRef(git)
        val hasUncommitedChanges = statusManager.hasUncommitedChanges(git)

        val statsSummary = if (hasUncommitedChanges) {
            statusManager.getStatusSummary(
                git = git,
                currentBranch = currentBranch,
                repositoryState = repositoryManager.getRepositoryState(git),
            )
        } else
            StatusSummary(0, 0, 0)

        val previousLogStatusValue = _logStatus.value

        if (previousLogStatusValue is LogStatus.Loaded) {
            val newLogStatusValue = LogStatus.Loaded(
                hasUncommitedChanges = hasUncommitedChanges,
                plotCommitList = previousLogStatusValue.plotCommitList,
                currentBranch = currentBranch,
                statusSummary = statsSummary,
            )

            _logStatus.value = newLogStatusValue
        }
    }

    suspend fun refresh(git: Git) {
        loadLog(git)
    }

    fun rebaseBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        rebaseManager.rebaseBranch(git, ref)
    }

    fun selectLogLine(selectedItem: SelectedItem) {
        tabState.newSelectedItem(selectedItem)
    }
}

sealed class LogStatus {
    object Loading : LogStatus()
    class Loaded(
        val hasUncommitedChanges: Boolean,
        val plotCommitList: GraphCommitList,
        val currentBranch: Ref?,
        val statusSummary: StatusSummary,
    ) : LogStatus()
}