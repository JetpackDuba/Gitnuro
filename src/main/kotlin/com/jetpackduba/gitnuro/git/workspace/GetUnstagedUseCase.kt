package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.extensions.flatListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Status
import javax.inject.Inject

class GetUnstagedUseCase @Inject constructor() {
    suspend operator fun invoke(status: Status) = withContext(Dispatchers.IO) {
        val untracked = status.untracked.map {
            StatusEntry(it, StatusType.ADDED)
        }
        val modified = status.modified.map {
            StatusEntry(it, StatusType.MODIFIED)
        }
        val missing = status.missing.map {
            StatusEntry(it, StatusType.REMOVED)
        }
        val conflicting = status.conflicting.map {
            StatusEntry(it, StatusType.CONFLICTING)
        }

        return@withContext flatListOf(
            untracked,
            modified,
            missing,
            conflicting,
        ).sortedBy { it.filePath }
    }
}