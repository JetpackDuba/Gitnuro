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
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.dircache.DirCacheEditor.PathEdit
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.Repository
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileWriter
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
        git.add()
            .addFilepattern(statusEntry.filePath)
            .setUpdate(statusEntry.statusType == StatusType.REMOVED)
            .call()
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
                        textLines.add(line.oldLineNumber + linesAdded, line.text)
                        linesAdded++
                    }
                    LineType.REMOVED -> {
                        textLines.removeAt(line.oldLineNumber + linesAdded)
                        linesAdded--
                    }
                    else -> throw NotImplementedError("Line type not implemented for stage hunk")
                }
            }

            val stagedFileText = textLines.joinToString("")
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
            val rawFileManager = rawFileManagerFactory.create(repository)
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
                textLines.add(line.newLineNumber + linesAdded - previouslyRemovedLines, line.text)
                linesAdded++
            }

            val stagedFileText = textLines.joinToString("")
            dirCacheEditor.add(HunkEdit(diffEntry.newPath, repository, ByteBuffer.wrap(stagedFileText.toByteArray())))
            dirCacheEditor.commit()

            completedWithErrors = false
        } finally {
            if (completedWithErrors)
                dirCache.unlock()
        }
    }

    private fun getTextLines(rawFile: RawText): List<String> {
        val content = rawFile.rawContent.toString(Charsets.UTF_8)//.removeSuffix(rawFile.lineDelimiter)
        val lineDelimiter: String? = rawFile.lineDelimiter

        return getTextLines(content, lineDelimiter)
    }

    private fun getTextLines(content: String, lineDelimiter: String?): List<String> {
        var splitted: List<String> = if (lineDelimiter != null) {
            content.split(lineDelimiter).toMutableList().apply {
                if (this.last() == "")
                    removeLast()
            }
        } else {
            listOf(content)
        }

        splitted = splitted.mapIndexed { index, line ->
            val lineWithBreak = line + lineDelimiter.orEmpty()

            if (index == splitted.count() - 1 && !content.endsWith(lineWithBreak)) {
                line
            } else
                lineWithBreak
        }

        return splitted
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
        if (staged || statusEntry.statusType == StatusType.CONFLICTING) {
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
            .setUpdate(true) // Modified and deleted files
            .call()
        git
            .add()
            .addFilepattern(".")
            .setUpdate(false) // For newly added files
            .call()
    }

    suspend fun getStatus(git: Git) =
        withContext(Dispatchers.IO) {
            git
                .status()
                .call()
        }

    suspend fun getStaged(status: Status) =
        withContext(Dispatchers.IO) {
            val added = status.added.map {
                StatusEntry(it, StatusType.ADDED)
            }
            val modified = status.changed.map {
                StatusEntry(it, StatusType.MODIFIED)
            }
            val removed = status.removed.map {
                StatusEntry(it, StatusType.REMOVED)
            }

            return@withContext flatListOf(
                added,
                modified,
                removed,
            )
        }

    suspend fun getUnstaged(status: Status) = withContext(Dispatchers.IO) {
        // TODO Test uninitialized modules after the refactor
//        val uninitializedSubmodules = submodulesManager.uninitializedSubmodules(git)

        val added = status.untracked.map {
            StatusEntry(it, StatusType.ADDED)
        }
        val modified = status.modified.map {
            StatusEntry(it, StatusType.MODIFIED)
        }
        val removed = status.missing.map {
            StatusEntry(it, StatusType.REMOVED)
        }
        val conflicting = status.conflicting.map {
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
        val status = getStatus(git)
        val staged = getStaged(status)
        val allChanges = staged.toMutableList()

        val unstaged = getUnstaged(status)

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
        val conflictingCount = changesGrouped[StatusType.CONFLICTING].countOrZero()

        return StatusSummary(
            modifiedCount = modifiedCount,
            deletedCount = deletedCount,
            addedCount = addedCount,
            conflictingCount = conflictingCount,
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

    suspend fun resetHunk(git: Git, diffEntry: DiffEntry, hunk: Hunk) = withContext(Dispatchers.IO) {
        val repository = git.repository

        try {
            val file = File(repository.directory.parent, diffEntry.oldPath)

            val content = file.readText()
            val textLines = getTextLines(content, content.lineDelimiter).toMutableList()
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
                textLines.add(line.newLineNumber + linesAdded - previouslyRemovedLines, line.text)
                linesAdded++
            }

            val stagedFileText = textLines.joinToString("")


            FileWriter(file).use { fw ->
                fw.write(stagedFileText)
            }
        } catch (ex: Exception) {
            throw Exception("Discard hunk failed. Check if the file still exists and has the write permissions set", ex)
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

data class StatusSummary(
    val modifiedCount: Int,
    val deletedCount: Int,
    val addedCount: Int,
    val conflictingCount: Int,
) {
    val total = modifiedCount +
            deletedCount +
            addedCount +
            conflictingCount
}