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
import org.eclipse.jgit.lib.Constants
import javax.inject.Inject

class StatusManager @Inject constructor() {
    private val _stageStatus = MutableStateFlow<StageStatus>(StageStatus.Loaded(listOf(), listOf()))

    val stageStatus: StateFlow<StageStatus>
        get() = _stageStatus

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

    suspend fun loadStatus(git: Git) = withContext(Dispatchers.IO) {
        val previousStatus = _stageStatus.value
        _stageStatus.value = StageStatus.Loading

        try {
            loadHasUncommitedChanges(git)

            val staged = git
                .diff()
                .setCached(true)
                .call()

            ensureActive()

            val unstaged = git
                .diff()
                .call()

            ensureActive()
            _stageStatus.value = StageStatus.Loaded(staged, unstaged)
        } catch(ex: Exception) {
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