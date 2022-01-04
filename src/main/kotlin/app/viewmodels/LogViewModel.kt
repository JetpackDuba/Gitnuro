package app.viewmodels

import app.git.*
import app.git.graph.GraphCommitList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class LogViewModel @Inject constructor(
    private val logManager: LogManager,
    private val statusManager: StatusManager,
    private val branchesManager: BranchesManager,
    private val tagsManager: TagsManager,
    private val tabState: TabState,
) {
    private val _logStatus = MutableStateFlow<LogStatus>(LogStatus.Loading)

    val logStatus: StateFlow<LogStatus>
        get() = _logStatus

    suspend fun loadLog(git: Git) {
        _logStatus.value = LogStatus.Loading

        val currentBranch = branchesManager.currentBranchRef(git)
        val log = logManager.loadLog(git, currentBranch)
        val hasUncommitedChanges = statusManager.hasUncommitedChanges(git)
        _logStatus.value = LogStatus.Loaded(hasUncommitedChanges, log, currentBranch)
    }

    fun checkoutCommit(revCommit: RevCommit) = tabState.safeProcessing { git ->
        logManager.checkoutCommit(git, revCommit)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun revertCommit(revCommit: RevCommit) = tabState.safeProcessing { git ->
        logManager.revertCommit(git, revCommit)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun resetToCommit(revCommit: RevCommit, resetType: ResetType) = tabState.safeProcessing { git ->
        logManager.resetToCommit(git, revCommit, resetType = resetType)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing { git ->
        branchesManager.checkoutRef(git, ref)

        return@safeProcessing RefreshType.ALL_DATA
    }


    fun createBranchOnCommit(branch: String, revCommit: RevCommit) = tabState.safeProcessing { git ->
        branchesManager.createBranchOnCommit(git, branch, revCommit)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun createTagOnCommit(tag: String, revCommit: RevCommit) = tabState.safeProcessing { git ->
        tagsManager.createTagOnCommit(git, tag, revCommit)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun mergeBranch(ref: Ref, fastForward: Boolean) = tabState.safeProcessing { git ->
        branchesManager.mergeBranch(git, ref, fastForward)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun deleteBranch(branch: Ref) =tabState.safeProcessing { git ->
        branchesManager.deleteBranch(git, branch)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun deleteTag(tag: Ref) = tabState.safeProcessing { git ->
        tagsManager.deleteTag(git, tag)

        return@safeProcessing RefreshType.ALL_DATA
    }

    suspend fun refresh(git: Git) {
        loadLog(git)
    }
}

sealed class LogStatus {
    object Loading : LogStatus()
    class Loaded(val hasUncommitedChanges: Boolean, val plotCommitList: GraphCommitList, val currentBranch: Ref?) : LogStatus()
}