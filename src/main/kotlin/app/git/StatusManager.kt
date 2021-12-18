package app.git

import app.extensions.filePath
import app.extensions.hasUntrackedChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import javax.inject.Inject

class StatusManager @Inject constructor(
    private val branchesManager: BranchesManager,
) {
    private val _stageStatus = MutableStateFlow<StageStatus>(StageStatus.Loaded(listOf(), listOf()))
    val stageStatus: StateFlow<StageStatus> = _stageStatus

    private val _repositoryState = MutableStateFlow(RepositoryState.SAFE)
    val repositoryState: StateFlow<RepositoryState> = _repositoryState

    private val _hasUncommitedChanges = MutableStateFlow<Boolean>(false)
    val hasUncommitedChanges: StateFlow<Boolean>
        get() = _hasUncommitedChanges

    suspend fun loadHasUncommitedChanges(git: Git) = withContext(Dispatchers.IO) {
        _hasUncommitedChanges.value = checkHasUncommitedChanges(git)
    }

    suspend fun checkHasUncommitedChanges(git: Git) = withContext(Dispatchers.IO) {
        val status = git
            .status()
            .call()

        return@withContext status.hasUncommittedChanges() || status.hasUntrackedChanges()
    }

    suspend fun loadRepositoryStatus(git: Git) = withContext(Dispatchers.IO) {
        _repositoryState.value = git.repository.repositoryState
    }

    suspend fun loadStatus(git: Git) = withContext(Dispatchers.IO) {
        val previousStatus = _stageStatus.value
        _stageStatus.value = StageStatus.Loading

        try {
            loadRepositoryStatus(git)

            loadHasUncommitedChanges(git)
            val currentBranch = branchesManager.currentBranchRef(git)
            val repositoryState = git.repository.repositoryState
            val staged = git.diff().apply {
                if(currentBranch == null && repositoryState != RepositoryState.MERGING && !repositoryState.isRebasing )
                    setOldTree(EmptyTreeIterator()) // Required if the repository is empty

                setCached(true)
            }
                .call()
                // TODO: Grouping and fitlering allows us to remove duplicates when conflicts appear, requires more testing (what happens in windows? /dev/null is a unix thing)
                .groupBy { it.oldPath }
                .map {
                    val entries = it.value

                    if(entries.count() > 1)
                        entries.filter { it.oldPath != "/dev/null" }
                    else
                        entries
                }.flatten()

            ensureActive()

            val unstaged = git
                .diff()
                .call()
                .groupBy { it.oldPath }
                .map {
                    val entries = it.value

                    if(entries.count() > 1)
                        entries.filter { it.newPath != "/dev/null" }
                    else
                        entries
                }.flatten()

            ensureActive()
            _stageStatus.value = StageStatus.Loaded(staged, unstaged)
        } catch (ex: Exception) {
            _stageStatus.value = previousStatus
            throw ex
        }

    }

    suspend fun stage(git: Git, diffEntry: DiffEntry) = withContext(Dispatchers.IO) {
        if (diffEntry.changeType == DiffEntry.ChangeType.DELETE) {
            git.rm()
                .addFilepattern(diffEntry.filePath)
                .call()
        } else {
            git.add()
                .addFilepattern(diffEntry.filePath)
                .call()
        }

        loadStatus(git)
    }

//    suspend fun stageHunk(git: Git) {
////        val repository = git.repository
////        val objectInserter = repository.newObjectInserter()
//
////        objectInserter.insert(Constants.OBJ_BLOB,)
//    }

    suspend fun unstage(git: Git, diffEntry: DiffEntry) = withContext(Dispatchers.IO) {
        git.reset()
            .addPath(diffEntry.filePath)
            .call()

        loadStatus(git)
    }

    suspend fun commit(git: Git, message: String) = withContext(Dispatchers.IO) {
        git.commit()
            .setMessage(message)
            .setAllowEmpty(false)
            .call()

        loadStatus(git)
    }

    suspend fun reset(git: Git, diffEntry: DiffEntry, staged: Boolean) = withContext(Dispatchers.IO) {
        if (staged) {
            git
                .reset()
                .addPath(diffEntry.filePath)
                .call()
        }

        git
            .checkout()
            .addPath(diffEntry.filePath)
            .call()

        loadStatus(git)
    }

    suspend fun unstageAll(git: Git) = withContext(Dispatchers.IO) {
        git
            .reset()
            .call()

        loadStatus(git)
    }

    suspend fun stageAll(git: Git) = withContext(Dispatchers.IO) {
        git
            .add()
            .addFilepattern(".")
            .call()

        loadStatus(git)
    }
}

sealed class StageStatus {
    object Loading : StageStatus()
    data class Loaded(val staged: List<DiffEntry>, val unstaged: List<DiffEntry>) : StageStatus()
}