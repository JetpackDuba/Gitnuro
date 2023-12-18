package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.extensions.flatListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Status
import javax.inject.Inject

class GetStagedUseCase @Inject constructor() {
    suspend operator fun invoke(status: Status) =
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
            ).sortedBy { it.filePath }
        }
}