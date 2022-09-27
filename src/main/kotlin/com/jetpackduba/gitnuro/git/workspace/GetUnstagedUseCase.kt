package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.extensions.flatListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Status
import javax.inject.Inject

class GetUnstagedUseCase @Inject constructor() {
    suspend operator fun invoke(status: Status) = withContext(Dispatchers.IO) {
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
}