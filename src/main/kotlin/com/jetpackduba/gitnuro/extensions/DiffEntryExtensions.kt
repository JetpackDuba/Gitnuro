package com.jetpackduba.gitnuro.extensions

import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import com.jetpackduba.gitnuro.theme.addFile
import com.jetpackduba.gitnuro.theme.conflictFile
import com.jetpackduba.gitnuro.theme.modifyFile
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

val StatusType.icon: ImageVector
    get() {
        return when (this) {
            StatusType.ADDED -> Icons.Default.Add
            StatusType.MODIFIED -> Icons.Default.Edit
            StatusType.REMOVED -> Icons.Default.Delete
            StatusType.CONFLICTING -> Icons.Default.Warning
        }
    }

val DiffEntry.icon: ImageVector
    get() {
        return when (this.changeType) {
            DiffEntry.ChangeType.ADD -> Icons.Default.Add
            DiffEntry.ChangeType.MODIFY -> Icons.Default.Edit
            DiffEntry.ChangeType.DELETE -> Icons.Default.Delete
            DiffEntry.ChangeType.COPY -> Icons.Default.Add
            DiffEntry.ChangeType.RENAME -> Icons.Default.Refresh
            else -> throw NotImplementedError("Unexpected ChangeType")
        }
    }

val StatusType.iconColor: Color
    @Composable
    get() {
        return when (this) {
            StatusType.ADDED -> MaterialTheme.colors.addFile
            StatusType.MODIFIED -> MaterialTheme.colors.modifyFile
            StatusType.REMOVED -> MaterialTheme.colors.error
            StatusType.CONFLICTING -> MaterialTheme.colors.conflictFile
        }
    }

val DiffEntry.iconColor: Color
    @Composable
    get() {
        return when (this.changeType) {
            DiffEntry.ChangeType.ADD -> MaterialTheme.colors.addFile
            DiffEntry.ChangeType.MODIFY -> MaterialTheme.colors.modifyFile
            DiffEntry.ChangeType.DELETE -> MaterialTheme.colors.error
            DiffEntry.ChangeType.COPY -> MaterialTheme.colors.addFile
            DiffEntry.ChangeType.RENAME -> MaterialTheme.colors.modifyFile
            else -> throw NotImplementedError("Unexpected ChangeType")
        }
    }

fun DiffEntry.toStatusType(): StatusType = when (this.changeType) {
    DiffEntry.ChangeType.ADD -> StatusType.ADDED
    DiffEntry.ChangeType.MODIFY -> StatusType.MODIFIED
    DiffEntry.ChangeType.DELETE -> StatusType.REMOVED
    DiffEntry.ChangeType.COPY -> StatusType.ADDED
    DiffEntry.ChangeType.RENAME -> StatusType.MODIFIED
    else -> throw NotImplementedError("Unexpected ChangeType")
}