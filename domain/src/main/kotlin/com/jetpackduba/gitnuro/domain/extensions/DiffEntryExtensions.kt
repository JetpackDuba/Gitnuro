package com.jetpackduba.gitnuro.domain.extensions

import com.jetpackduba.gitnuro.domain.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.domain.git.workspace.StatusType
import org.eclipse.jgit.diff.DiffEntry

val DiffEntry.parentDirectoryPath: String
    get() {
        val path = if (this.changeType == DiffEntry.ChangeType.DELETE) {
            this.oldPath
        } else
            this.newPath

        val pathSplit = path.split("/").toMutableList()
        pathSplit.removeLast()

        val directoryPath = pathSplit.joinToString("/")

        return if (directoryPath.isEmpty())
            ""
        else
            "${directoryPath}/"
    }

val StatusEntry.parentDirectoryPath: String
    get() {
        val pathSplit = this.filePath.split("/").toMutableList()
        pathSplit.removeLast()

        val directoryPath = pathSplit.joinToString("/")

        return if (directoryPath.isEmpty())
            ""
        else
            "${directoryPath}/"
    }

val StatusEntry.fileName: String
    get() {
        val pathSplit = filePath.split("/")

        return pathSplit.lastOrNull() ?: ""
    }

val DiffEntry.fileName: String
    get() {
        val path = if (this.changeType == DiffEntry.ChangeType.DELETE) {
            this.oldPath
        } else
            this.newPath

        val pathSplit = path.split("/")

        return pathSplit.lastOrNull() ?: ""
    }

val DiffEntry.filePath: String
    get() {
        val path = if (this.changeType == DiffEntry.ChangeType.DELETE) {
            this.oldPath
        } else
            this.newPath

        return path
    }

fun DiffEntry.toStatusType(): StatusType = when (this.changeType) {
    DiffEntry.ChangeType.ADD -> StatusType.ADDED
    DiffEntry.ChangeType.MODIFY -> StatusType.MODIFIED
    DiffEntry.ChangeType.DELETE -> StatusType.REMOVED
    DiffEntry.ChangeType.COPY -> StatusType.ADDED
    DiffEntry.ChangeType.RENAME -> StatusType.MODIFIED
    else -> throw NotImplementedError("Unexpected ChangeType")
}