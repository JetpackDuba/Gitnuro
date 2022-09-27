package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.extensions.countOrZero
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetStatusSummaryUseCase @Inject constructor(
    private val getStagedUseCase: GetStagedUseCase,
    private val getStatusUseCase: GetStatusUseCase,
    private val getUnstagedUseCase: GetUnstagedUseCase,
) {
    suspend operator fun invoke(git: Git): StatusSummary {
        val status = getStatusUseCase(git)
        val staged = getStagedUseCase(status)

        val unstaged = getUnstagedUseCase(status)
        val allChanges = staged + unstaged

        val groupedChanges = allChanges.groupBy {
            it.statusType
        }

        val deletedCount = groupedChanges[StatusType.REMOVED].countOrZero()
        val addedCount = groupedChanges[StatusType.ADDED].countOrZero()

        val modifiedCount = groupedChanges[StatusType.MODIFIED].countOrZero()
        val conflictingCount = groupedChanges[StatusType.CONFLICTING].countOrZero()

        return StatusSummary(
            modifiedCount = modifiedCount,
            deletedCount = deletedCount,
            addedCount = addedCount,
            conflictingCount = conflictingCount,
        )
    }
}