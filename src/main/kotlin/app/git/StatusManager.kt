package app.git

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.di.RawFileManagerFactory
import app.extensions.*
import app.git.diff.Hunk
import app.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.dircache.DirCacheEditor.PathEdit
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.time.Instant
import javax.inject.Inject


class StatusManager @Inject constructor(
    private val rawFileManagerFactory: RawFileManagerFactory,
    private val submodulesManager: SubmodulesManager,
) {
    suspend fun hasUncommitedChanges(git: Git) = withContext(Dispatchers.IO) {
        val status = git
            .status()
            .call()

        return@withContext status.hasUncommittedChanges() || status.hasUntrackedChanges()
    }

    suspend fun stage(git: Git, statusEntry: StatusEntry) = withContext(Dispatchers.IO) {
        if (statusEntry.statusType == StatusType.REMOVED) {
            git.rm()
                .addFilepattern(statusEntry.filePath)
                .call()
        } else {
            git.add()
                .addFilepattern(statusEntry.filePath)
                .call()
        }
    }

    suspend fun stageHunk(git: Git, diffEntry: DiffEntry, hunk: Hunk) = withContext(Dispatchers.IO) {
        val repository = git.repository
        val dirCache = repository.lockDirCache()
        val dirCacheEditor = dirCache.editor()
        var completedWithErrors = true

        try {
            val rawFileManager = rawFileManagerFactory.create(git.repository)
            val entryContent = rawFileManager.getRawContent(DiffEntry.Side.OLD, diffEntry)

            if (entryContent !is EntryContent.Text)
                return@withContext

            val textLines = getTextLines(entryContent.rawText).toMutableList()

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

            val stagedFileText = textLines.joinToString(entryContent.rawText.lineDelimiter)
            dirCacheEditor.add(HunkEdit(diffEntry.newPath, repository, ByteBuffer.wrap(stagedFileText.toByteArray())))
            dirCacheEditor.commit()

            completedWithErrors = false
        } finally {
            if (completedWithErrors)
                dirCache.unlock()
        }
    }

    suspend fun unstageHunk(git: Git, diffEntry: DiffEntry, hunk: Hunk) = withContext(Dispatchers.IO) {
        val repository = git.repository
        val dirCache = repository.lockDirCache()
        val dirCacheEditor = dirCache.editor()
        var completedWithErrors = true

        try {
            val rawFileManager = rawFileManagerFactory.create(git.repository)
            val entryContent = rawFileManager.getRawContent(DiffEntry.Side.NEW, diffEntry)

            if (entryContent !is EntryContent.Text)
                return@withContext

            val textLines = getTextLines(entryContent.rawText).toMutableList()

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
                // Check how many lines before this one have been deleted
                val previouslyRemovedLines = addedLines.count { it.newLineNumber < line.newLineNumber }
                textLines.add(line.newLineNumber + linesAdded - previouslyRemovedLines, line.text.withoutLineEnding)
                linesAdded++
            }

            val stagedFileText = textLines.joinToString(entryContent.rawText.lineDelimiter)
            dirCacheEditor.add(HunkEdit(diffEntry.newPath, repository, ByteBuffer.wrap(stagedFileText.toByteArray())))
            dirCacheEditor.commit()

            completedWithErrors = false
        } finally {
            if (completedWithErrors)
                dirCache.unlock()
        }
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

    suspend fun unstage(git: Git, statusEntry: StatusEntry) = withContext(Dispatchers.IO) {
        git.reset()
            .addPath(statusEntry.filePath)
            .call()
    }

    suspend fun commit(git: Git, message: String, amend: Boolean) = withContext(Dispatchers.IO) {
        git.commit()
            .setMessage(message)
            .setAllowEmpty(false)
            .setAmend(amend)
            .call()
    }

    suspend fun reset(git: Git, statusEntry: StatusEntry, staged: Boolean) = withContext(Dispatchers.IO) {
        if (staged) {
            git
                .reset()
                .addPath(statusEntry.filePath)
                .call()
        }

        git
            .checkout()
            .addPath(statusEntry.filePath)
            .call()
    }

    suspend fun unstageAll(git: Git) = withContext(Dispatchers.IO) {
        git
            .reset()
            .call()
    }

    suspend fun stageAll(git: Git) = withContext(Dispatchers.IO) {
        git
            .add()
            .addFilepattern(".")
            .call()
    }

    suspend fun getStaged(git: Git) =
        withContext(Dispatchers.IO) {

            // TODO Test on an empty repository or with a non-default state like merging or rebasing
            val statusResult = git
                .status()
                .call()

            val added = statusResult.added.map {
                StatusEntry(it, StatusType.ADDED)
            }
            val modified = statusResult.changed.map {
                StatusEntry(it, StatusType.MODIFIED)
            }
            val removed = statusResult.removed.map {
                StatusEntry(it, StatusType.REMOVED)
            }

            return@withContext flatListOf(
                added,
                modified,
                removed,
            )
        }

    suspend fun getUnstaged(git: Git) = withContext(Dispatchers.IO) {
        // TODO Test uninitialized modules after the refactor
//        val uninitializedSubmodules = submodulesManager.uninitializedSubmodules(git)

        val statusResult = git
            .status()
            .call()

        val added = statusResult.untracked.map {
            StatusEntry(it, StatusType.ADDED)
        }
        val modified = statusResult.modified.map {
            StatusEntry(it, StatusType.MODIFIED)
        }
        val removed = statusResult.missing.map {
            StatusEntry(it, StatusType.REMOVED)
        }
        val conflicting = statusResult.conflicting.map {
            StatusEntry(it, StatusType.CONFLICTING)
        }

        return@withContext flatListOf(
            added,
            modified,
            removed,
            conflicting,
        )
    }

    suspend fun getStatusSummary(git: Git): StatusSummary {
        val staged = getStaged(git)
        val allChanges = staged.toMutableList()

        val unstaged = getUnstaged(git)

        allChanges.addAll(unstaged)
        val groupedChanges = allChanges.groupBy {

        }
        val changesGrouped = groupedChanges.map {
            it.value
        }.flatten()
            .groupBy {
                it.statusType
            }

        val deletedCount = changesGrouped[StatusType.REMOVED].countOrZero()
        val addedCount = changesGrouped[StatusType.ADDED].countOrZero()

        val modifiedCount = changesGrouped[StatusType.MODIFIED].countOrZero()

        return StatusSummary(
            modifiedCount = modifiedCount,
            deletedCount = deletedCount,
            addedCount = addedCount,
        )
    }

    suspend fun stageUntrackedFiles(git: Git) = withContext(Dispatchers.IO) {
        val diffEntries = git
            .diff()
            .setShowNameAndStatusOnly(true)
            .call()

        val addedEntries = diffEntries.filter { it.changeType == DiffEntry.ChangeType.ADD }

        if (addedEntries.isNotEmpty()) {
            val addCommand = git
                .add()

            for (entry in addedEntries) {
                addCommand.addFilepattern(entry.newPath)
            }

            addCommand.call()
        }
    }
}


data class StatusEntry(val filePath: String, val statusType: StatusType) {
    val icon: ImageVector
        get() = statusType.icon

    val iconColor: Color
        @Composable
        get() = statusType.iconColor
}

enum class StatusType {
    ADDED,
    MODIFIED,
    REMOVED,
    CONFLICTING,
}

data class StatusSummary(val modifiedCount: Int, val deletedCount: Int, val addedCount: Int)