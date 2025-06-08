package com.jetpackduba.gitnuro.git.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.jetpackduba.gitnuro.extensions.icon
import com.jetpackduba.gitnuro.extensions.iconColor
import kotlin.io.path.Path
import kotlin.io.path.pathString

data class StatusEntry(
    val filePath: String,
    val statusType: StatusType
) {
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