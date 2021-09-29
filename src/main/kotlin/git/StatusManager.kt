package git

import extensions.filePath
import extensions.hasUntrackedChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry

class StatusManager {
    private val _stageStatus = MutableStateFlow<StageStatus>(StageStatus.Loaded(listOf(), listOf()))

    val stageStatus: StateFlow<StageStatus>
        get() = _stageStatus

    private val _hasUncommitedChanges = MutableStateFlow<Boolean>(false)
    val hasUncommitedChanges: StateFlow<Boolean>
        get() = _hasUncommitedChanges

    suspend fun loadHasUncommitedChanges(git: Git) = withContext(Dispatchers.IO) {
        val status = git
            .status()
            .call()

        val hasUncommitedChanges = status.hasUncommittedChanges() || status.hasUntrackedChanges()

        _hasUncommitedChanges.value = hasUncommitedChanges
    }

    suspend fun loadStatus(git: Git) = withContext(Dispatchers.IO) {
        _stageStatus.value = StageStatus.Loading

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
    }

    suspend fun stage(git: Git, diffEntry: DiffEntry) = withContext(Dispatchers.IO) {
        git.add()
            .addFilepattern(diffEntry.filePath)
            .call()

        loadStatus(git)
    }

    suspend fun unstage(git: Git, diffEntry: DiffEntry) = withContext(Dispatchers.IO) {
        git.reset()
            .addPath(diffEntry.filePath)
            .call()

        loadStatus(git)
    }

    suspend fun commit(git: Git, message: String) = withContext(Dispatchers.IO) {
        git.commit()
            .setMessage(message)
            .call()

        loadStatus(git)
    }

    suspend fun reset(git: Git, diffEntry: DiffEntry, staged: Boolean) = withContext(Dispatchers.IO) {
        println("Staged $staged")

        if(staged) {
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
}

sealed class StageStatus {
    object Loading : StageStatus()
    data class Loaded(val staged: List<DiffEntry>, val unstaged: List<DiffEntry>) : StageStatus()
}