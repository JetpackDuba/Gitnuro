package app.git

import app.di.RawFileManagerFactory
import app.extensions.filePath
import app.extensions.hasUntrackedChanges
import app.extensions.isMerging
import app.extensions.withoutLineEnding
import app.git.diff.Hunk
import app.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.dircache.DirCacheEditor.PathEdit
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.time.Instant
import javax.inject.Inject


class StatusManager @Inject constructor(
    private val branchesManager: BranchesManager,
    private val rawFileManagerFactory: RawFileManagerFactory,
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
                if (currentBranch == null && !repositoryState.isMerging && !repositoryState.isRebasing)
                    setOldTree(EmptyTreeIterator()) // Required if the repository is empty

                setCached(true)
            }
                .call()
                // TODO: Grouping and fitlering allows us to remove duplicates when conflicts appear, requires more testing (what happens in windows? /dev/null is a unix thing)
                .groupBy { it.oldPath }
                .map {
                    val entries = it.value

                    if (entries.count() > 1 && (repositoryState.isMerging || repositoryState.isRebasing))
                        entries.filter { entry -> entry.oldPath != "/dev/null" }
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

                    if (entries.count() > 1 && (repositoryState.isMerging || repositoryState.isRebasing))
                        entries.filter { entry -> entry.newPath != "/dev/null" }
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

    suspend fun stageHunk(git: Git, diffEntry: DiffEntry, hunk: Hunk) = withContext(Dispatchers.IO) {
        val repository = git.repository
        val dirCache = repository.lockDirCache()
        val dirCacheEditor = dirCache.editor()
        var completedWithErrors = true

        try {
            val rawFileManager = rawFileManagerFactory.create(git.repository)
            val rawFile = rawFileManager.getRawContent(DiffEntry.Side.OLD, diffEntry)
            val textLines = getTextLines(rawFile).toMutableList()

            val hunkLines = hunk.lines.filter { it.lineType != LineType.CONTEXT }

            var linesAdded = 0
            for (line in hunkLines) {
                when (line.lineType) {
                    LineType.ADDED -> {
                        textLines.add(line.oldLineNumber + linesAdded, line.text.withoutLineEnding)
                        linesAdded++
                    }
                    LineType.REMOVED -> {
                        textLines.removeAt(line.oldLineNumber + linesAdded)
                        linesAdded--
                    }
                    else -> throw NotImplementedError("Line type not implemented for stage hunk")
                }
            }

            val stagedFileText = textLines.joinToString(rawFile.lineDelimiter)
            dirCacheEditor.add(HunkEdit(diffEntry.newPath, repository, ByteBuffer.wrap(stagedFileText.toByteArray())))
            dirCacheEditor.commit()

            completedWithErrors = false

            loadStatus(git)
        } finally {
            if (completedWithErrors)
                dirCache.unlock()
        }
    }

    suspend fun unstageHunk(git: Git, diffEntry: DiffEntry, hunk: Hunk) = withContext(Dispatchers.IO) {
        val repository = git.repository
        val dirCache = repository.lockDirCache()
        val dirCacheEditor = dirCache.editor()

        val rawFileManager = rawFileManagerFactory.create(git.repository)
        val rawFile = rawFileManager.getRawContent(DiffEntry.Side.NEW, diffEntry)
        val textLines = getTextLines(rawFile).toMutableList()

        val hunkLines = hunk.lines.filter { it.lineType != LineType.CONTEXT }

        val addedLines = hunkLines
            .filter { it.lineType == LineType.ADDED }
            .sortedBy { it.newLineNumber }
        val removedLines = hunkLines
            .filter { it.lineType == LineType.REMOVED }
            .sortedBy { it.newLineNumber }

        var linesRemoved = 0

        // Start by removing the added lines to the index
        for (line in addedLines) {
            textLines.removeAt(line.newLineNumber + linesRemoved)
            linesRemoved--
        }

        var linesAdded = 0

        // Restore previously removed lines to the index
        for (line in removedLines) {
            textLines.add(line.newLineNumber + linesAdded, line.text.withoutLineEnding)
            linesAdded++
        }

        val stagedFileText = textLines.joinToString(rawFile.lineDelimiter)
        dirCacheEditor.add(HunkEdit(diffEntry.newPath, repository, ByteBuffer.wrap(stagedFileText.toByteArray())))
        dirCacheEditor.commit()

        loadStatus(git)
    }

    private fun getTextLines(rawFile: RawText): List<String> {
        val content = rawFile.rawContent.toString(Charsets.UTF_8)
        return content.split(rawFile.lineDelimiter).toMutableList()
    }

    private class HunkEdit(
        path: String?,
        private val repo: Repository,
        private val content: ByteBuffer,
    ) : PathEdit(path) {
        override fun apply(ent: DirCacheEntry) {
            val inserter: ObjectInserter = repo.newObjectInserter()
            if (ent.rawMode and FileMode.TYPE_MASK != FileMode.TYPE_FILE) {
                ent.fileMode = FileMode.REGULAR_FILE
            }
            ent.length = content.limit()
            ent.setLastModified(Instant.now())
            try {
                val `in` = ByteArrayInputStream(
                    content.array(), 0, content.limit()
                )
                ent.setObjectId(
                    inserter.insert(
                        Constants.OBJ_BLOB, content.limit().toLong(),
                        `in`
                    )
                )
                inserter.flush()
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
        }
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