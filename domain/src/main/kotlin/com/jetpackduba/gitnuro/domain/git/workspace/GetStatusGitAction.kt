package com.jetpackduba.gitnuro.domain.git.workspace

import com.jetpackduba.gitnuro.domain.extensions.flatListOf
import com.jetpackduba.gitnuro.domain.git.EntryType
import com.jetpackduba.gitnuro.domain.git.jgit
import com.jetpackduba.gitnuro.domain.models.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import org.eclipse.jgit.api.Status as JGitStatus

class GetStatusGitAction @Inject constructor() {
    suspend operator fun invoke(repository: String): Status {
        val status = withContext(Dispatchers.IO) {
            jgit(repository) {
                status()
                    .call()
            }
        }

        val staged = getStaged(status)
        val unstaged = getUnstaged(status)

        return Status(staged, unstaged)
    }

    private fun getUnstaged(status: JGitStatus): List<StatusEntry> {
        val untracked = status.untracked.map {
            StatusEntry(it, StatusType.ADDED, entryType = EntryType.UNSTAGED)
        }
        val modified = status.modified.map {
            StatusEntry(it, StatusType.MODIFIED, entryType = EntryType.UNSTAGED)
        }
        val missing = status.missing.map {
            StatusEntry(it, StatusType.REMOVED, entryType = EntryType.UNSTAGED)
        }
        val conflicting = status.conflicting.map {
            StatusEntry(it, StatusType.CONFLICTING, entryType = EntryType.UNSTAGED)
        }

        return flatListOf(
            untracked,
            modified,
            missing,
            conflicting,
        ).sortedBy { it.filePath }
    }

    private fun getStaged(status: JGitStatus): List<StatusEntry> {
        val added = status.added.toStatusEntries(StatusType.ADDED, EntryType.STAGED)
        val modified = status.changed.toStatusEntries(StatusType.MODIFIED, EntryType.STAGED)
        val removed = status.removed.toStatusEntries(StatusType.REMOVED, EntryType.STAGED)

        return flatListOf(
            added,
            modified,
            removed,
        ).sortedBy { it.filePath }
    }

    private fun Set<String>.toStatusEntries(statusType: StatusType, entryType: EntryType): List<StatusEntry> {
        return this
            .map {
                StatusEntry(it, statusType, entryType)
            }
    }
}