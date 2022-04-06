package app.extensions

import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.git.StatusType
import app.theme.addFile
import app.theme.conflictFile
import app.theme.modifyFile
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