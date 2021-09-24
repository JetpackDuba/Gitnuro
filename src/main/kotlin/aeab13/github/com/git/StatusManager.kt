package aeab13.github.com.git

import aeab13.github.com.extensions.filePath
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

    fun hasUncommitedChanges(git: Git): Boolean {
        return git
            .status()
            .call()
            .hasUncommittedChanges()
    }

    suspend fun loadStatus(git: Git) = withContext(Dispatchers.IO) {
        _stageStatus.value = StageStatus.Loading

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
}

sealed class StageStatus {
    object Loading : StageStatus()
    data class Loaded(val staged: List<DiffEntry>, val unstaged: List<DiffEntry>) : StageStatus()
}