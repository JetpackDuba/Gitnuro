package app.git

import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.di.RawFileManagerFactory
import app.extensions.*
import app.git.diff.Hunk
import app.git.diff.LineType
import app.theme.conflictFile
import kotlinx.coroutines.Dispatchers
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
    private val rawFileManagerFactory: RawFileManagerFactory,
) {
    suspend fun hasUncommitedChanges(git: Git) = withContext(Dispatchers.IO) {
        val status = git
            .status()
            .call()

        return@withContext status.hasUncommittedChanges() || status.hasUntrackedChanges()
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
                // Check how many lines before this one have been deleted
                val previouslyRemovedLines = addedLines.count { it.newLineNumber <= line.newLineNumber } - 1
                textLines.add(line.newLineNumber + linesAdded - previouslyRemovedLines, line.text.withoutLineEnding)
                linesAdded++
            }

            val stagedFileText = textLines.joinToString(rawFile.lineDelimiter)
            dirCacheEditor.add(HunkEdit(diffEntry.newPath, repository, ByteBuffer.wrap(stagedFileText.toByteArray())))
            dirCacheEditor.commit()

            completedWithErrors = false

//            loadStatus(git)
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

    suspend fun unstage(git: Git, diffEntry: DiffEntry) = withContext(Dispatchers.IO) {
        git.reset()
            .addPath(diffEntry.filePath)
            .call()
    }

    suspend fun commit(git: Git, message: String) = withContext(Dispatchers.IO) {
        git.commit()
            .setMessage(message)
            .setAllowEmpty(false)
            .call()
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

//        loadStatus(git)
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

    suspend fun getStaged(git: Git, currentBranch: Ref?, repositoryState: RepositoryState) = withContext(Dispatchers.IO) {
        return@withContext git
            .diff()
            .setShowNameAndStatusOnly(true).apply {
                if (currentBranch == null && !repositoryState.isMerging && !repositoryState.isRebasing)
                    setOldTree(EmptyTreeIterator()) // Required if the repository is empty

                setCached(true)
            }
            .call()
            // TODO: Grouping and fitlering allows us to remove duplicates when conflicts appear, requires more testing (what happens in windows? /dev/null is a unix thing)
            // TODO: Test if we should group by old path or new path
            .groupBy {
                if(it.newPath != "/dev/null")
                    it.newPath
                else
                    it.oldPath
            }
            .map {
                val entries = it.value

                val hasConflicts =
                    (entries.count() > 1 && (repositoryState.isMerging || repositoryState.isRebasing))

                StatusEntry(entries.first(), isConflict = hasConflicts)
            }
    }

    suspend fun getUnstaged(git: Git, repositoryState: RepositoryState) = withContext(Dispatchers.IO) {
        return@withContext git
            .diff()
            .setShowNameAndStatusOnly(true)
            .call()
            .groupBy {
                if(it.oldPath != "/dev/null")
                    it.oldPath
                else
                    it.newPath
            }
            .map {
                val entries = it.value

                val hasConflicts =
                    (entries.count() > 1 && (repositoryState.isMerging || repositoryState.isRebasing))

                StatusEntry(entries.first(), isConflict = hasConflicts)
            }
    }
}


data class StatusEntry(val diffEntry: DiffEntry, val isConflict: Boolean) {
    val icon: ImageVector
        get() {
            return if (isConflict)
                Icons.Default.Warning
            else
                diffEntry.icon
        }
    val iconColor: Color
        @Composable
        get() {
            return if (isConflict)
                MaterialTheme.colors.conflictFile
            else
                diffEntry.iconColor
        }
}