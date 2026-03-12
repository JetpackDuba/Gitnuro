package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.extensions.countOrZero
import com.jetpackduba.gitnuro.domain.interfaces.IGetStatusSummaryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusSummary
import com.jetpackduba.gitnuro.domain.models.StatusType
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetStatusSummaryGitAction @Inject constructor(
    private val getStatusGitAction: GetStatusGitAction,
) : IGetStatusSummaryGitAction {
    override suspend operator fun invoke(git: Git): StatusSummary {
        val status = getStatusGitAction(git.repository.directory.absolutePath)
        val staged = status.staged

        val unstaged = status.unstaged
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